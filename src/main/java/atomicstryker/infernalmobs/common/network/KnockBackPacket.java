package atomicstryker.infernalmobs.common.network;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record KnockBackPacket(float xv, float zv) implements FabricPacket {

    public static final PacketType<KnockBackPacket> TYPE = PacketType.create(new ResourceLocation(InfernalMobsCore.MOD_ID, "knockback"), KnockBackPacket::read);

    private static KnockBackPacket read(FriendlyByteBuf buf) {
        return new KnockBackPacket(buf.readFloat(), buf.readFloat());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeFloat(xv);
        buf.writeFloat(zv);
    }

    @Override
    public PacketType<KnockBackPacket> getType() {
        return TYPE;
    }
}
