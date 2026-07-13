package grimoire.modid.data;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import dev.onyxstudios.cca.api.v3.component.sync.AutoSyncedComponent;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.*;

public class QuestProgressComponent implements ComponentV3, AutoSyncedComponent {

    private final Map<Integer, List<String>> offeredQuests = new HashMap<>();
    private long lastRollDay = -1;

    private final Set<String> activeQuests = new HashSet<>();
    private final Set<String> completedThisRotation = new HashSet<>();

    private final List<String> lifetimeCompletedQuests = new ArrayList<>();

    private final Map<Integer, Integer> tierCompletions = new HashMap<>();
    private long manualRerollDay = -1;

    // rotation stuff

    public boolean needsRoll(long currentDay) {
        return currentDay != lastRollDay;
    }

    public void startNewRotation(long currentDay) {
        lastRollDay = currentDay;
        offeredQuests.clear();
        completedThisRotation.clear();
    }

    public void setOffered(int tier, List<String> questIds) {
        offeredQuests.put(tier, new ArrayList<>(questIds));
    }

    // removing unknown quests it should be in bountyboard.sweeporphans now
    // does not sweep the codex as intended
    public boolean removeUnknownQuests(Set<String> knownIds){
        boolean changed = activeQuests.removeIf(id -> !knownIds.contains(id));
        changed |= completedThisRotation.removeIf(id -> !knownIds.contains(id));

                for (List<String> offered : offeredQuests.values()){
                    changed |= offered.removeIf(id -> !knownIds.contains(id));
                }
       return changed;
    }

    public List<String> getOffered(int tier) {
        return offeredQuests.getOrDefault(tier, List.of());
    }

    public boolean isOffered(String questId) {
        for (List<String> list : offeredQuests.values()) {
            if (list.contains(questId)) return true;
        }
        return false;
    }

    // accepted bounties

    public boolean isActive(String questId) {
        return activeQuests.contains(questId);
    }

    public void accept(String questId) {
        activeQuests.add(questId);
    }

    public void removeActive(String questId) {
        activeQuests.remove(questId);
    }

    // daily lockout

    public boolean hasCompleted(String questId) {
        return completedThisRotation.contains(questId);
    }

    public boolean hasCompletedLifetime(String questId) {
        return lifetimeCompletedQuests.contains(questId);
    }

    public void markCompleted(String questId) {
        completedThisRotation.add(questId);
    }

    public int getCompletedCount() {
        return completedThisRotation.size();
    }

    // lifetime progress to tiers

    public void incrementCompletions(int tier) {
        tierCompletions.merge(tier, 1, Integer::sum);
    }

    public int getCompletions(int tier) {
        return tierCompletions.getOrDefault(tier, 0);
    }

    // this is for lifetime completions for quests

    public boolean recordLifetimeCompletion(String questId) {
        if (lifetimeCompletedQuests.contains(questId)) {
            return false;
        }

        lifetimeCompletedQuests.add(questId);
        return true;


}

    // manual reroll

    public boolean canManualReroll(long currentDay) {
        return currentDay != manualRerollDay;
    }

    public void markManualReroll(long currentDay) {
        manualRerollDay = currentDay;
    }

    // state changes i sure hope it does

    public int getActiveCount() {
        return activeQuests.size();
    }

    public int getOfferedTotal() {
        int total = 0;
        for (List<String> list : offeredQuests.values()) {
            total += list.size();
        }
        return total;
    }

    // NBT

    @Override
    public void readFromNbt(NbtCompound tag) {
        offeredQuests.clear();
        activeQuests.clear();
        completedThisRotation.clear();
        tierCompletions.clear();
        lifetimeCompletedQuests.clear();

        lastRollDay = tag.getLong("LastRollDay");
        manualRerollDay = tag.getLong("ManualRerollDay");

        readStringList(tag.getList("ActiveQuests", NbtElement.STRING_TYPE), activeQuests);
        readStringList(tag.getList("CompletedThisRotation", NbtElement.STRING_TYPE), completedThisRotation);

        if (tag.contains("LifetimeCompletedQuests", NbtElement.LIST_TYPE)) {
            readStringList(tag.getList("LifetimeCompletedQuests", NbtElement.STRING_TYPE), lifetimeCompletedQuests);
        }

        NbtCompound offered = tag.getCompound("OfferedQuests");
        for (String key : offered.getKeys()) {
            List<String> list = new ArrayList<>();
            NbtList nbtList = offered.getList(key, NbtElement.STRING_TYPE);
            for (int i = 0; i < nbtList.size(); i++) {
                list.add(nbtList.getString(i));
            }
            offeredQuests.put(Integer.parseInt(key), list);
        }

        NbtCompound completions = tag.getCompound("TierCompletions");
        for (String key : completions.getKeys()) {
            tierCompletions.put(Integer.parseInt(key), completions.getInt(key));
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        tag.putLong("LastRollDay", lastRollDay);
        tag.putLong("ManualRerollDay", manualRerollDay);

        tag.put("ActiveQuests", writeStringList(activeQuests));
        tag.put("CompletedThisRotation", writeStringList(completedThisRotation));
        tag.put("LifetimeCompletedQuests", writeStringList(lifetimeCompletedQuests));


        NbtCompound offered = new NbtCompound();
        for (Map.Entry<Integer, List<String>> entry : offeredQuests.entrySet()) {
            offered.put(String.valueOf(entry.getKey()), writeStringList(entry.getValue()));
        }
        tag.put("OfferedQuests", offered);

        NbtCompound completions = new NbtCompound();
        for (Map.Entry<Integer, Integer> entry : tierCompletions.entrySet()) {
            completions.putInt(String.valueOf(entry.getKey()), entry.getValue());
        }
        tag.put("TierCompletions", completions);
    }

    private static void readStringList(NbtList list, Set<String> into) {
        for (int i = 0; i < list.size(); i++) {
            into.add(list.getString(i));
        }
    }
    private static void readStringList(NbtList list, List<String> into) {
        for (int i = 0; i < list.size(); i++) {
            into.add(list.getString(i));
        }
    }


    private static NbtList writeStringList(Iterable<String> strings) {
        NbtList list = new NbtList();
        for (String s : strings) {
            list.add(NbtString.of(s));
        }
        return list;
    }
}