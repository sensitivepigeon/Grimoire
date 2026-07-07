package grimoire.modid;

import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModNetworking {

    public static final Identifier TURN_IN_QUEST = new Identifier(Grimoire.MOD_ID, "turn_in_quest");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(TURN_IN_QUEST, (server, player, handler, buf, responseSender) -> {
            String questId = buf.readString();

            server.execute(() -> {
                Quest quest = QuestManager.QUESTS.get(questId);
                if (quest == null) {
                    player.sendMessage(Text.literal("The Grimoire does not recognize this bounty."), false);
                    return;
                }

                QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(player);

                if (progress.hasCompleted(questId)) {
                    player.sendMessage(Text.literal("The Grimoire's pages are dim. This bounty is already fulfilled."), false);
                    return;
                }

                int count = player.getInventory().count(quest.requiredItem());

                if (count >= quest.requiredCount()) {
                    player.getInventory().remove(
                            stack -> stack.isOf(quest.requiredItem()),
                            quest.requiredCount(),
                            player.playerScreenHandler.getCraftingInput()
                    );

                    player.giveItemStack(new ItemStack(quest.rewardItem(), quest.rewardCount()));
                    progress.markCompleted(questId);
                    ModComponents.QUEST_PROGRESS.sync(player);

                    player.sendMessage(Text.literal("Bounty complete: " + quest.title()), false);
                } else {
                    player.sendMessage(Text.literal("You lack the required items... (" + count + "/" + quest.requiredCount() + ")"), false);
                }
            });
        });
    }
}