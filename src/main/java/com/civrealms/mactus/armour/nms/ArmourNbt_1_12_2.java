package com.civrealms.mactus.armour.nms;

import java.util.UUID;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.inventory.ItemStack;

public class ArmourNbt_1_12_2 implements ArmourNbt {

  public void setOwner(ItemStack item, UUID owner) {
    net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
    NBTTagCompound compound = nmsItem.getTag();
    if (compound == null) {
      compound = new NBTTagCompound();
    }
    compound.setString("cr_owner", owner.toString());
    nmsItem.setTag(compound);
  }

  public UUID getOwner(ItemStack item) {
    net.minecraft.server.v1_12_R1.ItemStack nmsItem = CraftItemStack.asNMSCopy(item);
    NBTTagCompound compound = nmsItem.getTag();
    if (compound == null) {
      return null;
    }
    String crOwner = compound.getString("cr_owner");
    if (crOwner == null) {
      return null;
    }
    return UUID.fromString(crOwner);
  }
}
