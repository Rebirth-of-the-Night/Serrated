package st.evening.mc.rotnsaws.feature.saw;

import net.minecraft.client.Minecraft;
import net.minecraft.client.audio.PositionedSoundRecord;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.resources.I18n;
import net.minecraft.init.SoundEvents;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.text.TextFormatting;
import net.minecraftforge.client.event.RenderTooltipEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.lwjgl.input.Mouse;
import st.evening.mc.rotnsaws.SerrConst;
import st.evening.mc.rotnsaws.Serrated;
import st.evening.mc.rotnsaws.compat.jei.SerrJeiCompat;
import st.evening.mc.rotnsaws.network.CPacketSetSawOutput;
import st.evening.mc.rotnsaws.util.SerrUtils;

import java.io.IOException;
import java.util.List;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = SerrConst.MOD_ID, value = Side.CLIENT)
public class GuiCuttingSaw extends GuiContainer {

    private static final ResourceLocation BG_LOCATION =
            new ResourceLocation(SerrConst.MOD_ID, "textures/gui/cutting_saw.png");

    private static final int RECIPE_X_CNT = 8, RECIPE_Y_CNT = 3, RECIPE_PAGE_CNT = RECIPE_X_CNT * RECIPE_Y_CNT;
    private static final int RECIPE_X = 30, RECIPE_DX = 16, RECIPE_X_LIMIT = RECIPE_X + RECIPE_DX * RECIPE_X_CNT;
    private static final int RECIPE_Y = 18, RECIPE_DY = 18, RECIPE_Y_LIMIT = RECIPE_Y + RECIPE_DY * RECIPE_Y_CNT;

    private static final int SCROLL_BAR_X = 160, SCROLL_BAR_Y = 18, SCROLL_BAR_WIDTH = 8, SCROLL_BAR_HEIGHT = 54;
    private static final int SCROLL_THUMB_HEIGHT = 13, SCROLL_THUMB_OFFSET = SCROLL_THUMB_HEIGHT / 2;
    private static final int SCROLL_THUMB_AREA_HEIGHT = SCROLL_BAR_HEIGHT - SCROLL_THUMB_HEIGHT;
    private static final int SCROLL_AREA_TOP = SCROLL_BAR_Y + SCROLL_THUMB_OFFSET;
    // note thumb_offset * 2 != thumb_height because of truncation
    private static final int SCROLL_AREA_HEIGHT = SCROLL_BAR_HEIGHT - SCROLL_THUMB_OFFSET * 2;

    private static final int SCROLL_WHEEL_AREA_WIDTH = RECIPE_X_LIMIT - RECIPE_X + SCROLL_BAR_WIDTH + 2;
    private static final int SCROLL_WHEEL_AREA_HEIGHT = RECIPE_Y_LIMIT - RECIPE_Y;

    private static int getScrollableRowCount(final int entryCount) {
        return MathHelper.ceil(entryCount / (float) RECIPE_X_CNT) - RECIPE_Y_CNT;
    }

    private final ContainerCuttingSaw container;

    private float scrollPos = 0F;
    private boolean hasScrollBar = true, clickScrolling = false, overWheelScrollArea = false;
    private ContainerCuttingSaw.OutputDisplay outputDisplay = ContainerCuttingSaw.OutputDisplay.BLOCKED;
    private int hoveredIndex = -1;

    public GuiCuttingSaw(final ContainerCuttingSaw container) {
        super(container);
        this.container = container;
        this.xSize = 176;
        this.ySize = 170;
    }

    @Override
    public void drawScreen(final int mouseX, final int mouseY, final float partialTicks) {
        drawDefaultBackground();
        super.drawScreen(mouseX, mouseY, partialTicks);
        renderHoveredToolTip(mouseX, mouseY);
    }

