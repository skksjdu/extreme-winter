package dev.extremewinter.client;

import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.environment.CanopySnow;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;

/** Reuse vanilla particles through the public level API; no weather/renderer replacement. */
public final class CanopySnowflakes {
    private CanopySnowflakes() { }

    public static void tick(ClientLevel world) {
        var player = Minecraft.getInstance().player;
        if (player != null && world.getGameTime() % 5 == 0) emit(world, player.blockPosition(), world.getRandom());
    }

    public static int emit(ClientLevel world, BlockPos origin, RandomSource random) {
        if (!ExtremeWinter.CONFIG.snowAccumulation || !world.dimension().equals(Level.OVERWORLD) || !world.isRaining()) return 0;
        int emitted = 0;
        for (int i = 0; i < 4; i++) {
            double x = origin.getX() + 0.5 + (random.nextDouble() - 0.5) * 12;
            double y = origin.getY() + 1 + random.nextDouble() * 3;
            double z = origin.getZ() + 0.5 + (random.nextDouble() - 0.5) * 12;
            var pos = BlockPos.containing(x, y, z);
            if (!CanopySnow.belowLeaves(world, pos) || !world.getBlockState(pos).isAir()
                    || !world.getBiome(pos).value().coldEnoughToSnow(pos, world.getSeaLevel())) continue;
            world.addParticle(ParticleTypes.SNOWFLAKE, x, y, z,
                    (random.nextDouble() - 0.5) * 0.02, -0.025, (random.nextDouble() - 0.5) * 0.02);
            emitted++;
        }
        return emitted;
    }
}
