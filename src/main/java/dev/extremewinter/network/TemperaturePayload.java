package dev.extremewinter.network;

import dev.extremewinter.ExtremeWinter;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

/** S2C only; clients cannot submit a temperature. Sent only to the owning player. */
public record TemperaturePayload(double value, double minimum, double maximum, int stage, int trend, int reasons, int warningSeconds, int protectionSeconds) implements CustomPacketPayload {
    public TemperaturePayload(double value, double minimum, double maximum, int stage) {
        this(value, minimum, maximum, stage, 0, 0, 0, 0);
    }
    public static final Type<TemperaturePayload> ID = new Type<>(Identifier.fromNamespaceAndPath(ExtremeWinter.ID, "temperature"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TemperaturePayload> CODEC = new StreamCodec<>() {
        public TemperaturePayload decode(RegistryFriendlyByteBuf b) {
            return new TemperaturePayload(b.readDouble(), b.readDouble(), b.readDouble(), b.readVarInt(),
                    b.readVarInt(), b.readVarInt(), b.readVarInt(), b.readVarInt());
        }
        public void encode(RegistryFriendlyByteBuf b, TemperaturePayload p) {
            b.writeDouble(p.value); b.writeDouble(p.minimum); b.writeDouble(p.maximum); b.writeVarInt(p.stage);
            b.writeVarInt(p.trend); b.writeVarInt(p.reasons); b.writeVarInt(p.warningSeconds); b.writeVarInt(p.protectionSeconds);
        }
    };

    @Override
    public Type<? extends CustomPacketPayload> type() { return ID; }
}
