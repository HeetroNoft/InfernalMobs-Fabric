package atomicstryker.infernalmobs.common;

import com.mojang.serialization.Codec;
import net.fabricmc.fabric.api.attachment.v1.AttachmentRegistry;
import net.fabricmc.fabric.api.attachment.v1.AttachmentTarget;
import net.fabricmc.fabric.api.attachment.v1.AttachmentType;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;

/**
 * Persistent per-entity data, stored with the Fabric Data Attachment API
 */
public final class InfernalMobsData {

    /**
     * saved modifier string, or the non infernal marker
     */
    private static final AttachmentType<String> MODIFIERS = AttachmentRegistry.createPersistent(
            new ResourceLocation(InfernalMobsCore.MOD_ID, "modifiers"), Codec.STRING);

    private static final AttachmentType<Float> MAX_HEALTH = AttachmentRegistry.createPersistent(
            new ResourceLocation(InfernalMobsCore.MOD_ID, "max_health"), Codec.FLOAT);

    private InfernalMobsData() {
    }

    /**
     * forces class loading so the attachment types get registered during mod init
     */
    public static void init() {
    }

    public static String getSavedMods(LivingEntity entity) {
        return entity.getAttachedOrElse(MODIFIERS, "");
    }

    public static void setSavedMods(LivingEntity entity, String mods) {
        entity.setAttached(MODIFIERS, mods);
    }

    public static float getMaxHealth(LivingEntity entity, float fallback) {
        return entity.getAttachedOrElse(MAX_HEALTH, fallback);
    }

    public static void setMaxHealth(LivingEntity entity, float maxHealth) {
        entity.setAttached(MAX_HEALTH, maxHealth);
    }

    /**
     * checks whether the entity carries the given key in its saved nbt data (top level or as a fabric attachment id)
     * or as a scoreboard tag
     */
    public static boolean hasTag(LivingEntity entity, String tag, CompoundTag entityNbt) {
        if (entity.getTags().contains(tag)) {
            return true;
        }
        return entityNbt.contains(tag) || entityNbt.getCompound(AttachmentTarget.NBT_ATTACHMENT_KEY).contains(tag);
    }

    public static CompoundTag saveEntityNbt(LivingEntity entity) {
        return entity.saveWithoutId(new CompoundTag());
    }
}
