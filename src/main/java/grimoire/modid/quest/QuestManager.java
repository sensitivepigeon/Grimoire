package grimoire.modid.quest;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import grimoire.modid.Grimoire;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.resource.Resource;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.List;

import static net.minecraft.client.realms.util.JsonUtils.getStringOr;

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
                manager.findResources("quests", path ->
                        path.getNamespace().equals(Grimoire.MOD_ID) &&
                                path.getPath().endsWith(".json")).entrySet()) {


            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream(), StandardCharsets.UTF_8)) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);

                String path = entry.getKey().getPath();
                String questId = path.substring("quests/".length(), path.length() - ".json".length());

                String title = requireString(json, "title", entry.getKey());
                if (title == null) {
                    continue;
                }

                String lore = getStringOr(json, "lore", "");
                String description = getStringOr(json, "description", "");


                String patron = getStringOr(json, "patron", "");
                int format = getIntOr(json, "format", 1);
                int tier = getIntOr(json, "tier", 1);
                Item requiredItem = resolveItem(json, "required_item", entry.getKey());
                if (requiredItem == null) {
                    Grimoire.LOGGER.warn("Skipping quest {} because one or more item IDs could not be resolved", entry.getKey());
                    continue;
                }

                Integer requiredCount = requirePositiveInt(json, "required_count", entry.getKey());
                if (requiredCount == null) {
                    Grimoire.LOGGER.warn("Skipping quest {} because the required count is invalid", entry.getKey());
                    continue;
                }

                List<RewardEntry> rewards = parseRewards(json, entry.getKey());
                if (rewards.isEmpty()) {
                    Grimoire.LOGGER.warn("Skipping quest {} because it has no valid rewards", entry.getKey());
                    continue;
                }
                boolean repeatable = true;
                if (json.has("repeatable")) {
                    repeatable = json.get("repeatable").getAsBoolean();
                }

                List<String> requiresQuest = parseRequiresQuest(json);

                QUESTS.put(questId, new Quest(questId, title, lore, description, tier, patron, format,
                        requiredItem, requiredCount, rewards, repeatable, requiresQuest));

            } catch (Exception e) {
                Grimoire.LOGGER.warn("Failed to load quest {}", entry.getKey(), e);
            }
        }

        Grimoire.LOGGER.info("Loaded {} quest(s)", QUESTS.size());

        // this is done after quests load entirely
        for (Quest quest : QUESTS.values()) {
            for (String prereq : quest.requiresQuest()) {
                if (prereq.equals(quest.id())) {
                    Grimoire.LOGGER.warn("Quest '{}' lists itself as a prerequisite and it can never be offered.", quest.id());
                } else if (!QUESTS.containsKey(prereq)) {
                    Grimoire.LOGGER.warn("Quest '{}' requires '{}', which does not exist. It will stay hidden until the quest is present.", quest.id(), prereq);
                }
            }
        }
    }

    // rewards array or single reward

    private static List<RewardEntry> parseRewards(JsonObject json, Identifier fileName) {
        List<RewardEntry> rewards = new ArrayList<>();

        if (json.has("rewards") && json.get("rewards").isJsonArray()) {
            JsonArray rewardArray = json.getAsJsonArray("rewards");

            for (int i = 0; i < rewardArray.size(); i++) {
                if (!rewardArray.get(i).isJsonObject()) {
                    Grimoire.LOGGER.warn("Quest file {} has invalid reward entry at index {}", fileName, i);
                    continue;
                }

                JsonObject rewardJson = rewardArray.get(i).getAsJsonObject();
                Item rewardItem = resolveItem(rewardJson, "item", fileName);
                Integer rewardCount = requirePositiveInt(rewardJson, "count", fileName);

                if (rewardItem == null || rewardCount == null) {
                    Grimoire.LOGGER.warn("Quest file {} has invalid reward entry at index {}", fileName, i);
                    continue;
                }

                String nbt = rewardJson.has("nbt") ? rewardJson.get("nbt").getAsString() : "";
                rewards.add(new RewardEntry(rewardItem, rewardCount, nbt));
            }

            return rewards;
        }

        Item rewardItem = resolveItem(json, "reward_item", fileName);
        Integer rewardCount = requirePositiveInt(json, "reward_count", fileName);

        if (rewardItem != null && rewardCount != null) {
            rewards.add(new RewardEntry(rewardItem, rewardCount, ""));
        }

        return rewards;
    }


    private static Item resolveItem(JsonObject json, String keyName, Identifier fileName) {
        String rawId = getStringOr(json, keyName, "");
        Identifier id = Identifier.tryParse(rawId);

        if (rawId.isEmpty()) {
            Grimoire.LOGGER.warn("Quest file {} has empty {} item ID", fileName, keyName);
            return null;
        }

        if (id == null) {
            Grimoire.LOGGER.warn("Quest file {} has invalid {} item ID syntax: '{}'", fileName, keyName, rawId);
            return null;
        }

        if (!Registries.ITEM.containsId(id)) {
            Grimoire.LOGGER.warn("Quest file {} references unknown {} item ID: '{}'", fileName, keyName, rawId);
            return null;
        }

        return Registries.ITEM.get(id);
    }

    private static String requireString(JsonObject json, String keyName, Identifier fileName) {
        if (!json.has(keyName) || json.get(keyName).isJsonNull()) {
            Grimoire.LOGGER.warn("Quest file {} is missing required key field '{}'", fileName, keyName);
            return null;
        }

        return json.get(keyName).getAsString();
    }

    private static Integer requirePositiveInt(JsonObject json, String keyName, Identifier fileName) {
        if (!json.has(keyName) || json.get(keyName).isJsonNull()) {
            Grimoire.LOGGER.warn("Quest file {} is missing required number '{}'", fileName, keyName);
            return null;
        }

        if (!json.get(keyName).isJsonPrimitive() || !json.get(keyName).getAsJsonPrimitive().isNumber()) {
            Grimoire.LOGGER.warn("Quest file {} has a non-numeric value for '{}'", fileName, keyName);
            return null;
        }

        int value = json.get(keyName).getAsInt();
        if (value <= 0) {
            Grimoire.LOGGER.warn("Quest file {} has a non-positive required number '{}': {}", fileName, keyName, value);
            return null;
        }

        return value;
    }

    private static List<String> parseRequiresQuest(JsonObject json) {
        List<String> result = new ArrayList<>();
        if (!json.has("requires_quest")) {
            return result;                       // absent = no prereq
        }
        JsonElement elem = json.get("requires_quest");
        if (elem.isJsonArray()) {
            for (JsonElement e : elem.getAsJsonArray()) {
                String id = e.getAsString();
                if (!id.isEmpty()) result.add(id);   // skip empty strings defensively
            }
        } else {
            String id = elem.getAsString();
            if (!id.isEmpty()) result.add(id);       // single string -> length-1 list
        }
        return result;
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
                Grimoire.LOGGER.warn("Failed to load tiers.json:", e);
            }
        }

        for (Quest quest : QUESTS.values()) {
            TIERS.computeIfAbsent(quest.tier(),
                    t -> new TierConfig(t, "Tier " + t, 3, 5));
        }

        Grimoire.LOGGER.info("Loaded {} tier(s)", TIERS.size());
    }

    private static String getStringOr(JsonObject json, String key, String fallback) {
        return json.has(key) ? json.get(key).getAsString() : fallback;
    }

    private static int getIntOr(JsonObject json, String key, int fallback) {
        return json.has(key) ? json.get(key).getAsInt() : fallback;
    }
}