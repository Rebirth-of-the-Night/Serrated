package st.evening.mc.rotnsaws.feature.saw;

import net.minecraft.block.state.IBlockState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RenderCuttingSaw extends TileEntitySpecialRenderer<TileEntityCuttingSaw> {
    @Override
    public void render(final TileEntityCuttingSaw te, final double x, final double y, final double z,
                       final float partialTicks, final int destroyStage, final float alpha) {
        final ItemStack stack = te.getImmersiveProcessingStack();
        if (stack.isEmpty()) return;
        final IBlockState state = te.getWorld().getBlockState(te.getPos());
        if (!(state.getBlock() instanceof BlockCuttingSaw)) return;
        GlStateManager.pushMatrix();
        GlStateManager.pushAttrib();
        switch (state.getValue(BlockCuttingSaw.FACING)) {
            case UP:
                GlStateManager.translate(x + 0.5D, y + 0.9D, z + 0.5D);
                if (!(stack.getItem() instanceof ItemBlock)) {
                    GlStateManager.rotate(90F, 1F, 0F, 0F);
                }
                break;
            case DOWN:
                GlStateManager.translate(x + 0.5D, y + 0.1D, z + 0.5D);
                if (!(stack.getItem() instanceof ItemBlock)) {
                    GlStateManager.rotate(-90F, 1F, 0F, 0F);
                }
                break;
            case NORTH:
                GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.1D);
                break;
            case EAST:
                GlStateManager.translate(x + 0.9D, y + 0.5D, z + 0.5D);
                GlStateManager.rotate(270F, 0F, 1F, 0F);
                break;
            case SOUTH:
                GlStateManager.translate(x + 0.5D, y + 0.5D, z + 0.9D);
                GlStateManager.rotate(180F, 0F, 1F, 0F);
                break;
            case WEST:
                GlStateManager.translate(x + 0.1D, y + 0.5D, z + 0.5D);
                GlStateManager.rotate(90F, 0F, 1F, 0F);
                break;
        }
        GlStateManager.scale(0.75F, 0.75F, 0.75F);
        RenderHelper.enableStandardItemLighting();
        Minecraft.getMinecraft().getRenderItem().renderItem(stack, ItemCameraTransforms.TransformType.FIXED);
        RenderHelper.disableStandardItemLighting();
        GlStateManager.popAttrib();
        GlStateManager.popMatrix();
    }
}
