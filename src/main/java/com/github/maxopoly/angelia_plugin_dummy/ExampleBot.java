package com.github.maxopoly.angelia_plugin_dummy;

import com.github.maxopoly.angeliacore.connection.ServerConnection;
import com.github.maxopoly.angeliacore.plugin.AngeliaPlugin;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.Option;
import org.kohsuke.MetaInfServices;

@MetaInfServices(AngeliaPlugin.class)
public class ExampleBot extends AngeliaPlugin {

    public ExampleBot() {
        super("ExampleBot");
    }

    @Override
    public void start() {
        // TODO Auto-generated method stub

    }

    @Override
    public String getHelp() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected List<Option> createOptions() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    protected void parseOptions(ServerConnection connection, Map<String, List<String>> args) {
        // TODO Auto-generated method stub

    }

    @Override
    public void tearDown() {
        // TODO Auto-generated method stub

    }

    @Override
    public AngeliaPlugin transistionToNewConnection(ServerConnection newConnection) {
        // TODO Auto-generated method stub
        return null;
    }

}
