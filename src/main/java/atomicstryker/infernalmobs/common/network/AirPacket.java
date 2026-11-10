package atomicstryker.infernalmobs.common.network;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record AirPacket(int air) implements FabricPacket {

    public static final PacketType<AirPacket> TYPE = PacketType.create(new ResourceLocation(InfernalMobsCore.MOD_ID, "airchoke"), AirPacket::read);

    private static AirPacket read(FriendlyByteBuf buf) {
        return new AirPacket(buf.readInt());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeInt(air);
    }

    @Override
    public PacketType<AirPacket> getType() {
        return TYPE;
    }
}
