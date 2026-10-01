package dev.extremewinter.test.metrics;

import java.util.ArrayList;
import java.util.List;

/** Test-only, server-thread timing of real callbacks. Nested mod operations are counted once. */
public final class WinterWorkProfiler {
    private static boolean enabled;
    private static int depth;
    private static long started, accumulated;
    private static final List<Long> samples = new ArrayList<>();
    private WinterWorkProfiler() { }
    public static void enable() { enabled = true; depth = 0; accumulated = 0; samples.clear(); }
    public static void disable() { enabled = false; depth = 0; accumulated = 0; }
    public static void enter() { if (enabled && depth++ == 0) started = System.nanoTime(); }
    public static void leave() { if (enabled && --depth == 0) accumulated += System.nanoTime() - started; }
    public static void finishTick() {
        if (!enabled) return;
        if (depth != 0) throw new AssertionError("Unbalanced test timing scopes: " + depth);
        samples.add(accumulated); accumulated = 0;
    }
    public static List<Long> samples() { return List.copyOf(samples); }
}
