package nerarth.mods.redstone_essentials.block.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public class RedstoneCapacitorBlockEntity extends BlockEntity {
    private static final String TAG_NEXT_STEP_GAME_TIME = "NextStepGameTime";
    private static final String TAG_PENDING_MODE = "PendingMode";

    private long nextStepGameTime = -1L;
    private int pendingMode = 0;

    public RedstoneCapacitorBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.REDSTONE_CAPACITOR, pos, state);
    }

    public long getNextStepGameTime() {
        return this.nextStepGameTime;
    }

    public int getPendingMode() {
        return this.pendingMode;
    }

    public void setSchedule(long gameTime, int mode) {
        this.nextStepGameTime = gameTime;
        this.pendingMode = mode;
        setChanged();
    }

    public void clearSchedule() {
        if (this.nextStepGameTime != -1L || this.pendingMode != 0) {
            this.nextStepGameTime = -1L;
            this.pendingMode = 0;
            setChanged();
        }
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong(TAG_NEXT_STEP_GAME_TIME, this.nextStepGameTime);
        output.putInt(TAG_PENDING_MODE, this.pendingMode);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        this.nextStepGameTime = input.getLongOr(TAG_NEXT_STEP_GAME_TIME, -1L);
        this.pendingMode = input.getIntOr(TAG_PENDING_MODE, 0);
    }
}
