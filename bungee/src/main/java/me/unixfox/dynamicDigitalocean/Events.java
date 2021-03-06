package me.unixfox.dynamicDigitalocean;

import java.util.concurrent.TimeUnit;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class Events implements Listener {

    private final String domainName;
    private final DynamicDigitalocean plugin;

    public Events(final DynamicDigitalocean plugin) {
        this.plugin = plugin;
        this.domainName = plugin.getDigitalOceanConfig().getString("general.domain");
    }

    @EventHandler
    public void onConnect(ServerConnectEvent event) {
        DigitaloceanApi digitaloceanApiWrapper = plugin.getDigitalOceanWrapper();
        ProxiedPlayer player = event.getPlayer();
        ServerInfo server = event.getTarget();
        String serverName = server.getName().toLowerCase();
        String fqdn = serverName + "." + domainName;
        CheckServer serverChecker = new CheckServer(server);
        if (!serverChecker.isOnline() && serverName.substring(0, 4).equals("dydo")) {
            event.setCancelled(true);
            player.disconnect(TextComponent.fromLegacyText("Hello " + player.getDisplayName() + "!\n" + "The server is not ready yet. Please come back in 3 minutes."));
            plugin.getProxy().getScheduler().schedule(plugin, new Runnable() {
                @Override
                public void run() {
                    if (!digitaloceanApiWrapper.hasDroplet(fqdn)) {
                        digitaloceanApiWrapper.createDroplet(serverName);
                    }
                }
            }, 0, 0, TimeUnit.MINUTES);
        }

    }

}
