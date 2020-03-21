package me.unixfox.dynamicDigitalocean;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class Events implements Listener {

    public Events() {
    }

    @EventHandler
    public void onConnect(ServerConnectEvent event) {
        DigitaloceanApi digitaloceanApiWrapper = new DigitaloceanApi();
        ProxiedPlayer player = event.getPlayer();
        ServerInfo server = event.getTarget();
        String serverName = server.getName().toLowerCase();
        System.out.println(serverName);
        CheckServer serverChecker = new CheckServer(server);
        if (!serverChecker.isOnline() && serverName.substring(0, 4).equals("dydo")) {
            System.out.println("digitalocean");
            event.setCancelled(true);
            player.disconnect(TextComponent.fromLegacyText("offline"));
            if (!digitaloceanApiWrapper.hasDroplet(serverName)) {
                digitaloceanApiWrapper.createDroplet(serverName);
            }
        }

    }

}
