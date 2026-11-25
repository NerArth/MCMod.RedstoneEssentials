package nerarth.mods.redstone_essentials.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.llamalad7.mixinextras.sugar.Local;
import nerarth.mods.redstone_essentials.block.TunedRedstoneCrystalBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.SculkSensorBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SculkSensorBlock.class)
public abstract class SculkSensorBlockMixin {
    @WrapOperation(
            method = "tryResonateVibration",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/world/level/Level;gameEvent(Lnet/minecraft/resources/ResourceKey;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/gameevent/GameEvent$Context;)V"
            )
    )
    private static void redstoneEssentials$notifyCrystalOnResonance(
            Level level,
            ResourceKey<GameEvent> event,
            BlockPos resonatorPos,
            GameEvent.Context context,
            Operation<Void> original,
            @Local(argsOnly = true) BlockPos sensorPos,
            @Local(argsOnly = true) int frequency
    ) {
        original.call(level, event, resonatorPos, context);
        if (level instanceof ServerLevel serverLevel && frequency >= 1 && frequency <= 15) {
            TunedRedstoneCrystalBlock.notifyAdjacentFromResonator(serverLevel, resonatorPos, sensorPos, frequency);
        }
    }
}
