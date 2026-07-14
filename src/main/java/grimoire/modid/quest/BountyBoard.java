package grimoire.modid.quest;

import grimoire.modid.Grimoire;
import grimoire.modid.data.ModComponents;
import grimoire.modid.data.QuestProgressComponent;
import grimoire.modid.event.QuestTomeEvents;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import grimoire.modid.item.ModItems;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class BountyBoard {

    // if you change this please change mirror in GrimoireScreen check
    public static final int MAX_ACTIVE_BOUNTIES = 3;

    // current day tick check yknow
    public static long currentDay(ServerPlayerEntity player) {
        return player.getWorld().getTimeOfDay() / 24000L;
    }

    // ladder rules, mirrored in GrimoireScreen
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

    // gross code for a filter that excludes actives and today's completions
    public static void rollOffers(QuestProgressComponent progress) {
        int maxTier = highestUnlockedTier(progress);
        Random random = new Random();


        for (int tier = 1; tier <= maxTier; tier++) {
            TierConfig config = QuestManager.TIERS.get(tier);
            if (config == null) continue;

            List<String> pool = new ArrayList<>();
            for (Quest quest : QuestManager.QUESTS.values())
            {
                boolean lifetimeBlocked = !quest.repeatable() && progress.hasCompletedLifetime(quest.id());
                boolean prereqBlocked = !quest.requiresQuest().isEmpty()
                        && !progress.hasCompletedLifetime(quest.requiresQuest());


                if (quest.tier() == tier
                        && !lifetimeBlocked
                        && !prereqBlocked
                        && !progress.isActive(quest.id())
                        && !progress.hasCompleted(quest.id()))
                {
                    pool.add(quest.id());

                }
            }

            Collections.shuffle(pool, random);
            int count = Math.min(config.offersPerRotation(), pool.size());
            progress.setOffered(tier, pool.subList(0, count));
        }
    }

    // evicting quests that no longer exist in data and fixing ghost slots. syncs only if something is removed
    private static void sweepOrphans(ServerPlayerEntity player, QuestProgressComponent progress){
        boolean changed = progress.removeUnknownQuests(QuestManager.QUESTS.keySet());

        if (changed) {
            Grimoire.LOGGER.info("Evicted orphaned quest IDs for {}", player.getName().getString());
            ModComponents.QUEST_PROGRESS.sync(player);
        }

    }

    // this makes rotations sync with join and tome open and only then and sweeps orphan
    // also posts line of daily update for players with tome within inventory
    public static void ensureFreshRotation(ServerPlayerEntity player) {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(player);
        long day = currentDay(player);

        sweepOrphans(player, progress);

        if (progress.needsRoll(day)) {
            progress.startNewRotation(day);
            rollOffers(progress);
            ModComponents.QUEST_PROGRESS.sync(player);
            QuestTomeEvents.ROTATION_ROLLED.invoker().onRolled(player);

            if (player.getInventory().count(ModItems.GRIMOIRE_TOME) > 0) {
                player.sendMessage(Text.literal("[QuestTome] The Book is updated with new bargains for the day."), false);
            }
        }
    }


    // just manual reroll things hashtag girlboss also no new rotation for daily lockout
    public static void manualReroll(ServerPlayerEntity player) {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(player);
        long day = currentDay(player);

        if (!progress.canManualReroll(day)) {
            player.sendMessage(Text.literal("[QuestTome] The pages refuse to shuffle again today."), false);
            return;
        }

        progress.markManualReroll(day);
        rollOffers(progress);
        ModComponents.QUEST_PROGRESS.sync(player);
        QuestTomeEvents.BARGAIN_REROLLED.invoker().onRerolled(player);
        player.sendMessage(Text.literal("[QuestTome] The Book's pages flutter and rearrange..."), false);
    }
    // this one was super easy but i almost broke everything here lol. this is a public front door for player quest sweeps
    public static void sweepOrphansFor(ServerPlayerEntity player) {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(player);
        sweepOrphans(player, progress);
    }
}
