package dev.extremewinter.survival;

import com.mojang.serialization.MapCodec;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.network.WinterStatusSync;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public final class WeatherInstrumentBlock extends Block {
    public static final MapCodec<WeatherInstrumentBlock> CODEC = simpleCodec(WeatherInstrumentBlock::new);
    public WeatherInstrumentBlock(Properties properties) { super(properties); }
    @Override public MapCodec<WeatherInstrumentBlock> codec() { return CODEC; }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (player instanceof ServerPlayer p) {
            var status = WinterStatusSync.status(p, ExtremeWinter.CONFIG);
            if (status.weather() < 0) p.sendSystemMessage(Component.translatable("message.extreme_winter.weather_unavailable"));
            else p.sendSystemMessage(Component.translatable("message.extreme_winter.weather_instrument",
                    Component.translatable("weather.extreme_winter." + status.weather()),
                    status.nextBlizzardSeconds(), status.eventSeconds()));
        }
        return InteractionResult.SUCCESS;
    }
    @Override protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
            Player player, InteractionHand hand, BlockHitResult hit) { return useWithoutItem(state, level, pos, player, hit); }
}
