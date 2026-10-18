package st.evening.mc.rotnsaws.compat.crt;

import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.block.IBlockState;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import net.minecraft.item.ItemStack;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.SerrConst;
import st.evening.mc.rotnsaws.feature.saw.CuttingSawRecipe;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.List;

@ZenRegister
@ZenClass("mods." + SerrConst.MOD_ID + ".CuttingSawOutputBuilder")
public class CrTCuttingSawOutputBuilder {
    @ZenMethod
    public static CrTCuttingSawOutputBuilder newOutput(final IItemStack outputItem,
                                                       @Optional(valueLong = 0) final int duration,
                                                       @Optional @Nullable final IBlockState outputBlock) {
        return new CrTCuttingSawOutputBuilder(duration, outputItem, outputBlock);
    }

    private final int duration;
    private final IItemStack item;
    @Nullable
    private final IBlockState block;
    private final List<IItemStack> extra = new ArrayList<>();

    public CrTCuttingSawOutputBuilder(final int duration, final IItemStack item, @Nullable final IBlockState block) {
        this.duration = duration;
        this.item = item;
        this.block = block;
    }

    public CuttingSawRecipe.Output build() {
        final List<ItemStack> builtExtra = new ArrayList<>();
        for (final IItemStack extraStack : extra) {
            builtExtra.add(CraftTweakerMC.getItemStack(extraStack));
        }
        return new CuttingSawRecipe.Output(duration,
                CraftTweakerMC.getItemStack(item),
                CraftTweakerMC.getBlockState(block),
                builtExtra);
    }

    @ZenMethod
    public CrTCuttingSawOutputBuilder addExtra(final IItemStack stack) {
        extra.add(stack);
        return this;
    }
}
