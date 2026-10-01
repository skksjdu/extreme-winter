package dev.extremewinter.client;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.client.hud.TemperatureHud;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Small native effects only. Server weather, heat and damage do not depend on these switches. */
public final class SnowstormEffects {
    private SnowstormEffects() { }
    public static void tick(ClientLevel world) {
        var player = Minecraft.getInstance().player;
        var status = TemperatureHud.winter();
        if (player == null || status == null || status.weather() != 3 || !world.dimension().equals(Level.OVERWORLD)) return;
        if (world.getGameTime() % 5 == 0) emit(world, player.blockPosition(), world.getRandom());
        if (ExtremeWinter.CONFIG.blizzardWind && world.getGameTime() % 100 == 0) {
            boolean roof = TemperatureHud.current() != null && (TemperatureHud.current().reasons() & 1) != 0;
            world.playLocalSound(player.getX(), player.getY(), player.getZ(), SoundEvents.ELYTRA_FLYING,
                    SoundSource.WEATHER, roof ? .035f : .12f, .7f, false);
        }
    }
    public static int emit(ClientLevel world, BlockPos origin, RandomSource random) {
        var status = TemperatureHud.winter();
        if (!ExtremeWinter.CONFIG.blizzardParticles || status == null || status.weather() != 3
                || !world.dimension().equals(Level.OVERWORLD)) return 0;
        int emitted = 0;
        for (int i = 0; i < 4; i++) {
            double x = origin.getX() + .5 + (random.nextDouble() - .5) * 12;
            double y = origin.getY() + 1 + random.nextDouble() * 3;
            double z = origin.getZ() + .5 + (random.nextDouble() - .5) * 12;
            var pos = BlockPos.containing(x, y, z);
            if (!world.hasChunkAt(pos) || !world.getBlockState(pos).isAir() || !world.canSeeSky(pos)
                    || !world.getBiome(pos).value().coldEnoughToSnow(pos, world.getSeaLevel())) continue;
            world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z, .10, -.035, .025);
            emitted++;
        }
        return emitted;
    }
}
