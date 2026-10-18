package st.evening.mc.rotnsaws.init;

import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import st.evening.mc.rotnsaws.SerrConst;

@GameRegistry.ObjectHolder(SerrConst.MOD_ID)
@Mod.EventBusSubscriber(modid = SerrConst.MOD_ID)
public enum SerrSounds {
    ;

    public static final SoundEvent sawActive = newSoundEvent("block.saw.active");
    public static final SoundEvent sawCut = newSoundEvent("block.saw.cut");
    public static final SoundEvent sawWorking = newSoundEvent("block.saw.working");
    public static final SoundEvent stonecutterActive = newSoundEvent("block.stonecutter.active");
    public static final SoundEvent stonecutterCut = newSoundEvent("block.stonecutter.cut");
    public static final SoundEvent stonecutterWorking = newSoundEvent("block.stonecutter.working");

    private static SoundEvent newSoundEvent(final String path) {
        final ResourceLocation name = new ResourceLocation(SerrConst.MOD_ID, path);
        final SoundEvent sound = new SoundEvent(name);
        sound.setRegistryName(name);
        return sound;
    }

    @SubscribeEvent
    public static void onRegisterSoundEvents(final RegistryEvent.Register<SoundEvent> event) {
        final IForgeRegistry<SoundEvent> reg = event.getRegistry();
        reg.register(sawCut);
        reg.register(sawWorking);
        reg.register(stonecutterCut);
        reg.register(stonecutterWorking);
    }
}
