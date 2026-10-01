package dev.extremewinter.survival;

/** A multiplier on native random growth attempts; never destroys crops or gates bonemeal. */
public final class WinterFarmingModel {
    private WinterFarmingModel() { }
    public static double growthChance(int stage, boolean overworld, boolean greenhouse) {
        if (!overworld || greenhouse || stage < 3) return 1;
        return stage == 3 ? .5 : .25;
    }
}
