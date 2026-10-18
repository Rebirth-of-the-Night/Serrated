package st.evening.mc.rotnsaws.compat.bwm;

import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public interface BwmMechanicalHandler {
    int getPower();

    boolean updatePower();

    boolean setPowerFromSync(int power);

    void writeToNbt(NBTTagCompound tag);

    void readFromNbt(NBTTagCompound tag);

    interface Host {
        boolean canConnectPower(final EnumFacing from);

        int getMinimumInput();

        int getMaximumInput();

        World getBlockWorld();

        BlockPos getBlockPos();

        Block getBlock();
    }

    enum Noop implements BwmMechanicalHandler {
        INSTANCE;

        @Override
        public int getPower() {
            return 0;
        }

        @Override
        public boolean updatePower() {
            return false;
        }

        @Override
        public boolean setPowerFromSync(final int power) {
            return false;
        }

        @Override
        public void writeToNbt(final NBTTagCompound tag) {}

        @Override
        public void readFromNbt(final NBTTagCompound tag) {}
    }
}