    @Override
    protected void drawGuiContainerBackgroundLayer(final float partialTicks, final int mouseX, final int mouseY) {
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.pushMatrix();
        GlStateManager.translate(guiLeft, guiTop, 0F);

        mc.getTextureManager().bindTexture(BG_LOCATION);
        drawTexturedModalRect(0, 0, 0, 0, xSize, ySize);

        outputDisplay = container.checkOutputDisplay();
        switch (outputDisplay) {
            case SLOT:
                drawTexturedModalRect(7, 35, 176, 18, 18, 38);
                break;
            case BLOCKED:
                drawTexturedModalRect(7, 35, 194, 18, 18, 38);
                break;
            case DISPLAY:
                drawTexturedModalRect(7, 35, 212, 18, 18, 38);
                break;
        }

        final CuttingSawRecipe recipe = container.getActiveRecipe();
        if (recipe == null) {
            scrollPos = 0F;
            hasScrollBar = false;
            hoveredIndex = -1;
        } else {
            final List<CuttingSawRecipe.Output> outputs = recipe.getOutputs();
            hasScrollBar = outputs.size() > RECIPE_PAGE_CNT;
            final int scrollOffset;
            if (hasScrollBar) {
                overWheelScrollArea = isPointInRegion(
                        RECIPE_X, RECIPE_Y, SCROLL_WHEEL_AREA_WIDTH, SCROLL_WHEEL_AREA_HEIGHT, mouseX, mouseY);
                scrollOffset = RECIPE_X_CNT * Math.round(scrollPos * getScrollableRowCount(outputs.size()));
            } else {
                scrollOffset = 0;
            }

            if (hasScrollBar) {
                drawTexturedModalRect(SCROLL_BAR_X, SCROLL_BAR_Y + Math.round(scrollPos * SCROLL_THUMB_AREA_HEIGHT),
                        176, 56, SCROLL_BAR_WIDTH, SCROLL_THUMB_HEIGHT);
            }

            int rx = RECIPE_X, ry = RECIPE_Y;
            final int iLimit = Math.min(outputs.size(), scrollOffset + RECIPE_PAGE_CNT);
            final int activeOutputIndex = container.getActiveOutput();
            for (int i = scrollOffset; i < iLimit; i++) {
                if (i == activeOutputIndex) {
                    drawTexturedModalRect(rx, ry, 192, 0, RECIPE_DX, RECIPE_DY);
                } else {
                    drawTexturedModalRect(rx, ry, 176, 0, RECIPE_DX, RECIPE_DY);
                }
                rx += RECIPE_DX;
                if (rx >= RECIPE_X_LIMIT) {
                    rx = RECIPE_X;
                    ry += RECIPE_DY;
                }
            }

            RenderHelper.enableGUIStandardItemLighting();
            itemRender.zLevel = 100F;
            rx = RECIPE_X;
            ry = RECIPE_Y + 1;
            for (int i = scrollOffset; i < iLimit; i++) {
                drawOutputIcon(outputs.get(i), rx, ry);
                rx += RECIPE_DX;
                if (rx >= RECIPE_X_LIMIT) {
                    rx = RECIPE_X;
                    ry += RECIPE_DY;
                }
            }

            if (outputDisplay == ContainerCuttingSaw.OutputDisplay.DISPLAY) {
                if (activeOutputIndex >= 0 && activeOutputIndex < outputs.size()) {
                    drawOutputIcon(outputs.get(activeOutputIndex), 8, 56);
                }
            }
            itemRender.zLevel = 0F;
            RenderHelper.disableStandardItemLighting();

            setHoveredIndex:
            {
                findHover:
                {
                    final int imx = mouseX - guiLeft;
                    if (imx < RECIPE_X || imx >= RECIPE_X_LIMIT) break findHover;
                    final int imy = mouseY - guiTop;
                    if (imy < RECIPE_Y || imy >= RECIPE_Y_LIMIT) break findHover;
                    final int index = scrollOffset
                            + RECIPE_X_CNT * ((imy - RECIPE_Y) / RECIPE_DY) + (imx - RECIPE_X) / RECIPE_DX;
                    if (index >= outputs.size()) break findHover;
                    hoveredIndex = index;
                    break setHoveredIndex;
                }
                hoveredIndex = -1;
            }
        }
        GlStateManager.popMatrix();
    }

