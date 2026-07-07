package grimoire.modid;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class QuestManager implements SimpleSynchronousResourceReloadListener {

    public static final Map<String, Quest> QUESTS = new HashMap<>();
    public static final Map<Integer, TierConfig> TIERS = new HashMap<>();
    private static final Gson GSON = new Gson();

    @Override
    public Identifier getFabricId() {
        return new Identifier(Grimoire.MOD_ID, "quests");
    }

    @Override
    public void reload(ResourceManager manager) {
        loadQuests(manager);
        loadTiers(manager);
    }

    private void loadQuests(ResourceManager manager) {
        QUESTS.clear();

        for (Map.Entry<Identifier, Resource> entry :
                manager.findResources("quests", path -> path.getPath().endsWith(".json")).entrySet()) {

            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);

                String path = entry.getKey().getPath();
                String questId = path.substring("quests/".length(), path.length() - ".json".length());

                String title = json.get("title").getAsString();
                String lore = getStringOr(json, "lore", "");
                int tier = getIntOr(json, "tier", 1);
                Item requiredItem = Registries.ITEM.get(new Identifier(json.get("required_item").getAsString()));
                int requiredCount = json.get("required_count").getAsInt();
                Item rewardItem = Registries.ITEM.get(new Identifier(json.get("reward_item").getAsString()));
                int rewardCount = json.get("reward_count").getAsInt();

                QUESTS.put(questId, new Quest(questId, title, lore, tier,
                        requiredItem, requiredCount, rewardItem, rewardCount));

            } catch (Exception e) {
                System.err.println("[Grimoire] Failed to load quest " + entry.getKey() + ": " + e);
            }
        }

        System.out.println("[Grimoire] Loaded " + QUESTS.size() + " quest(s)");
    }

    private void loadTiers(ResourceManager manager) {
        TIERS.clear();

        Optional<Resource> resource = manager.getResource(new Identifier(Grimoire.MOD_ID, "tiers.json"));

        if (resource.isPresent()) {
            try (InputStreamReader reader = new InputStreamReader(resource.get().getInputStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);
                JsonArray tierArray = json.getAsJsonArray("tiers");

                for (JsonElement element : tierArray) {
                    JsonObject tierJson = element.getAsJsonObject();
                    int tier = tierJson.get("tier").getAsInt();
                    String name = getStringOr(tierJson, "name", "Tier " + tier);
                    int offers = getIntOr(tierJson, "offers_per_rotation", 3);
                    int toUnlock = getIntOr(tierJson, "completions_to_unlock_next", 5);

                    TIERS.put(tier, new TierConfig(tier, name, offers, toUnlock));
                }
            } catch (Exception e) {
                System.err.println("[Grimoire] Failed to load tiers.json: " + e);
            }
        }

        for (Quest quest : QUESTS.values()) {
            TIERS.computeIfAbsent(quest.tier(),
                    t -> new TierConfig(t, "Tier " + t, 3, 5));
        }

        System.out.println("[Grimoire] Loaded " + TIERS.size() + " tier(s)");
    }

    private static String getStringOr(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static int getIntOr(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }
}