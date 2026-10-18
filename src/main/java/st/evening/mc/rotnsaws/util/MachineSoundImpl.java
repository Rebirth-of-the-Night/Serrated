package st.evening.mc.rotnsaws.util;

import net.minecraft.client.audio.ITickableSound;
import net.minecraft.client.audio.PositionedSound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MachineSoundImpl extends PositionedSound implements MachineSound, ITickableSound {
    private final TileEntity owner;
    private final float maxVolume;

    private float targetVolume = 0;
    private boolean dead = false;
    private int age = 0;

    public MachineSoundImpl(final TileEntity owner, final SoundEvent sound, final float volume) {
        super(sound, SoundCategory.BLOCKS);
        this.owner = owner;
        this.maxVolume = volume;
        this.volume = 0.01F;
        this.repeat = true;
        this.repeatDelay = 0;
    }

    @Override
    public void setPlaying(final boolean playing) {
        targetVolume = playing ? maxVolume : 0F;
    }

    @Override
    public boolean isDonePlaying() {
        return dead;
    }

    @Override
    public void update() {
        if (dead) return;
        ++age;
        if (age % 20 == 4) {
            if (owner.isInvalid()) {
                dead = true;
                return;
            }
        }
        final float diff = targetVolume - volume;
        if (Math.abs(diff) > 0.01F) {
            volume += diff * 0.33F;
            final BlockPos pos = owner.getPos();
            this.xPosF = pos.getX() + 0.5F;
            this.yPosF = pos.getY() + 0.5F;
            this.zPosF = pos.getZ() + 0.5F;
        } else {
            volume = targetVolume;
        }
    }
}
