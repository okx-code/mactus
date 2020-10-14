package com.civrealms.mactus.armour.nms;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class DummyArmourNbt implements ArmourNbt {

  public ItemStack setOwner(ItemStack item, UUID owner) {
    return item;
  }

  public UUID getOwner(ItemStack item) {
    return null;
  }
}
