package grimoire.modid;

import grimoire.modid.advancement.BargainCompletedCriterion;
import grimoire.modid.event.GrimoireEvents;
import grimoire.modid.item.ModItems;
import grimoire.modid.network.ModNetworking;
import grimoire.modid.quest.BountyBoard;
import grimoire.modid.quest.QuestManager;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.minecraft.advancement.criterion.Criteria;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.ResourcePackActivationType;
import net.fabricmc.loader.api.FabricLoader;

import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Identifier;
import net.minecraft.resource.ResourceType;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Grimoire implements ModInitializer {
	public static final String MOD_ID = "grimoire";

	// grimoire is the modid for QuestTome, NOT QuestTome
	// the logger will announce grimoire

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize(){

		ResourceManagerHelper.registerBuiltinResourcePack(
				id("base_bargains"),
				FabricLoader.getInstance().getModContainer("grimoire").orElseThrow(),
				ResourcePackActivationType.DEFAULT_ENABLED
		);

		ModItems.register();
		ModNetworking.registerServerReceivers();
		ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(new QuestManager());

		// player join fresh board
			ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			BountyBoard.ensureFreshRotation(handler.player);
			ModNetworking.syncQuestsTo(handler.player);

			});
		BargainCompletedCriterion bargainCompletedCriterion = Criteria.register(new BargainCompletedCriterion());

		GrimoireEvents.BARGAIN_COMPLETED.register((player, questId, tier, patron) -> {
			bargainCompletedCriterion.trigger(player);
		});
		// successful reloads only! sweep orphans and push fresh quest data
		ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, resourceManager, success) -> {
			if (!success) return;

			for (ServerPlayerEntity player : server.getPlayerManager().getPlayerList()) {
				BountyBoard.sweepOrphansFor(player);
				ModNetworking.syncQuestsTo(player);
			}

		})
;}

	public static Identifier id(String path) {
		return new Identifier(MOD_ID, path);
	}
}
