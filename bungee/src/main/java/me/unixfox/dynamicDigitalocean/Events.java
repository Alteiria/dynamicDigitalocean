package me.unixfox.dynamicDigitalocean;

import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.event.ServerConnectEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class Events implements Listener {

    public Events() {
    }

    @EventHandler
    public void onConnect(ServerConnectEvent event) {
        ProxiedPlayer player = event.getPlayer();
        CheckServer server = new CheckServer(event.getTarget());
        if (!server.isOnline()) {
            event.setCancelled(true);
            player.disconnect(TextComponent.fromLegacyText("offline"));
        }

    }
}
