package st.evening.mc.rotnsaws.compat.bwm;

import betterwithmods.api.tile.IMechanicalPower;
import betterwithmods.common.BWMBlocks;
import betterwithmods.common.blocks.BlockAesthetic;
import betterwithmods.common.damagesource.BWDamageSource;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.DamageSource;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.CapabilityInject;
import net.minecraftforge.fml.common.Loader;
import org.jspecify.annotations.Nullable;
import st.evening.mc.rotnsaws.util.SawDamageSource;

public enum SerrBwmCompat {
    ;

    public static final String MOD_ID = "betterwithmods";
    public static final boolean LOADED = Loader.isModLoaded(MOD_ID);

    @Nullable
    @CapabilityInject(IMechanicalPower.class)
    public static Capability<IMechanicalPower> CAP_MECHANICAL_POWER;

    public static BwmMechanicalHandler newMechanicalHandler(final BwmMechanicalHandler.Host host) {
        return LOADED ? Impl.newMechanicalHandlerImpl(host) : BwmMechanicalHandler.Noop.INSTANCE;
    }

    public static ChoppingBlockResult isChoppingBlock(final IBlockState state) {
        return LOADED ? Impl.isChoppingBlockImpl(state) : ChoppingBlockResult.NO;
    }

    public static void stainChoppingBlock(final World world, final BlockPos pos) {
        if (LOADED) {
            Impl.stainChoppingBlockImpl(world, pos);
        }
    }

    public static DamageSource getSawDamageSource(final World world, final boolean chop) {
        return LOADED ? Impl.getSawDamageSourceImpl(chop) : new SawDamageSource(world, chop);
    }

    public enum ChoppingBlockResult {
        CLEAN, STAINED, NO
    }

    private enum Impl {
        ;

        static BwmMechanicalHandler newMechanicalHandlerImpl(final BwmMechanicalHandler.Host host) {
            return new BwmMechanicalHandlerImpl(host);
        }

        static ChoppingBlockResult isChoppingBlockImpl(final IBlockState state) {
            if (state.getBlock() != BWMBlocks.AESTHETIC) return ChoppingBlockResult.NO;
            switch (state.getValue(BlockAesthetic.TYPE)) {
                case CHOPBLOCK:
                    return ChoppingBlockResult.CLEAN;
                case CHOPBLOCKBLOOD:
                    return ChoppingBlockResult.STAINED;
                default:
                    return ChoppingBlockResult.NO;
            }
        }

        static void stainChoppingBlockImpl(final World world, final BlockPos pos) {
            world.setBlockState(pos, BWMBlocks.AESTHETIC.getDefaultState()
                    .withProperty(BlockAesthetic.TYPE, BlockAesthetic.EnumType.CHOPBLOCKBLOOD));
        }

        static DamageSource getSawDamageSourceImpl(final boolean chop) {
            return chop ? BWDamageSource.getChoppingBlockDamage() : BWDamageSource.getSawDamage();
        }
    }
}
