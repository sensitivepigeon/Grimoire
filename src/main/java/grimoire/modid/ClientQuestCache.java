package grimoire.modid;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class ClientQuestCache {

    public static final List<Quest> QUESTS = new ArrayList<>();

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SYNC_QUESTS, (client, handler, buf, responseSender) -> {

            int count = buf.readInt();
            List<Quest> received = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                String id = buf.readString();
                String title = buf.readString();
                Item requiredItem = Registries.ITEM.get(new Identifier(buf.readString()));
                int requiredCount = buf.readInt();
                Item rewardItem = Registries.ITEM.get(new Identifier(buf.readString()));
                int rewardCount = buf.readInt();
                received.add(new Quest(id, title, requiredItem, requiredCount, rewardItem, rewardCount));
            }

            client.execute(() -> {
                QUESTS.clear();
                QUESTS.addAll(received);
            });
        });
    }
}