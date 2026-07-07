package grimoire.modid.quest;

import grimoire.modid.data.ModComponents;
import grimoire.modid.data.QuestProgressComponent;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BountyBoard {

    public static final int MAX_ACTIVE_BOUNTIES = 3;

    public static long currentDay(ServerPlayerEntity player) {
        return player.getWorld().getTimeOfDay() / 24000L;
    }

    public static int highestUnlockedTier(QuestProgressComponent progress) {
        int tier = 1;
        while (QuestManager.TIERS.containsKey(tier + 1)) {
            TierConfig config = QuestManager.TIERS.get(tier);
            if (progress.getCompletions(tier) >= config.completionsToUnlockNext()) {
                tier++;
            } else {
                break;
            }
        }
        return tier;
    }

    public static void rollOffers(QuestProgressComponent progress) {
        int maxTier = highestUnlockedTier(progress);
        Random random = new Random();

        for (int tier = 1; tier <= maxTier; tier++) {
            TierConfig config = QuestManager.TIERS.get(tier);
            if (config == null) continue;

            List<String> pool = new ArrayList<>();
            for (Quest quest : QuestManager.QUESTS.values()) {
                if (quest.tier() == tier
                        && !progress.isActive(quest.id())
                        && !progress.hasCompleted(quest.id())) {
                    pool.add(quest.id());
                }
            }

            Collections.shuffle(pool, random);
            int count = Math.min(config.offersPerRotation(), pool.size());
            progress.setOffered(tier, pool.subList(0, count));
        }
    }

    public static void ensureFreshRotation(ServerPlayerEntity player) {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(player);
        long day = currentDay(player);

        if (progress.needsRoll(day)) {
            progress.startNewRotation(day);
            rollOffers(progress);
            ModComponents.QUEST_PROGRESS.sync(player);
        }
    }

    public static void manualReroll(ServerPlayerEntity player) {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(player);
        long day = currentDay(player);

        if (!progress.canManualReroll(day)) {
            player.sendMessage(Text.literal("The pages refuse to shuffle again today."), false);
            return;
        }

        progress.markManualReroll(day);
        rollOffers(progress);
        ModComponents.QUEST_PROGRESS.sync(player);
        player.sendMessage(Text.literal("The Grimoire's pages flutter and rearrange..."), false);
    }
}
