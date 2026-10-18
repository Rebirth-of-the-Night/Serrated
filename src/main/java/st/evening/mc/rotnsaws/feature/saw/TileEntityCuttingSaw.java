package st.evening.mc.rotnsaws.feature.saw;

import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.minecraft.block.Block;
import net.minecraft.block.BlockLiquid;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.inventory.InventoryHelper;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.NetworkManager;
import net.minecraft.network.play.server.SPacketUpdateTileEntity;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumParticleTypes;
import net.minecraft.util.ITickable;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.Constants;
import net.minecraftforge.fluids.IFluidBlock;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.items.ItemHandlerHelper;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.SerrConfig;
import st.evening.mc.rotnsaws.Serrated;
import st.evening.mc.rotnsaws.compat.bwm.BwmMechanicalHandler;
import st.evening.mc.rotnsaws.compat.bwm.SerrBwmCompat;
import st.evening.mc.rotnsaws.network.SPacketSyncSawPower;
import st.evening.mc.rotnsaws.network.SPacketSyncSawRecipe;
import st.evening.mc.rotnsaws.util.MachineSound;

import java.util.function.Predicate;

// loosely based on BWM's TileSaw
public abstract class TileEntityCuttingSaw extends TileEntity implements ITickable, BwmMechanicalHandler.Host {

    private static final String TAG_NO_SAW = "rotn$noSaw";

    private final CuttingSawType type;

    private final BwmMechanicalHandler bwmMech = SerrBwmCompat.newMechanicalHandler(this);

    private final Object2IntMap<String> immersiveRecipeConfig = new Object2IntOpenHashMap<>();
    private ItemStack immersiveFocus = ItemStack.EMPTY;
    @Nullable
    private CuttingSawRecipe immersiveRecipe = null;
    @Nullable
    private CraftingJob craftingJob = null;

    private boolean powerSyncDirty = true;
    private boolean jobSyncDirty = true;
    private int uiStateClock = 0;

    @Nullable
    private final MachineSound activeSound, workingSound;

    protected TileEntityCuttingSaw(final CuttingSawType type) {
        this.type = type;
        this.immersiveRecipeConfig.defaultReturnValue(0); // should already be 0, but just to be sure
        this.activeSound = SerrConfig.activeSound
                ? Serrated.PROXY.newMachineSound(this, type.getActiveSound(), 0.75F) : null;
        this.workingSound = SerrConfig.workingSound
                ? Serrated.PROXY.newMachineSound(this, type.getWorkingSound(), 0.25F) : null;
    }

    public CuttingSawType getType() {
        return type;
    }

    @Override
    public World getBlockWorld() {
        return world;
    }

    @Override
    public BlockPos getBlockPos() {
        return pos;
    }

    @Override
    public Block getBlock() {
        return getBlockType();
    }

    @Override
    public boolean hasCapability(final Capability<?> capability, @Nullable final EnumFacing facing) {
        return capability == SerrBwmCompat.CAP_MECHANICAL_POWER || super.hasCapability(capability, facing);
    }

    @SuppressWarnings("unchecked")
    @Override
    public @Nullable <T> T getCapability(final Capability<T> capability, @Nullable final EnumFacing facing) {
        if (capability == SerrBwmCompat.CAP_MECHANICAL_POWER) {
            return (T) bwmMech; // cast fails for the nop implementation, but that shouldn't be able to happen
        }
        return super.getCapability(capability, facing);
    }

    public ItemStack getImmersiveFocus() {
        return immersiveFocus; // please do not mutate :(
    }

    @Nullable
    public CuttingSawRecipe getImmersiveRecipe() {
        return immersiveRecipe;
    }

    public int getImmersiveOutput(final String recipeId) {
        return immersiveRecipeConfig.getInt(recipeId);
    }

    public ItemStack getImmersiveProcessingStack() {
        return craftingJob instanceof ItemJob ? ((ItemJob) craftingJob).inputStack : ItemStack.EMPTY;
    }

    public int getUiStateClock() {
        return uiStateClock;
    }

