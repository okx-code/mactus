package com.civrealms.mactus.armour.nms;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public class DummyArmourNbt implements ArmourNbt {

  public void setOwner(ItemStack item, UUID owner) {

  }

  public UUID getOwner(ItemStack item) {
    return null;
  }
}
