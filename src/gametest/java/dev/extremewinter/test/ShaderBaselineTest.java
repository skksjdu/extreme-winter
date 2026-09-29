package dev.extremewinter.test;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.loader.api.FabricLoader;

/** Separate diagnostic JAR: runs Eclipse without Extreme Winter or its resource pack. */
public final class ShaderBaselineTest implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (FabricLoader.getInstance().isModLoaded("extreme_winter")) throw new AssertionError("Baseline must exclude Extreme Winter");
        try (var game = context.worldBuilder().create()) {
            game.getClientWorld().waitForChunksRender();
            context.waitTicks(40);
            context.runOnClient(client -> {
                try {
                    Class<?> api = Class.forName("net.irisshaders.iris.api.v0.IrisApi");
                    Object iris = api.getMethod("getInstance").invoke(null);
                    if (!(boolean) api.getMethod("isShaderPackInUse").invoke(iris)) throw new AssertionError("Eclipse is not active");
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
            });
            context.takeScreenshot("eclipse-without-extreme-winter");
        }
        System.out.println("TEST Eclipse baseline without Extreme Winter PASSED");
    }
}
