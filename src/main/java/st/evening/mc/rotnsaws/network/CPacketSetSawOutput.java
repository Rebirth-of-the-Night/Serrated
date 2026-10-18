package st.evening.mc.rotnsaws.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.feature.saw.ContainerCuttingSaw;

public class CPacketSetSawOutput implements IMessage {
    private int windowId;
    private int outputIndex;

    public CPacketSetSawOutput(final int windowId, final int outputIndex) {
        this.windowId = windowId;
        this.outputIndex = outputIndex;
    }

    public CPacketSetSawOutput() {}

    @Override
    public void toBytes(final ByteBuf buf) {
        buf.writeByte(windowId);
        new PacketBuffer(buf).writeVarInt(outputIndex);
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        windowId = buf.readByte();
        outputIndex = new PacketBuffer(buf).readVarInt();
    }

    public static class Handler implements IMessageHandler<CPacketSetSawOutput, IMessage> {
        @Nullable
        @Override
        public IMessage onMessage(final CPacketSetSawOutput message, final MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().player;
            player.server.addScheduledTask(() -> {
                final Container container = player.openContainer;
                if (container == null || container.windowId != message.windowId
                        || !(container instanceof ContainerCuttingSaw)) {
                    return;
                }
                ((ContainerCuttingSaw) container).setOutputIndexFromGui(message.outputIndex);
            });
            return null;
        }
    }
}
