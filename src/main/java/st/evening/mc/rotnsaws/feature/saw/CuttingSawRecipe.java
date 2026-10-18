package st.evening.mc.rotnsaws.feature.saw;

import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.crafting.Ingredient;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Predicate;

public class CuttingSawRecipe {
    private final String id;
    private final Ingredient input;
    @Nullable
    private final Predicate<IBlockState> inputBlock;
    private final boolean blockOnly;
    private final List<Output> outputs;

    public CuttingSawRecipe(final String id, final Ingredient input, @Nullable final Predicate<IBlockState> inputBlock,
                            final boolean blockOnly, final List<Output> outputs) {
        this.id = id;
        this.input = input;
        this.inputBlock = inputBlock;
        this.blockOnly = blockOnly;
        this.outputs = outputs;
    }

    public String getId() {
        return id;
    }

    public Ingredient getInput() {
        return input;
    }

    @Nullable
    public Predicate<IBlockState> getInputBlock() {
        return inputBlock;
    }

    public boolean isBlockOnly() {
        return inputBlock != null && blockOnly;
    }

    public List<Output> getOutputs() {
        return outputs;
    }

    @Nullable
    public Output getOutput(final int index) {
        if (index < 0 || index >= outputs.size()) return null;
        return outputs.get(index);
    }

    public static class Output {
        private final int duration;
        private final ItemStack item;
        @Nullable
        private final IBlockState block;
        private final List<ItemStack> extra;

        public Output(final int duration, final ItemStack item, @Nullable final IBlockState block,
                      final List<ItemStack> extra) {
            this.duration = duration;
            this.item = item;
            this.block = block;
            this.extra = extra;
        }

        public int getDuration() {
            return duration;
        }

        public ItemStack getItem() {
            return item;
        }

        @Nullable
        public IBlockState getBlock() {
            return block;
        }

        public List<ItemStack> getExtra() {
            return extra;
        }
    }
}
