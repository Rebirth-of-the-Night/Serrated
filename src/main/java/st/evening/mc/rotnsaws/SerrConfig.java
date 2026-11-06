package st.evening.mc.rotnsaws;

import net.minecraftforge.common.config.Config;
import st.evening.mc.rotnsaws.feature.saw.CuttingSawType;

@Config(modid = SerrConst.MOD_ID)
public class SerrConfig {
    @Config.Comment("Config for the saw.")
    public static SawBlockConfig saw = new SawBlockConfig();

    @Config.Comment("Config for the stonecutter.")
    public static SawBlockConfig stonecutter = new SawBlockConfig();

    public static SawBlockConfig getSawConfig(final CuttingSawType type) {
        switch (type) {
            case SAW:
                return saw;
            case STONECUTTER:
                return stonecutter;
        }
        throw new IllegalStateException(type.toString());
    }

    public static class SawBlockConfig {
        @Config.Comment({
                "Amount of BWM mechanical power required for the cutting saw to work.",
                "Set to zero to disable the power requirement."
        })
        @Config.RangeInt(min = 0)
        public int powerRequired = 0;

        @Config.Comment("Maximum amount of BWM mechanical power the cutting saw can handle before it overpowers.")
        @Config.RangeInt(min = 0)
        public int powerMaximum = 1;

        @Config.Comment("Does the cutting saw do in-world crafting?")
        @Config.RequiresWorldRestart
        public boolean immersiveMode = false;

        @Config.Comment({
                "How much damage does the cutting saw do to entities coming into contact with the blade?",
                "Set to zero to disable saw damage."
        })
        @Config.RangeDouble(min = 0D)
        public double contactDamage = 4D;

        @Config.Comment({
                "How much additional damage does the cutting saw do to entities when a BWM chopping block is in place?",
                "Only works if contactDamage is greater than zero!",
                "Set to zero to disable the chopping block interaction."
        })
        @Config.RangeDouble(min = 0)
        public double chopBonusDamage = 8D;

        @Config.Comment({
                "Can the cutting saw be rotated by right-clicking it with an empty hand while sneaking?",
                "This is useful for when immersive mode is enabled and there aren't any modded wrenches available."
        })
        public boolean rotateWithHand = true;

        @Config.Comment("Does the cutting saw constantly play a sound while active?")
        @Config.RequiresWorldRestart
        public boolean activeSound = false;

        @Config.Comment("Does the cutting saw play a sound while processing a recipe?")
        @Config.RequiresWorldRestart
        public boolean workingSound = true;

        @Config.Comment("Does the cutting saw play a sound when a recipe finishes?")
        public boolean finishedSound = true;

        @Config.Comment({
                "The number of rows of extra outputs (4 per row) that are shown in the cutting saw's JEI pages.",
                "If there isn't enough space for all of a recipe's outputs, they'll render out-of-bounds!"
        })
        @Config.RangeInt(min = 0)
        @Config.RequiresMcRestart
        public int jeiExtraOutputRows = 1;
    }
}
