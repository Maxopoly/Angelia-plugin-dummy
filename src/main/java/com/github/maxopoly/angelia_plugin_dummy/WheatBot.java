package com.github.maxopoly.angelia_plugin_dummy;

import com.github.maxopoly.angeliacore.actions.ActionLock;
import com.github.maxopoly.angeliacore.actions.ActionQueue;
import com.github.maxopoly.angeliacore.actions.CodeAction;
import com.github.maxopoly.angeliacore.actions.actions.DetectAndEatFood;
import com.github.maxopoly.angeliacore.actions.actions.LookAtAndBreakBlock;
import com.github.maxopoly.angeliacore.actions.actions.MoveTo;
import com.github.maxopoly.angeliacore.connection.DisconnectReason;
import com.github.maxopoly.angeliacore.connection.ServerConnection;
import com.github.maxopoly.angeliacore.event.AngeliaEventHandler;
import com.github.maxopoly.angeliacore.event.AngeliaListener;
import com.github.maxopoly.angeliacore.event.events.ActionQueueEmptiedEvent;
import com.github.maxopoly.angeliacore.event.events.HealthChangeEvent;
import com.github.maxopoly.angeliacore.event.events.HungerChangeEvent;
import com.github.maxopoly.angeliacore.model.location.Location;
import com.github.maxopoly.angeliacore.model.location.MovementDirection;
import com.github.maxopoly.angeliacore.plugin.AngeliaPlugin;
import com.github.maxopoly.angeliacore.util.HorizontalField;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.apache.commons.cli.Option;
import org.apache.logging.log4j.Logger;
import org.kohsuke.MetaInfServices;

@MetaInfServices(AngeliaPlugin.class)
public class WheatBot extends AngeliaPlugin implements AngeliaListener {

	protected ServerConnection connection;
	private Logger logger;
	protected HorizontalField field;
	protected Iterator<Location> locIterator;
	protected ActionQueue queue;
	private int wheat;

	public WheatBot() {
		super("WheatBot");
	}

	@Override
	public void start() {
		connection.getEventHandler().registerListener(this);
		if (connection.getPlayerStatus().getHunger() < 20) {
			eat();
		}
		queueEmpty(null);
	}


    @AngeliaEventHandler
    public void queueEmpty(ActionQueueEmptiedEvent e) {
        if (locIterator.hasNext()) {
            Location target = locIterator.next();
            breakWheat(target);
        } else {
            atEndOfField();
        }
    }


	private void atEndOfField() {
	    //we're done, so let's log off
		logger.info("Harvested entire field for an estimated total of " + wheat + " wheat");
		connection.close(DisconnectReason.Intentional_Disconnect);
	}

	@AngeliaEventHandler
	public void hungerChange(HungerChangeEvent e) {
	    //eat if hungry
		if (e.getNewValue() > e.getOldValue()) {
			return;
		}
		eat();
	}

	@AngeliaEventHandler
	public void damaged(HealthChangeEvent e) {
	    //log off if damaged
		if (e.getNewValue() < e.getOldValue()) {
			connection.getLogger().info("Received damage, health is " + e.getNewValue() + ". Logging off");
			connection.close(DisconnectReason.Intentional_Disconnect);
		}
	}

	   private void eat() {
	        final DetectAndEatFood eat = new DetectAndEatFood(connection);
	        queue.queue(eat);
	        queue.queue(new CodeAction(connection) {

	            @Override
	            public ActionLock[] getActionLocks() {
	                return new ActionLock[0];
	            }

	            @Override
	            public void execute() {
	                if (!eat.foundFood()) {
	                    logger.info("Disconnecting as no food could be found");
	                    connection.close(DisconnectReason.Intentional_Disconnect);
	                }
	            }
	        });
	    }

	    private void breakWheat(Location loc) {
	        queue.queue(new LookAtAndBreakBlock(connection, loc, 0));
	        queue.queue(new MoveTo(connection, loc.getBlockCenterXZ(), MoveTo.WALKING_SPEED));
	    }

