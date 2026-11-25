package nerarth.mods.redstone_essentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RedstoneWireBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.redstone.ExperimentalRedstoneUtils;
import net.minecraft.world.level.redstone.Orientation;

/**
 * 6-way directional analog T-flip-flop / signal-strength latch.
 * {@link #FACING} points toward the back input face; {@code FACING.getOpposite()} is the front output face.
 */
public class TunedRedstoneCrystalBlock extends DirectionalBlock {
    public static final BooleanProperty POWERED = BlockStateProperties.POWERED;
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    public TunedRedstoneCrystalBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, POWER);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        return defaultBlockState().setValue(FACING, context.getNearestLookingDirection().getOpposite());
    }

    @Override
    protected BlockState rotate(BlockState state, Rotation rotation) {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @Override
    protected BlockState mirror(BlockState state, Mirror mirror) {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!oldState.is(this) && level instanceof ServerLevel serverLevel) {
            checkAndToggle(state, serverLevel, pos);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block neighborBlock,
            Orientation orientation,
            boolean movedByPiston
    ) {
        if (level instanceof ServerLevel serverLevel) {
            checkAndToggle(state, serverLevel, pos);
        }
    }

    private void checkAndToggle(BlockState state, ServerLevel level, BlockPos pos) {
        int input = getBackInputSignal(level, pos, state);
        boolean nowPowered = input > 0;
        boolean wasPowered = state.getValue(POWERED);
        boolean sidePowered = hasSideSignal(level, pos, state);
        int currentPower = state.getValue(POWER);

        if (sidePowered) {
            if (nowPowered) {
                int targetPower = Math.clamp(input, 1, 15);
                if (targetPower == currentPower && wasPowered) {
                    return;
                }
                BlockState nextState = state
                        .setValue(POWERED, true)
                        .setValue(POWER, targetPower);
                level.setBlock(pos, nextState, Block.UPDATE_ALL);
                if (targetPower != currentPower) {
                    level.playSound(
                            null,
                            pos,
                            currentPower == 0 ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.AMETHYST_BLOCK_CHIME,
                            SoundSource.BLOCKS,
                            1.0F,
                            currentPower == 0 ? 1.0F : 0.7F + (targetPower * 0.05F)
                    );
                    updateFrontNeighbors(level, pos, nextState);
                    level.updateNeighbourForOutputSignal(pos, this);
                }
            } else if (wasPowered) {
                level.setBlock(pos, state.setValue(POWERED, false), Block.UPDATE_ALL);
            }
            return;
        }

        if (nowPowered == wasPowered) {
            return;
        }

        BlockState nextState = state.setValue(POWERED, nowPowered);
        boolean outputChanged = false;

        if (!wasPowered && nowPowered) {
            int nextPower = currentPower == 0 ? Math.clamp(input, 1, 15) : 0;
            nextState = nextState.setValue(POWER, nextPower);
            outputChanged = (nextPower != currentPower);
            level.playSound(
                    null,
                    pos,
                    nextPower > 0 ? SoundEvents.COPPER_BULB_TURN_ON : SoundEvents.COPPER_BULB_TURN_OFF,
                    SoundSource.BLOCKS,
                    1.0F,
                    1.0F
            );
        }

        level.setBlock(pos, nextState, Block.UPDATE_ALL);
        if (outputChanged) {
            updateFrontNeighbors(level, pos, nextState);
            level.updateNeighbourForOutputSignal(pos, this);
        }
    }

    public static boolean hasSideSignal(Level level, BlockPos pos, BlockState state) {
        Direction backDir = state.getValue(FACING);
        Direction frontDir = backDir.getOpposite();
        for (Direction dir : Direction.values()) {
            if (dir == backDir || dir == frontDir) {
                continue;
            }
            if (level.getSignal(pos.relative(dir), dir) > 0) {
                return true;
            }
        }
        return false;
    }

    /**
     * Scans only the 5 non-sensor faces around a resonating Amethyst block and notifies any
     * Tuned Redstone Crystal whose back input face touches that resonator.
     */
    public static void notifyAdjacentFromResonator(
            ServerLevel level,
            BlockPos resonatorPos,
            BlockPos sensorPos,
            int frequency
    ) {
        if (frequency < 1 || frequency > 15) {
            return;
        }

        BlockPos.MutableBlockPos cursor = new BlockPos.MutableBlockPos();
        for (Direction dir : Direction.values()) {
            cursor.setWithOffset(resonatorPos, dir);
            if (cursor.equals(sensorPos)) {
                continue;
            }
            BlockState adjState = level.getBlockState(cursor);
            if (adjState.getBlock() instanceof TunedRedstoneCrystalBlock crystal
                    && adjState.getValue(FACING) == dir.getOpposite()) {
                crystal.onAmethystResonance(level, cursor.immutable(), adjState, frequency);
            }
        }
    }

    public void onAmethystResonance(ServerLevel level, BlockPos pos, BlockState state, int frequency) {
        int clamped = Math.clamp(frequency, 1, 15);
        int currentPower = state.getValue(POWER);
        boolean sidePowered = hasSideSignal(level, pos, state);
        int nextPower = sidePowered ? clamped : (currentPower == 0 ? clamped : 0);
        if (nextPower == currentPower) {
            return;
        }

        BlockState nextState = state.setValue(POWER, nextPower);
        level.setBlock(pos, nextState, Block.UPDATE_ALL);
        level.playSound(
                null,
                pos,
                nextPower > 0 ? SoundEvents.AMETHYST_BLOCK_CHIME : SoundEvents.COPPER_BULB_TURN_OFF,
                SoundSource.BLOCKS,
                1.0F,
                nextPower > 0 ? 0.7F + (nextPower * 0.05F) : 1.0F
        );
        updateFrontNeighbors(level, pos, nextState);
        level.updateNeighbourForOutputSignal(pos, this);
    }

    private int getBackInputSignal(Level level, BlockPos pos, BlockState state) {
        Direction backDir = state.getValue(FACING);
        BlockPos backPos = pos.relative(backDir);
        int signal = level.getSignal(backPos, backDir);
        if (signal >= 15) {
            return signal;
        }
        BlockState backState = level.getBlockState(backPos);
        if (backState.is(Blocks.REDSTONE_WIRE)) {
            return Math.max(signal, backState.getValue(RedstoneWireBlock.POWER));
        }
        return signal;
    }

    private void updateFrontNeighbors(Level level, BlockPos pos, BlockState state) {
        Direction outputDir = state.getValue(FACING).getOpposite();
        BlockPos frontPos = pos.relative(outputDir);
        Orientation orientation = ExperimentalRedstoneUtils.initialOrientation(level, outputDir, null);
        level.neighborChanged(frontPos, this, orientation);
        level.updateNeighborsAtExceptFromFacing(frontPos, this, outputDir.getOpposite(), orientation);
    }

    @Override
    protected void affectNeighborsAfterRemoval(BlockState state, ServerLevel level, BlockPos pos, boolean movedByPiston) {
        if (!movedByPiston && state.getValue(POWER) > 0) {
            updateFrontNeighbors(level, pos, state);
        }
    }

    @Override
    protected boolean isSignalSource(BlockState state) {
        return true;
    }

    @Override
    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        Direction facing = state.getValue(FACING);
        return direction == facing || direction == facing.getOpposite();
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(POWER);
    }

    @Override
    protected int getDirectSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getSignal(level, pos, direction);
    }

    @Override
    protected int getSignal(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        return state.getValue(FACING) == direction ? ownSignal(state, level, pos) : 0;
    }

    @Override
    protected boolean hasAnalogOutputSignal(BlockState state) {
        return true;
    }

    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return state.getValue(POWER);
    }
}
