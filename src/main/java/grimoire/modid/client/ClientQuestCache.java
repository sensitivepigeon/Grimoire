package grimoire.modid.client;

import grimoire.modid.network.ModNetworking;
import grimoire.modid.quest.Quest;
import grimoire.modid.quest.TierConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ClientQuestCache {

    public static final List<Quest> QUESTS = new ArrayList<>();

    public static final Map<Integer, TierConfig> TIERS = new HashMap<>();

    public static Quest byId(String id) {
        for (Quest quest : QUESTS) {
            if (quest.id().equals(id)) return quest;
        }
        return null;
    }

    public static void registerClientReceivers() {
        ClientPlayNetworking.registerGlobalReceiver(ModNetworking.SYNC_QUESTS, (client, handler, buf, responseSender) -> {

            int count = buf.readInt();
            List<Quest> received = new ArrayList<>();

            for (int i = 0; i < count; i++) {
                String id = buf.readString();
                String title = buf.readString();
                String lore = buf.readString();
                String description = buf.readString();
                String patron = buf.readString();
                int format = buf.readInt();
                int tier = buf.readInt();

                String requiredItemId = buf.readString();
                Item requiredItem = Registries.ITEM.get(new Identifier(requiredItemId));

                int requiredCount = buf.readInt();
                String rewardItemID = buf.readString();
                Item rewardItem = Registries.ITEM.get(new Identifier(rewardItemID));


                int rewardCount = buf.readInt();

                boolean repeatable = buf.readBoolean();
                String requiresQuest = buf.readString();
                received.add(new Quest(id, title, lore, description, tier, patron, format, requiredItem, requiredCount, rewardItem, rewardCount, repeatable, requiresQuest));
            }

            int tierCount = buf.readInt();

            Map<Integer, TierConfig> receivedTiers = new HashMap<>();
            for (int i = 0; i < tierCount; i++) {
                int t = buf.readInt();
                receivedTiers.put(t, new TierConfig(t, buf.readString(), buf.readInt(), buf.readInt()));
            }

            client.execute(() -> {
                TIERS.clear();
                TIERS.putAll(receivedTiers);
                QUESTS.clear();
                QUESTS.addAll(received);
            });
        });
    }
}