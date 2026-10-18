package st.evening.mc.rotnsaws.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;

import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.feature.saw.TileEntityCuttingSaw;

import java.io.IOException;

@SuppressWarnings("NotNullFieldNotInitialized")
public class SPacketSyncSawRecipe implements IMessage {
    private BlockPos pos;
    @Nullable
    private NBTTagCompound dataTag;

    public SPacketSyncSawRecipe(final BlockPos pos, @Nullable final NBTTagCompound dataTag) {
        this.pos = pos;
        this.dataTag = dataTag;
    }

    public SPacketSyncSawRecipe() {}

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer pb = new PacketBuffer(buf);
        pb.writeBlockPos(pos);
        pb.writeCompoundTag(dataTag);
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer pb = new PacketBuffer(buf);
        pos = pb.readBlockPos();
        try {
            dataTag = pb.readCompoundTag();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class Handler implements IMessageHandler<SPacketSyncSawRecipe, IMessage> {
        @Nullable
        @Override
        public IMessage onMessage(final SPacketSyncSawRecipe message, final MessageContext ctx) {
            final Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                if (mc.world == null) return;
                final TileEntity te = mc.world.getTileEntity(message.pos);
                if (!(te instanceof TileEntityCuttingSaw)) return;
                ((TileEntityCuttingSaw) te).setRecipeStateFromSync(message.dataTag);
            });
            return null;
        }
    }
}
