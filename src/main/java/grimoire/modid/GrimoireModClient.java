package grimoire.modid;

import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.MinecraftClient;

public class GrimoireModClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        // client-only setup goes here later
        ClientQuestCache.registerClientReceivers();
    }

    public static void openGrimoireScreen() {
        MinecraftClient.getInstance().setScreen(new GrimoireScreen());
    }
}