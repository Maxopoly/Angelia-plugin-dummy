package com.github.maxopoly.angelia_plugin_dummy;

import java.util.HashSet;
import java.util.Set;

import com.github.maxopoly.angeliacore.actions.ActionLock;
import com.github.maxopoly.angeliacore.actions.CodeAction;
import com.github.maxopoly.angeliacore.actions.actions.DetectAndEatFood;
import com.github.maxopoly.angeliacore.connection.DisconnectReason;
import com.github.maxopoly.angeliacore.event.AngeliaEventHandler;
import com.github.maxopoly.angeliacore.event.AngeliaListener;
import com.github.maxopoly.angeliacore.event.events.HealthChangeEvent;
import com.github.maxopoly.angeliacore.event.events.HungerChangeEvent;
import com.github.maxopoly.angeliacore.event.events.PlayerSpawnEvent;
import com.github.maxopoly.angeliacore.plugin.AngeliaLoad;
import com.github.maxopoly.angeliacore.plugin.AngeliaPlugin;
import com.github.maxopoly.angeliacore.plugin.parameter.LoadPolicy;
import com.github.maxopoly.angeliacore.plugin.parameter.ParameterLoad;

/**
 * Logs off if an unauthorized player comes within render distance
 * 
 * Use like this:
 * 
 * runplugin safetybot allowedPlayers="Maxopoly Frensin ttk2"
 *
 */
@AngeliaLoad(name = "SafetyBot", version = "1.0")
public class SafetyBot extends AngeliaPlugin implements AngeliaListener {

	@ParameterLoad(configId="allowedPlayers")
	private String allowedPlayers;
	private Set<String> allowedPlayerSet;

	@AngeliaEventHandler
	public void damaged(HealthChangeEvent e) {
		if (e.getNewValue() < e.getOldValue()) {
			connection.getLogger().info("Received damage, health is " + e.getNewValue() + ". Logging off");
			connection.close(DisconnectReason.Intentional_Disconnect);
		}
	}

	private void eat() {
		DetectAndEatFood eat = new DetectAndEatFood(connection);
		connection.getActionQueue().queue(eat);
		connection.getActionQueue().queue(new CodeAction(connection) {

			@Override
			public void execute() {
				if (!eat.foundFood()) {
					connection.getLogger().info("Disconnecting as no food could be found");
					connection.close(DisconnectReason.Intentional_Disconnect);
				}
			}

			@Override
			public ActionLock[] getActionLocks() {
				return new ActionLock[0];
			}
		});
	}

	@AngeliaEventHandler
	public void hungerChange(HungerChangeEvent e) {
		if (e.getNewValue() > e.getOldValue() || e.getNewValue() > 15) {
			return;
		}
		eat();
	}

	@AngeliaEventHandler
	public void playerNearby(PlayerSpawnEvent e) {
		if (e.getOnlinePlayer() != null && e.getOnlinePlayer().getName() != null) {
			if (allowedPlayerSet.contains(e.getOnlinePlayer().getName().toLowerCase())) {
				connection.getLogger().info(e.getOnlinePlayer().getName()
						+ " entered radar distance, but he is whitelisted, so ignoring it");
				return;
			}
			connection.getLogger().info("Logging off, because " + e.getOnlinePlayer().getName() + " is nearby");
		}
		connection.close(DisconnectReason.Intentional_Disconnect);
	}

	@Override
	public void start() {
		allowedPlayerSet = new HashSet<>();
		if (allowedPlayers != null) {
			for (String player : allowedPlayers.split(" ")) {
				connection.getLogger().info("Adding " + player + " as allowed player");
				//we ignore upper and lower case for convenience
				allowedPlayerSet.add(player.toLowerCase());
			}
		}
		connection.getEventHandler().registerListener(this);
	}

	@Override
	public void stop() {
		connection.getEventHandler().unregisterListener(this);
	}
}
