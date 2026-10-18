package st.evening.mc.rotnsaws;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.item.Item;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.SoundEvent;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.client.model.ModelLoader;
import net.minecraftforge.fml.client.registry.ClientRegistry;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.feature.saw.RenderCuttingSaw;
import st.evening.mc.rotnsaws.feature.saw.TileEntityCuttingSaw;
import st.evening.mc.rotnsaws.init.SerrBlocks;
import st.evening.mc.rotnsaws.util.MachineSound;
import st.evening.mc.rotnsaws.util.MachineSoundImpl;

import java.util.Objects;

public class ClientProxy extends CommonProxy {
    @Override
    public void onPreInit(final FMLPreInitializationEvent event) {
        super.onPreInit(event);

        final RenderCuttingSaw sawRenderer = new RenderCuttingSaw();
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCuttingSaw.Saw.class, sawRenderer);
        ClientRegistry.bindTileEntitySpecialRenderer(TileEntityCuttingSaw.Stonecutter.class, sawRenderer);
    }

    @SubscribeEvent
    public void onRegisterModels(ModelRegistryEvent event) {
        registerBlockItemModel(SerrBlocks.saw);
        registerBlockItemModel(SerrBlocks.stonecutter);
    }

    private static void registerBlockItemModel(final Block block) {
        registerItemModel(Item.getItemFromBlock(block));
    }

    private static void registerItemModel(final Item item) {
        ModelLoader.setCustomModelResourceLocation(item, 0,
                new ModelResourceLocation(Objects.requireNonNull(item.getRegistryName()), "inventory"));
    }

    @Nullable
    @Override
    public MachineSound newMachineSound(final TileEntity owner, final SoundEvent soundEvent, final float volume) {
        if (FMLCommonHandler.instance().getEffectiveSide().isServer()) return null;
        final MachineSoundImpl sound = new MachineSoundImpl(owner, soundEvent, volume);
        Minecraft.getMinecraft().getSoundHandler().playSound(sound);
        return sound;
    }
}
