package st.evening.mc.rotnsaws.compat.jei;

import mezz.jei.api.IGuiHelper;
import mezz.jei.api.gui.IDrawable;
import mezz.jei.api.gui.IDrawableBuilder;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.gui.IRecipeLayout;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.recipe.IRecipeCategory;
import net.minecraft.client.resources.I18n;
import net.minecraft.util.ResourceLocation;
import st.evening.mc.rotnsaws.SerrConfig;
import st.evening.mc.rotnsaws.SerrConst;
import st.evening.mc.rotnsaws.feature.saw.CuttingSawType;
import st.evening.mc.rotnsaws.init.SerrItems;

public class JeiCuttingSawRecipeCategory implements IRecipeCategory<JeiCuttingSawRecipeWrapper> {

    private final CuttingSawType type;
    private final IDrawable bg;

    public JeiCuttingSawRecipeCategory(final CuttingSawType type, final IGuiHelper guiHelper) {
        this.type = type;
        final IDrawableBuilder bgBuilder = guiHelper.drawableBuilder(
                        new ResourceLocation(SerrConst.MOD_ID, "textures/gui/jei/cutting_saw.png"), 0, 0, 66, 18)
                .setTextureSize(159, 18);
        if (SerrConfig.jeiExtraOutputRows > 0) {
            bgBuilder.addPadding(SerrJeiPlugin.vPadding, SerrJeiPlugin.vPadding, 0, 159 - 66);
        }
        this.bg = bgBuilder.build();
    }

    @Override
    public String getUid() {
        return type.getJeiUid();
    }

    @Override
    public String getTitle() {
        return I18n.format(type.getContainerTranslationKey());
    }

    @Override
    public String getModName() {
        return I18n.format(SerrItems.creativeTab.getTranslationKey());
    }

    @Override
    public IDrawable getBackground() {
        return bg;
    }

    @Override
    public void setRecipe(final IRecipeLayout recipeLayout, final JeiCuttingSawRecipeWrapper recipeWrapper,
                          final IIngredients ingredients) {
        final IGuiItemStackGroup itemGroup = recipeLayout.getItemStacks();
        itemGroup.init(0, true, 0, SerrJeiPlugin.vPadding);
        itemGroup.init(1, false, 48, SerrJeiPlugin.vPadding);
        recipeWrapper.initExtraSlots(itemGroup);
        itemGroup.set(ingredients);
    }

}
