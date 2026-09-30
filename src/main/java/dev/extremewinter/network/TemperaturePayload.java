package dev.extremewinter.network;

import dev.extremewinter.ExtremeWinter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C only; clients cannot submit a temperature. Sent only to the owning player. */
public record TemperaturePayload(double value, double minimum, double maximum, int stage) implements CustomPacketPayload {
    public static final Type<TemperaturePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "temperature"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TemperaturePayload> CODEC = StreamCodec.composite(
            ByteBufCodecs.DOUBLE, TemperaturePayload::value,
            ByteBufCodecs.DOUBLE, TemperaturePayload::minimum,
            ByteBufCodecs.DOUBLE, TemperaturePayload::maximum,
            ByteBufCodecs.VAR_INT, TemperaturePayload::stage,
            TemperaturePayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}
