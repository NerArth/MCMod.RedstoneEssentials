package nerarth.mods.redstone_essentials.block;

import nerarth.mods.redstone_essentials.block.entity.RedstoneCapacitorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;

/**
 * Flat directional analog charge-storage block with delta-based charging, pure exponential RC discharge,
 * and backpressure retention ({@code O >= C}).
 */
public class RedstoneCapacitorBlock extends DiodeBlock implements EntityBlock {
    public static final IntegerProperty CHARGE = IntegerProperty.create("charge", 0, 15);
    private static final int MAX_WAKEUP_TICKS = 2;

    /**
     * Game ticks per +1 charge step indexed by {@code delta = input - charge} (1..15).
     * Total 0 -> 15 charge time: 53 ticks (~2.65s).
     */
    private static final int[] CHARGE_TICKS_BY_DELTA = {
            0, 8, 6, 6, 4, 4, 4, 3, 3, 3, 2, 2, 2, 2, 2, 2
    };

    /**
     * Game ticks per -1 discharge step indexed by current {@code charge} (1..15).
     * Total 15 -> 0 discharge tail: 323 ticks (~16.15s).
     */
    private static final int[] DISCHARGE_TICKS_BY_CHARGE = {
            0, 58, 49, 41, 34, 28, 23, 19, 16, 13, 11, 9, 7, 6, 5, 4
    };

    public RedstoneCapacitorBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(CHARGE, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, CHARGE);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new RedstoneCapacitorBlockEntity(pos, state);
    }

    @Override
    protected int getDelay(BlockState state) {
        return 2;
    }

    @Override
    public boolean isLocked(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        return state.getValue(CHARGE) > 0;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(CHARGE);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        // Weak forward power only: powers direct #capacitor_loads without strongly powering non-load solid blocks.
        return 0;
    }

    @Override
    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        Direction facing = state.getValue(FACING);
        return direction == facing || direction == facing.getOpposite();
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return false;
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        super.onPlace(state, level, pos, oldState, movedByPiston);
        if (!level.isClientSide() && !oldState.is(this)) {
            evaluateAndSchedule(level, pos, state);
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // Charging schedule is handled by evaluateAndSchedule in onPlace.
    }

    @Override
    protected void checkTickOnNeighbor(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide()) {
            evaluateAndSchedule(level, pos, state);
        }
    }

    @Override
    protected void tick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
        if (!(level.getBlockEntity(pos) instanceof RedstoneCapacitorBlockEntity be)) {
            return;
        }
        int pendingMode = be.getPendingMode();
        if (pendingMode == 0) {
            return;
        }

        long now = level.getGameTime();
        long targetTime = be.getNextStepGameTime();
        if (now < targetTime) {
            int remaining = (int) Math.max(1L, targetTime - now);
            level.scheduleTick(pos, this, Math.min(remaining, MAX_WAKEUP_TICKS));
            return;
        }

        be.clearSchedule();
        StepPlan plan = evaluateDesiredAction(level, pos, state);
        if (plan.mode == 0) {
            return;
        }

        int currentCharge = state.getValue(CHARGE);
        int nextCharge = Math.clamp(currentCharge + plan.mode, 0, 15);
        if (nextCharge == currentCharge) {
            return;
        }

        BlockState nextState = state
                .setValue(CHARGE, nextCharge)
                .setValue(POWERED, nextCharge > 0);
        level.setBlock(pos, nextState, Block.UPDATE_CLIENTS);
        updateNeighborsInFront(level, pos, nextState);
        evaluateAndSchedule(level, pos, nextState);
    }

    private void evaluateAndSchedule(Level level, BlockPos pos, BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof RedstoneCapacitorBlockEntity be)) {
            return;
        }

        StepPlan plan = evaluateDesiredAction(level, pos, state);
        if (plan.mode == 0) {
            be.clearSchedule();
            return;
        }

        long now = level.getGameTime();
        long targetTime = now + plan.delayTicks;
        if (be.getPendingMode() != plan.mode
                || be.getNextStepGameTime() < 0L
                || targetTime < be.getNextStepGameTime()) {
            be.setSchedule(targetTime, plan.mode);
            level.scheduleTick(pos, this, Math.min(plan.delayTicks, MAX_WAKEUP_TICKS));
        } else if (!level.getBlockTicks().hasScheduledTick(pos, this)) {
            int remaining = (int) Math.max(1L, be.getNextStepGameTime() - now);
            level.scheduleTick(pos, this, Math.min(remaining, MAX_WAKEUP_TICKS));
        }
    }

    private StepPlan evaluateDesiredAction(Level level, BlockPos pos, BlockState state) {
        int input = getInputSignal(level, pos, state);
        int charge = state.getValue(CHARGE);

        if (input > charge) {
            int delta = Math.clamp(input - charge, 1, 15);
            return new StepPlan(1, CHARGE_TICKS_BY_DELTA[delta]);
        }

        if (input < charge) {
            Direction outputDir = state.getValue(FACING).getOpposite();
            if (getExternalOutputSignal(level, pos, outputDir) >= charge) {
                return StepPlan.IDLE;
            }
            return new StepPlan(-1, DISCHARGE_TICKS_BY_CHARGE[charge]);
        }

        return StepPlan.IDLE;
    }

    /**
     * Measures the strongest external redstone signal entering {@code outputPos} without counting
     * this Capacitor's own power (including indirect bounce-back through {@code outputPos} into
     * adjacent conductive blocks such as the floor beneath redstone dust).
     */
    private static int getExternalOutputSignal(Level level, BlockPos capPos, Direction outputDir) {
        BlockPos outputPos = capPos.relative(outputDir);
        BlockState outputState = level.getBlockState(outputPos);

        // Non-wire loads (variode, repeater, comparator, piston, etc.) are not shared wire nodes:
        // only signal emitted directly by outputPos back toward the Capacitor counts as backpressure.
        if (!outputState.is(Blocks.REDSTONE_WIRE)) {
            return outputState.getSignal(level, outputPos, outputDir);
        }

        Direction fromCapacitor = outputDir.getOpposite();
        int maxExternal = 0;

        for (Direction side : Direction.values()) {
            if (side == fromCapacitor) {
                continue;
            }
            BlockPos neighborPos = outputPos.relative(side);
            BlockState neighborState = level.getBlockState(neighborPos);
            int signal;
            if (neighborState.is(Blocks.REDSTONE_WIRE)) {
                signal = Math.max(0, neighborState.getValue(RedstoneWireBlock.POWER) - 1);
            } else {
                signal = neighborState.getSignal(level, neighborPos, side);
                if (neighborState.isRedstoneConductor(level, neighborPos)) {
                    signal = Math.max(signal, getDirectSignalToExcluding(level, neighborPos, outputPos));
                }
            }
            if (signal > maxExternal) {
                maxExternal = signal;
                if (maxExternal >= 15) {
                    return 15;
                }
            }
        }
        return maxExternal;
    }

    private static int getDirectSignalToExcluding(Level level, BlockPos pos, BlockPos excludedSource) {
        int max = 0;
        for (Direction dir : Direction.values()) {
            BlockPos sourcePos = pos.relative(dir);
            if (sourcePos.equals(excludedSource)) {
                continue;
            }
            int direct = level.getDirectSignal(sourcePos, dir);
            if (direct >= 15) {
                return 15;
            }
            if (direct > max) {
                max = direct;
            }
        }
        return max;
    }

    private record StepPlan(int mode, int delayTicks) {
        private static final StepPlan IDLE = new StepPlan(0, 0);
    }
}
