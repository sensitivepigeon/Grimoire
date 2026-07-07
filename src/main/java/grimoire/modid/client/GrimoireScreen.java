package grimoire.modid.client;

import grimoire.modid.data.ModComponents;
import grimoire.modid.data.QuestProgressComponent;
import grimoire.modid.network.ModNetworking;
import grimoire.modid.quest.Quest;
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
    private int stateSnapshot;

    @Override
    protected void init() {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);
        this.stateSnapshot = snapshot(progress);

        int y = 40;

        for (Quest quest : ClientQuestCache.QUESTS) {
            String id = quest.id();
            boolean done = progress.hasCompleted(id);
            boolean active = progress.isActive(id);
            boolean offered = progress.isOffered(id);

            if (!done && !active && !offered) continue;

            String prefix = done ? "✔ " : (active ? "Turn In: " : "Accept: ");
            String label = prefix + quest.title() + " (" + quest.requiredCount() + " "
                    + quest.requiredItem().getName().getString() + ")";

            ButtonWidget button = ButtonWidget.builder(Text.literal(label), b -> {
                        PacketByteBuf buf = PacketByteBufs.create();
                        buf.writeString(id);
                        ClientPlayNetworking.send(active ? ModNetworking.TURN_IN_QUEST : ModNetworking.ACCEPT_QUEST, buf);
                    })
                    .dimensions(this.width / 2 - 150, y, 300, 20)
                    .build();

            button.active = !done;
            this.addDrawableChild(button);
            y += 24;
        }

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Reroll"), b ->
                                ClientPlayNetworking.send(ModNetworking.REROLL, PacketByteBufs.create()))
                        .dimensions(this.width / 2 - 105, this.height - 40, 100, 20)
                        .build()
        );

        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Close"), b -> this.close())
                        .dimensions(this.width / 2 + 5, this.height - 40, 100, 20)
                        .build()
        );
    }

    private int snapshot(QuestProgressComponent progress) {
        return progress.getCompletedCount() * 10000
                + progress.getActiveCount() * 100
                + progress.getOfferedTotal();
    }

    @Override
    public void tick() {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);
        if (snapshot(progress) != this.stateSnapshot) {
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