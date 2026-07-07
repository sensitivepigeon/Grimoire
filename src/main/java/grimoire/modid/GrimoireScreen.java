package grimoire.modid;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public class GrimoireScreen extends Screen {

    public GrimoireScreen() {
        super(Text.literal("Grimoire"));
    }

    @Override
    protected void init() {
        int y = 40;

        for (Quest quest : ClientQuestCache.QUESTS) {
            String label = quest.title() + ": "
                    + quest.requiredCount() + " " + quest.requiredItem().getName().getString()
                    + " → "
                    + quest.rewardCount() + " " + quest.rewardItem().getName().getString();

            final String questId = quest.id();

            this.addDrawableChild(
                    ButtonWidget.builder(Text.literal(label), button -> {
                                PacketByteBuf buf = PacketByteBufs.create();
                                buf.writeString(questId);
                                ClientPlayNetworking.send(ModNetworking.TURN_IN_QUEST, buf);
                            })
                            .dimensions(this.width / 2 - 150, y, 300, 20)
                            .build()
            );

            y += 24;
        }

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                        .dimensions(this.width / 2 - 50, this.height - 40, 100, 20)
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Bounties will appear here..."),
                this.width / 2,
                60,
                0xFFFFFF
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}