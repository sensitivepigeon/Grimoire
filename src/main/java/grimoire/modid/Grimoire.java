package grimoire.modid;

import grimoire.modid.item.ModItems;
import grimoire.modid.network.ModNetworking;
import grimoire.modid.quest.BountyBoard;
import grimoire.modid.quest.QuestManager;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;

import net.minecraft.util.Identifier;
import net.minecraft.resource.ResourceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Grimoire implements ModInitializer {
	public static final String MOD_ID = "grimoire";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		ModItems.register();
		ModNetworking.registerServerReceivers();
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new QuestManager());
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			BountyBoard.ensureFreshRotation(handler.player);
			ModNetworking.syncQuestsTo(handler.player);
		});
	}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
