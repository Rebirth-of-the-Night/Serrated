package st.evening.mc.rotnsaws;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.ConfigManager;
import net.minecraftforge.fml.client.event.ConfigChangedEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.feature.saw.TileEntityCuttingSaw;
import st.evening.mc.rotnsaws.network.CPacketSetSawOutput;
import st.evening.mc.rotnsaws.network.SPacketSyncSawPower;
import st.evening.mc.rotnsaws.network.SPacketSyncSawRecipe;
import st.evening.mc.rotnsaws.network.SPacketSyncSawUi;
import st.evening.mc.rotnsaws.util.MachineSound;

public class CommonProxy {
    private final SimpleNetworkWrapper network = NetworkRegistry.INSTANCE.newSimpleChannel(SerrConst.MOD_ID);

    public void onPreInit(final FMLPreInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(this);

        GameRegistry.registerTileEntity(TileEntityCuttingSaw.Saw.class, new ResourceLocation(SerrConst.MOD_ID, "saw"));
        GameRegistry.registerTileEntity(TileEntityCuttingSaw.Stonecutter.class,
                new ResourceLocation(SerrConst.MOD_ID, "stonecutter"));

        network.registerMessage(new SPacketSyncSawPower.Handler(), SPacketSyncSawPower.class, 0, Side.CLIENT);
        network.registerMessage(new SPacketSyncSawRecipe.Handler(), SPacketSyncSawRecipe.class, 1, Side.CLIENT);
        network.registerMessage(new SPacketSyncSawUi.Handler(), SPacketSyncSawUi.class, 2, Side.CLIENT);
        network.registerMessage(new CPacketSetSawOutput.Handler(), CPacketSetSawOutput.class, 3, Side.SERVER);

        NetworkRegistry.INSTANCE.registerGuiHandler(Serrated.INSTANCE, new SerrGuiHandler());
    }

    public SimpleNetworkWrapper getNetwork() {
        return network;
    }

    @SubscribeEvent
    public void onConfigChanged(final ConfigChangedEvent event) {
        if (event.getModID().equals(SerrConst.MOD_ID)) {
            ConfigManager.sync(SerrConst.MOD_ID, Config.Type.INSTANCE);
        }
    }

    @Nullable
    public MachineSound newMachineSound(final TileEntity owner, final SoundEvent soundEvent, final float volume) {
        return null;
    }
}
