package st.evening.mc.rotnsaws.compat.jei;

import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import mezz.jei.api.gui.IGuiItemStackGroup;
import mezz.jei.api.ingredients.IIngredients;
import mezz.jei.api.ingredients.VanillaTypes;
import mezz.jei.api.recipe.IRecipeWrapper;
import mezz.jei.api.recipe.IStackHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.resources.I18n;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.SerrConfig;
import st.evening.mc.rotnsaws.SerrConst;
import st.evening.mc.rotnsaws.feature.saw.CuttingSawRecipe;

import javax.vecmath.Point2i;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JeiCuttingSawRecipeWrapper implements IRecipeWrapper {
    private static final int EXTRA_X = 87, EXTRA_X_LIMIT = 159;
    private static final Int2ObjectMap<@Nullable List<Point2i>> slotPosCache = new Int2ObjectOpenHashMap<>();

    private static List<Point2i> getSlotPositions(final int count) {
        final List<Point2i> cached = slotPosCache.get(count - 1);
        if (cached != null) return cached;

        final List<Point2i> positions = new ArrayList<>();
        final int rowCount = MathHelper.ceil(count / 4F), cellCount = rowCount * 4;
        int x = EXTRA_X;
        int y = 9 - (18 * rowCount) / 2;
        for (int i = 0; i < cellCount; i++) {
            positions.add(new Point2i(x, y));
            x += 18;
            if (x >= EXTRA_X_LIMIT) {
                x = EXTRA_X;
                y += 18;
            }
        }

        for (int i = 4; i > 0; i--) {
            slotPosCache.put(cellCount - i, positions);
        }
        return positions;
    }

    private final CuttingSawRecipe recipe;
    private final CuttingSawRecipe.Output output;
    private final IStackHelper stackHelper;

    public JeiCuttingSawRecipeWrapper(final CuttingSawRecipe recipe, final CuttingSawRecipe.Output output,
                                      final IStackHelper stackHelper) {
        this.recipe = recipe;
        this.output = output;
        this.stackHelper = stackHelper;
    }

    @Override
    public void getIngredients(final IIngredients ingredients) {
        ingredients.setInputLists(VanillaTypes.ITEM,
                Collections.singletonList(stackHelper.toItemStackList(recipe.getInput())));
        final List<List<ItemStack>> outputLists = new ArrayList<>();
        outputLists.add(stackHelper.toItemStackList(output.getItem()));
        for (final ItemStack extraStack : output.getExtra()) {
            outputLists.add(stackHelper.toItemStackList(extraStack));
        }
        ingredients.setOutputLists(VanillaTypes.ITEM, outputLists);
    }

    public void initExtraSlots(final IGuiItemStackGroup itemGroup) {
        if (output.getExtra().isEmpty()) return;
        final int count = output.getExtra().size();
        final List<Point2i> positions = getSlotPositions(count);
        for (int i = 0; i < count; i++) {
            final Point2i pos = positions.get(i);
            itemGroup.init(2 + i, false, pos.x, SerrJeiPlugin.vPadding + pos.y);
        }
    }

    @Override
    public void drawInfo(final Minecraft mc, final int recipeWidth, final int recipeHeight,
                         final int mouseX, final int mouseY) {
        if (!output.getExtra().isEmpty()) {
            Gui.drawModalRectWithCustomSizedTexture(66, SerrJeiPlugin.vPadding, 66, 0, 159 - 66, 18, 159, 18);
            final int count = output.getExtra().size();
            final List<Point2i> positions = getSlotPositions(count);
            for (int i = 0; i < count; i++) {
                final Point2i pos = positions.get(i);
                Gui.drawModalRectWithCustomSizedTexture(pos.x, SerrJeiPlugin.vPadding + pos.y, 48, 0, 18, 18, 159, 18);
            }
        }
        if (SerrConfig.immersiveMode && recipe.isBlockOnly()) {
            mc.fontRenderer.drawString(I18n.format("tooltip." + SerrConst.MOD_ID + ".in_world_only"),
                    0, SerrJeiPlugin.vPadding + 21, 0xDE0000);
        }
    }
}
