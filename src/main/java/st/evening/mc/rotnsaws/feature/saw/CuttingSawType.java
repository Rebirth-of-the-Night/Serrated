package st.evening.mc.rotnsaws.feature.saw;

import net.minecraft.block.Block;
import net.minecraft.util.SoundEvent;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.SerrConst;
import st.evening.mc.rotnsaws.init.SerrBlocks;
import st.evening.mc.rotnsaws.init.SerrSounds;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

public enum CuttingSawType {
    SAW {
        @Override
        public Block getBlock() {
            return SerrBlocks.saw;
        }

        @Override
        public TileEntityCuttingSaw createTileEntity() {
            return new TileEntityCuttingSaw.Saw();
        }

        @Override
        public SoundEvent getActiveSound() {
            return SerrSounds.sawActive;
        }

        @Override
        public SoundEvent getCutSound() {
            return SerrSounds.sawCut;
        }

        @Override
        public SoundEvent getWorkingSound() {
            return SerrSounds.sawWorking;
        }
    },
    STONECUTTER {
        @Override
        public Block getBlock() {
            return SerrBlocks.stonecutter;
        }

        @Override
        public TileEntityCuttingSaw createTileEntity() {
            return new TileEntityCuttingSaw.Stonecutter();
        }

        @Override
        public SoundEvent getActiveSound() {
            return SerrSounds.stonecutterActive;
        }

        @Override
        public SoundEvent getCutSound() {
            return SerrSounds.stonecutterCut;
        }

        @Override
        public SoundEvent getWorkingSound() {
            return SerrSounds.stonecutterWorking;
        }
    };

    private final Map<String, CuttingSawRecipe> recipes = new LinkedHashMap<>();

    public void addRecipe(final CuttingSawRecipe recipe) {
        recipes.put(recipe.getId(), recipe);
    }

    public Collection<CuttingSawRecipe> getRecipes() {
        return recipes.values();
    }

    @Nullable
    public CuttingSawRecipe getRecipeById(final String id) {
        return recipes.get(id);
    }

    public abstract Block getBlock();

    public abstract TileEntityCuttingSaw createTileEntity();

    public abstract SoundEvent getActiveSound();

    public abstract SoundEvent getCutSound();

    public abstract SoundEvent getWorkingSound();

    public String getBlockTranslationKey() {
        return SerrConst.MOD_ID + "." + name().toLowerCase(Locale.ROOT);
    }

    public String getContainerTranslationKey() {
        return "container." + SerrConst.MOD_ID + "." + name().toLowerCase(Locale.ROOT);
    }

    public String getJeiUid() {
        return SerrConst.MOD_ID + "." + name();
    }
}