	@Override
	public String getHelp() {
		return "Harvests a wheat field";
	}

	@Override
	protected void parseOptions(ServerConnection connection, Map<String, List<String>> args) {
		this.connection = connection;
		this.logger = connection.getLogger();
		this.queue = connection.getActionQueue();
		int lowerX, upperX, lowerZ, upperZ, y;
		MovementDirection startingDirection, secondaryDirection;
		List<String> xCoords = args.get("x");
		List<String> zCoords = args.get("z");
		try {
			lowerX = Integer.parseInt(xCoords.get(0));
			upperX = Integer.parseInt(xCoords.get(1));
			if (upperX < lowerX) {
				// swap them
				int temp = lowerX;
				lowerX = upperX;
				upperX = temp;
			}
			lowerZ = Integer.parseInt(zCoords.get(0));
			upperZ = Integer.parseInt(zCoords.get(1));
			if (upperZ < lowerZ) {
				// swap them
				int temp = lowerZ;
				lowerZ = upperZ;
				upperZ = temp;
			}
		} catch (NumberFormatException e) {
			connection.getLogger().info("One of the provided coordinates was not a proper number");
			finish();
			return;
		}
		y = connection.getPlayerStatus().getLocation().getBlockY();
		try {
			startingDirection = MovementDirection.valueOf(args.get("dir1").get(0).toUpperCase());
			secondaryDirection = MovementDirection.valueOf(args.get("dir2").get(0).toUpperCase());
		} catch (IllegalArgumentException e) {
			connection.getLogger().info("One of the provided movement directions could not be parsed");
			finish();
			return;
		}
		this.field = new HorizontalField(lowerX, upperX, lowerZ, upperZ, y, startingDirection, secondaryDirection,
				true, 1);
		this.locIterator = field.iterator();
		if (args.containsKey("ff")) {
			// fast forward to current location
			Location playerLoc = connection.getPlayerStatus().getLocation().toBlockLocation();
			boolean found = false;
			// check starting location
			if (playerLoc.equals(field.getStartingLocation())) {
				found = true;
			}
			while (!found && locIterator.hasNext()) {
				Location loc = locIterator.next();
				if (loc.equals(playerLoc)) {
					found = true;
					break;
				}
			}
			if (!found) {
				// could not fast forward
				connection.getLogger().warn(
						"Could not fast forward to player location as it was not inside the field. Exiting.");
				finish();
			}
			// already there
			return;
		}
		queue.queue(new MoveTo(connection, field.getStartingLocation().getBlockCenterXZ(), MoveTo.SPRINTING_SPEED));

	}

	@Override
	public void tearDown() {
		connection.getEventHandler().unregisterListener(this);
	}

	@Override
	protected List<Option> createOptions() {
		List<Option> options = new LinkedList<Option>();
		options.add(Option.builder("x").longOpt("x").numberOfArgs(2).required()
				.desc("lower and upper x coordinates limiting the bounding box within which the bot will mine").build());
		options.add(Option.builder("z").longOpt("z").numberOfArgs(2).required()
				.desc("lower and upper z coordinates limiting the bounding box within which the bot will mine").build());
		options.add(Option.builder("dir1").longOpt("startingDirection").numberOfArgs(1).required()
				.desc("Cardinal direction in which the bot will begin to move").build());
		options.add(Option.builder("dir2").longOpt("secondaryDirection").numberOfArgs(1).required()
				.desc("Secondary direction in which the bot will move after finish a line").build());
		options.add(Option.builder("ff").longOpt("fast-forward").hasArg(false).required(false).build());
		return options;
	}

	@Override
	public AngeliaPlugin transistionToNewConnection(ServerConnection newConnection) {
		tearDown();
		this.connection = newConnection;
		this.logger = newConnection.getLogger();
		this.queue = newConnection.getActionQueue();
		connection.getEventHandler().registerListener(this);
		return this;
	}

}
