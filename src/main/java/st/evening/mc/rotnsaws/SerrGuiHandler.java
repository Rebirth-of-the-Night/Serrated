package st.evening.mc.rotnsaws;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.fml.common.network.IGuiHandler;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.feature.saw.ContainerCuttingSaw;
import st.evening.mc.rotnsaws.feature.saw.GuiCuttingSaw;
import st.evening.mc.rotnsaws.feature.saw.TileEntityCuttingSaw;

public class SerrGuiHandler implements IGuiHandler {
    public static final int CUTTING_SAW = 0;

    @Nullable
    @Override
    public Object getServerGuiElement(final int id, final EntityPlayer player, final World world,
                                      final int x, final int y, final int z) {
        final TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (te == null) return null;
        switch (id) {
            case CUTTING_SAW:
                if (!(te instanceof TileEntityCuttingSaw)) return null;
                return ContainerCuttingSaw.create((TileEntityCuttingSaw) te, player.inventory);
            default:
                return null;
        }
    }

    @Nullable
    @Override
    public Object getClientGuiElement(final int id, final EntityPlayer player, final World world,
                                      final int x, final int y, final int z) {
        final TileEntity te = world.getTileEntity(new BlockPos(x, y, z));
        if (te == null) return null;
        switch (id) {
            case CUTTING_SAW:
                if (!(te instanceof TileEntityCuttingSaw)) return null;
                return new GuiCuttingSaw(ContainerCuttingSaw.create((TileEntityCuttingSaw) te, player.inventory));
            default:
                return null;
        }
    }
}
