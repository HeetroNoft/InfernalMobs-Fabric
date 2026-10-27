package atomicstryker.infernalmobs.client;

import atomicstryker.infernalmobs.common.InfernalMobsCore;
import atomicstryker.infernalmobs.common.MobModifier;
import atomicstryker.infernalmobs.common.SidedCache;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElement;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;

import java.util.Map;

public class RendererBossGlow {

    protected static long nextParticleTimeMillis;

    public static class InfernalMobsBossGlowOverlay implements HudElement {
        @Override
        public void extractRenderState(@NotNull GuiGraphicsExtractor guiGraphics, DeltaTracker partialTick) {
            Minecraft mc = Minecraft.getInstance();
            Entity viewEnt = mc.getCameraEntity();
            if (mc.isPaused() || viewEnt == null) {
                return;
            }
            if (System.currentTimeMillis() < nextParticleTimeMillis) {
                return;
            }
            nextParticleTimeMillis = System.currentTimeMillis() + 100L;
            Vec3 curPos = viewEnt.position();
            Map<LivingEntity, MobModifier> mobsmap = SidedCache.getInfernalMobs(viewEnt.level());
            mobsmap.keySet().stream().filter(ent -> ent.shouldRenderAtSqrDistance(curPos.distanceToSqr(ent.position()))
                    && ent.isAlive()).forEach(ent -> ent.level().addParticle(ParticleTypes.WITCH,
                    ent.getX() + (ent.level().getRandom().nextDouble() - 0.5D) * (double) ent.getBbWidth(),
                    ent.getY() + ent.level().getRandom().nextDouble() * (double) ent.getBbHeight() - 0.25D,
                    ent.getZ() + (ent.level().getRandom().nextDouble() - 0.5D) * (double) ent.getBbWidth(),
                    (ent.level().getRandom().nextDouble() - 0.5D) * 2.0D,
                    -ent.level().getRandom().nextDouble(),
                    (ent.level().getRandom().nextDouble() - 0.5D) * 2.0D));
        }
    }
}
