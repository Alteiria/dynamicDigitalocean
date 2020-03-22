package me.unixfox.dynamicDigitalocean;

import de.leonhard.storage.Toml;
import net.md_5.bungee.api.plugin.Plugin;

public class DynamicDigitalocean extends Plugin {
    @Override
    public void onEnable() {
        Toml toml = new Toml("config", "plugins/dynamicDigitalocean");
        toml.setDefault("general.apiKey", "yourapikey");
        toml.setDefault("general.region", "ams3");
        toml.setDefault("general.size", "s-2vcpu-4gb");
        toml.setDefault("general.sshKeyID", "0");
        toml.setDefault("general.imageID", "50944795");
        toml.setDefault("general.domain", "example.org");
        getLogger().info("Yay! It loads!");
        getProxy().getPluginManager().registerListener(this, new Events(this));
    }
}
