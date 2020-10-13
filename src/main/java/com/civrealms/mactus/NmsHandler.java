package com.civrealms.mactus;

import com.civrealms.mactus.armour.nms.ArmourNbt;
import com.civrealms.mactus.armour.nms.ArmourNbt_1_12_2;
import com.civrealms.mactus.armour.nms.DummyArmourNbt;
import org.bukkit.Bukkit;

public class NmsHandler {
  private final ArmourNbt armourNbt;

  public NmsHandler() {
    String[] serverPackage = Bukkit.getServer().getClass().getPackage().getName().split("\\.");
    if (serverPackage.length > 3 && serverPackage[3].equals("v1_12_R1")) {
      this.armourNbt = new ArmourNbt_1_12_2();
    } else {
      this.armourNbt = new DummyArmourNbt();
    }
  }

  public ArmourNbt getArmourNbt() {
    return this.armourNbt;
  }
}
