package atomicstryker.infernalmobs.common.network;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record HealthPacket(String stringData, int entID, float health, float maxhealth) implements FabricPacket {

    public static final PacketType<HealthPacket> TYPE = PacketType.create(new ResourceLocation(InfernalMobsCore.MOD_ID, "health"), HealthPacket::read);

    private static HealthPacket read(FriendlyByteBuf buf) {
        return new HealthPacket(buf.readUtf(), buf.readInt(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(stringData);
        buf.writeInt(entID);
        buf.writeFloat(health);
        buf.writeFloat(maxhealth);
    }

    @Override
    public PacketType<HealthPacket> getType() {
        return TYPE;
    }
}
