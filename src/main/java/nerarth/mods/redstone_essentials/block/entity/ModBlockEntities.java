package nerarth.mods.redstone_essentials.block.entity;

import nerarth.mods.redstone_essentials.RedstoneEssentials;
import nerarth.mods.redstone_essentials.block.ModBlocks;
import net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.block.entity.BlockEntityType;

public final class ModBlockEntities {
    public static final BlockEntityType<RedstoneCapacitorBlockEntity> REDSTONE_CAPACITOR = register(
            "redstone_capacitor",
            FabricBlockEntityTypeBuilder.create(RedstoneCapacitorBlockEntity::new, ModBlocks.REDSTONE_CAPACITOR).build()
    );

    private ModBlockEntities() {
    }

    private static <T extends BlockEntityType<?>> T register(String name, T type) {
        Identifier id = Identifier.fromNamespaceAndPath(RedstoneEssentials.MOD_ID, name);
        ResourceKey<BlockEntityType<?>> key = ResourceKey.create(Registries.BLOCK_ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, key, type);
    }

    public static void initialize() {
        // Trigger static field initialization
    }
}
