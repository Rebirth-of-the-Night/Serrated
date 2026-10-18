package st.evening.mc.rotnsaws.init;

import net.minecraft.creativetab.CreativeTabs;
import net.minecraft.item.ItemStack;
import st.evening.mc.rotnsaws.SerrConst;

public enum SerrItems {
    ;

    public static final CreativeTabs creativeTab = new CreativeTabs(SerrConst.MOD_ID) {
        @Override
        public ItemStack createIcon() {
            return new ItemStack(SerrBlocks.saw);
        }
    };
}
