package st.evening.mc.rotnsaws.feature.saw;

import betterwithmods.api.block.IOverpower;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDirectional;
import net.minecraft.block.ITileEntityProvider;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.block.properties.PropertyBool;
import net.minecraft.block.state.BlockFaceShape;
import net.minecraft.block.state.BlockStateContainer;
import net.minecraft.block.state.IBlockState;
import net.minecraft.client.resources.I18n;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.BlockRenderLayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.IBlockAccess;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.Optional;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.SerrConfig;
import st.evening.mc.rotnsaws.SerrConst;
import st.evening.mc.rotnsaws.SerrGuiHandler;
import st.evening.mc.rotnsaws.Serrated;
import st.evening.mc.rotnsaws.compat.bwm.SerrBwmCompat;

import java.util.List;
import java.util.Random;

// loosely based on BWM's BlockSaw
@Optional.Interface(iface = "betterwithmods.api.block.IOverpower", modid = SerrBwmCompat.MOD_ID)
public class BlockCuttingSaw extends BlockDirectional implements ITileEntityProvider, IOverpower {
    public static final PropertyBool AXLES = PropertyBool.create("axles");
    public static final PropertyBool ROLL = PropertyBool.create("roll");
    public static final PropertyBool ACTIVE = PropertyBool.create("active");

    public static boolean isAxleHere(final EnumFacing blockDir, final boolean blockRoll, final EnumFacing face) {
        if (face.getAxis() == blockDir.getAxis()) {
            return face != blockDir;
        }
        switch (face.getAxis()) {
            case X:
                return blockRoll;
            case Y:
                return !blockRoll;
            case Z:
                return (blockDir.getAxis() != EnumFacing.Axis.Y) == blockRoll;
        }
        throw new IllegalStateException("Illegal axis " + face.getAxis() + " for face " + face);
    }

    public static boolean isAxleHere(final IBlockState state, final EnumFacing face) {
        return isAxleHere(state.getValue(FACING), state.getValue(ROLL), face);
    }

    private static final AxisAlignedBB BB_UP;
    private static final AxisAlignedBB BB_DOWN;
    private static final AxisAlignedBB BB_NORTH;
    private static final AxisAlignedBB BB_EAST;
    private static final AxisAlignedBB BB_SOUTH;
    private static final AxisAlignedBB BB_WEST;

    static {
        final double p = 0.75D, n = 1 - p;
        BB_UP = new AxisAlignedBB(0D, 0D, 0D, 1D, p, 1D);
        BB_DOWN = new AxisAlignedBB(0D, 1D, 0D, 1D, n, 1D);
        BB_NORTH = new AxisAlignedBB(0D, 0D, 1D, 1D, 1D, n);
        BB_EAST = new AxisAlignedBB(0D, 0D, 0D, p, 1D, 1D);
        BB_SOUTH = new AxisAlignedBB(0D, 0D, 0D, 1D, 1D, p);
        BB_WEST = new AxisAlignedBB(1D, 0D, 0D, n, 1D, 1D);
    }

    private final CuttingSawType type;

    public BlockCuttingSaw(final CuttingSawType type, final Material material, final SoundType soundType) {
        super(material);
        this.type = type;
        setHardness(2F);
        setSoundType(soundType);
        setTranslationKey(type.getBlockTranslationKey());
        setDefaultState(blockState.getBaseState()
                .withProperty(FACING, EnumFacing.UP)
                .withProperty(ROLL, false)
                .withProperty(AXLES, false)
                .withProperty(ACTIVE, false));
    }

    @Override
    protected BlockStateContainer createBlockState() {
        return new BlockStateContainer(this, FACING, ROLL, AXLES, ACTIVE);
    }

