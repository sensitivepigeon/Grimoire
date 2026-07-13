package grimoire.modid.quest;

import net.minecraft.item.Item;

public record Quest(String id, String title, String lore, String description, int tier, int format, String patron,
                    Item requiredItem, int requiredCount,
                    Item rewardItem, int rewardCount, boolean repeatable) {
}