    private void drawOutputIcon(final CuttingSawRecipe.Output output, final int x, final int y) {
        itemRender.renderItemAndEffectIntoGUI(output.getItem(), x, y);
        itemRender.renderItemOverlayIntoGUI(fontRenderer, output.getItem(), x, y, null);
        if (!output.getExtra().isEmpty()) {
            mc.getTextureManager().bindTexture(BG_LOCATION);
            GlStateManager.disableDepth();
            drawTexturedModalRect(x, y, 176, 69, 16, 16);
            GlStateManager.enableDepth();
        }
    }

    @Override
    protected void drawGuiContainerForegroundLayer(final int mouseX, final int mouseY) {
        GlStateManager.disableLighting();
        final TileEntityCuttingSaw saw = container.getSaw();
        fontRenderer.drawString(I18n.format(saw.getType().getContainerTranslationKey()), 8, 6, 0x404040);
        fontRenderer.drawString(
                container.getPlayerInventory().getDisplayName().getUnformattedText(), 8, ySize - 96 + 2, 0x404040);
    }

    @Override
    protected void renderHoveredToolTip(final int mouseX, final int mouseY) {
        if (mc.player.inventory.getItemStack().isEmpty()) {
            if (hoveredIndex >= 0) {
                final CuttingSawRecipe recipe = container.getActiveRecipe();
                if (recipe != null) {
                    final CuttingSawRecipe.Output output = recipe.getOutput(hoveredIndex);
                    if (output != null) {
                        drawOutputTooltip(output, mouseX, mouseY);
                    }
                }
                return;
            } else if (outputDisplay == ContainerCuttingSaw.OutputDisplay.BLOCKED
                    && isPointInRegion(8, 56, 16, 16, mouseX, mouseY)) {
                drawHoveringText(TextFormatting.RED + I18n.format("tooltip." + SerrConst.MOD_ID + ".needs_power"),
                        mouseX, mouseY);
                return;
            } else if (outputDisplay == ContainerCuttingSaw.OutputDisplay.DISPLAY
                    && isPointInRegion(8, 56, 16, 16, mouseX, mouseY)) {
                final CuttingSawRecipe recipe = container.getActiveRecipe();
                if (recipe != null) {
                    final CuttingSawRecipe.Output output = recipe.getOutput(container.getActiveOutput());
                    if (output != null) {
                        drawOutputTooltip(output, mouseX, mouseY);
                    }
                }
                return;
            } else if (SerrJeiCompat.hasRecipeGui() && isPointInRegion(7, 35, 18, 20, mouseX, mouseY)) {
                drawHoveringText(I18n.format("jei.tooltip.show.recipes"), mouseX, mouseY);
                return;
            }
        }
        super.renderHoveredToolTip(mouseX, mouseY);
    }

    private void drawOutputTooltip(final CuttingSawRecipe.Output output, final int mouseX, final int mouseY) {
        final List<String> tooltip = getItemToolTip(output.getItem());
        final List<ItemStack> extra = output.getExtra();
        if (!extra.isEmpty()) {
            tooltipIconsToDraw = extra;
            tooltip.add("");
            tooltip.add(I18n.format("tooltip." + SerrConst.MOD_ID + ".extra_outputs"));
            final String gap = StringUtils.repeat(
                    ' ', SerrUtils.ceilDiv(10, fontRenderer.getCharWidth(' ')));
            for (final ItemStack extraStack : extra) {
                if (extraStack.getCount() > 1) {
                    tooltip.add(gap + TextFormatting.GRAY + extraStack.getCount() + " x "
                            + extraStack.getDisplayName());
                } else {
                    tooltip.add(gap + TextFormatting.GRAY + extraStack.getDisplayName());
                }
            }
        }
        drawHoveringText(tooltip, mouseX, mouseY);
        tooltipIconsToDraw = null;
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        if (!hasScrollBar || !overWheelScrollArea) return;
        final int delta = Mouse.getEventDWheel();
        if (delta == 0) return;
        final CuttingSawRecipe recipe = container.getActiveRecipe();
        if (recipe == null) return; // shouldn't happen but might as well check
        scrollPos = MathHelper.clamp(
                scrollPos - Integer.signum(delta) / (float) getScrollableRowCount(recipe.getOutputs().size()), 0F, 1F);
    }

