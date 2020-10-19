package com.civrealms.mactus;

import com.civrealms.mactus.armour.ArmourType;
import com.civrealms.mactus.armour.nms.ArmourNbt;
import java.util.Map;
import java.util.UUID;
import java.util.WeakHashMap;
import java.util.logging.Logger;
import org.bukkit.ChatColor;
import org.bukkit.Effect;
import org.bukkit.Material;
import org.bukkit.attribute.Attribute;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDamageEvent.DamageModifier;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.player.PlayerAnimationEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.PotionMeta;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;
import org.bukkit.util.Vector;

public class EntityDamageListener implements Listener {

  public static int ILL_FIT_COOLDOWN = 60_000;

  private final Map<Entity, Long> animationTimer = new WeakHashMap<>();
  private final Map<Player, Long> lastJoined = new WeakHashMap<>();
  private final Map<Player, Long> illFitTimer = new WeakHashMap<>();
  private final Map<Player, Long> webCooldown = new WeakHashMap<>();

  private final Logger logger;
  private final ArmourNbt armourNbt;

  public EntityDamageListener(Logger logger, ArmourNbt armourNbt) {
    this.logger = logger;
    this.armourNbt = armourNbt;
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
  public void onGappleEat(PlayerItemConsumeEvent event) {
    if (event.getItem().getType() == Material.GOLDEN_APPLE
        && event.getItem().getDurability() == 0) {
      event.getPlayer().setCooldown(Material.GOLDEN_APPLE, 1200);
    }
  }

  @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
  public void onPotionSplash(PotionSplashEvent event) {
    PotionMeta pm = (PotionMeta) event.getPotion().getItem().getItemMeta();
    if (pm.getBasePotionData().getType().equals(PotionType.AWKWARD)
        || pm.getBasePotionData().getType().equals(PotionType.MUNDANE)
        || pm.getBasePotionData().getType().equals(PotionType.THICK)) {
      for (LivingEntity le : event.getAffectedEntities()) {
        if (le instanceof Player) {
          Player p = (Player) le;
          double currHealthPercent = (p.getHealth() / p.getAttribute(Attribute.GENERIC_MAX_HEALTH)
              .getDefaultValue());
          p.setHealth(
              Math.max(currHealthPercent - 0.2, 0.1) * p.getAttribute(Attribute.GENERIC_MAX_HEALTH)
                  .getDefaultValue());
        }
      }
      return;
    }
    for (PotionEffect pe : event.getPotion().getEffects()) {
      for (LivingEntity le : event.getAffectedEntities()) {
        if (pe.getType().equals(PotionEffectType.HARM) && le instanceof Player) {
          continue;
        }
        if (pm.getBasePotionData().getType().equals(PotionType.INSTANT_HEAL) && le instanceof Player
            && !le.isDead()) {
          event.setCancelled(true);
          Player p = (Player) le;
          double currHealthPercent = (p.getHealth() / p.getAttribute(Attribute.GENERIC_MAX_HEALTH)
              .getDefaultValue());
          p.setHealth(
              Math.min(currHealthPercent + 0.25, 1.0) * p.getAttribute(Attribute.GENERIC_MAX_HEALTH)
                  .getDefaultValue());
          continue;
        }
        le.removePotionEffect(pe.getType());
        le.addPotionEffect(pe);
      }
    }
    event.setCancelled(true);
  }

  @EventHandler
  public void onJoin(PlayerJoinEvent event) {
    this.lastJoined.put(event.getPlayer(), System.currentTimeMillis());
  }

  @EventHandler
  public void webCooldown(BlockPlaceEvent event) {
    if (event.getBlock().getType() == Material.WEB) {
      if (this.webCooldown.containsKey(event.getPlayer())
          && System.currentTimeMillis() - this.webCooldown.get(event.getPlayer()) < 5000) {
        event.setCancelled(true);
        event.getPlayer().sendMessage(ChatColor.RED + "Cooldown not complete for web placement.");
      } else {
        ItemStack[] inv = event.getPlayer().getInventory().getContents();
        for (ItemStack is : inv) {
          if (ArmourType.getArmorType(is) != null) {
            this.webCooldown.put(event.getPlayer(), System.currentTimeMillis());
            event.getPlayer().setCooldown(Material.WEB, 100);
            return;
          }
        }
      }
    }
  }

  @EventHandler(ignoreCancelled = false, priority = EventPriority.HIGHEST)
  public void onPlayerAnimation(PlayerAnimationEvent event) {
    this.animationTimer.put(event.getPlayer(), System.currentTimeMillis());
  }

  @SuppressWarnings("deprecation")
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

    if (entity instanceof Player) {
      Player player = (Player) entity;
      boolean joinedRecently = this.lastJoined.containsKey(player)
          && System.currentTimeMillis() - this.lastJoined.get(player) < 2000;
      String world = player.getWorld().getName();
      if ((cause == DamageCause.VOID || cause == DamageCause.SUFFOCATION
          || cause == DamageCause.DROWNING)
          && joinedRecently && world.equals("aqua")) {
        event.setCancelled(true);
      } else if (cause == DamageCause.FALL && joinedRecently && !world.equals("prison")) {
        event.setCancelled(true);
      }
    }

    double armourReduction = this.getArmourReduction(entity);
    double enchantReduction = this.getEnchantReduction(entity, cause);

    event.setDamage(DamageModifier.ARMOR, event.getDamage() * armourReduction);
    event.setDamage(DamageModifier.MAGIC, event.getDamage() * enchantReduction);

    //default "weapon" damage 1.5 covers fists and stuff and mobs like spiders
    //checks for projectile or melee because I'm not sure if fire damage while in a fire is "entity on entity" or not, this just clearly makes it mobs and players only.
    double weaponTargetDamage = 0.375;
    if (event instanceof EntityDamageByEntityEvent && (cause == DamageCause.PROJECTILE
        || cause == DamageCause.ENTITY_ATTACK)) {
      EntityDamageByEntityEvent byEntityEvent = (EntityDamageByEntityEvent) event;

      //cps ANIMATION EVENT VERSION, up here at top because it applies to mobs too
      Entity attacker = byEntityEvent.getDamager();
      if (!this.animationTimer.containsKey(attacker)) {
        this.animationTimer.put(attacker, System.currentTimeMillis()
            - 1000); //allow first hit to pass if hasn't swung yet since logging on
      }
      long animationGap = (System.currentTimeMillis() - this.animationTimer.get(attacker));
      double cpsMultiplier;
      if (animationGap > 238) { //6.8 cps or lower, 3 ticks at 20 tps, 2 ticks at 13 tps or lower
        cpsMultiplier = 1.0; //100%
      } else if (animationGap > 147) {
        cpsMultiplier = 0.9; //80%
      } else if (animationGap > 80) {
        cpsMultiplier = 0.7; //60%
      } else {
        event.setCancelled(true);
        if (attacker instanceof Player) {
          attacker.sendMessage(ChatColor.RED + "Attacking too quickly.");
        }
        return;
      }

      //protect squid from deathrays so that guardians don't take over the whole mob cap
      if (event.getEntity().getType().equals(EntityType.SQUID)
          && byEntityEvent.getDamager().getType().equals(EntityType.GUARDIAN)) {
        event.setCancelled(true);
        return;
      }

      if (event.getEntity().getType().equals(EntityType.ENDERMITE)) {
        return;
      }

      //choose weapon properly for bows or other weapons (bows require tracing back the ownership of the arrow...)
      ItemStack weapon = null;
      if (byEntityEvent.getDamager() instanceof Arrow && ((Arrow) (byEntityEvent.getDamager()))
          .getShooter() instanceof Player) {
        weapon = ((Player) (((Arrow) (byEntityEvent.getDamager())).getShooter())).getInventory()
            .getItemInMainHand();
      } else if (byEntityEvent.getDamager() instanceof Player) {
        weapon = ((Player) byEntityEvent.getDamager()).getInventory().getItemInMainHand();
      }

      //if it's a real weapon, substitute in special civrealms damage amounts
      if (weapon != null) {
        if (weapon.getType() == Material.WOOD_SWORD && weapon.hasItemMeta() && weapon.getItemMeta()
            .hasEnchant(Enchantment.KNOCKBACK)) {
          event.setCancelled(true);
          if ((attacker instanceof Player)) {
            attacker.sendMessage(ChatColor.RED
                + "KB test swords are disabled until further notice / until new testing is needed.");
          }
        }
        if (weapon.getType().equals(Material.BOW)) {
          //LOG.info("arrow vel: " + ((Arrow)((EntityDamageByEntityEvent)event).getDamager()).getVelocity());
          Vector v = byEntityEvent.getDamager().getVelocity();
          weaponTargetDamage =
              (v.length() / 3.0)
                  * 6.0; //7.5 typical full draw across a short field. 100 blocks above full draw straight down would be 9.3, limp wristed point blank 1.2 damage.
        } else if (weapon.getType().equals(Material.WOOD_SWORD)) {
          weaponTargetDamage = 0.1;
        } else if (weapon.getType().equals(Material.STONE_SWORD) || weapon.getType()
            .equals(Material.STONE_AXE)) {
          weaponTargetDamage = 1.2;
        } else if (weapon.getType().equals(Material.GOLD_SWORD) || weapon.getType()
            .equals(Material.GOLD_AXE)) {
          weaponTargetDamage = 2.65;
        } else if (weapon.getType().equals(Material.IRON_SWORD) || weapon.getType()
            .equals(Material.IRON_AXE)) {
          if (weapon.hasItemMeta() && weapon.getItemMeta().hasLore() && weapon.getItemMeta()
              .getLore().get(0).equals("Made of tempered steel")) {
            weaponTargetDamage = 4;
          } else {
            weaponTargetDamage = 1.8;
          }
        } else if (weapon.getType().equals(Material.DIAMOND_SWORD) || weapon.getType()
            .equals(Material.DIAMOND_AXE)) {
          weaponTargetDamage = 6.6;
        }

        // damage multiplier
        weaponTargetDamage *= 2;

        //boost damage for enchanted weapons
        if (weapon.getEnchantments().containsKey(Enchantment.ARROW_DAMAGE)) { //power
          weaponTargetDamage = weaponTargetDamage * Math
              .pow(1.17, weapon.getEnchantments().get(Enchantment.ARROW_DAMAGE));
        } else if (weapon.getEnchantments().containsKey(Enchantment.DAMAGE_ALL)) { //sharpness
          weaponTargetDamage = weaponTargetDamage * Math
              .pow(1.17, weapon.getEnchantments().get(Enchantment.DAMAGE_ALL));
        }

        //LOG.info("pre cps or crit modifier, target damage: " + weaponTargetDamage);

        //double damage versus mobs, prior to calculating absolute armor amounts just on the chance the mob has armor or pvp relevant enchants
        if (!(event.getEntity() instanceof Player)) {
          weaponTargetDamage = weaponTargetDamage * 2.0;
        }

        //now calculate the final 3 values for player on player, damage, armor absorb abs amount and enchant absorb abs amount:
        weaponTargetDamage = weaponTargetDamage * cpsMultiplier;
        double armorTargetDamageRemoved =
            -armourReduction * weaponTargetDamage; //arbitrarily choose to apply armor part first
        double enchantTargetDamageRemoved = -enchantReduction * weaponTargetDamage;

        //then apply them:
        if (!Double.isNaN(weaponTargetDamage)) {
          event.setDamage(weaponTargetDamage);
        }
        if (!Double.isNaN(armorTargetDamageRemoved)) {
          event.setDamage(DamageModifier.ARMOR, armorTargetDamageRemoved);
        }
        if (!Double.isNaN(enchantTargetDamageRemoved)) {
          event.setDamage(DamageModifier.MAGIC, enchantTargetDamageRemoved);
        }

        if ((event.getEntity() instanceof Player)) {
          this.logger.info("Name of person hit: " + ((Player) event.getEntity()).getDisplayName());
          this.logger
              .info("Health before: " + ((Player) event.getEntity()).getHealth() + " cps modifier: "
                  + cpsMultiplier + " Armor = " + armorTargetDamageRemoved + " Enchant = "
                  + enchantTargetDamageRemoved + " Dmg = " + weaponTargetDamage
                  + " Net = " + (weaponTargetDamage + armorTargetDamageRemoved
                  + enchantTargetDamageRemoved)
                  + " armourReduction = " + armourReduction
                  + " enchantReduction = " + enchantReduction);
        }
        //LOG.info("origDamage" + event.getDamage());
        //LOG.info("cps modifier: " + cpsMultiplier + " animation gap: " + AnimationGap);
        //LOG.info("post cps modifier, damage: " + event.getDamage());
        //LOG.info("FINAL ARMOR ABSORB = " + armorTargetDamageRemoved + " FINAL ENCHANT ABSORB = " + enchantTargetDamageRemoved + " WEAPON TARGET DMG = " + weaponTargetDamage);
        //LOG.info("Net pvp damage result" + (weaponTargetDamage + armorTargetDamageRemoved + enchantTargetDamageRemoved));

      }

    }

    //SPECIAL DAMAGE MODIFYING AREAS, so far only falling is modified from vanilla.
    if (cause.equals(DamageCause.FALL)) {
      if (event.getEntity() instanceof Player) {
        if (event.getEntity().getLocation().getBlock().getType().equals(Material.BIRCH_FENCE)
            || event.getEntity().getLocation().getBlock().getRelative(0, -1, 0).getType()
            .equals(Material.BIRCH_FENCE)) {
          event.setCancelled(true); //negate fall damage at the top or bottom of scaffolding
          return;
        }
      }
    }

    //decipe whether to apply health pot auto splash
    if (event.getEntity() instanceof Player) {
      Player p = (Player) event.getEntity();
      int potCount = -1;
      boolean potUsed = false;
      if (p.getHealth() < 6) {
        for (ItemStack is : p.getInventory().getContents()) {
          if (is != null && is.getType().equals(Material.SPLASH_POTION)) {
            PotionMeta pm = (PotionMeta) is.getItemMeta();
            double currHealthPercent = (p.getHealth() / p.getAttribute(Attribute.GENERIC_MAX_HEALTH)
                .getDefaultValue());
            if (pm.getBasePotionData().getType().equals(PotionType.INSTANT_HEAL)
                && currHealthPercent < 0.35
                && !potUsed) { //checks health again so it won't use them all at once. First check is to avoid searching an entinre inventory every damage.
              is.setAmount(0);
              p.setHealth(Math.min(currHealthPercent + 0.25, 1.0) * p
                  .getAttribute(Attribute.GENERIC_MAX_HEALTH).getDefaultValue());
              p.sendMessage(ChatColor.GREEN + "Healing Potion automatically used");
              p.getWorld().playEffect(p.getLocation(), Effect.POTION_BREAK, 0xFFFF0000);
              //p.getWorld().spawnParticle(Particle.SPELL_INSTANT, p.getLocation(),50);
              potUsed = true; //I want to keep looping to count all the pots even if one already used, but not use 2+ of them
              potCount++;
            }
          }
        }
      }
      this.logger.info("Health pots they had available (-1 is N/A): " + potCount);
    }
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
          // If you don't own the armour, it costs 5% protection per piece
          // This makes prot 4 you don't own equal to steel
          illFitPenalty -= 0.05;
        }
      }
    }

    if (illFitPenalty < 1 && entity instanceof Player) {
      long lastMessage = this.illFitTimer.get(entity);
      if ((lastMessage == 0 || System.currentTimeMillis() - lastMessage > ILL_FIT_COOLDOWN)
          && Math.random() < 0.3) {
        this.illFitTimer.put((Player) entity, System.currentTimeMillis());
        entity.sendMessage(
            ChatColor.RED + "Your ill-fitting armor pinches, chafes, and isn't helping much.");
      }
    }

    return (multiplier * illFitPenalty) / 4;
  }

  private double getArmourReduction(ArmourType type) {
    if (type == null) {
      return 0;
    }
    switch (type) {
      case DIAMOND:
        return 0.9;
      case IRON:
        return 0.85;
      case GOLD:
        return 0.75;
      case CHAIN:
        return 0.7;
      case LEATHER:
        return 0.65;
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
    // prot 1 -> 0.0148
    // prot 2 -> 0.0264
    // prot 3 -> 0.0348
    // prot 1 -> 0.0400
    return 0.004 * (10 - ((envProtLevels / 4D - 4) * (envProtLevels / 5D - 5)) / 2);
  }

  private double getProjectileProtectionReduction(int projProtLevels) {
    // proj prot 16 -> 0.0551
    return 0.0044 * (13.395 - ((projProtLevels / 4D - 4.7) * (projProtLevels / 5D - 5.7)) / 2);
  }
}
