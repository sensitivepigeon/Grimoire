package grimoire.modid.network;

import grimoire.modid.Grimoire;
import grimoire.modid.data.ModComponents;
import grimoire.modid.data.QuestProgressComponent;
import grimoire.modid.quest.BountyBoard;
import grimoire.modid.quest.Quest;
import grimoire.modid.quest.QuestManager;
import grimoire.modid.quest.RewardEntry;
import grimoire.modid.quest.TierConfig;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class ModNetworking {
    public static final Identifier SYNC_QUESTS = new Identifier(Grimoire.MOD_ID, "sync_quests");
    public static final Identifier ACCEPT_QUEST = new Identifier(Grimoire.MOD_ID, "accept_quest");
    public static final Identifier REROLL = new Identifier(Grimoire.MOD_ID, "reroll");
    public static void syncQuestsTo(ServerPlayerEntity player) {
        PacketByteBuf buf = PacketByteBufs.create();

        buf.writeInt(QuestManager.QUESTS.size());
        for (Quest quest : QuestManager.QUESTS.values()) {
            buf.writeString(quest.id());
            buf.writeString(quest.title());
            buf.writeString(quest.lore());
            buf.writeString(quest.description());
            buf.writeString(quest.patron());
            buf.writeInt(quest.format());
            buf.writeInt(quest.tier());
            buf.writeString(Registries.ITEM.getId(quest.requiredItem()).toString());
            buf.writeInt(quest.requiredCount());

            buf.writeInt(quest.rewards().size());
            for (RewardEntry reward : quest.rewards()) {
                buf.writeString(Registries.ITEM.getId(reward.item()).toString());
                buf.writeInt(reward.count());
            }

            buf.writeBoolean(quest.repeatable());
            buf.writeString(quest.requiresQuest());
        }

        buf.writeInt(QuestManager.TIERS.size());
        for (TierConfig tier : QuestManager.TIERS.values()) {
            buf.writeInt(tier.tier());
            buf.writeString(tier.name());
            buf.writeInt(tier.offersPerRotation());
            buf.writeInt(tier.completionsToUnlockNext());
        }

        ServerPlayNetworking.send(player, SYNC_QUESTS, buf);
    }

    public static final Identifier TURN_IN_QUEST = new Identifier(Grimoire.MOD_ID, "turn_in_quest");

    public static void registerServerReceivers() {
        ServerPlayNetworking.registerGlobalReceiver(ACCEPT_QUEST, (server, player, handler, buf, responseSender) -> {
            String questId = buf.readString();

            server.execute(() -> {
                Quest quest = QuestManager.QUESTS.get(questId);
                if (quest == null) return;

                QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(player);


                if (!quest.repeatable() && progress.hasCompletedLifetime(questId)) {
                    player.sendMessage(Text.literal("The Book refuses to reopen an already fulfilled bargain."), false);
                    return;
                }

                if (!quest.requiresQuest().isEmpty() && !progress.hasCompletedLifetime(quest.requiresQuest())) {
                    player.sendMessage(Text.literal("The Book requires the prerequisite to be completed first."), false);
                    return;
                }

                if (progress.hasCompleted(questId)) {
                    player.sendMessage(Text.literal("That bargain is already struck today."), false);
                    return;
                }
                if (progress.isActive(questId)) {
                    player.sendMessage(Text.literal("You have already accepted this bargain."), false);
                    return;
                }
                if (progress.getActiveCount() >= BountyBoard.MAX_ACTIVE_BOUNTIES) {
                    player.sendMessage(Text.literal("The Book refuses - try completing more bargains. ("
                            + BountyBoard.MAX_ACTIVE_BOUNTIES + " max)"), false);
                    return;
                }
                if (!progress.isOffered(questId)) {
                    player.sendMessage(Text.literal("The Book does not currently offer this bargain."), false);
                    return;
                }



                    progress.accept(questId);
                ModComponents.QUEST_PROGRESS.sync(player);
                player.sendMessage(Text.literal("Bargain accepted: " + quest.title()), false);
            });
        });

        ServerPlayNetworking.registerGlobalReceiver(REROLL, (server, player, handler, buf, responseSender) -> {
            server.execute(() -> BountyBoard.manualReroll(player));
        });

        ServerPlayNetworking.registerGlobalReceiver(TURN_IN_QUEST, (server, player, handler, buf, responseSender) -> {
            String questId = buf.readString();

            server.execute(() -> {
                Quest quest = QuestManager.QUESTS.get(questId);
                if (quest == null) {
                    player.sendMessage(Text.literal("The Book does not recognize this bargain."), false);
                    return;
                }



                QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(player);
                if (!quest.repeatable() && progress.hasCompletedLifetime(questId)) {
                    progress.removeActive(questId);
                    ModComponents.QUEST_PROGRESS.sync(player);
                    player.sendMessage(Text.literal("The Book removes an already fulfilled bargain from your accepted bargains."), false);
                    return;
                }

                if (!quest.requiresQuest().isEmpty() && !progress.hasCompletedLifetime(quest.requiresQuest())) {
                    progress.removeActive(questId);
                    ModComponents.QUEST_PROGRESS.sync(player);
                    player.sendMessage(Text.literal("Not yet, You. The Book won't allow this without the prerequisite."), false);
                    return;
                }


                if (progress.hasCompleted(questId)) {
                    player.sendMessage(Text.literal("The Book's pages are dim. This bargain is already fulfilled."), false);
                    return;
                }

                if (!progress.isActive(questId)) {
                    player.sendMessage(Text.literal("You must accept this bargain before fulfilling it."), false);
                    return;
                }

                int count = player.getInventory().count(quest.requiredItem());

                if (count >= quest.requiredCount()) {
                    player.getInventory().remove(
                            stack -> stack.isOf(quest.requiredItem()),
                            quest.requiredCount(),
                            player.playerScreenHandler.getCraftingInput()
                    );
                    for (RewardEntry reward : quest.rewards()) {
                        player.giveItemStack(new ItemStack(reward.item(), reward.count()));
                    }

                    progress.removeActive(questId);
                    progress.markCompleted(questId);
                    progress.recordLifetimeCompletion(questId);
                    progress.incrementCompletions(quest.tier());
                    ModComponents.QUEST_PROGRESS.sync(player);

                    player.sendMessage(Text.literal("Bargain complete: " + quest.title()), false);
                } else {
                    player.sendMessage(Text.literal("You lack the required items... (" + count + "/" + quest.requiredCount() + ")"), false);
                }
            });
        });
    }
}