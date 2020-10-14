package com.civrealms.mactus.armour;

import com.civrealms.mactus.armour.nms.ArmourNbt;
import java.util.ArrayList;
import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

/**
 * "Wears in" player armour when a player takes damage wearing it, identifying it uniquely to the
 * wearer and givings nerfs if others try to wear it.
 */
public class WearInListener implements Listener {

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
    ItemStack[] armorContents = player.getEquipment().getArmorContents();
    for (int i = 0; i < armorContents.length; i++) {
      ItemStack armour = armorContents[i];
      ArmourType type = ArmourType.getArmorType(armour);
      if (type != null && this.armourNbt.getOwner(armour) != null) {
        ItemMeta meta = armour.getItemMeta();
        List<String> loreArray = meta.getLore();
        if (loreArray == null) {
          loreArray = new ArrayList<>();
        }
        int noOwnerIndex = loreArray.indexOf("No Owner");
        if (noOwnerIndex >= 0) {
          loreArray.remove(noOwnerIndex);
        }
        if (!loreArray.contains(player.getDisplayName())) {
          loreArray.add(player.getDisplayName());
        }
        meta.setLore(loreArray);
        armour.setItemMeta(meta);

        armorContents[i] = this.armourNbt.setOwner(armour, player.getUniqueId());
        worn = true;
      }
    }

    if (worn) {
      player.sendMessage(ChatColor.GRAY + "Your have worn in your armour.");
    }
  }
}
