package nerarth.mods.redstone_essentials;

import net.fabricmc.api.ModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RedstoneEssentials implements ModInitializer {
    public static final String MOD_ID = "redstone_essentials";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("Redstone Essentials initialized");
    }
}
