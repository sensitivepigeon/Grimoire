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

                int ironCount = player.getInventory().count(Items.IRON_INGOT);

                if (ironCount >= 10) {
                    player.getInventory().remove(
                            stack -> stack.isOf(Items.IRON_INGOT),
                            10,
                            player.playerScreenHandler.getCraftingInput()
                    );

                    player.giveItemStack(new ItemStack(Items.DIAMOND, 1));

                    player.sendMessage(Text.literal("Bounty complete! The Grimoire rewards you."), false);
                } else {
                    player.sendMessage(Text.literal("The Grimoire senses you lack the required iron... (" + ironCount + "/10)"), false);
                }
            });
        });
    }
}