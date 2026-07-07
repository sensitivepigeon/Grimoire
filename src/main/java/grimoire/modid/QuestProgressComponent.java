package grimoire.modid;

import dev.onyxstudios.cca.api.v3.component.ComponentV3;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;

import java.util.HashSet;
import java.util.Set;

public class QuestProgressComponent implements ComponentV3 {

    private final Set<String> completedQuests = new HashSet<>();

    public boolean hasCompleted(String questId) {
        return completedQuests.contains(questId);
    }

    public void markCompleted(String questId) {
        completedQuests.add(questId);
    }

    public int getCompletedCount() {
        return completedQuests.size();
    }

    @Override
    public void readFromNbt(NbtCompound tag) {
        completedQuests.clear();
        NbtList list = tag.getList("CompletedQuests", NbtString.STRING_TYPE);
        for (int i = 0; i < list.size(); i++) {
            completedQuests.add(list.getString(i));
        }
    }

    @Override
    public void writeToNbt(NbtCompound tag) {
        NbtList list = new NbtList();
        for (String questId : completedQuests) {
            list.add(NbtString.of(questId));
        }
        tag.put("CompletedQuests", list);
    }
}