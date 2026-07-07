package grimoire.modid;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;

public class GrimoireScreen extends Screen {

    private static final int PANEL_WIDTH = 340;

    private int panelLeft;
    private int panelTop;
    private int panelHeight;
    private int completedAtInit;

    public GrimoireScreen() {
        super(Text.literal("Grimoire"));
    }

    @Override
    protected void init() {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);
        this.completedAtInit = progress.getCompletedCount();

        int questCount = ClientQuestCache.QUESTS.size();
        this.panelHeight = 60 + questCount * 24 + 40;
        this.panelLeft = (this.width - PANEL_WIDTH) / 2;
        this.panelTop = (this.height - panelHeight) / 2;

        int y = panelTop + 40;

        for (Quest quest : ClientQuestCache.QUESTS) {
            boolean done = progress.hasCompleted(quest.id());

            String label = (done ? "✔ " : "") + quest.title() + ": "
                    + quest.requiredCount() + " " + quest.requiredItem().getName().getString()
                    + " → "
                    + quest.rewardCount() + " " + quest.rewardItem().getName().getString();

            final String questId = quest.id();

            ButtonWidget button = ButtonWidget.builder(Text.literal(label), b -> {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeString(questId);
                        ClientPlayNetworking.send(ModNetworking.TURN_IN_QUEST, buf);
                    })
                    .dimensions(panelLeft + 10, y, PANEL_WIDTH - 20, 20)
                    .build();

            button.active = !done;

            this.addDrawableChild(button);
            y += 24;
        }

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Close"), b -> this.close())
                        .dimensions(this.width / 2 - 50, panelTop + panelHeight - 30, 100, 20)
                        .build()
        );
    }

    @Override
    public void tick() {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);
        if (progress.getCompletedCount() != this.completedAtInit) {
            this.clearAndInit();
        }
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + panelHeight, 0xCC1A1A2E);
        context.fill(panelLeft, panelTop, panelLeft + PANEL_WIDTH, panelTop + 2, 0xFF8888CC);
        context.fill(panelLeft, panelTop + panelHeight - 2, panelLeft + PANEL_WIDTH, panelTop + panelHeight, 0xFF8888CC);
        context.fill(panelLeft, panelTop, panelLeft + 2, panelTop + panelHeight, 0xFF8888CC);
        context.fill(panelLeft + PANEL_WIDTH - 2, panelTop, panelLeft + PANEL_WIDTH, panelTop + panelHeight, 0xFF8888CC);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("— Grimoire of Bounties —"),
                this.width / 2,
                panelTop + 15,
                0xE0C468
        );

        if (ClientQuestCache.QUESTS.isEmpty()) {
            context.drawCenteredTextWithShadow(
                    this.textRenderer,
                    Text.literal("The pages are blank..."),
                    this.width / 2,
                    panelTop + 50,
                    0xAAAAAA
            );
        }

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}