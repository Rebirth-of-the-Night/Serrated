package st.evening.mc.rotnsaws;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

@SuppressWarnings("NotNullFieldNotInitialized")
@Mod(modid = SerrConst.MOD_ID, version = SerrConst.VERSION, useMetadata = true)
public class Serrated {
    @Mod.Instance
    public static Serrated INSTANCE;

    @SidedProxy(serverSide = "st.evening.mc.rotnsaws.CommonProxy",
            clientSide = "st.evening.mc.rotnsaws.ClientProxy")
    public static CommonProxy PROXY;

    public static Logger LOGGER;

    @Mod.EventHandler
    public void onPreInit(final FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
        PROXY.onPreInit(event);
    }
}
