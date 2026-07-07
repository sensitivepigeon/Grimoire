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
            server.execute(() -> {

                QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(player);

                if (progress.hasCompleted("iron_bounty")) {
                    player.sendMessage(Text.literal("The Grimoire's pages are dim. This bounty is already fulfilled."), false);
                    return;
                }

                int ironCount = player.getInventory().count(Items.IRON_INGOT);

                if (ironCount >= 10) {
                    player.getInventory().remove(
                            stack -> stack.isOf(Items.IRON_INGOT),
                            10,
                            player.playerScreenHandler.getCraftingInput()
                    );

                    player.giveItemStack(new ItemStack(Items.DIAMOND, 1));
                    progress.markCompleted("iron_bounty");
                    ModComponents.QUEST_PROGRESS.sync(player);

                    player.sendMessage(Text.literal("Bounty complete! The Grimoire rewards you."), false);
                } else {
                    player.sendMessage(Text.literal("The Grimoire senses you lack the required iron... (" + ironCount + "/10)"), false);
                }
            });
        });
    }
}