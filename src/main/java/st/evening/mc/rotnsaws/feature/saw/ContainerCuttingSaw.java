package st.evening.mc.rotnsaws.feature.saw;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IContainerListener;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.items.wrapper.EmptyHandler;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.SerrConfig;
import st.evening.mc.rotnsaws.Serrated;
import st.evening.mc.rotnsaws.network.SPacketSyncSawUi;

import java.util.Collections;

public abstract class ContainerCuttingSaw extends Container {
    public static ContainerCuttingSaw create(final TileEntityCuttingSaw saw, final InventoryPlayer playerInv) {
        return SerrConfig.immersiveMode ? new Immersive(saw, playerInv) : new Workbench(saw, playerInv);
    }

    protected final TileEntityCuttingSaw saw;
    protected final InventoryPlayer playerInv;

    public ContainerCuttingSaw(final TileEntityCuttingSaw saw, final InventoryPlayer playerInv) {
        this.saw = saw;
        this.playerInv = playerInv;
    }

    protected void addPlayerInventory() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 9; j++) {
                addSlotToContainer(new Slot(playerInv, 9 + 9 * i + j, 8 + 18 * j, 88 + 18 * i));
            }
        }
        for (int i = 0; i < 9; i++) {
            addSlotToContainer(new Slot(playerInv, i, 8 + 18 * i, 146));
        }
    }

    public TileEntityCuttingSaw getSaw() {
        return saw;
    }

    public InventoryPlayer getPlayerInventory() {
        return playerInv;
    }

    @Nullable
    public abstract CuttingSawRecipe getActiveRecipe();

    public abstract int getActiveOutput();

    public abstract OutputDisplay checkOutputDisplay();

    public abstract void setOutputIndexFromGui(final int outputIndex);

    @Override
    public boolean canInteractWith(final EntityPlayer player) {
        return player.getDistanceSq(saw.getPos()) <= 64D;
    }

    public enum OutputDisplay {
        SLOT, BLOCKED, DISPLAY
    }

    public static class Workbench extends ContainerCuttingSaw {
        private static final int SLOT_INPUT = 0;
        private static final int SLOT_OUTPUT = 1;

        private final InputInventory inputInv = new InputInventory();
        private final ResultInventory resultInv = new ResultInventory();
        private final SlotOutput outputSlot;

        @Nullable
        private CuttingSawRecipe activeRecipe = null;
        private int activeOutput = -1;
        private long lastSoundTime = -1L;

        public Workbench(final TileEntityCuttingSaw saw, final InventoryPlayer playerInv) {
            super(saw, playerInv);
            addSlotToContainer(new SlotItemHandler(inputInv, 0, 8, 18));
            outputSlot = new SlotOutput(resultInv, 8, 56);
            addSlotToContainer(outputSlot);
            addPlayerInventory();
        }

        @Nullable
        @Override
        public CuttingSawRecipe getActiveRecipe() {
            return activeRecipe;
        }

        @Override
        public int getActiveOutput() {
            return activeOutput;
        }

        @Override
        public OutputDisplay checkOutputDisplay() {
            if (saw.isActive()) {
                outputSlot.setEnabled(true);
                return OutputDisplay.SLOT;
            } else {
                outputSlot.setEnabled(false);
                return OutputDisplay.BLOCKED;
            }
        }

        @Override
        public void setOutputIndexFromGui(final int outputIndex) {
            if (outputIndex >= 0 && outputIndex != activeOutput
                    && activeRecipe != null && outputIndex < activeRecipe.getOutputs().size()) {
                activeOutput = outputIndex;
                updateResult();
            }
        }

        public void updateResult() {
            final ItemStack stack = inputInv.getStackInSlot(0);
            if (!stack.isEmpty()) {
                getNewResult:
                {
                    if (activeRecipe == null) break getNewResult;
                    if (!activeRecipe.getInput().apply(stack)) {
                        activeRecipe = null;
                        activeOutput = -1;
                        break getNewResult;
                    }
                    final CuttingSawRecipe.Output output = activeRecipe.getOutput(activeOutput);
                    if (output == null) break getNewResult;
                    resultInv.setStackInSlot(0, output.getItem().copy());
                    return;
                }
            }
            resultInv.setStackInSlot(0, ItemStack.EMPTY);
        }

        @Override
        public boolean canMergeSlot(final ItemStack stack, final Slot slot) {
            return slot.slotNumber != SLOT_OUTPUT;
        }

        @Override
        public ItemStack transferStackInSlot(final EntityPlayer player, final int index) {
            final Slot slot = getSlot(index);
            if (!slot.getHasStack()) return ItemStack.EMPTY;
            final ItemStack inSlot = slot.getStack();
            final ItemStack inSlotBefore = inSlot.copy();
            switch (index) {
                case SLOT_INPUT:
                    if (!mergeItemStack(inSlot, 2, 38, false)) return ItemStack.EMPTY;
                    break;
                case SLOT_OUTPUT:
                    if (!mergeItemStack(inSlot, 2, 38, true)) return ItemStack.EMPTY;
                    resultInv.extractItem(0, inSlotBefore.getCount() - inSlot.getCount(), false);
                    break;
                default:
                    if (!mergeItemStack(inSlot, 0, 1, false)) return ItemStack.EMPTY;
                    break;
            }
            if (inSlot.isEmpty()) {
                slot.putStack(ItemStack.EMPTY);
            } else {
                slot.onSlotChanged();
            }
            if (inSlot.getCount() == inSlotBefore.getCount()) return ItemStack.EMPTY;
            slot.onTake(player, inSlot);
            return inSlotBefore;
        }

        @Override
        public void onContainerClosed(final EntityPlayer player) {
            super.onContainerClosed(player);
            if (player.getEntityWorld().isRemote) return;
            final ItemStack stack = inputInv.getStackInSlot(0);
            if (!stack.isEmpty()) {
                if (player.isEntityAlive() && !(player instanceof EntityPlayerMP && ((EntityPlayerMP) player).hasDisconnected())) {
                    player.inventory.placeItemBackInInventory(saw.getWorld(), stack);
                } else {
                    player.dropItem(stack, false);
                }
            }
        }

        private class SlotOutput extends SlotItemHandler {
            private boolean enabled = true;

            public SlotOutput(final IItemHandler itemHandler, final int xPosition, final int yPosition) {
                super(itemHandler, 0, xPosition, yPosition);
            }

            void setEnabled(final boolean enabled) {
                this.enabled = enabled;
            }

            @Override
            public boolean isEnabled() {
                return enabled;
            }

            @Override
            public boolean isItemValid(final ItemStack stack) {
                return false;
            }

            @Override
            public boolean canTakeStack(final EntityPlayer player) {
                return !SerrConfig.immersiveMode && saw.isActive();
            }

            @Override
            public ItemStack onTake(final EntityPlayer thePlayer, final ItemStack stack) {
                final int crafted = resultInv.getAndResetCraftedCount();
                if (crafted > 0 && activeRecipe != null && !thePlayer.world.isRemote) {
                    final CuttingSawRecipe.Output output = activeRecipe.getOutput(activeOutput);
                    if (output != null) {
                        for (int i = 0; i < crafted; i++) {
                            for (final ItemStack extraStack : output.getExtra()) {
                                thePlayer.inventory.placeItemBackInInventory(thePlayer.world, extraStack.copy());
                            }
                        }
                    }
                }
                updateResult();
                return stack;
            }
        }

        private class InputInventory extends ItemStackHandler {
            InputInventory() {
                super(1);
            }

            @Override
            public void setSize(final int size) {
                Collections.fill(stacks, ItemStack.EMPTY);
            }

            @Override
            protected void onContentsChanged(final int slot) {
                final ItemStack stack = getStackInSlot(0);
                if (!stack.isEmpty() && (activeRecipe == null || !activeRecipe.getInput().apply(stack))) {
                    findNewRecipe:
                    {
                        for (final CuttingSawRecipe recipe : saw.getType().getRecipes()) {
                            if (recipe.getInput().apply(stack)) {
                                activeRecipe = recipe;
                                activeOutput = 0;
                                break findNewRecipe;
                            }
                        }
                        activeRecipe = null;
                        activeOutput = -1;
                    }
                }
                updateResult();
            }
        }

        private class ResultInventory extends ItemStackHandler {
            private int craftedCount = 0;

            public ResultInventory() {
                super(1);
            }

            public int getAndResetCraftedCount() {
                final int result = craftedCount;
                craftedCount = 0;
                return result;
            }

            @Override
            public void setSize(final int size) {
                Collections.fill(stacks, ItemStack.EMPTY);
            }

            @Override
            public ItemStack insertItem(final int slot, final ItemStack stack, final boolean simulate) {
                return ItemStack.EMPTY;
            }

            @Override
            public ItemStack extractItem(final int slot, final int amount, final boolean simulate) {
                if (activeRecipe == null || SerrConfig.immersiveMode) return ItemStack.EMPTY;
                final CuttingSawRecipe.Output output = activeRecipe.getOutput(activeOutput);
                if (output == null || !saw.isActive()) return ItemStack.EMPTY;
                final ItemStack outputStack = output.getItem();
                final int inputAmount = amount / outputStack.getCount();
                if (inputAmount <= 0) return ItemStack.EMPTY;

                final ItemStack trial = inputInv.extractItem(0, inputAmount, true);
                if (trial.isEmpty() || !activeRecipe.getInput().apply(trial)) return ItemStack.EMPTY;
                if (simulate) {
                    return ItemHandlerHelper.copyStackWithSize(outputStack, outputStack.getCount() * trial.getCount());
                }

                final int consumed = inputInv.extractItem(0, inputAmount, false).getCount();
                if (consumed <= 0) return ItemStack.EMPTY;
                craftedCount += consumed;
                final long now = saw.getWorld().getTotalWorldTime();
                if (now > lastSoundTime) {
                    lastSoundTime = now;
                    saw.getWorld().playSound(
                            null, saw.getPos(), saw.getType().getCutSound(), SoundCategory.BLOCKS, 0.75F, 1F);
                }
                return ItemHandlerHelper.copyStackWithSize(outputStack, outputStack.getCount() * consumed);
            }

            @Override
            public boolean isItemValid(final int slot, final ItemStack stack) {
                return false;
            }
        }
    }

    public static class Immersive extends ContainerCuttingSaw {
        private int lastSentStateClock = -1;

        public Immersive(final TileEntityCuttingSaw saw, final InventoryPlayer playerInv) {
            super(saw, playerInv);
            addSlotToContainer(new SlotFocus(8, 18));
            addPlayerInventory();
        }

        @Nullable
        @Override
        public CuttingSawRecipe getActiveRecipe() {
            return saw.getImmersiveRecipe();
        }

        @Override
        public int getActiveOutput() {
            final CuttingSawRecipe recipe = saw.getImmersiveRecipe();
            return recipe != null ? saw.getImmersiveOutput(recipe.getId()) : -1;
        }

        @Override
        public OutputDisplay checkOutputDisplay() {
            return saw.isActive() ? OutputDisplay.DISPLAY : OutputDisplay.BLOCKED;
        }

        @Override
        public void setOutputIndexFromGui(final int outputIndex) {
            if (!saw.getWorld().isRemote) {
                saw.setImmersiveOutputIndexFromGui(outputIndex);
            }
        }

        @Override
        public boolean canMergeSlot(final ItemStack stack, final Slot slot) {
            return slot.slotNumber != 0;
        }

        @Override
        public ItemStack transferStackInSlot(final EntityPlayer player, final int index) {
            if (index == 0) {
                saw.setImmersiveFocusFromGui(ItemStack.EMPTY);
            } else {
                final Slot slot = getSlot(index);
                if (slot.getHasStack()) {
                    saw.setImmersiveFocusFromGui(slot.getStack());
                }
            }
            return ItemStack.EMPTY;
        }

        @Override
        public void detectAndSendChanges() {
            super.detectAndSendChanges();
            final int now = saw.getUiStateClock();
            if (now > lastSentStateClock) {
                lastSentStateClock = now;
                final CuttingSawRecipe recipe = saw.getImmersiveRecipe();
                final SPacketSyncSawUi message = recipe != null
                        ? new SPacketSyncSawUi(windowId, now, recipe.getId(), saw.getImmersiveOutput(recipe.getId()))
                        : new SPacketSyncSawUi(windowId, now, "", -1);
                for (final IContainerListener listener : listeners) {
                    if (listener instanceof EntityPlayerMP) {
                        Serrated.PROXY.getNetwork().sendTo(message, (EntityPlayerMP) listener);
                    }
                }
            }
        }

        public class SlotFocus extends SlotItemHandler {
            @Nullable
            private ItemStack queuedChange = null;

            public SlotFocus(final int x, final int y) {
                super(EmptyHandler.INSTANCE, 0, x, y);
            }

            @Override
            public ItemStack getStack() {
                return saw.getImmersiveFocus();
            }

            @Override
            public void putStack(final ItemStack stack) {
                saw.setImmersiveFocusFromGui(stack);
                queuedChange = null;
            }

            @Override
            public ItemStack decrStackSize(final int amount) {
                return ItemStack.EMPTY;
            }

            @Override
            public boolean isItemValid(final ItemStack stack) {
                queuedChange = stack;
                return false;
            }

            @Override
            public boolean canTakeStack(final EntityPlayer player) {
                queuedChange = player.inventory.getItemStack();
                return false;
            }

            @Override
            public void onSlotChanged() {
                if (queuedChange != null) {
                    putStack(queuedChange);
                }
            }
        }
    }
}