    public boolean isActive() {
        return bwmMech.getPower() >= SerrConfig.powerRequired;
    }

    public void setImmersiveFocusFromGui(final ItemStack stack) {
        immersiveFocus = stack.isEmpty() ? ItemStack.EMPTY : ItemHandlerHelper.copyStackWithSize(stack, 1);
        if (world.isRemote) return;
        markDirty();
        updateRecipe();
    }

    public void setImmersiveOutputIndexFromGui(final int outputIndex) {
        if (immersiveRecipe == null || outputIndex < 0 || outputIndex >= immersiveRecipe.getOutputs().size()) return;
        if (immersiveRecipeConfig.put(immersiveRecipe.getId(), outputIndex) != outputIndex) {
            markDirty();
            uiStateClock++;
            tryStartBlockCrafting();
        }
    }

    public void setPowerFromSync(final int power) {
        if (bwmMech.setPowerFromSync(power)) {
            world.markBlockRangeForRenderUpdate(pos, pos);
        }
    }

    public void setRecipeStateFromSync(@Nullable final NBTTagCompound tag) {
        craftingJob = tag != null ? CraftingJob.readFromNbt(type, tag) : null;
    }

    public void setUiStateFromSync(final int stateClock, final String recipeId, final int outputIndex) {
        this.uiStateClock = stateClock;
        if (immersiveRecipe == null || !immersiveRecipe.getId().equals(recipeId)) {
            final CuttingSawRecipe recipe = type.getRecipeById(recipeId);
            if (recipe == null) {
                immersiveRecipe = null;
                return;
            }
            immersiveRecipe = recipe;
        }
        immersiveRecipeConfig.put(immersiveRecipe.getId(),
                outputIndex >= 0 && outputIndex < immersiveRecipe.getOutputs().size() ? outputIndex : -1);
    }

    private void updateRecipe() {
        if (immersiveFocus.isEmpty()) {
            if (immersiveRecipe != null) {
                immersiveRecipe = null;
                uiStateClock++;
            }
            return;
        }
        if (immersiveRecipe == null || !immersiveRecipe.getInput().apply(immersiveFocus)) {
            for (final CuttingSawRecipe recipe : type.getRecipes()) {
                if (recipe.getInput().apply(immersiveFocus)) {
                    immersiveRecipe = recipe;
                    uiStateClock++;
                    tryStartBlockCrafting();
                    return;
                }
            }
            if (immersiveRecipe != null) {
                immersiveRecipe = null;
                uiStateClock++;
            }
        }
    }

    @Override
    public boolean canConnectPower(final EnumFacing from) {
        final IBlockState state = world.getBlockState(pos);
        return state.getBlock() instanceof BlockCuttingSaw && BlockCuttingSaw.isAxleHere(state, from);
    }

    @Override
    public int getMinimumInput() {
        return SerrConfig.powerRequired;
    }

    @Override
    public int getMaximumInput() {
        return SerrConfig.powerMaximum;
    }

    public void checkPower() {
        if (bwmMech.updatePower() && !world.isRemote) {
            tryStartBlockCrafting();
            markDirty();
            powerSyncDirty = true;
        }
    }

    public void tryDamageEntity(final EntityLivingBase entity) {
        if (!isActive()) return;
        if (SerrBwmCompat.LOADED && SerrConfig.chopBonusDamage > 0) {
            final IBlockState sawState = world.getBlockState(pos);
            if (!(sawState.getBlock() instanceof BlockCuttingSaw)) return;
            BlockPos searchPos = pos.offset(sawState.getValue(BlockCuttingSaw.FACING));
            boolean shouldStain = true;
            for (int i = 0; i < 3; i++) {
                final IBlockState state = world.getBlockState(searchPos);
                final SerrBwmCompat.ChoppingBlockResult result = SerrBwmCompat.isChoppingBlock(state);
                if (result != SerrBwmCompat.ChoppingBlockResult.NO) {
                    if (entity.attackEntityFrom(SerrBwmCompat.getSawDamageSource(world, true),
                            (float) (SerrConfig.contactDamage + SerrConfig.chopBonusDamage))) {
                        entity.recentlyHit = 60;
                        if (shouldStain && result == SerrBwmCompat.ChoppingBlockResult.CLEAN) {
                            SerrBwmCompat.stainChoppingBlock(world, searchPos);
                        }
                    }
                    return;
                }
                if (!world.isAirBlock(searchPos)) {
                    final Block block = state.getBlock();
                    if (!(block instanceof BlockLiquid || block instanceof IFluidBlock)) break;
                    shouldStain = false;
                }
                searchPos = searchPos.down();
            }
        }
        if (entity.attackEntityFrom(SerrBwmCompat.getSawDamageSource(world, false), (float) SerrConfig.contactDamage)) {
            entity.recentlyHit = 60;
        }
    }

