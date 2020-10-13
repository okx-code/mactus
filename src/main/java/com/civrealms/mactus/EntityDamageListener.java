package com.civrealms.mactus;

import com.civrealms.mactus.armour.ArmourType;
import com.civrealms.mactus.armour.nms.ArmourNbt;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import org.bukkit.ChatColor;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.inventory.ItemStack;

public class EntityDamageListener implements Listener {
  public static int ILL_FIT_COOLDOWN = 60_000;

  private final Map<Player, Long> illFitTimer = new WeakHashMap<>();

  private final ArmourNbt armourNbt;

  public EntityDamageListener(ArmourNbt armourNbt) {
    this.armourNbt = armourNbt;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
  public void onEntityDamage(EntityDamageEvent event) {
    DamageCause cause = event.getCause();
    if (cause == DamageCause.ENTITY_SWEEP_ATTACK) {
      event.setCancelled(true);
      return;
    }
    if (!(event.getEntity() instanceof LivingEntity)) {
      return;
    }

    LivingEntity entity = (LivingEntity) event.getEntity();
    if (Double.isNaN(entity.getHealth()) && !(entity instanceof Player)) {
      // "remove weird buggy ghost cows and shit on hitting them"
      // idk if this is needed, but it's in the old plugin
      event.getEntity().remove();
      return;
    }

    double armourReduction = this.getArmourReduction(entity);
    double enchantReduction = this.getEnchantReduction(entity, cause);

    event.setDamage(DamageModifier.ARMOR, event.getDamage() * armourReduction);
    event.setDamage(DamageModifier.MAGIC, event.getDamage() * enchantReduction);
  }
  private double getArmourReduction(LivingEntity entity) {
    ItemStack[] contents = entity.getEquipment().getArmorContents();
    if (contents == null) {
      return 0;
    }

    double multiplier = 0;
    double illFitPenalty = 1;
    for (ItemStack item : contents) {
      if (item != null) {
        ArmourType type = ArmourType.getArmorType(item);
        multiplier += this.getArmourReduction(type);
        UUID owner = this.armourNbt.getOwner(item);
        if (type != ArmourType.CHAIN && owner != null && !entity.getUniqueId().equals(owner)) {
          // If you don't own the armour, it costs 4% protection per piece
          // This makes prot 4 you don't own equal to unenchanted diamond
          illFitPenalty -= 0.04;
        }
      }
    }

    if (illFitPenalty < 1 && entity instanceof Player) {
      long lastMessage = this.illFitTimer.get(entity);
      if ((lastMessage == 0 || System.currentTimeMillis() - lastMessage > ILL_FIT_COOLDOWN)
          && Math.random() < 0.3) {
        this.illFitTimer.put((Player) entity, System.currentTimeMillis());
        entity.sendMessage(ChatColor.RED + "Your ill-fitting armor pinches, chafes, and isn't helping much.");
      }
    }


    return (multiplier * illFitPenalty) / 4;
  }

  private double getArmourReduction(ArmourType type) {
    switch (type) {
      case DIAMOND:
        return 0.7;
      case IRON:
        return 0.65;
      case GOLD:
        return 0.6;
      case CHAIN:
        return 0.55;
      case LEATHER:
        return 0.5;
      default:
        return 0;
    }
  }

  private double getEnchantReduction(LivingEntity entity, DamageCause cause) {
    ItemStack[] contents = entity.getEquipment().getArmorContents();
    if (contents == null) {
      return 0;
    }

    int projProtLevels = 0;
    int envProtLevels = 0;

    for (ItemStack item : contents) {
      envProtLevels += item.getEnchantmentLevel(Enchantment.PROTECTION_ENVIRONMENTAL);
      if (cause == DamageCause.PROJECTILE) {
        projProtLevels += item.getEnchantmentLevel(Enchantment.PROTECTION_PROJECTILE);
      }
    }

    return this.getEnvironmentalProtectionReduction(envProtLevels)
        + this.getProjectileProtectionReduction(projProtLevels);
  }

  private double getEnvironmentalProtectionReduction(int envProtLevels) {
    // prot 1 -> 0.04
    // prot 2 -> 0.07
    // prot 3 -> 0.09
    // prot 1 -> 0.10
    double inv = 4 - (Math.min(envProtLevels, 16) / 4D);
    return ((inv * (inv + 1)) / 2 + 10) * 0.01;
  }

  private double getProjectileProtectionReduction(int projProtLevels) {
    return Math.min(projProtLevels, 16) * 0.01;
  }

}
