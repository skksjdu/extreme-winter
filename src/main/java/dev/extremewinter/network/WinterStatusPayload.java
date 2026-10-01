package dev.extremewinter.network;
import dev.extremewinter.ExtremeWinter;
import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

/** Climate stage is independent of TemperaturePayload's danger stage. */
public record WinterStatusPayload(int winterStage,int weather,int nextBlizzardSeconds,int eventSeconds,long elapsedTicks) implements CustomPacketPayload {
    public static final Type<WinterStatusPayload> ID=new Type<>(Identifier.fromNamespaceAndPath(ExtremeWinter.ID,"winter_status"));
    public static final StreamCodec<RegistryFriendlyByteBuf,WinterStatusPayload> CODEC=new StreamCodec<>() {
        public WinterStatusPayload decode(RegistryFriendlyByteBuf b) {return new WinterStatusPayload(b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarInt(),b.readVarLong());}
        public void encode(RegistryFriendlyByteBuf b,WinterStatusPayload p) {b.writeVarInt(p.winterStage);b.writeVarInt(p.weather);b.writeVarInt(p.nextBlizzardSeconds);b.writeVarInt(p.eventSeconds);b.writeVarLong(p.elapsedTicks);}
    };
    public Type<? extends CustomPacketPayload> type() {return ID;}
}