    public void tryStartBlockCrafting() {
        if (craftingJob != null || !SerrConfig.immersiveMode) return;
        final IBlockState sawState = world.getBlockState(pos);
        if (!(sawState.getBlock() instanceof BlockCuttingSaw)) return;
        final IBlockState targetState = world.getBlockState(pos.offset(sawState.getValue(BlockCuttingSaw.FACING)));
        final CuttingSawRecipe recipe;
        findRecipe:
        {
            for (final CuttingSawRecipe cand : type.getRecipes()) {
                final Predicate<IBlockState> input = cand.getInputBlock();
                if (input != null && input.test(targetState)) {
                    recipe = cand;
                    break findRecipe;
                }
            }
            return;
        }
        final int outputIndex = immersiveRecipeConfig.getInt(recipe.getId());
        final CuttingSawRecipe.Output output = recipe.getOutput(outputIndex);
        if (output == null) return;
        craftingJob = new BlockJob(recipe, output, outputIndex);
        markDirty();
        jobSyncDirty = true;
    }

    public void tryStartItemCrafting(final EntityItem entity) {
        if (craftingJob != null || !SerrConfig.immersiveMode) {
            return;
        }
        final NBTTagCompound data = entity.getEntityData();
        if (data.hasKey(TAG_NO_SAW)) return;
        final ItemStack entityStack = entity.getItem();
        final CuttingSawRecipe recipe;
        findRecipe:
        {
            for (final CuttingSawRecipe cand : type.getRecipes()) {
                if (!cand.isBlockOnly() && cand.getInput().apply(entityStack)) {
                    recipe = cand;
                    break findRecipe;
                }
            }
            data.setBoolean(TAG_NO_SAW, true); // don't try to process the same entity again
            return;
        }
        final int outputIndex = immersiveRecipeConfig.getInt(recipe.getId());
        final CuttingSawRecipe.Output output = recipe.getOutput(outputIndex);
        if (output == null) return;
        final ItemStack inputStack = entityStack.splitStack(1);
        if (entityStack.isEmpty()) {
            entity.setDead();
        }
        craftingJob = new ItemJob(recipe, output, outputIndex, inputStack);
        markDirty();
        jobSyncDirty = true;
    }

    @Override
    public void update() {
        if (isActive()) {
            if (craftingJob != null) {
                if (world.isRemote) {
                    craftingJob.playWorkEffect(this);
                } else {
                    switch (craftingJob.work(this)) {
                        case NOT_DONE:
                            break;
                        case DONE:
                            if (SerrConfig.finishedSound) {
                                world.playSound(null, pos, type.getCutSound(), SoundCategory.BLOCKS,
                                        0.75F, 0.93F + 0.14F * world.rand.nextFloat());
                            }
                            craftingJob.finishRecipe(this);
                            // falls through
                        case FAIL:
                            craftingJob = null;
                            tryStartBlockCrafting();
                            jobSyncDirty = true;
                            break;
                    }
                    markDirty();
                }
                if (workingSound != null) {
                    workingSound.setPlaying(true);
                }
            } else if (workingSound != null) {
                workingSound.setPlaying(false);
            }
            if (activeSound != null) {
                activeSound.setPlaying(true);
            }
        } else if (activeSound != null) {
            activeSound.setPlaying(false);
        }

        if (!world.isRemote) {
            if (powerSyncDirty) {
                powerSyncDirty = false;
                sendToAllTracking(new SPacketSyncSawPower(pos, bwmMech.getPower()));
            }
            if (jobSyncDirty) {
                jobSyncDirty = false;
                if (craftingJob != null) {
                    final NBTTagCompound tag = new NBTTagCompound();
                    craftingJob.writeToNbt(tag);
                    sendToAllTracking(new SPacketSyncSawRecipe(pos, tag));
                } else {
                    sendToAllTracking(new SPacketSyncSawRecipe(pos, null));
                }
            }
        }
    }

