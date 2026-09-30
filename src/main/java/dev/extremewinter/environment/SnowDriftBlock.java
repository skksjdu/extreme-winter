package dev.extremewinter.environment;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.SnowLayerBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.EntityCollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** Vanilla snow geometry/texture and melting, with vanilla falling entities and layer conservation. */
public final class SnowDriftBlock extends SnowLayerBlock implements Fallable {
    public static final MapCodec<SnowLayerBlock> CODEC = simpleCodec(SnowDriftBlock::new);
    private static final VoxelShape[] PLAYER_SHAPES = new VoxelShape[8];

    static {
        for (int layers = 1; layers <= 8; layers++) {
            PLAYER_SHAPES[layers - 1] = Block.box(0, 0, 0, 16, layers, 16);
        }
    }

    public SnowDriftBlock(Properties settings) { super(settings); }
    @Override public MapCodec<SnowLayerBlock> codec() { return CODEC; }

    public static int layers(BlockState state) {
        return state.getBlock() instanceof SnowDriftBlock || state.is(Blocks.SNOW) ? state.getValue(LAYERS) : 0;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext context) {
        // Players compress the surface to half its visible height (at most half a block).
        if (context instanceof EntityCollisionContext entityContext && entityContext.getEntity() instanceof Player) {
            return PLAYER_SHAPES[state.getValue(LAYERS) - 1];
        }
        // Falling snow and other entities retain solid support, preserving stacked-snow gravity.
        return getShape(state, world, pos, context);
    }

    @Override
    protected void onPlace(BlockState state, Level world, BlockPos pos, BlockState oldState, boolean notify) {
        world.scheduleTick(pos, this, 2);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader world, ScheduledTickAccess ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        // SnowBlock normally removes unsupported snow immediately. Schedule gravity instead.
        ticks.scheduleTick(pos, this, 2);
        return state;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        return layers(world.getBlockState(pos.below())) > 0 || super.canSurvive(state, world, pos);
    }

    @Override
    protected boolean canBeReplaced(BlockState state, BlockPlaceContext context) {
        if (context instanceof DirectionalPlaceContext) return state.getValue(LAYERS) < 8;
        if (context.getItemInHand().is(asItem()) && state.getValue(LAYERS) < 8) return true;
        return super.canBeReplaced(state, context);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState existing = context.getLevel().getBlockState(context.getClickedPos());
        return existing.is(this) ? existing.setValue(LAYERS, Math.min(8, existing.getValue(LAYERS) + 1)) : defaultBlockState();
    }

    @Override
    protected void tick(BlockState state, ServerLevel world, BlockPos pos, RandomSource random) {
        BlockState below = world.getBlockState(pos.below());
        int belowLayers = layers(below);
        if (belowLayers > 0 && belowLayers < 8) {
            int moved = Math.min(8 - belowLayers, state.getValue(LAYERS));
            world.setBlock(pos.below(), defaultBlockState().setValue(LAYERS, belowLayers + moved), Block.UPDATE_ALL);
            int remaining = state.getValue(LAYERS) - moved;
            world.setBlock(pos, remaining == 0 ? Blocks.AIR.defaultBlockState() : state.setValue(LAYERS, remaining), Block.UPDATE_ALL);
        } else if (FallingBlock.isFree(below) && pos.getY() >= world.getMinY()) {
            FallingBlockEntity.fall(world, pos, state);
        }
    }

    @Override
    public void onLand(Level world, BlockPos pos, BlockState state, BlockState replaced, FallingBlockEntity entity) {
        int total = state.getValue(LAYERS) + layers(replaced);
        world.setBlock(pos, state.setValue(LAYERS, Math.min(8, total)), Block.UPDATE_ALL);
        if (total > 8) {
            if (world.getBlockState(pos.above()).isAir() && world.isInWorldBounds(pos.above())) {
                world.setBlock(pos.above(), state.setValue(LAYERS, total - 8), Block.UPDATE_ALL);
            } else {
                Block.popResource(world, pos, new ItemStack(Items.SNOWBALL, total - 8));
            }
        }
    }

    @Override
    public void onBrokenAfterFall(Level world, BlockPos pos, FallingBlockEntity entity) {
        int extra = layers(entity.getBlockState()) - 1;
        // Vanilla drops one block item afterwards. Add the remaining layer items.
        if (extra > 0) Block.popResource(world, pos, new ItemStack(asItem(), extra));
    }
}
