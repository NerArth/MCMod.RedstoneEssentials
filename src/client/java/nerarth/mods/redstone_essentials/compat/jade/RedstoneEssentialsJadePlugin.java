package nerarth.mods.redstone_essentials.compat.jade;

import nerarth.mods.redstone_essentials.RedstoneEssentials;
import nerarth.mods.redstone_essentials.block.RedstoneCapacitorBlock;
import nerarth.mods.redstone_essentials.block.RedstoneVariodeBlock;
import nerarth.mods.redstone_essentials.block.TunedRedstoneCrystalBlock;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.BlockState;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.IWailaClientRegistration;
import snownee.jade.api.IWailaPlugin;
import snownee.jade.api.WailaPlugin;
import snownee.jade.api.config.IPluginConfig;
import snownee.jade.api.theme.IThemeHelper;

@WailaPlugin
public final class RedstoneEssentialsJadePlugin implements IWailaPlugin {
    private static final Identifier TUNED_CRYSTAL_UID =
            Identifier.fromNamespaceAndPath(RedstoneEssentials.MOD_ID, "tuned_redstone_crystal");
    private static final Identifier VARIODE_UID =
            Identifier.fromNamespaceAndPath(RedstoneEssentials.MOD_ID, "redstone_variode");
    private static final Identifier CAPACITOR_UID =
            Identifier.fromNamespaceAndPath(RedstoneEssentials.MOD_ID, "redstone_capacitor");

    @Override
    public void registerClient(IWailaClientRegistration registration) {
        registration.registerBlockComponent(TunedCrystalProvider.INSTANCE, TunedRedstoneCrystalBlock.class);
        registration.registerBlockComponent(VariodeProvider.INSTANCE, RedstoneVariodeBlock.class);
        registration.registerBlockComponent(CapacitorProvider.INSTANCE, RedstoneCapacitorBlock.class);
    }

    private enum TunedCrystalProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            BlockState state = accessor.getBlockState();
            boolean retune = TunedRedstoneCrystalBlock.hasSideSignal(accessor.getLevel(), accessor.getPosition(), state);
            IThemeHelper theme = IThemeHelper.get();
            Component modeText = retune
                    ? theme.success(Component.translatable("tooltip.redstone_essentials.state_on"))
                    : theme.danger(Component.translatable("tooltip.redstone_essentials.state_off"));
            tooltip.add(Component.translatable("tooltip.redstone_essentials.retune_mode", modeText));
        }

        @Override
        public Identifier getUid() {
            return TUNED_CRYSTAL_UID;
        }
    }

    private enum VariodeProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            int drop = accessor.getBlockState().getValue(RedstoneVariodeBlock.ATTENUATION).drop();
            tooltip.add(Component.translatable(
                    "tooltip.redstone_essentials.attenuation",
                    IThemeHelper.get().info("-" + drop)
            ));
        }

        @Override
        public Identifier getUid() {
            return VARIODE_UID;
        }
    }

    private enum CapacitorProvider implements IBlockComponentProvider {
        INSTANCE;

        @Override
        public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
            int charge = accessor.getBlockState().getValue(RedstoneCapacitorBlock.CHARGE);
            tooltip.add(Component.translatable(
                    "tooltip.redstone_essentials.charge",
                    IThemeHelper.get().info(charge)
            ));
        }

        @Override
        public Identifier getUid() {
            return CAPACITOR_UID;
        }
    }
}
