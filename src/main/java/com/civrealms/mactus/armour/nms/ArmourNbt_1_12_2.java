package com.civrealms.mactus.armour.nms;

import java.util.UUID;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class ArmourNbt_1_12_2 implements ArmourNbt {

  public static final String CR_OWNER = "cr_owner";

  public ItemStack setOwner(ItemStack item, UUID owner) {
    net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
    NBTTagCompound compound = nmsItem.getTag();
    if (compound == null) {
      compound = new NBTTagCompound();
    }
    compound.setString(CR_OWNER, owner.toString());
    nmsItem.setTag(compound);
    return CraftItemStack.asBukkitCopy(nmsItem);
  }

  public UUID getOwner(ItemStack item) {
    net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
    NBTTagCompound compound = nmsItem.getTag();
    if (compound == null || !compound.hasKey(CR_OWNER)) {
      return null;
    }
    String crOwner = compound.getString(CR_OWNER);
    if (crOwner == null) {
      return null;
    }
    try {
      return UUID.fromString(crOwner);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
