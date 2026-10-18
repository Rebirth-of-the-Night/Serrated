package st.evening.mc.rotnsaws.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.network.PacketBuffer;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.feature.saw.TileEntityCuttingSaw;

@SuppressWarnings("NotNullFieldNotInitialized")
public class SPacketSyncSawPower implements IMessage {
    private BlockPos pos;
    private int power;

    public SPacketSyncSawPower(final BlockPos pos, final int power) {
        this.pos = pos;
        this.power = power;
    }

    public SPacketSyncSawPower() {}

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer pb = new PacketBuffer(buf);
        pb.writeBlockPos(pos);
        pb.writeVarInt(power);
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer pb = new PacketBuffer(buf);
        pos = pb.readBlockPos();
        power = pb.readVarInt();
    }

    public static class Handler implements IMessageHandler<SPacketSyncSawPower, IMessage> {
        @Nullable
        @Override
        public IMessage onMessage(final SPacketSyncSawPower message, final MessageContext ctx) {
            final Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                if (mc.world == null) return;
                final TileEntity te = mc.world.getTileEntity(message.pos);
                if (!(te instanceof TileEntityCuttingSaw)) return;
                ((TileEntityCuttingSaw) te).setPowerFromSync(message.power);
            });
            return null;
        }
    }
}
