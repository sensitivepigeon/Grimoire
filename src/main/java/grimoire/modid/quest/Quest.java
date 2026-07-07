package grimoire.modid.quest;

import net.minecraft.item.Item;

public record Quest(String id, String title, String lore, int tier,
                    Item requiredItem, int requiredCount,
                    Item rewardItem, int rewardCount) {
}