    @Override
    public int getMetaFromState(final IBlockState state) {
        return (state.getValue(FACING).getIndex() << 1) | (state.getValue(ROLL) ? 1 : 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getStateFromMeta(final int meta) {
        return getDefaultState()
                .withProperty(FACING, EnumFacing.byIndex(meta >>> 1))
                .withProperty(ROLL, (meta & 1) != 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getActualState(final IBlockState state, final IBlockAccess world, final BlockPos pos) {
        if (SerrConfig.powerRequired > 0) {
            final TileEntity te = world.getTileEntity(pos);
            return state.withProperty(AXLES, true)
                    .withProperty(ACTIVE, te instanceof TileEntityCuttingSaw && ((TileEntityCuttingSaw) te).isActive());
        } else {
            return state.withProperty(AXLES, false).withProperty(ACTIVE, true);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public IBlockState getStateForPlacement(final World world, final BlockPos pos, final EnumFacing facing,
                                            final float hitX, final float hitY, final float hitZ, final int meta,
                                            final EntityLivingBase placer) {
        if (facing.getAxis() == EnumFacing.Axis.Y) {
            return getDefaultState().withProperty(FACING, facing)
                    .withProperty(ROLL, placer.getHorizontalFacing().getAxis() == EnumFacing.Axis.X);
        } else {
            final EnumFacing.Axis placerAxis = EnumFacing.getDirectionFromEntityLiving(pos, placer).getAxis();
            return getDefaultState().withProperty(FACING, facing)
                    .withProperty(ROLL, placerAxis != EnumFacing.Axis.Y && placerAxis != facing.getAxis());
        }
    }

    @Override
    public void onBlockAdded(final World world, final BlockPos pos, final IBlockState state) {
        world.scheduleBlockUpdate(pos, this, tickRate(world), 5);
    }

    @Override
    public TileEntity createNewTileEntity(final World worldIn, final int meta) {
        return type.createTileEntity();
    }

    @Override
    public boolean rotateBlock(final World world, final BlockPos pos, final EnumFacing axis) {
        final TileEntity te = world.getTileEntity(pos);
        final IBlockState state = world.getBlockState(pos);
        final EnumFacing facing = state.getValue(FACING);
        if (facing.getAxis() == axis.getAxis()) {
            world.setBlockState(pos, state.withProperty(ROLL, !state.getValue(ROLL)));
        } else {
            switch (axis) {
                case UP:
                    world.setBlockState(pos, state.withProperty(FACING, facing.rotateY()));
                    break;
                case DOWN:
                    world.setBlockState(pos, state.withProperty(FACING, facing.rotateYCCW()));
                    break;
                case EAST:
                    world.setBlockState(pos,
                            state.withProperty(FACING, facing.rotateAround(EnumFacing.Axis.X)));
                    break;
                case SOUTH:
                    world.setBlockState(pos, state
                            .withProperty(FACING, facing.rotateAround(EnumFacing.Axis.Z))
                            .withProperty(ROLL, !state.getValue(ROLL)));
                    break;
                case WEST:
                    switch (facing) {
                        case UP:
                            world.setBlockState(pos, state.withProperty(FACING, EnumFacing.SOUTH));
                            break;
                        case SOUTH:
                            world.setBlockState(pos, state.withProperty(FACING, EnumFacing.DOWN));
                            break;
                        case DOWN:
                            world.setBlockState(pos, state.withProperty(FACING, EnumFacing.NORTH));
                            break;
                        case NORTH:
                            world.setBlockState(pos, state.withProperty(FACING, EnumFacing.UP));
                            break;
                    }
                    break;
                case NORTH:
                    switch (facing) {
                        case UP:
                            world.setBlockState(pos, state.withProperty(FACING, EnumFacing.WEST)
                                    .withProperty(ROLL, !state.getValue(ROLL)));
                            break;
                        case WEST:
                            world.setBlockState(pos, state.withProperty(FACING, EnumFacing.DOWN)
                                    .withProperty(ROLL, !state.getValue(ROLL)));
                            break;
                        case DOWN:
                            world.setBlockState(pos, state.withProperty(FACING, EnumFacing.EAST)
                                    .withProperty(ROLL, !state.getValue(ROLL)));
                            break;
                        case EAST:
                            world.setBlockState(pos, state.withProperty(FACING, EnumFacing.UP)
                                    .withProperty(ROLL, !state.getValue(ROLL)));
                            break;
                    }
                    break;
            }
        }
        if (te != null) {
            te.validate();
            world.setTileEntity(pos, te);
            world.scheduleBlockUpdate(pos, this, 2, 5);
        }
        return true;
    }

    @Override
    public boolean onBlockActivated(final World world, final BlockPos pos, final IBlockState state,
                                    final EntityPlayer player, final EnumHand hand, final EnumFacing facing,
                                    final float hitX, final float hitY, final float hitZ) {
        if (world.isRemote) return true;
        if (player.getHeldItemMainhand().isEmpty() && player.getHeldItemOffhand().isEmpty() && player.isSneaking()) {
            world.playSound(null, pos, getSoundType(state, world, pos, player).getPlaceSound(),
                    SoundCategory.BLOCKS, 1F, world.rand.nextFloat() * 0.1F + 0.9F);
            rotateBlock(world, pos, facing);
        } else {
            player.openGui(Serrated.INSTANCE, SerrGuiHandler.CUTTING_SAW, world, pos.getX(), pos.getY(), pos.getZ());
        }
        return true;
    }

    @SuppressWarnings("deprecation")
    @Override
    public void neighborChanged(final IBlockState state, final World world, final BlockPos pos, final Block block,
                                final BlockPos other) {
        world.scheduleBlockUpdate(pos, this, tickRate(world), 5);
        if (!other.equals(pos.offset(state.getValue(FACING)))) return;
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCuttingSaw) {
            ((TileEntityCuttingSaw) te).tryStartBlockCrafting();
        }
    }

    @Override
    public void onEntityCollision(final World world, final BlockPos pos, final IBlockState state, final Entity entity) {
        if (world.isRemote) return;
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCuttingSaw) {
            if (entity instanceof EntityItem) {
                ((TileEntityCuttingSaw) te).tryStartItemCrafting((EntityItem) entity);
            } else if (SerrConfig.contactDamage > 0 && entity instanceof EntityLivingBase) {
                ((TileEntityCuttingSaw) te).tryDamageEntity((EntityLivingBase) entity);
            }
        }
    }

    @Override
    public void updateTick(final World world, final BlockPos pos, final IBlockState state, final Random rand) {
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCuttingSaw) {
            ((TileEntityCuttingSaw) te).checkPower();
        }
    }

    @Override
    public void breakBlock(final World world, final BlockPos pos, final IBlockState state) {
        final TileEntity te = world.getTileEntity(pos);
        if (te instanceof TileEntityCuttingSaw) {
            ((TileEntityCuttingSaw) te).onBreak();
        }
        super.breakBlock(world, pos, state);
    }

    @Optional.Method(modid = SerrBwmCompat.MOD_ID)
    @Override
    public void overpower(final World world, final BlockPos pos) {
        world.playSound(null, pos, SoundEvents.ENTITY_GENERIC_EXPLODE, SoundCategory.BLOCKS,
                0.3F, 0.45F + 0.1F * world.rand.nextFloat());
        world.setBlockToAir(pos);
    }

    @SuppressWarnings("deprecation")
    @Override
    public AxisAlignedBB getBoundingBox(final IBlockState state, final IBlockAccess source, final BlockPos pos) {
        switch (state.getValue(FACING)) {
            case UP:
                return BB_UP;
            case DOWN:
                return BB_DOWN;
            case NORTH:
                return BB_NORTH;
            case EAST:
                return BB_EAST;
            case SOUTH:
                return BB_SOUTH;
            case WEST:
                return BB_WEST;
        }
        throw new IllegalStateException(state.getValue(FACING).toString());
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockFaceShape getBlockFaceShape(final IBlockAccess world, final IBlockState state, final BlockPos pos,
                                            final EnumFacing face) {
        return face == state.getValue(FACING).getOpposite() ? BlockFaceShape.SOLID : BlockFaceShape.UNDEFINED;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isTopSolid(final IBlockState state) {
        return state.getValue(FACING) == EnumFacing.DOWN;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isSideSolid(final IBlockState state, final IBlockAccess world, final BlockPos pos,
                               final EnumFacing side) {
        return side != state.getValue(FACING);
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isOpaqueCube(final IBlockState state) {
        return false;
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean isFullCube(final IBlockState state) {
        return false;
    }

    @Override
    public BlockRenderLayer getRenderLayer() {
        return BlockRenderLayer.CUTOUT;
    }

    @Override
    public void addInformation(final ItemStack stack, @Nullable final World worldIn, final List<String> tooltip,
                               final ITooltipFlag flags) {
        if (SerrConfig.rotateWithHand) {
            tooltip.add(I18n.format("tooltip." + SerrConst.MOD_ID + ".rotate_with_hand"));
        }
    }
}
