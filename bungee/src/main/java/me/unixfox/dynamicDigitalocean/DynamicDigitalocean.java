package me.unixfox.dynamicDigitalocean;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

import de.leonhard.storage.Toml;
import net.md_5.bungee.api.ProxyServer;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class DynamicDigitalocean extends Plugin {

    private DigitaloceanApi digitaloceanApiWrapper = null;
    private Toml digitalOConfig = null;

    @Override
    public void onEnable() {
        this.digitalOConfig = new Toml("config", "plugins/dynamicDigitalocean");
        this.digitalOConfig.setDefault("general.apiKey", "yourapikey");
        this.digitalOConfig.setDefault("general.region", "ams3");
        this.digitalOConfig.setDefault("general.size", "s-2vcpu-4gb");
        this.digitalOConfig.setDefault("general.sshKeyID", "0");
        this.digitalOConfig.setDefault("general.imageID", "50944795");
        this.digitalOConfig.setDefault("general.domain", "example.org");
        this.digitaloceanApiWrapper = new DigitaloceanApi(this);
        getLogger().info("Yay! It loads!");
        getProxy().getPluginManager().registerListener(this, new Events(this));
        refreshDOServers(this.digitalOConfig.getString("general.domain"));
    }

    @Override
    public void onDisable() {
        getProxy().getPluginManager().unregisterListeners(this);
        getProxy().getScheduler().cancel(this);
    }

    private Runnable refreshServers = null;
    private ScheduledTask refreshServersTask = null;

    private void refreshServers0(final String domainName) {
        ProxyServer.getInstance().getServers().forEach((name, server) -> {
            if (server.getName().substring(0, 4).equalsIgnoreCase("dydo")) {
                String fqdn = server.getName().toLowerCase() + "." + domainName;
                if (digitaloceanApiWrapper.hasDroplet(fqdn)) {
                    String IPv4address = digitaloceanApiWrapper.getDropletFirstIPv4(fqdn);
                    if (IPv4address != null) {
                        removeServer(server.getName());
                        addServer(server.getName(), new InetSocketAddress(IPv4address, 25565), server.getMotd(),
                                server.isRestricted());
                    }
                }
            }
        });
    }

    public void refreshDOServers(final String domainName) {
        if (this.refreshServers == null && domainName != null) {
            this.refreshServers = () -> refreshServers0(domainName);
        }
        if (this.refreshServersTask != null) {
            getProxy().getScheduler().cancel(this.refreshServersTask);
        }
        if (domainName != null) {
            this.refreshServersTask = getProxy().getScheduler().schedule(this, this.refreshServers, 0, 30, TimeUnit.SECONDS);
        }
    }

    public DigitaloceanApi getDigitalOceanWrapper() {
        return digitaloceanApiWrapper;
    }

    public Toml getDigitalOceanConfig() {
        return digitalOConfig;
    }

    public static void addServer(String name, InetSocketAddress address, String motd, boolean restricted) {
        ProxyServer.getInstance().getServers().put(name,
                ProxyServer.getInstance().constructServerInfo(name, address, motd, restricted));
    }

    public static void removeServer(String name) {
        ProxyServer.getInstance().getServers().remove(name);
    }
}
