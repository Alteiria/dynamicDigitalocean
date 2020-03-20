package me.unixfox.dynamicDigitalocean;

import net.md_5.bungee.api.plugin.Plugin;

public class DynamicDigitalocean extends Plugin {
    @Override
    public void onEnable() {
        getLogger().info("Yay! It loads!");
        getProxy().getPluginManager().registerListener(this, new Events());
    }
}
