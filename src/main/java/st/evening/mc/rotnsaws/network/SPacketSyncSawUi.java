package st.evening.mc.rotnsaws.network;

import io.netty.buffer.ByteBuf;
import net.minecraft.client.Minecraft;
import net.minecraft.inventory.Container;
import net.minecraft.network.PacketBuffer;
import net.minecraftforge.fml.common.network.simpleimpl.IMessage;
import net.minecraftforge.fml.common.network.simpleimpl.IMessageHandler;
import net.minecraftforge.fml.common.network.simpleimpl.MessageContext;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.feature.saw.ContainerCuttingSaw;

@SuppressWarnings("NotNullFieldNotInitialized")
public class SPacketSyncSawUi implements IMessage {
    private int windowId;
    private int stateClock;
    private String activeRecipeId;
    private int activeOutput;

    public SPacketSyncSawUi(final int windowId, final int stateClock, final String activeRecipeId,
                            final int activeOutput) {
        this.windowId = windowId;
        this.stateClock = stateClock;
        this.activeRecipeId = activeRecipeId;
        this.activeOutput = activeOutput;
    }

    public SPacketSyncSawUi() {}

    @Override
    public void toBytes(final ByteBuf buf) {
        final PacketBuffer pb = new PacketBuffer(buf);
        pb.writeByte(windowId);
        pb.writeVarInt(stateClock);
        pb.writeString(activeRecipeId);
        pb.writeVarInt(activeOutput);
    }

    @Override
    public void fromBytes(final ByteBuf buf) {
        final PacketBuffer pb = new PacketBuffer(buf);
        windowId = pb.readByte();
        stateClock = pb.readVarInt();
        activeRecipeId = pb.readString(128);
        activeOutput = pb.readVarInt();
    }

    public static class Handler implements IMessageHandler<SPacketSyncSawUi, IMessage> {
        @Nullable
        @Override
        public IMessage onMessage(final SPacketSyncSawUi message, final MessageContext ctx) {
            final Minecraft mc = Minecraft.getMinecraft();
            mc.addScheduledTask(() -> {
                if (mc.player == null) return;
                final Container container = mc.player.openContainer;
                if (container == null || container.windowId != message.windowId
                        || !(container instanceof ContainerCuttingSaw)) {
                    return;
                }
                ((ContainerCuttingSaw) container).getSaw().setUiStateFromSync(
                        message.stateClock, message.activeRecipeId, message.activeOutput);
            });
            return null;
        }
    }
}
