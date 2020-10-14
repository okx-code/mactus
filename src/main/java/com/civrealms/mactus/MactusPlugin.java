package com.civrealms.mactus;

import com.civrealms.mactus.armour.WearInListener;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;

public class MactusPlugin extends JavaPlugin {
    @Override
    public void onEnable() {
        NmsHandler nmsHandler = new NmsHandler();

        PluginManager pm = this.getServer().getPluginManager();
        pm.registerEvents(new EntityDamageListener(this.getLogger(), nmsHandler.getArmourNbt()), this);
        pm.registerEvents(new WearInListener(nmsHandler.getArmourNbt()), this);
    }
}
