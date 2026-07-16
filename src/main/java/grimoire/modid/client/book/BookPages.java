package grimoire.modid.client.book;

import grimoire.modid.client.ClientQuestCache;
import grimoire.modid.data.QuestProgressComponent;
import grimoire.modid.quest.Quest;
import grimoire.modid.quest.TierConfig;

import java.util.ArrayList;
import java.util.List;


final class BookPages {

    private BookPages() {}   // toolbox, not a thing. no instances.


    record BookPage(int tier, List<Quest> entries, boolean locked) {
    }



    static List<Quest> buildActives(QuestProgressComponent progress) {
        List<Quest> actives = new ArrayList<>();
        for (Quest quest : ClientQuestCache.QUESTS) {
            if (progress.isActive(quest.id())) {
                actives.add(quest);
            }
        }
        return actives;
    }

    // Lifetime completions!
    static List<Quest> buildCodex(QuestProgressComponent progress) {
        List<Quest> completed = new ArrayList<>();
        for (String id : progress.getLifetimeCompleted()) {
            Quest quest = ClientQuestCache.byId(id);
            if (quest != null) {
                completed.add(quest);   // null = lost bargain, vanishes
            }
        }
        return completed;
    }

    static List<BookPage> buildPages(QuestProgressComponent progress) {
        List<BookPage> pages = new ArrayList<>();
        int unlocked = highestUnlockedTier(progress);

        List<Integer> tierNumbers = new ArrayList<>(ClientQuestCache.TIERS.keySet());
        tierNumbers.sort(Integer::compareTo);

        for (int tier : tierNumbers) {
            if (tier > unlocked) {
                pages.add(new BookPage(tier, List.of(), true));
                continue;
            }
            List<Quest> entries = new ArrayList<>();
            for (String id : progress.getOffered(tier)) {
                Quest quest = ClientQuestCache.byId(id);
                if (quest != null) {
                    entries.add(quest);
                }
            }
            pages.add(new BookPage(tier, entries, false));
        }

        if (pages.isEmpty()) {
            pages.add(new BookPage(1, List.of(), false));
        }
        return pages;
    }


    static int highestUnlockedTier(QuestProgressComponent progress) {
        int tier = 1;
        while (ClientQuestCache.TIERS.containsKey(tier + 1)) {
            TierConfig config = ClientQuestCache.TIERS.get(tier);
            if (config != null && progress.getCompletions(tier) >= config.completionsToUnlockNext()) {
                tier++;
            } else {
                break;
            }
        }
        return tier;
    }


    static int snapshot(QuestProgressComponent progress) {
        int snap = 0;
        snap = snap * 31 + progress.getCompletedCount();
        snap = snap * 31 + progress.getActiveCount();
        for (int tier : ClientQuestCache.TIERS.keySet()) {
            snap = snap * 31 + progress.getOffered(tier).hashCode();
        }
        return snap;
    }
}