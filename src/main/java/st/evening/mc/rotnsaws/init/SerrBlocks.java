package st.evening.mc.rotnsaws.init;

import net.minecraft.block.Block;
import net.minecraft.block.SoundType;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.registries.IForgeRegistry;
import st.evening.mc.rotnsaws.SerrConst;
import st.evening.mc.rotnsaws.feature.saw.BlockCuttingSaw;
import st.evening.mc.rotnsaws.feature.saw.CuttingSawType;

import java.util.Objects;

@GameRegistry.ObjectHolder(SerrConst.MOD_ID)
@Mod.EventBusSubscriber(modid = SerrConst.MOD_ID)
public enum SerrBlocks {
    ;

    public static final Block saw = new BlockCuttingSaw(CuttingSawType.SAW, Material.WOOD, SoundType.WOOD)
            .setCreativeTab(SerrItems.creativeTab)
            .setRegistryName(new ResourceLocation(SerrConst.MOD_ID, "saw"));
    public static final Block stonecutter = new BlockCuttingSaw(CuttingSawType.STONECUTTER, Material.ROCK, SoundType.STONE)
            .setCreativeTab(SerrItems.creativeTab)
            .setRegistryName(new ResourceLocation(SerrConst.MOD_ID, "stonecutter"));

    @SubscribeEvent
    public static void onRegisterBlocks(final RegistryEvent.Register<Block> event) {
        final IForgeRegistry<Block> reg = event.getRegistry();
        reg.register(saw);
        reg.register(stonecutter);
    }

    @SubscribeEvent
    public static void onRegisterItems(final RegistryEvent.Register<Item> event) {
        final IForgeRegistry<Item> reg = event.getRegistry();
        reg.register(new ItemBlock(saw).setRegistryName(Objects.requireNonNull(saw.getRegistryName())));
        reg.register(new ItemBlock(stonecutter).setRegistryName(Objects.requireNonNull(stonecutter.getRegistryName())));
    }
}