    @Override
    protected void mouseClicked(final int mouseX, final int mouseY, final int mouseButton) throws IOException {
        if (hoveredIndex >= 0) {
            if (mouseButton != 0) return;
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1F));
            container.setOutputIndexFromGui(hoveredIndex);
            Serrated.PROXY.getNetwork().sendToServer(new CPacketSetSawOutput(container.windowId, hoveredIndex));
        } else if (isPointInRegion(SCROLL_BAR_X, SCROLL_BAR_Y, SCROLL_BAR_WIDTH, SCROLL_BAR_HEIGHT, mouseX, mouseY)) {
            if (mouseButton != 0 || !hasScrollBar) return;
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1F));
            clickScrolling = true;
        } else if (isPointInRegion(7, 35, 18, 20, mouseX, mouseY)) {
            if (mouseButton != 0 || !SerrJeiCompat.hasRecipeGui()) return;
            mc.getSoundHandler().playSound(PositionedSoundRecord.getMasterRecord(SoundEvents.UI_BUTTON_CLICK, 1F));
            SerrJeiCompat.openRecipeGui(container.getSaw().getType().getJeiUid());
        } else {
            super.mouseClicked(mouseX, mouseY, mouseButton);
        }
    }

    @Override
    protected void mouseClickMove(final int mouseX, final int mouseY, final int clickedMouseButton,
                                  final long timeSinceLastClick) {
        if (hasScrollBar && clickScrolling) {
            scrollPos = MathHelper.clamp((mouseY - guiTop - SCROLL_AREA_TOP) / (float) SCROLL_AREA_HEIGHT, 0F, 1F);
        } else {
            super.mouseClickMove(mouseX, mouseY, clickedMouseButton, timeSinceLastClick);
        }
    }

    @Override
    protected void mouseReleased(final int mouseX, final int mouseY, final int state) {
        if (clickScrolling) {
            clickScrolling = false;
        } else {
            super.mouseReleased(mouseX, mouseY, state);
        }
    }

    @Nullable
    private static List<ItemStack> tooltipIconsToDraw = null;

    @SubscribeEvent
    public static void onDrawTooltip(RenderTooltipEvent.PostBackground event) {
        if (tooltipIconsToDraw == null || tooltipIconsToDraw.isEmpty()) return;
        final int dy = event.getFontRenderer().FONT_HEIGHT + 1;
        GlStateManager.color(1F, 1F, 1F, 1F);
        GlStateManager.enableDepth();
        GlStateManager.enableLighting();
        RenderHelper.enableGUIStandardItemLighting();
        GlStateManager.pushMatrix();
        GlStateManager.translate(
                event.getX() - 0.5F, event.getY() + event.getHeight() - tooltipIconsToDraw.size() * dy + 0.5F, 0F);
        final Minecraft mc = Minecraft.getMinecraft();
        final RenderItem itemRenderer = mc.getRenderItem();
        itemRenderer.zLevel = 500F;
        final float scaleFactor = dy / 16F;
        for (int i = 0; i < tooltipIconsToDraw.size(); i++) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(0F, i * dy, 0F);
            GlStateManager.scale(scaleFactor, scaleFactor, scaleFactor);
            itemRenderer.renderItemAndEffectIntoGUI(mc.player, tooltipIconsToDraw.get(i), 0, 0);
            GlStateManager.popMatrix();
        }
        itemRenderer.zLevel = 0F;
        GlStateManager.popMatrix();
        RenderHelper.disableStandardItemLighting();
        GlStateManager.disableLighting();
        GlStateManager.disableDepth();
    }

}
