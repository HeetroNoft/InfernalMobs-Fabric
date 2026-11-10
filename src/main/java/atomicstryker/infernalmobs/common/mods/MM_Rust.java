package atomicstryker.infernalmobs.common.mods;

import atomicstryker.infernalmobs.common.MobModifier;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

public class MM_Rust extends MobModifier {

    private static String[] suffix = {"ofDecay", "theEquipmentHaunter"};
    private static String[] prefix = {"rusting", "decaying"};

    public MM_Rust() {
        super();
    }

    public MM_Rust(MobModifier next) {
        super(next);
    }

    @Override
    public String getModName() {
        return "Rust";
    }

    @Override
    public float onHurt(LivingEntity mob, DamageSource source, float damage) {
        if (isDirectAttack(source)
                && (source.getDirectEntity() instanceof Player p)
                && wantsToAttack(mob, p)) {
            ItemStack equippedStack = p.getItemBySlot(EquipmentSlot.MAINHAND);
            equippedStack.hurtAndBreak(4, (LivingEntity) source.getDirectEntity(), attacker -> attacker.broadcastBreakEvent(EquipmentSlot.MAINHAND));
        }

        return super.onHurt(mob, source, damage);
    }

    @Override
    public float onAttack(LivingEntity entity, DamageSource source, float damage) {
        if (entity instanceof Player) {
            hurtArmor((Player) entity, entity.damageSources().magic(), damage * 3);
        }
        return super.onAttack(entity, source, damage);
    }

    private void hurtArmor(Player player, DamageSource damageSource, float pDamageAmount) {
        if (pDamageAmount > 0.0F) {
            int i = (int) Math.max(1.0F, pDamageAmount / 4.0F);
            for (EquipmentSlot equipmentSlot : new EquipmentSlot[]{EquipmentSlot.FEET, EquipmentSlot.LEGS, EquipmentSlot.CHEST, EquipmentSlot.HEAD}) {
                ItemStack itemstack = player.getItemBySlot(equipmentSlot);
                if (!(damageSource.is(DamageTypeTags.IS_FIRE) && itemstack.getItem().isFireResistant())) {
                    itemstack.hurtAndBreak(i, player, owner -> owner.broadcastBreakEvent(equipmentSlot));
                    break;
                }
            }
        }
    }

    @Override
    protected String[] getModNameSuffix() {
        return suffix;
    }

    @Override
    protected String[] getModNamePrefix() {
        return prefix;
    }

}