    private void dropOutputStack(final ItemStack stack) {
        final EntityItem entity = new EntityItem(world, pos.getX() + 0.5D, pos.getY() + 0.5D, pos.getZ() + 0.5D);
        entity.motionX = 0.05D * world.rand.nextGaussian();
        entity.motionY = 0.05D * world.rand.nextGaussian();
        entity.motionZ = 0.05D * world.rand.nextGaussian();
        final IBlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BlockCuttingSaw) {
            switch (state.getValue(BlockCuttingSaw.FACING)) {
                case UP:
                    entity.posY += 0.4D;
                    entity.motionY += 0.1D;
                    break;
                case DOWN:
                    entity.posY -= 0.4D;
                    entity.motionY -= 0.1D;
                    break;
                case NORTH:
                    entity.posZ -= 0.4D;
                    entity.motionZ -= 0.1D;
                    break;
                case EAST:
                    entity.posX += 0.4D;
                    entity.motionX += 0.1D;
                    break;
                case SOUTH:
                    entity.posZ += 0.4D;
                    entity.motionZ += 0.1D;
                    break;
                case WEST:
                    entity.posX -= 0.4D;
                    entity.motionX -= 0.1D;
                    break;
            }
        }
        entity.setItem(stack);
        entity.setDefaultPickupDelay();
        entity.getEntityData().setBoolean(TAG_NO_SAW, true); // don't let an output go back in as an input
        world.spawnEntity(entity);
    }

    public void onBreak() {
        if (!world.isRemote && craftingJob != null) {
            craftingJob.onBreak(this);
        }
        if (activeSound != null) {
            activeSound.setPlaying(false);
        }
        if (workingSound != null) {
            workingSound.setPlaying(false);
        }
    }

    private void sendToAllTracking(final IMessage message) {
        Serrated.PROXY.getNetwork().sendToAllTracking(message,
                new NetworkRegistry.TargetPoint(world.provider.getDimension(), pos.getX(), pos.getY(), pos.getZ(), 0));
    }

    @Override
    public @Nullable SPacketUpdateTileEntity getUpdatePacket() {
        return new SPacketUpdateTileEntity(pos, -999, getUpdateTag());
    }

    @Override
    public void onDataPacket(final NetworkManager net, final SPacketUpdateTileEntity pkt) {
        handleUpdateTag(pkt.getNbtCompound());
    }

    @Override
    public NBTTagCompound getUpdateTag() {
        return writeToNBT(new NBTTagCompound());
    }

    @Override
    public NBTTagCompound writeToNBT(final NBTTagCompound compound) {
        super.writeToNBT(compound);
        bwmMech.writeToNbt(compound);
        if (!immersiveRecipeConfig.isEmpty()) {
            final NBTTagCompound configTag = new NBTTagCompound();
            for (final Object2IntMap.Entry<String> entry : immersiveRecipeConfig.object2IntEntrySet()) {
                configTag.setInteger(entry.getKey(), entry.getIntValue());
            }
            compound.setTag("config", configTag);
        }
        if (!immersiveFocus.isEmpty()) {
            compound.setTag("focus", immersiveFocus.serializeNBT());
        }
        if (immersiveRecipe != null) {
            compound.setString("recipe", immersiveRecipe.getId());
        }
        if (craftingJob != null) {
            final NBTTagCompound jobTag = new NBTTagCompound();
            craftingJob.writeToNbt(jobTag);
            compound.setTag("job", jobTag);
        }
        return compound;
    }

    @Override
    public void readFromNBT(final NBTTagCompound compound) {
        super.readFromNBT(compound);
        bwmMech.readFromNbt(compound);
        immersiveFocus = compound.hasKey("focus", Constants.NBT.TAG_COMPOUND)
                ? new ItemStack(compound.getCompoundTag("focus")) : ItemStack.EMPTY;

        immersiveRecipeConfig.clear();
        final NBTTagCompound configTag = compound.getCompoundTag("config");
        for (final String recipeId : configTag.getKeySet()) {
            immersiveRecipeConfig.put(recipeId, configTag.getInteger(recipeId));
        }

        readRecipe:
        {
            if (!immersiveFocus.isEmpty() && compound.hasKey("recipe", Constants.NBT.TAG_STRING)) {
                final String recipeId = compound.getString("recipe");
                final CuttingSawRecipe recipe = type.getRecipeById(recipeId);
                if (recipe == null) {
                    Serrated.LOGGER.warn("Could not load unknown {} recipe: {}", type, recipeId);
                } else if (!recipe.getInput().apply(immersiveFocus)) {
                    Serrated.LOGGER.warn("{} recipe {} does not apply to focus: {}",
                            type, recipeId, immersiveFocus.getItem().getRegistryName());
                } else {
                    immersiveRecipe = recipe;
                    break readRecipe;
                }
            }
            immersiveRecipe = null;
        }

        craftingJob = compound.hasKey("job", Constants.NBT.TAG_COMPOUND)
                ? CraftingJob.readFromNbt(type, compound.getCompoundTag("job")) : null;
    }

    private enum WorkResult {
        NOT_DONE, DONE, FAIL
    }

    private static abstract class CraftingJob {

        protected final CuttingSawRecipe recipe;
        protected final CuttingSawRecipe.Output output;
        private final int outputIndex;

        private int progress = 0;

        CraftingJob(final CuttingSawRecipe recipe, final CuttingSawRecipe.Output output, final int outputIndex) {
            this.recipe = recipe;
            this.output = output;
            this.outputIndex = outputIndex;
        }

        WorkResult work(final TileEntityCuttingSaw saw) {
            if (progress >= output.getDuration()) return WorkResult.DONE;
            progress++;
            return WorkResult.NOT_DONE;
        }

        abstract void playWorkEffect(final TileEntityCuttingSaw saw);

        abstract void finishRecipe(final TileEntityCuttingSaw saw);

        void onBreak(final TileEntityCuttingSaw saw) {}

        void writeToNbt(NBTTagCompound tag) {
            tag.setString("recipe", recipe.getId());
            tag.setInteger("output", outputIndex);
            tag.setInteger("progress", progress);
        }

        @Nullable
        static CraftingJob readFromNbt(final CuttingSawType type, final NBTTagCompound tag) {
            switch (tag.getString("type")) {
                case "block":
                    return BlockJob.readFromNbt(type, tag);
                case "item":
                    return ItemJob.readFromNbt(type, tag);
                default:
                    return null;
            }
        }
    }

    private static class BlockJob extends CraftingJob {

        BlockJob(final CuttingSawRecipe recipe, final CuttingSawRecipe.Output output, final int outputIndex) {
            super(recipe, output, outputIndex);
        }

        @Override
        WorkResult work(final TileEntityCuttingSaw saw) {
            final Predicate<IBlockState> input = recipe.getInputBlock();
            if (input == null) return WorkResult.FAIL;
            final IBlockState state = saw.world.getBlockState(saw.pos);
            if (!(state.getBlock() instanceof BlockCuttingSaw) ||
                    !input.test(saw.world.getBlockState(saw.pos.offset(state.getValue(BlockCuttingSaw.FACING))))) {
                return WorkResult.FAIL;
            }
            return super.work(saw);
        }

        @Override
        void playWorkEffect(final TileEntityCuttingSaw saw) {
            final Predicate<IBlockState> input = recipe.getInputBlock();
            if (input == null) return;
            final IBlockState sawState = saw.world.getBlockState(saw.pos);
            if (!(sawState.getBlock() instanceof BlockCuttingSaw)) return;
            final IBlockState targetState = saw.world.getBlockState(
                    saw.pos.offset(sawState.getValue(BlockCuttingSaw.FACING)));
            if (!input.test(targetState)) return;
            final IBlockState state = saw.world.getBlockState(saw.pos);
            if (!(state.getBlock() instanceof BlockCuttingSaw)) return;
            switch (state.getValue(BlockCuttingSaw.FACING)) {
                case UP:
                    saw.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                            saw.pos.getX() + 0.5D, saw.pos.getY() + 1D, saw.pos.getZ() + 0.5D,
                            0.1D * saw.world.rand.nextGaussian(), 0D, 0.1D * saw.world.rand.nextGaussian(),
                            Block.getStateId(targetState));
                    break;
                case DOWN:
                    saw.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                            saw.pos.getX() + 0.5D, saw.pos.getY(), saw.pos.getZ() + 0.5D,
                            0.1D * saw.world.rand.nextGaussian(), 0D, 0.1D * saw.world.rand.nextGaussian(),
                            Block.getStateId(targetState));
                    break;
                case NORTH:
                    saw.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                            saw.pos.getX() + 0.5D, saw.pos.getY() + 0.5D, saw.pos.getZ(),
                            0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(), 0D,
                            Block.getStateId(targetState));
                    break;
                case EAST:
                    saw.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                            saw.pos.getX() + 1D, saw.pos.getY() + 0.5D, saw.pos.getZ() + 0.5D,
                            0D, 0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(),
                            Block.getStateId(targetState));
                    break;
                case SOUTH:
                    saw.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                            saw.pos.getX() + 0.5D, saw.pos.getY() + 0.5D, saw.pos.getZ() + 1D,
                            0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(), 0D,
                            Block.getStateId(targetState));
                    break;
                case WEST:
                    saw.world.spawnParticle(EnumParticleTypes.BLOCK_CRACK,
                            saw.pos.getX(), saw.pos.getY() + 0.5D, saw.pos.getZ() + 0.5D,
                            0D, 0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(),
                            Block.getStateId(targetState));
                    break;
            }
        }

        @Override
        void finishRecipe(final TileEntityCuttingSaw saw) {
            final IBlockState state = saw.world.getBlockState(saw.pos);
            if (!(state.getBlock() instanceof BlockCuttingSaw)) return;
            if (output.getBlock() != null) {
                saw.world.setBlockState(saw.pos.offset(state.getValue(BlockCuttingSaw.FACING)), output.getBlock());
            } else {
                saw.world.setBlockToAir(saw.pos.offset(state.getValue(BlockCuttingSaw.FACING)));
                saw.dropOutputStack(output.getItem().copy());
            }
            for (final ItemStack extraStack : output.getExtra()) {
                saw.dropOutputStack(extraStack.copy());
            }
        }

        @Override
        void writeToNbt(final NBTTagCompound tag) {
            tag.setString("type", "block");
            super.writeToNbt(tag);
        }

        @Nullable
        static BlockJob readFromNbt(final CuttingSawType type, final NBTTagCompound tag) {
            final String recipeId = tag.getString("recipe");
            final CuttingSawRecipe recipe = type.getRecipeById(recipeId);
            if (recipe == null) {
                Serrated.LOGGER.warn("Block job could not load unknown {} recipe: {}", type, recipeId);
                return null;
            }
            final int outputIndex = tag.getInteger("output");
            final CuttingSawRecipe.Output output = recipe.getOutput(outputIndex);
            if (output == null) return null;
            return new BlockJob(recipe, output, outputIndex);
        }

    }

    private static class ItemJob extends CraftingJob {

        private final ItemStack inputStack;

        public ItemJob(final CuttingSawRecipe recipe, final CuttingSawRecipe.Output output, final int outputIndex,
                       final ItemStack inputStack) {
            super(recipe, output, outputIndex);
            this.inputStack = inputStack;
        }

        @Override
        void playWorkEffect(final TileEntityCuttingSaw saw) {
            final IBlockState state = saw.world.getBlockState(saw.pos);
            if (state.getBlock() instanceof BlockCuttingSaw) {
                switch (state.getValue(BlockCuttingSaw.FACING)) {
                    case UP:
                        saw.world.spawnParticle(EnumParticleTypes.ITEM_CRACK,
                                saw.pos.getX() + 0.5D, saw.pos.getY() + 0.9D, saw.pos.getZ() + 0.5D,
                                0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(),
                                Item.getIdFromItem(inputStack.getItem()), inputStack.getMetadata());
                        break;
                    case DOWN:
                        saw.world.spawnParticle(EnumParticleTypes.ITEM_CRACK,
                                saw.pos.getX() + 0.5D, saw.pos.getY() + 0.1D, saw.pos.getZ() + 0.5D,
                                0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(),
                                Item.getIdFromItem(inputStack.getItem()), inputStack.getMetadata());
                        break;
                    case NORTH:
                        saw.world.spawnParticle(EnumParticleTypes.ITEM_CRACK,
                                saw.pos.getX() + 0.5D, saw.pos.getY() + 0.5D, saw.pos.getZ() + 0.1D,
                                0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(),
                                Item.getIdFromItem(inputStack.getItem()), inputStack.getMetadata());
                        break;
                    case EAST:
                        saw.world.spawnParticle(EnumParticleTypes.ITEM_CRACK,
                                saw.pos.getX() + 0.9D, saw.pos.getY() + 0.5D, saw.pos.getZ() + 0.5D,
                                0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(),
                                Item.getIdFromItem(inputStack.getItem()), inputStack.getMetadata());
                        break;
                    case SOUTH:
                        saw.world.spawnParticle(EnumParticleTypes.ITEM_CRACK,
                                saw.pos.getX() + 0.5D, saw.pos.getY() + 0.5D, saw.pos.getZ() + 0.9D,
                                0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(),
                                Item.getIdFromItem(inputStack.getItem()), inputStack.getMetadata());
                        break;
                    case WEST:
                        saw.world.spawnParticle(EnumParticleTypes.ITEM_CRACK,
                                saw.pos.getX() + 0.1D, saw.pos.getY() + 0.5D, saw.pos.getZ() + 0.5D,
                                0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(), 0.1D * saw.world.rand.nextGaussian(),
                                Item.getIdFromItem(inputStack.getItem()), inputStack.getMetadata());
                        break;
                }
            }
        }

        @Override
        void finishRecipe(final TileEntityCuttingSaw saw) {
            saw.dropOutputStack(output.getItem().copy());
            for (final ItemStack extraStack : output.getExtra()) {
                saw.dropOutputStack(extraStack.copy());
            }
        }

        @Override
        void onBreak(final TileEntityCuttingSaw saw) {
            InventoryHelper.spawnItemStack(saw.world, saw.pos.getX(), saw.pos.getY(), saw.pos.getZ(), inputStack);
        }

        @Override
        void writeToNbt(final NBTTagCompound tag) {
            tag.setString("type", "item");
            super.writeToNbt(tag);
            tag.setTag("input", inputStack.serializeNBT());
        }

        @Nullable
        static ItemJob readFromNbt(final CuttingSawType type, final NBTTagCompound tag) {
            final String recipeId = tag.getString("recipe");
            final CuttingSawRecipe recipe = type.getRecipeById(recipeId);
            if (recipe == null) {
                Serrated.LOGGER.warn("Item job could not load unknown {} recipe: {}", type, recipeId);
                return null;
            }
            final int outputIndex = tag.getInteger("output");
            final CuttingSawRecipe.Output output = recipe.getOutput(outputIndex);
            if (output == null) return null;
            return new ItemJob(recipe, output, outputIndex, new ItemStack(tag.getCompoundTag("input")));
        }

    }

    public static class Saw extends TileEntityCuttingSaw {

        public Saw() {
            super(CuttingSawType.SAW);
        }

    }

    public static class Stonecutter extends TileEntityCuttingSaw {

        public Stonecutter() {
            super(CuttingSawType.STONECUTTER);
        }

    }

}
