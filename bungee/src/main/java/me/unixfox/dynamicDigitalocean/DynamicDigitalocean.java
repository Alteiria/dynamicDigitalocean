package me.unixfox.dynamicDigitalocean;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import de.leonhard.storage.Toml;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Plugin;

public class DynamicDigitalocean extends Plugin {

    @Override
    public void onEnable() {
        Toml toml = new Toml("config", "plugins/dynamicDigitalocean");
        DigitaloceanApi digitaloceanApiWrapper = new DigitaloceanApi(this);
        toml.setDefault("general.apiKey", "yourapikey");
        toml.setDefault("general.region", "ams3");
        toml.setDefault("general.size", "s-2vcpu-4gb");
        toml.setDefault("general.sshKeyID", "0");
        toml.setDefault("general.imageID", "50944795");
        toml.setDefault("general.domain", "example.org");
        String domainName = toml.get("general.domain", "example.org");
        getLogger().info("Yay! It loads!");
        getProxy().getPluginManager().registerListener(this, new Events(this));
        refreshDOServers(digitaloceanApiWrapper, domainName);
    }

    public void refreshDOServers(DigitaloceanApi digitaloceanApiWrapper, String domainName) {
        ProxyServer.getInstance().getServers().forEach((k, v) -> {
            if (v.getName().toLowerCase().substring(0, 4).equals("dydo")) {
                String fqdn = v.getName().toLowerCase() + "." + domainName;
                if (digitaloceanApiWrapper.hasDroplet(fqdn)) {
                    String IPv4address = digitaloceanApiWrapper.getDropletFirstIPv4(fqdn);
                    if (IPv4address != "0.0.0.0") {
                        removeServer(v.getName());
                        addServer(v.getName(), new InetSocketAddress(IPv4address, 25565), v.getMotd(),
                                v.isRestricted());
                    }
                }
            }
        });
        getProxy().getScheduler().schedule(this, new Runnable() {
            @Override
            public void run() {
                refreshDOServers(digitaloceanApiWrapper, domainName);
            }
        }, 30, 0, TimeUnit.SECONDS);
    }

    public static void addServer(String name, InetSocketAddress address, String motd, boolean restricted) {
        ProxyServer.getInstance().getServers().put(name,
                ProxyServer.getInstance().constructServerInfo(name, address, motd, restricted));
    }

    public static void removeServer(String name) {
        ProxyServer.getInstance().getServers().remove(name);
    }
}
