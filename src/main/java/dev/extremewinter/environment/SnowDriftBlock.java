package dev.extremewinter.environment;

import com.mojang.serialization.MapCodec;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.EntityShapeContext;
import net.minecraft.block.Falling;
import net.minecraft.block.FallingBlock;
import net.minecraft.block.SnowBlock;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.FallingBlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.AutomaticItemPlacementContext;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.random.Random;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;
import net.minecraft.world.tick.ScheduledTickView;

/** Vanilla snow geometry/texture and melting, with vanilla falling entities and layer conservation. */
public final class SnowDriftBlock extends SnowBlock implements Falling {
    public static final MapCodec<SnowBlock> CODEC = createCodec(SnowDriftBlock::new);
    private static final VoxelShape[] PLAYER_SHAPES = new VoxelShape[8];

    static {
        for (int layers = 1; layers <= 8; layers++) {
            PLAYER_SHAPES[layers - 1] = Block.createCuboidShape(0, 0, 0, 16, layers, 16);
        }
    }

    public SnowDriftBlock(Settings settings) { super(settings); }
    @Override public MapCodec<SnowBlock> getCodec() { return CODEC; }

    public static int layers(BlockState state) {
        return state.getBlock() instanceof SnowDriftBlock || state.isOf(Blocks.SNOW) ? state.get(LAYERS) : 0;
    }

    @Override
    protected VoxelShape getCollisionShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        // Players compress the surface to half its visible height (at most half a block).
        if (context instanceof EntityShapeContext entityContext && entityContext.getEntity() instanceof PlayerEntity) {
            return PLAYER_SHAPES[state.get(LAYERS) - 1];
        }
        // Falling snow and other entities retain solid support, preserving stacked-snow gravity.
        return getOutlineShape(state, world, pos, context);
    }

    @Override
    protected void onBlockAdded(BlockState state, World world, BlockPos pos, BlockState oldState, boolean notify) {
        world.scheduleBlockTick(pos, this, 2);
    }

    @Override
    protected BlockState getStateForNeighborUpdate(BlockState state, WorldView world, ScheduledTickView ticks,
            BlockPos pos, Direction direction, BlockPos neighborPos, BlockState neighborState, Random random) {
        // SnowBlock normally removes unsupported snow immediately. Schedule gravity instead.
        ticks.scheduleBlockTick(pos, this, 2);
        return state;
    }

    @Override
    protected boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return layers(world.getBlockState(pos.down())) > 0 || super.canPlaceAt(state, world, pos);
    }

    @Override
    protected boolean canReplace(BlockState state, ItemPlacementContext context) {
        if (context instanceof AutomaticItemPlacementContext) return state.get(LAYERS) < 8;
        if (context.getStack().isOf(asItem()) && state.get(LAYERS) < 8) return true;
        return super.canReplace(state, context);
    }

    @Override
    public BlockState getPlacementState(ItemPlacementContext context) {
        BlockState existing = context.getWorld().getBlockState(context.getBlockPos());
        return existing.isOf(this) ? existing.with(LAYERS, Math.min(8, existing.get(LAYERS) + 1)) : getDefaultState();
    }

    @Override
    protected void scheduledTick(BlockState state, ServerWorld world, BlockPos pos, Random random) {
        BlockState below = world.getBlockState(pos.down());
        int belowLayers = layers(below);
        if (belowLayers > 0 && belowLayers < 8) {
            int moved = Math.min(8 - belowLayers, state.get(LAYERS));
            world.setBlockState(pos.down(), getDefaultState().with(LAYERS, belowLayers + moved), Block.NOTIFY_ALL);
            int remaining = state.get(LAYERS) - moved;
            world.setBlockState(pos, remaining == 0 ? Blocks.AIR.getDefaultState() : state.with(LAYERS, remaining), Block.NOTIFY_ALL);
        } else if (FallingBlock.canFallThrough(below) && pos.getY() >= world.getBottomY()) {
            FallingBlockEntity.spawnFromBlock(world, pos, state);
        }
    }

    @Override
    public void onLanding(World world, BlockPos pos, BlockState state, BlockState replaced, FallingBlockEntity entity) {
        int total = state.get(LAYERS) + layers(replaced);
        world.setBlockState(pos, state.with(LAYERS, Math.min(8, total)), Block.NOTIFY_ALL);
        if (total > 8) {
            if (world.getBlockState(pos.up()).isAir() && world.isInBuildLimit(pos.up())) {
                world.setBlockState(pos.up(), state.with(LAYERS, total - 8), Block.NOTIFY_ALL);
            } else {
                Block.dropStack(world, pos, new ItemStack(Items.SNOWBALL, total - 8));
            }
        }
    }

    @Override
    public void onDestroyedOnLanding(World world, BlockPos pos, FallingBlockEntity entity) {
        int extra = layers(entity.getBlockState()) - 1;
        // Vanilla drops one block item afterwards. Add the remaining layer items.
        if (extra > 0) Block.dropStack(world, pos, new ItemStack(asItem(), extra));
    }
}
