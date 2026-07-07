package grimoire.modid;

import com.google.gson.Gson;
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

public class QuestManager implements SimpleSynchronousResourceReloadListener {

    public static final Map<String, Quest> QUESTS = new HashMap<>();
    private static final Gson GSON = new Gson();

    @Override
    public Identifier getFabricId() {
        return new Identifier(Grimoire.MOD_ID, "quests");
    }

    @Override
    public void reload(ResourceManager manager) {
        QUESTS.clear();

        for (Map.Entry<Identifier, Resource> entry :
                manager.findResources("quests", path -> path.getPath().endsWith(".json")).entrySet()) {

            try (InputStreamReader reader = new InputStreamReader(entry.getValue().getInputStream())) {
                JsonObject json = GSON.fromJson(reader, JsonObject.class);

                String path = entry.getKey().getPath();
                String questId = path.substring("quests/".length(), path.length() - ".json".length());

                String title = json.get("title").getAsString();
                Item requiredItem = Registries.ITEM.get(new Identifier(json.get("required_item").getAsString()));
                int requiredCount = json.get("required_count").getAsInt();
                Item rewardItem = Registries.ITEM.get(new Identifier(json.get("reward_item").getAsString()));
                int rewardCount = json.get("reward_count").getAsInt();

                QUESTS.put(questId, new Quest(questId, title, requiredItem, requiredCount, rewardItem, rewardCount));

            } catch (Exception e) {
                System.err.println("[Grimoire] Failed to load quest " + entry.getKey() + ": " + e);
            }
        }

        System.out.println("[Grimoire] Loaded " + QUESTS.size() + " quest(s)");
    }
}