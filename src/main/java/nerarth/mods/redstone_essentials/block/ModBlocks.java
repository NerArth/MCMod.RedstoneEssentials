package nerarth.mods.redstone_essentials.block;

import nerarth.mods.redstone_essentials.RedstoneEssentials;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;

import java.util.function.Function;

public final class ModBlocks {
    public static final Block TUNED_REDSTONE_CRYSTAL = register(
            "tuned_redstone_crystal",
            TunedRedstoneCrystalBlock::new,
            BlockBehaviour.Properties.of()
                    .mapColor(MapColor.COLOR_PURPLE)
                    .requiresCorrectToolForDrops()
                    .strength(3.0F, 3.0F)
                    .sound(SoundType.COPPER_BULB)
                    .isRedstoneConductor((state, level, pos) -> false)
                    .lightLevel(state -> {
                        int power = state.getValue(TunedRedstoneCrystalBlock.POWER);
                        return power > 0 ? Math.max(4, power) : 0;
                    })
    );

    public static final Block REDSTONE_VARIODE = register(
            "redstone_variode",
            RedstoneVariodeBlock::new,
            BlockBehaviour.Properties.of()
                    .instabreak()
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.POPPED)
    );

    public static final Block REDSTONE_CAPACITOR = register(
            "redstone_capacitor",
            RedstoneCapacitorBlock::new,
            BlockBehaviour.Properties.of()
                    .instabreak()
                    .sound(SoundType.STONE)
                    .pushReaction(PushReaction.POPPED)
    );

    private ModBlocks() {
    }

    private static Block register(
            String name,
            Function<BlockBehaviour.Properties, Block> factory,
            BlockBehaviour.Properties properties
    ) {
        Identifier id = Identifier.fromNamespaceAndPath(RedstoneEssentials.MOD_ID, name);
        ResourceKey<Block> blockKey = ResourceKey.create(Registries.BLOCK, id);
        Block block = factory.apply(properties.setId(blockKey));
        Registry.register(BuiltInRegistries.BLOCK, blockKey, block);

        ResourceKey<Item> itemKey = ResourceKey.create(Registries.ITEM, id);
        BlockItem blockItem = new BlockItem(block, new Item.Properties().setId(itemKey).useBlockDescriptionPrefix());
        Registry.register(BuiltInRegistries.ITEM, itemKey, blockItem);

        return block;
    }

    public static void initialize() {
        // Trigger static field initialization
    }
}
