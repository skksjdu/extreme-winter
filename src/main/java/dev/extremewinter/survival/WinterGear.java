package dev.extremewinter.survival;

import com.mojang.serialization.Codec;
import dev.extremewinter.ExtremeWinter;
import dev.extremewinter.temperature.TemperatureData;
import dev.extremewinter.temperature.TemperatureModel;
import dev.extremewinter.temperature.WinterWorldState;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Portable effects use the same saved running clock as the winter, never calendar time. */
public final class WinterGear {
    private static final Codec<Long> NON_NEGATIVE_TICK = Codec.LONG.validate(t -> t >= 0
            ? com.mojang.serialization.DataResult.success(t) : com.mojang.serialization.DataResult.error(() -> "Negative running tick"));
    public static final DataComponentType<Boolean> INSULATION = component("insulation", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final DataComponentType<Boolean> FILLED = component("bottle_filled", Codec.BOOL, ByteBufCodecs.BOOL);
    public static final DataComponentType<Integer> CHARGE = component("bottle_charge_ticks", Codec.intRange(0, 200), ByteBufCodecs.VAR_INT);
    public static final DataComponentType<Long> EXPIRY = component("bottle_expiry_tick", NON_NEGATIVE_TICK, ByteBufCodecs.VAR_LONG);
    public static final DataComponentType<Long> LAST_CHARGE = component("bottle_last_charge_tick", NON_NEGATIVE_TICK, ByteBufCodecs.VAR_LONG);
    public static final TagKey<Item> ARMOR = TagKey.create(Registries.ITEM, id("insulatable_armor"));
    public static final AttachmentType<Long> STEW_UNTIL = AttachmentRegistry.create(id("warming_stew_until_tick"),
            b -> b.initializer(() -> 0L).persistent(Codec.LONG));
    public static final int BOTTLE_DURATION = 12 * 60 * 20;
    private static long clientClock;
    private WinterGear() { }
    public static Identifier id(String name) { return Identifier.fromNamespaceAndPath(ExtremeWinter.ID, name); }
    private static <T> DataComponentType<T> component(String name, Codec<T> codec,
            net.minecraft.network.codec.StreamCodec<? super net.minecraft.network.RegistryFriendlyByteBuf, T> stream) {
        return Registry.register(BuiltInRegistries.DATA_COMPONENT_TYPE, id(name),
                DataComponentType.<T>builder().persistent(codec).networkSynchronized(stream).build());
    }
    public static void initialize() { }
    public static boolean canLine(ItemStack stack) { return stack.is(ARMOR) && stack.has(DataComponents.EQUIPPABLE); }
    public static boolean lined(ItemStack stack) { return stack.getOrDefault(INSULATION, false); }
    public static double airMultiplier(ServerPlayer player) {
        double leather = 0, lining = 0;
        for (var slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            var stack = player.getItemBySlot(slot);
            if (!canLine(stack) || stack.get(DataComponents.EQUIPPABLE).slot() != slot) continue;
            double weight = switch (slot) { case HEAD, FEET -> .15; case CHEST -> .40; case LEGS -> .30; default -> 0; };
            if (stack.is(Items.LEATHER_HELMET) || stack.is(Items.LEATHER_CHESTPLATE)
                    || stack.is(Items.LEATHER_LEGGINGS) || stack.is(Items.LEATHER_BOOTS)) leather += weight;
            if (lined(stack)) lining += weight;
        }
        return TemperatureModel.gearAirMultiplier(leather, lining,
                player.getAttachedOrCreate(STEW_UNTIL) > WinterWorldState.get(player.level()).elapsedTicks());
    }
    public static long remaining(ItemStack stack, long now) {
        if (!stack.getOrDefault(FILLED, false)) return 0;
        return Math.clamp(stack.getOrDefault(EXPIRY, 0L) - now, 0, BOTTLE_DURATION);
    }
    public static double portableGain(ServerPlayer player) {
        long now = WinterWorldState.get(player.level()).elapsedTicks();
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(WinterItems.HOT_WATER_BOTTLE) && remaining(stack, now) > 0) return .03;
        }
        return 0;
    }
    public static void eatStew(ServerPlayer player) {
        TemperatureData.set(player, TemperatureModel.clamp(TemperatureData.get(player) + 10, ExtremeWinter.CONFIG));
        player.setAttached(STEW_UNTIL, WinterWorldState.get(player.level()).elapsedTicks() + 5 * 60 * 20);
    }
    public static long clientClock() { return clientClock; }
    public static void updateClientClock(long ticks) { clientClock = Math.max(0, ticks); }
    public static void tickClientClock() { if (clientClock < Long.MAX_VALUE) clientClock++; }
}
