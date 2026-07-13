package grimoire.modid.quest;

import net.minecraft.item.Item;

public record Quest(String id, String title, String lore, String description, int tier, String patron, int format,
                    Item requiredItem, int requiredCount,
                    Item rewardItem, int rewardCount) {
}