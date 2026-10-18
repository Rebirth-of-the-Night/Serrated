package st.evening.mc.rotnsaws.compat.crt;

import crafttweaker.CraftTweakerAPI;
import crafttweaker.IAction;
import crafttweaker.annotations.ZenRegister;
import crafttweaker.api.block.IBlockState;
import crafttweaker.api.block.IBlockStateMatcher;
import crafttweaker.api.item.IIngredient;
import crafttweaker.api.item.IItemStack;
import crafttweaker.api.minecraft.CraftTweakerMC;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.SerrConst;
import st.evening.mc.rotnsaws.feature.saw.CuttingSawRecipe;
import st.evening.mc.rotnsaws.feature.saw.CuttingSawType;
import stanhebben.zenscript.annotations.Optional;
import stanhebben.zenscript.annotations.ZenClass;
import stanhebben.zenscript.annotations.ZenMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@ZenRegister
@ZenClass("mods." + SerrConst.MOD_ID + ".CuttingSawRecipeBuilder")
public class CrTCuttingSawRecipeBuilder {
    @ZenMethod
    public static CrTCuttingSawRecipeBuilder newRecipe(final IIngredient inputItem,
                                                       @Optional @Nullable final IBlockStateMatcher inputBlock,
                                                       @Optional final boolean blockOnly) {
        return new CrTCuttingSawRecipeBuilder(inputItem, inputBlock, blockOnly);
    }

    private final IIngredient input;
    @Nullable
    private final IBlockStateMatcher inputBlock;
    private final boolean blockOnly;
    private final List<CrTCuttingSawOutputBuilder> outputs = new ArrayList<>();

    public CrTCuttingSawRecipeBuilder(final IIngredient input, @Nullable final IBlockStateMatcher inputBlock,
                                      final boolean blockOnly) {
        this.input = input;
        this.inputBlock = inputBlock;
        this.blockOnly = blockOnly;
    }

    @ZenMethod
    public CrTCuttingSawRecipeBuilder addOutput(final IItemStack outputItem,
                                                @Optional(valueLong = 0) final int duration,
                                                @Optional @Nullable final IBlockState outputBlock) {
        outputs.add(new CrTCuttingSawOutputBuilder(duration, outputItem, outputBlock));
        return this;
    }

    @ZenMethod
    public CrTCuttingSawRecipeBuilder addOutput(final CrTCuttingSawOutputBuilder builder) {
        outputs.add(builder);
        return this;
    }

    @ZenMethod
    public CrTCuttingSawRecipeBuilder addToSaw(final String id) {
        addToRegistry(CuttingSawType.SAW, id);
        return this;
    }

    @ZenMethod
    public CrTCuttingSawRecipeBuilder addToStonecutter(final String id) {
        addToRegistry(CuttingSawType.STONECUTTER, id);
        return this;
    }

    private CuttingSawRecipe build(final String id) {
        final List<CuttingSawRecipe.Output> outputs = new ArrayList<>();
        for (final CrTCuttingSawOutputBuilder builder : this.outputs) {
            outputs.add(builder.build());
        }
        return new CuttingSawRecipe(id,
                CraftTweakerMC.getIngredient(input),
                inputBlock != null ? getBlockMatcher(inputBlock) : null,
                blockOnly, outputs);
    }

    private void addToRegistry(final CuttingSawType type, final String id) {
        if (outputs.isEmpty()) {
            throw new IllegalArgumentException("Recipe has no outputs: " + id);
        }
        CraftTweakerAPI.apply(new IAction() {
            @Override
            public void apply() {
                type.addRecipe(build(id));
            }

            @Override
            public String describe() {
                return "Adding " + type + " recipe: " + id;
            }
        });
    }

    private static Predicate<net.minecraft.block.state.IBlockState> getBlockMatcher(final IBlockStateMatcher matcher) {
        return state -> matcher.matches(CraftTweakerMC.getBlockState(state));
    }
}
