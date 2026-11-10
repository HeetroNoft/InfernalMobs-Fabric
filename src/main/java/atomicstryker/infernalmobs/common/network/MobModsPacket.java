package atomicstryker.infernalmobs.common.network;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.fabricmc.fabric.api.networking.v1.PacketType;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record MobModsPacket(String stringData, int entID, byte sentFromServer) implements FabricPacket {

    public static final PacketType<MobModsPacket> TYPE = PacketType.create(new ResourceLocation(InfernalMobsCore.MOD_ID, "mobmods"), MobModsPacket::read);

    private static MobModsPacket read(FriendlyByteBuf buf) {
        return new MobModsPacket(buf.readUtf(), buf.readInt(), buf.readByte());
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeUtf(stringData);
        buf.writeInt(entID);
        buf.writeByte(sentFromServer);
    }

    @Override
    public PacketType<MobModsPacket> getType() {
        return TYPE;
    }
}
