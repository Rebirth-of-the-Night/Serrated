package st.evening.mc.rotnsaws.compat.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.IJeiRuntime;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.IModRegistry;
import mezz.jei.api.JEIPlugin;
import mezz.jei.api.recipe.IRecipeCategoryRegistration;
import mezz.jei.api.recipe.IStackHelper;
import net.minecraft.item.ItemStack;
import st.evening.mc.rotnsaws.SerrConfig;
import st.evening.mc.rotnsaws.feature.saw.CuttingSawRecipe;
import st.evening.mc.rotnsaws.feature.saw.CuttingSawType;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@JEIPlugin
public class SerrJeiPlugin implements IModPlugin {
    private static int computeVerticalPadding(final CuttingSawType type) {
        return Math.max((SerrConfig.getSawConfig(type).jeiExtraOutputRows - 1) * 9, 4);
    }

    @Override
    public void registerCategories(final IRecipeCategoryRegistration registry) {
        final IGuiHelper guiHelper = registry.getJeiHelpers().getGuiHelper();
        for (final CuttingSawType type : CuttingSawType.values()) {
            registry.addRecipeCategories(
                    new JeiCuttingSawRecipeCategory(type, computeVerticalPadding(type), guiHelper));
        }
    }

    @Override
    public void register(final IModRegistry registry) {
        final IStackHelper stackHelper = registry.getJeiHelpers().getStackHelper();
        for (final CuttingSawType type : CuttingSawType.values()) {
            final String uid = type.getJeiUid();
            final int vPadding = computeVerticalPadding(type);
            final List<JeiCuttingSawRecipeWrapper> wrappers = new ArrayList<>();
            for (final CuttingSawRecipe recipe : type.getRecipes()) {
                for (final CuttingSawRecipe.Output output : recipe.getOutputs()) {
                    wrappers.add(new JeiCuttingSawRecipeWrapper(type, recipe, output, vPadding, stackHelper));
                }
            }
            registry.addRecipes(wrappers, uid);
            registry.addRecipeCatalyst(new ItemStack(type.getBlock()), uid);
        }
    }

    @Override
    public void onRuntimeAvailable(final IJeiRuntime jeiRuntime) {
        SerrJeiCompat.setRecipeGuiHandler(
                uid -> jeiRuntime.getRecipesGui().showCategories(Collections.singletonList(uid)));
    }
}
