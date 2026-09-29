package dev.extremewinter.network;

import dev.extremewinter.ExtremeWinter;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;

/** S2C only; clients cannot submit a temperature. Sent only to the owning player. */
public record TemperaturePayload(double value, double minimum, double maximum, int stage) implements CustomPayload {
    public static final Id<TemperaturePayload> ID = new Id<>(Identifier.of(ExtremeWinter.ID, "temperature"));
    public static final PacketCodec<RegistryByteBuf, TemperaturePayload> CODEC = PacketCodec.tuple(
            PacketCodecs.DOUBLE, TemperaturePayload::value,
            PacketCodecs.DOUBLE, TemperaturePayload::minimum,
            PacketCodecs.DOUBLE, TemperaturePayload::maximum,
            PacketCodecs.VAR_INT, TemperaturePayload::stage,
            TemperaturePayload::new);

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}
