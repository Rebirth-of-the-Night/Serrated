package st.evening.mc.rotnsaws.compat.bwm;

import betterwithmods.api.BWMAPI;
import betterwithmods.api.tile.IMechanicalPower;
import net.minecraft.block.Block;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

class BwmMechanicalHandlerImpl implements BwmMechanicalHandler, IMechanicalPower {
    private final Host host;

    private int power = 0;

    BwmMechanicalHandlerImpl(final Host host) {
        this.host = host;
    }

    @Override
    public Block getBlock() {
        return host.getBlock();
    }

    @Override
    public World getBlockWorld() {
        return host.getBlockWorld();
    }

    @Override
    public BlockPos getBlockPos() {
        return host.getBlockPos();
    }

    @Override
    public int getPower() {
        return power;
    }

    @Override
    public boolean updatePower() {
        final int /* my power! */ newPower = calculateInput();
        if (power == newPower) return false;
        power = newPower;
        return true;
    }

    @Override
    public boolean setPowerFromSync(final int power) {
        if (this.power == power) return false;
        this.power = power;
        return true;
    }

    @Override
    public int getMechanicalOutput(final EnumFacing face) {
        return -1;
    }

    @Override
    public int getMechanicalInput(final EnumFacing face) {
        return host.canConnectPower(face)
                ? BWMAPI.IMPLEMENTATION.getPowerOutput(getBlockWorld(), getBlockPos().offset(face), face.getOpposite())
                : 0;
    }

    @Override
    public int getMinimumInput(final EnumFacing face) {
        return host.getMinimumInput();
    }

    @Override
    public int getMaximumInput(final EnumFacing face) {
        return host.getMaximumInput();
    }

    @Override
    public void writeToNbt(final NBTTagCompound tag) {
        tag.setInteger("power", power);
    }

    @Override
    public void readFromNbt(final NBTTagCompound tag) {
        power = tag.getInteger("power");
    }

    @Override
    public boolean overpowerChance() {
        return true;
    }
}
