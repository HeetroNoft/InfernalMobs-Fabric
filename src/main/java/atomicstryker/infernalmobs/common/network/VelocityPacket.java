package atomicstryker.infernalmobs.common.network;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record VelocityPacket(float xv, float yv, float zv) implements FabricPacket {

    public static final PacketType<VelocityPacket> TYPE = PacketType.create(new ResourceLocation(InfernalMobsCore.MOD_ID, "velocity"), VelocityPacket::read);

    private static VelocityPacket read(FriendlyByteBuf buf) {
        return new VelocityPacket(buf.readFloat(), buf.readFloat(), buf.readFloat());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(xv);
        buf.writeFloat(yv);
        buf.writeFloat(zv);
    }

    @Override
    public PacketType<VelocityPacket> getType() {
        return TYPE;
    }
}
