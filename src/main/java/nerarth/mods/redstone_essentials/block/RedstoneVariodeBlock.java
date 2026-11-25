package nerarth.mods.redstone_essentials.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DiodeBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

/**
 * 0-tick one-way diode and 4-notch binary attenuator ({@code out = max(0, in - R)}).
 */
public class RedstoneVariodeBlock extends DiodeBlock {
    public static final EnumProperty<Attenuation> ATTENUATION = EnumProperty.create("attenuation", Attenuation.class);
    public static final IntegerProperty POWER = BlockStateProperties.POWER;

    private static final int MAX_SYNC_DEPTH = 32;
    private static final ThreadLocal<Integer> SYNC_DEPTH = ThreadLocal.withInitial(() -> 0);

    public RedstoneVariodeBlock(BlockBehaviour.Properties properties) {
        super(properties);
        registerDefaultState(this.stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(POWERED, false)
                .setValue(ATTENUATION, Attenuation.ONE)
                .setValue(POWER, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, POWERED, ATTENUATION, POWER);
    }

    @Override
    protected int getDelay(BlockState state) {
        return 0;
    }

    @Override
    public boolean isLocked(LevelReader level, BlockPos pos, BlockState state) {
        return false;
    }

    @Override
    protected boolean shouldTurnOn(Level level, BlockPos pos, BlockState state) {
        return computeOutputPower(level, pos, state) > 0;
    }

    @Override
    protected int ownSignal(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(POWER);
    }

    @Override
    protected boolean shouldRedstoneWireConnectTo(BlockState state, BlockGetter level, BlockPos pos, Direction direction) {
        Direction facing = state.getValue(FACING);
        return direction == facing || direction == facing.getOpposite();
    }

    @Override
    protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean movedByPiston) {
        if (!level.isClientSide() && !oldState.is(this)) {
            if (!updateOutputImmediately(level, pos, state)) {
                updateNeighborsInFront(level, pos, state);
            }
        }
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, LivingEntity placer, ItemStack stack) {
        // Output is already synchronized immediately in onPlace without scheduling a delayed tick.
    }

    @Override
    protected void checkTickOnNeighbor(Level level, BlockPos pos, BlockState state) {
        if (!level.isClientSide()) {
            updateOutputImmediately(level, pos, state);
        }
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            BlockHitResult hitResult
    ) {
        if (!player.getAbilities().mayBuild) {
            return InteractionResult.PASS;
        }

        Attenuation next = state.getValue(ATTENUATION).next();
        BlockState cycled = state.setValue(ATTENUATION, next);
        if (!level.isClientSide()) {
            int newPower = computeOutputPower(level, pos, cycled);
            BlockState updated = cycled
                    .setValue(POWER, newPower)
                    .setValue(POWERED, newPower > 0);
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            level.playSound(null, pos, SoundEvents.COMPARATOR_CLICK, SoundSource.BLOCKS, 0.3F, 0.5F + next.ordinal() * 0.12F);
            if (newPower != state.getValue(POWER)) {
                updateNeighborsInFront(level, pos, updated);
            }
        }
        return InteractionResult.SUCCESS;
    }

    private int computeOutputPower(Level level, BlockPos pos, BlockState state) {
        int input = getInputSignal(level, pos, state);
        return Math.max(0, input - state.getValue(ATTENUATION).drop());
    }

    private boolean updateOutputImmediately(Level level, BlockPos pos, BlockState state) {
        int depth = SYNC_DEPTH.get();
        if (depth >= MAX_SYNC_DEPTH) {
            return false;
        }

        int newPower = computeOutputPower(level, pos, state);
        boolean newPowered = newPower > 0;
        if (newPower == state.getValue(POWER) && newPowered == state.getValue(POWERED)) {
            return false;
        }

        BlockState updated = state
                .setValue(POWER, newPower)
                .setValue(POWERED, newPowered);

        SYNC_DEPTH.set(depth + 1);
        try {
            level.setBlock(pos, updated, Block.UPDATE_CLIENTS);
            updateNeighborsInFront(level, pos, updated);
        } finally {
            if (depth == 0) {
                SYNC_DEPTH.remove();
            } else {
                SYNC_DEPTH.set(depth);
            }
        }
        return true;
    }

    public enum Attenuation implements StringRepresentable {
        ONE("1", 1),
        TWO("2", 2),
        FOUR("4", 4),
        EIGHT("8", 8);

        private static final Attenuation[] VALUES = values();

        private final String serializedName;
        private final int drop;

        Attenuation(String serializedName, int drop) {
            this.serializedName = serializedName;
            this.drop = drop;
        }

        @Override
        public String getSerializedName() {
            return this.serializedName;
        }

        public int drop() {
            return this.drop;
        }

        public Attenuation next() {
            return VALUES[(this.ordinal() + 1) % VALUES.length];
        }
    }
}
