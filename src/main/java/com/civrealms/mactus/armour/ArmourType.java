package com.civrealms.mactus.armour;

import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;

public enum ArmourType {
  DIAMOND(Material.DIAMOND_HELMET, Material.DIAMOND_CHESTPLATE, Material.DIAMOND_LEGGINGS, Material.DIAMOND_BOOTS),
  GOLD(Material.GOLD_HELMET, Material.GOLD_CHESTPLATE, Material.GOLD_LEGGINGS, Material.GOLD_BOOTS),
  LEATHER(Material.LEATHER_HELMET, Material.LEATHER_CHESTPLATE, Material.LEATHER_LEGGINGS, Material.LEATHER_BOOTS),
  CHAIN(Material.CHAINMAIL_HELMET, Material.CHAINMAIL_CHESTPLATE, Material.CHAINMAIL_LEGGINGS, Material.CHAINMAIL_BOOTS),
  IRON(Material.IRON_HELMET, Material.IRON_CHESTPLATE, Material.IRON_LEGGINGS, Material.IRON_BOOTS);

  private final Material[] armour;

  ArmourType(Material... armour) {
    this.armour = armour;
  }

  public static ArmourType getArmorType(ItemStack item) {
    if (item == null) {
      return null;
    }

    for (ArmourType armourType : values()) {
      for (Material material : armourType.armour) {
        if (material == item.getType()) {
          return armourType;
        }
      }
    }
    return null;
  }
}
