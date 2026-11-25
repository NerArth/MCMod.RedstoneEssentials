package nerarth.mods.redstone_essentials.item;

import nerarth.mods.redstone_essentials.RedstoneEssentials;
import nerarth.mods.redstone_essentials.block.ModBlocks;
import net.fabricmc.fabric.api.creativetab.v1.CreativeModeTabEvents;
import net.fabricmc.fabric.api.creativetab.v1.FabricCreativeModeTab;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class ModCreativeTabs {
    private static final ResourceKey<CreativeModeTab> VANILLA_REDSTONE_BLOCKS_TAB = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.withDefaultNamespace("redstone_blocks")
    );

    public static final ResourceKey<CreativeModeTab> REDSTONE_ESSENTIALS_TAB_KEY = ResourceKey.create(
            Registries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(RedstoneEssentials.MOD_ID, "redstone_essentials")
    );

    public static final CreativeModeTab REDSTONE_ESSENTIALS_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            REDSTONE_ESSENTIALS_TAB_KEY,
            FabricCreativeModeTab.builder()
                    .title(Component.translatable("itemGroup.redstone_essentials.redstone_essentials"))
                    .icon(() -> new ItemStack(ModBlocks.TUNED_REDSTONE_CRYSTAL))
                    .displayItems((parameters, output) -> {
                        output.accept(ModBlocks.TUNED_REDSTONE_CRYSTAL);
                        output.accept(ModBlocks.REDSTONE_VARIODE);
                        output.accept(ModBlocks.REDSTONE_CAPACITOR);
                    })
                    .build()
    );

    private ModCreativeTabs() {
    }

    public static void initialize() {
        CreativeModeTabEvents.modifyOutputEvent(VANILLA_REDSTONE_BLOCKS_TAB).register(entries ->
                entries.insertAfter(
                        Items.COMPARATOR,
                        ModBlocks.TUNED_REDSTONE_CRYSTAL,
                        ModBlocks.REDSTONE_VARIODE,
                        ModBlocks.REDSTONE_CAPACITOR
                )
        );
    }
}
