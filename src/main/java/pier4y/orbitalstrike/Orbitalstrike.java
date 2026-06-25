package pier4y.orbitalstrike;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pier4y.orbitalstrike.command.OscCommand;
import pier4y.orbitalstrike.event.FishingRodUseHandler;
import pier4y.orbitalstrike.strike.StrikeManager;

public class Orbitalstrike implements ModInitializer {
	public static final String MOD_ID = "orbitalstrike";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("Initializing Orbital Strike Mod...");

		// Register Brigadier Command
		CommandRegistrationCallback.EVENT.register(OscCommand::register);

		// Register End Server Tick to process active sequential strikes
		ServerTickEvents.END_SERVER_TICK.register(server -> StrikeManager.tick());

		// Register fishing rod interception handler
		FishingRodUseHandler.register();
	}
}