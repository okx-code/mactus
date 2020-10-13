package com.civrealms.mactus.armour.nms;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public interface ArmourNbt {
  void setOwner(ItemStack item, UUID owner);
  UUID getOwner(ItemStack item);
}
