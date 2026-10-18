package st.evening.mc.rotnsaws.util;

import com.mojang.authlib.GameProfile;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.World;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.util.FakePlayerFactory;
import org.jspecify.annotations.Nullable;

import java.util.UUID;

// only used as a fallback if BWM's saw damage source is not available
public class SawDamageSource extends EntityDamageSource {
    private static final GameProfile FAKE_PROFILE =
            new GameProfile(UUID.fromString("382aeb30-d1c6-4a93-bf2f-af2744caf4af"), "[Cutting Saw]");

    private final boolean chop;

    public SawDamageSource(final World world, final boolean chop) {
        super("player", world instanceof WorldServer ? FakePlayerFactory.get((WorldServer) world, FAKE_PROFILE) : null);
        this.chop = chop;
    }

    public boolean isDifficultyScaled() {
        return false;
    }

    public boolean isUnblockable() {
        return false;
    }

    public boolean getIsThornsDamage() {
        return false;
    }

    @Nullable
    public Entity getTrueSource() {
        return null;
    }

    public ITextComponent getDeathMessage(EntityLivingBase killed) {
        return new TextComponentTranslation(
                chop ? "death.attack.chopping_block" : "death.attack.cutting_saw", killed.getDisplayName());
    }

    public boolean equals(Object o) {
        return o instanceof SawDamageSource && chop == ((SawDamageSource) o).chop;
    }

    @Override
    public int hashCode() {
        return Boolean.hashCode(chop);
    }
}
