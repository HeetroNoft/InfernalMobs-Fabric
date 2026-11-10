package atomicstryker.infernalmobs.client;

import atomicstryker.infernalmobs.common.Config;
import atomicstryker.infernalmobs.common.InfernalMobsCore;
import atomicstryker.infernalmobs.common.MobModifier;
import atomicstryker.infernalmobs.common.SidedCache;
import atomicstryker.infernalmobs.common.mods.MM_Gravity;
import atomicstryker.infernalmobs.common.network.AirPacket;
import atomicstryker.infernalmobs.common.network.HealthPacket;
import atomicstryker.infernalmobs.common.network.KnockBackPacket;
import atomicstryker.infernalmobs.common.network.MobModsPacket;
import atomicstryker.infernalmobs.common.network.VelocityPacket;
import me.shedaniel.autoconfig.AutoConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientEntityEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.fabricmc.fabric.api.networking.v1.FabricPacket;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Enemy;

public class InfernalMobsClient implements ClientModInitializer {

    private static Minecraft mc;
    private static ClientLevel lastLevel;

    @Override
    public void onInitializeClient() {
        mc = Minecraft.getInstance();

        ClientPlayNetworking.registerGlobalReceiver(HealthPacket.TYPE, (packet, player, responseSender) -> onHealthPacket(packet.entID(), packet.health(), packet.maxhealth()));
        ClientPlayNetworking.registerGlobalReceiver(MobModsPacket.TYPE, (packet, player, responseSender) -> onMobModsPacketToClient(packet));
        ClientPlayNetworking.registerGlobalReceiver(AirPacket.TYPE, (packet, player, responseSender) -> OverlayChoking.handleAirPacket(packet));
        ClientPlayNetworking.registerGlobalReceiver(VelocityPacket.TYPE, (packet, player, responseSender) -> mc.player.push(packet.xv(), packet.yv(), packet.zv()));
        ClientPlayNetworking.registerGlobalReceiver(KnockBackPacket.TYPE, (packet, player, responseSender) -> MM_Gravity.knockBack(mc.player, packet.xv(), packet.zv()));

        // client starting point, also local servers
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> InfernalMobsCore.instance().initIfNeeded(handler.getLevel()));
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> SidedCache.getInfernalMobs(handler.getLevel()).clear());
        // Fabric API for 1.20.1 has no client world change event, the level instance changes on every dimension change
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (client.level != null && client.level != lastLevel) {
                InfernalMobsCore.clearAllElitesOfLevel(client.level);
            }
            lastLevel = client.level;
        });
        ClientEntityEvents.ENTITY_LOAD.register((entity, level) -> onEntityJoinedWorld(entity));
        ClientTickEvents.END_WORLD_TICK.register(level -> InfernalMobsCore.instance().onTick(level));

        HudRenderCallback.EVENT.register(new OverlayBossBar.InfernalMobsHealthBarGuiOverlay());
        HudRenderCallback.EVENT.register(new OverlayChoking.InfernalMobsChokingGuiOverlay());
        HudRenderCallback.EVENT.register(new RendererBossGlow.InfernalMobsBossGlowOverlay());

        // the loot lists get item suggestions in the config screen
        AutoConfig.getGuiRegistry(Config.class).registerPredicateProvider(new LootListGuiProvider(),
                field -> field.getName().startsWith("droppedItemIDs"));
    }

    private void onEntityJoinedWorld(Entity entity) {
        if (mc.player != null && (entity instanceof Mob || (entity instanceof LivingEntity && entity instanceof Enemy))) {
            MobModsPacket mobModsPacket = new MobModsPacket(mc.player.getName().getString(), entity.getId(), (byte) 0);
            sendToServer(mobModsPacket);
            InfernalMobsCore.LOGGER.trace("onEntityJoinedWorld {}, ent-id {} querying modifiers from server", entity, entity.getId());
        }
    }

    private void onHealthPacket(int entID, float health, float maxhealth) {
        Entity ent = Minecraft.getInstance().level.getEntity(entID);
        if (ent instanceof LivingEntity) {
            MobModifier mod = InfernalMobsCore.getMobModifiers((LivingEntity) ent);
            if (mod != null) {
                mod.setActualHealth(health, maxhealth);
            }
        }
    }

    private void onMobModsPacketToClient(MobModsPacket mobModsPacket) {
        InfernalMobsCore.instance().addRemoteEntityModifiers(mc.level, mobModsPacket.entID(), mobModsPacket.stringData());
    }

    /**
     * packets are optional, only send them if the server has Infernal Mobs installed
     */
    public static void sendToServer(FabricPacket payload) {
        if (ClientPlayNetworking.canSend(payload.getType())) {
            ClientPlayNetworking.send(payload);
        }
    }
}
