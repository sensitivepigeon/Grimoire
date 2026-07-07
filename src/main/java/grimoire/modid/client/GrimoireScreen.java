package grimoire.modid.client;

import grimoire.modid.client.ClientQuestCache;
import grimoire.modid.data.ModComponents;
import grimoire.modid.data.QuestProgressComponent;
import grimoire.modid.network.ModNetworking;
import grimoire.modid.quest.Quest;
import grimoire.modid.quest.TierConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class GrimoireScreen extends Screen {

    private static final int BOOK_WIDTH = 320;
    private static final int BOOK_HEIGHT = 250;
    private static final int HEADER_HEIGHT = 34;
    private static final int ENTRY_HEIGHT = 60;
    private static final int ENTRIES_PER_PAGE = 3;

    private int bookLeft;
    private int bookTop;
    private int pageIndex = 0;
    private int stateSnapshot;

    private final List<BookPage> pages = new ArrayList<>();

    private record BookPage(int tier, List<Quest> entries, boolean locked) {
    }

    public GrimoireScreen() {
        super(Text.literal("Grimoire"));
    }

    @Override
    protected void init() {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);
        this.stateSnapshot = snapshot(progress);
        this.bookLeft = (this.width - BOOK_WIDTH) / 2;
        this.bookTop = (this.height - BOOK_HEIGHT) / 2;

        buildPages(progress);
        if (pageIndex >= pages.size()) pageIndex = pages.size() - 1;
        if (pageIndex < 0) pageIndex = 0;

        BookPage page = pages.get(pageIndex);

        if (!page.locked()) {
            int y = bookTop + HEADER_HEIGHT;
            for (Quest quest : page.entries()) {
                addEntryButton(progress, quest, y);
                y += ENTRY_HEIGHT;
            }
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            pageIndex--;
            this.clearAndInit();
        }).dimensions(bookLeft + 8, bookTop + BOOK_HEIGHT - 26, 20, 20).build())
                .active = pageIndex > 0;

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            pageIndex++;
            this.clearAndInit();
        }).dimensions(bookLeft + BOOK_WIDTH - 28, bookTop + BOOK_HEIGHT - 26, 20, 20).build())
                .active = pageIndex < pages.size() - 1;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reroll"), b ->
                ClientPlayNetworking.send(ModNetworking.REROLL, PacketByteBufs.create())
        ).dimensions(bookLeft + BOOK_WIDTH / 2 - 52, bookTop + BOOK_HEIGHT - 26, 50, 20).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> this.close())
                .dimensions(bookLeft + BOOK_WIDTH / 2 + 2, bookTop + BOOK_HEIGHT - 26, 50, 20).build());
    }

    private void buildPages(QuestProgressComponent progress) {
        pages.clear();
        int unlocked = highestUnlockedTier(progress);

        List<Integer> tierNumbers = new ArrayList<>(ClientQuestCache.TIERS.keySet());
        tierNumbers.sort(Integer::compareTo);

        for (int tier : tierNumbers) {
            if (tier > unlocked) {
                pages.add(new BookPage(tier, List.of(), true));
                continue;
            }

            List<Quest> entries = new ArrayList<>();
            for (Quest quest : ClientQuestCache.QUESTS) {
                if (quest.tier() == tier && progress.isActive(quest.id())) {
                    entries.add(quest);
                }
            }
            for (String id : progress.getOffered(tier)) {
                Quest quest = ClientQuestCache.byId(id);
                if (quest != null && !entries.contains(quest)) {
                    entries.add(quest);
                }
            }

            if (entries.isEmpty()) {
                pages.add(new BookPage(tier, List.of(), false));
            } else {
                for (int start = 0; start < entries.size(); start += ENTRIES_PER_PAGE) {
                    int end = Math.min(start + ENTRIES_PER_PAGE, entries.size());
                    pages.add(new BookPage(tier, entries.subList(start, end), false));
                }
            }
        }

        if (pages.isEmpty()) {
            pages.add(new BookPage(1, List.of(), false));
        }
    }

    private void addEntryButton(QuestProgressComponent progress, Quest quest, int entryTop) {
        String id = quest.id();
        boolean done = progress.hasCompleted(id);
        boolean active = progress.isActive(id);

        boolean atCap = !active && !done
                && progress.getActiveCount() >= 3;

        String label;
        if (done) {
            label = "Fulfilled";
        } else if (active) {
            int held = this.client.player.getInventory().count(quest.requiredItem());
            label = "Turn In (" + Math.min(held, quest.requiredCount()) + "/" + quest.requiredCount() + ")";
        } else {
            label = "Accept";
        }

        ButtonWidget button = ButtonWidget.builder(Text.literal(label), b -> {
            PacketByteBuf buf = PacketByteBufs.create();
            buf.writeString(id);
            ClientPlayNetworking.send(active ? ModNetworking.TURN_IN_QUEST : ModNetworking.ACCEPT_QUEST, buf);
        }).dimensions(bookLeft + 30, entryTop + 38, 110, 18).build();

        button.active = !done && !atCap;
        this.addDrawableChild(button);
    }

    private int highestUnlockedTier(QuestProgressComponent progress) {
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
        drawBookBackground(context);

        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);
        BookPage page = pages.get(pageIndex);
        TierConfig config = ClientQuestCache.TIERS.get(page.tier());
        String tierName = config != null ? config.name() : "Tier " + page.tier();

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("— " + tierName + " —"),
                bookLeft + BOOK_WIDTH / 2, bookTop + 10, 0xE0C468);

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Oaths sworn: " + progress.getActiveCount() + "/3"),
                bookLeft + BOOK_WIDTH / 2, bookTop + BOOK_HEIGHT - 42, 0x9A9186);

        if (page.locked()) {
            TierConfig prev = ClientQuestCache.TIERS.get(page.tier() - 1);
            int need = prev != null ? prev.completionsToUnlockNext() : 0;
            int have = progress.getCompletions(page.tier() - 1);

            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("This page is sealed.").formatted(Formatting.ITALIC),
                    bookLeft + BOOK_WIDTH / 2, bookTop + 100, 0x888888);
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Fulfill " + (need - have) + " more bounties of the prior rank."),
                    bookLeft + BOOK_WIDTH / 2, bookTop + 116, 0x888888);
        } else {
            String sub;
            if (config != null && ClientQuestCache.TIERS.containsKey(page.tier() + 1)) {
                sub = progress.getCompletions(page.tier()) + "/" + config.completionsToUnlockNext() + " to next rank";
            } else {
                sub = progress.getCompletions(page.tier()) + " fulfilled";
            }
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal(sub), bookLeft + BOOK_WIDTH / 2, bookTop + 22, 0x888888);

            if (page.entries().isEmpty()) {
                context.drawCenteredTextWithShadow(this.textRenderer,
                        Text.literal("The pages are blank until tomorrow...").formatted(Formatting.ITALIC),
                        bookLeft + BOOK_WIDTH / 2, bookTop + 100, 0x888888);
            }

            int y = bookTop + HEADER_HEIGHT;
            for (Quest quest : page.entries()) {
                drawEntry(context, progress, quest, y);
                y += ENTRY_HEIGHT;
            }
        }

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawEntry(DrawContext context, QuestProgressComponent progress, Quest quest, int entryTop) {
        int x = bookLeft + 14;
        boolean done = progress.hasCompleted(quest.id());
        int titleColor = done ? 0x777777 : 0xF0E6D2;

        String title = quest.title() + (progress.isActive(quest.id()) ? "  ●" : "");
        context.drawTextWithShadow(this.textRenderer, Text.literal(title), x, entryTop + 2, titleColor);

        context.drawItem(new ItemStack(quest.rewardItem(), quest.rewardCount()), x + 2, entryTop + 15);

        String lore = this.textRenderer.trimToWidth(quest.lore(), BOOK_WIDTH - 60);
        context.drawTextWithShadow(this.textRenderer,
                Text.literal(lore).formatted(Formatting.ITALIC), x + 24, entryTop + 15, 0x9A9186);

        String req = quest.requiredCount() + " × " + quest.requiredItem().getName().getString()
                + "  →  " + quest.rewardCount() + " × " + quest.rewardItem().getName().getString();
        context.drawTextWithShadow(this.textRenderer, Text.literal(req), x + 24, entryTop + 26, 0xC8BFA8);

        context.fill(bookLeft + 12, entryTop + ENTRY_HEIGHT - 2,
                bookLeft + BOOK_WIDTH - 12, entryTop + ENTRY_HEIGHT - 1, 0x33FFFFFF);
    }

    private void drawBookBackground(DrawContext context) {
        context.fill(bookLeft, bookTop, bookLeft + BOOK_WIDTH, bookTop + BOOK_HEIGHT, 0xF2211A14);
        context.fill(bookLeft + 4, bookTop + 4, bookLeft + BOOK_WIDTH - 4, bookTop + BOOK_HEIGHT - 4, 0xFF2E2620);
        context.fill(bookLeft, bookTop, bookLeft + BOOK_WIDTH, bookTop + 2, 0xFF8A6D3B);
        context.fill(bookLeft, bookTop + BOOK_HEIGHT - 2, bookLeft + BOOK_WIDTH, bookTop + BOOK_HEIGHT, 0xFF8A6D3B);
        context.fill(bookLeft, bookTop, bookLeft + 2, bookTop + BOOK_HEIGHT, 0xFF8A6D3B);
        context.fill(bookLeft + BOOK_WIDTH - 2, bookTop, bookLeft + BOOK_WIDTH, bookTop + BOOK_HEIGHT, 0xFF8A6D3B);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}