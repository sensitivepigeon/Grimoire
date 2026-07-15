package grimoire.modid.quest;

import net.minecraft.item.Item;

import java.util.List;

public record Quest(String id, String title, String lore, String description, int tier, String patron, int format,
                    Item requiredItem, int requiredCount,
                    List<RewardEntry> rewards, boolean repeatable, List<String> requiresQuest) {
}