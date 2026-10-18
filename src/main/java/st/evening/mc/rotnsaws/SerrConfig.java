package st.evening.mc.rotnsaws;

import net.minecraftforge.common.config.Config;

@Config(modid = SerrConst.MOD_ID)
public class SerrConfig {
    @Config.Comment({
            "Amount of BWM mechanical power required for cutting saws to work.",
            "Set to zero to disable the power requirement."
    })
    @Config.RangeInt(min = 0)
    public static int powerRequired = 0;

    @Config.Comment("Maximum amount of BWM mechanical power a cutting saw can handle before it overpowers.")
    @Config.RangeInt(min = 0)
    public static int powerMaximum = 1;

    @Config.Comment("Do cutting saws do in-world crafting?")
    @Config.RequiresWorldRestart
    public static boolean immersiveMode = false;

    @Config.Comment({
            "How much damage do cutting saws do to entities coming into contact with the blade?",
            "Set to zero to disable saw damage."
    })
    @Config.RangeDouble(min = 0D)
    public static double contactDamage = 4D;

    @Config.Comment({
            "How much additional damage do cutting saws do to entities when a BWM chopping block is in place?",
            "Only works if contactDamage is greater than zero!",
            "Set to zero to disable the chopping block interaction."
    })
    @Config.RangeDouble(min = 0)
    public static double chopBonusDamage = 8D;

    @Config.Comment({
            "Can cutting saws be rotated by shift-right-clicking them?",
            "This is useful for when immersive mode is enabled and there aren't any modded wrenches available."
    })
    public static boolean rotateWithHand = true;

    @Config.Comment("Do cutting saws constantly play a sound while active?")
    @Config.RequiresWorldRestart
    public static boolean activeSound = false;

    @Config.Comment("Do cutting saws play a sound while processing a recipe?")
    @Config.RequiresWorldRestart
    public static boolean workingSound = true;

    @Config.Comment("Do cutting saws play a sound when a recipe finishes?")
    public static boolean finishedSound = true;

    @Config.Comment({
            "The number of rows of extra cutting saw outputs (4 per row) that are shown in JEI.",
            "If there isn't enough space for all of a recipe's outputs, they'll render out-of-bounds!"
    })
    @Config.RangeInt(min = 0)
    @Config.RequiresMcRestart
    public static int jeiExtraOutputRows = 1;
}
