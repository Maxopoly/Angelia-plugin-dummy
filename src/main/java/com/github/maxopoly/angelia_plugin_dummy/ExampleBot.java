package com.github.maxopoly.angelia_plugin_dummy;

import org.apache.logging.log4j.Logger;

import com.github.maxopoly.angeliacore.connection.ServerConnection;
import com.github.maxopoly.angeliacore.libs.yaml.config.PluginConfig;
import com.github.maxopoly.angeliacore.plugin.AngeliaLoad;
import com.github.maxopoly.angeliacore.plugin.AngeliaPlugin;

@AngeliaLoad(name = "ExampleBot", version = "1.0")
public class ExampleBot extends AngeliaPlugin {

	@Override
	public void start() {
		// showcasing commonly needed objects and how to get them:

		// holds everything about the current connection
		ServerConnection connection = this.connection;
		// logger, use this instead of System.out
		Logger logger = connection.getLogger();
		// autoloaded yaml flatfile config
		PluginConfig config = getConfig();

	}

	@Override
	public void stop() {

	}

}
