package com.civrealms.mactus.armour;

import com.civrealms.mactus.armour.nms.ArmourNbt;
import java.util.ArrayList;
import net.minecraft.server.v1_12_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.craftbukkit.v1_12_R1.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * "Wears in" player armour when a player takes damage wearing it,
 * identifying it uniquely to the wearer and givings nerfs if others try to wear it.
 */
public class WearInListener {
  private final ArmourNbt armourNbt;

  public WearInListener(ArmourNbt armourNbt) {
    this.armourNbt = armourNbt;
  }

  @EventHandler
  public void on(EntityDamageEvent event) {
    if (!(event.getEntity() instanceof Player)) {
      return;
    }
    Player player = (Player) event.getEntity();

    boolean worn = false;
    for (ItemStack armour : player.getEquipment().getArmorContents()) {
      ArmourType type = ArmourType.getArmorType(armour);
      if ((type == ArmourType.LEATHER && armour.getItemMeta().getLore().size() == 1)
          || (type == ArmourType.LEATHER && armour.getItemMeta().getLore().size() == 2 && armour.getItemMeta().getLore().contains("Branded Item"))
          || (armour.getItemMeta().getLore().contains("No Owner"))) {
        ItemMeta meta = armour.getItemMeta();
        ArrayList<String> loreArray = (ArrayList<String>) meta.getLore();
        if (armour.getItemMeta().getLore().contains("No Owner")) {
          loreArray.remove(1);
        }
        loreArray.add(player.getDisplayName());
        meta.setLore(loreArray);
        armour.setItemMeta(meta);

        this.armourNbt.setOwner(armour, player.getUniqueId());
        worn = true;
      }
    }

    if (worn) {
      player.sendMessage(ChatColor.GRAY + "Your have worn in your armour.");
    }
  }
}
