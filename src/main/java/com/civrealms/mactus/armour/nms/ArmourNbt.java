package com.civrealms.mactus.armour.nms;

import java.util.UUID;
import org.bukkit.inventory.ItemStack;

public interface ArmourNbt {
  ItemStack setOwner(ItemStack item, UUID owner);
  UUID getOwner(ItemStack item);
}
