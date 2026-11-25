package nerarth.mods.redstone_essentials;

import nerarth.mods.redstone_essentials.block.ModBlocks;
import nerarth.mods.redstone_essentials.block.entity.ModBlockEntities;
import nerarth.mods.redstone_essentials.item.ModCreativeTabs;
import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedstoneEssentials implements ModInitializer {
    public static final String MOD_ID = "redstone_essentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        ModBlocks.initialize();
        ModBlockEntities.initialize();
        ModCreativeTabs.initialize();
        LOGGER.info("Redstone Essentials initialized");
    }
}
