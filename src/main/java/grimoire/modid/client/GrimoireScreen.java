package grimoire.modid.client;

import grimoire.modid.data.ModComponents;
import grimoire.modid.Grimoire;
import net.minecraft.util.Identifier;
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

    private static final int BOOK_WIDTH = 400;
    private static final int BOOK_HEIGHT = 230;
    private static final int PAGE_WIDTH = 200;
    private static final int MAX_OATHS = 3;
    private static final int OATH_CARD_HEIGHT = 52;
    private static final int OFFER_ENTRY_HEIGHT = 52;
    private static final Identifier BOOK_TEXTURE =
            new Identifier(Grimoire.MOD_ID, "textures/gui/grimoire_book.png");
    private static final boolean USE_TEXTURE = true;   // flip false to fall back to the fills

    private int bookLeft;
    private int bookTop;
    private int leftPageCenter;
    private int rightPageLeft;
    private int rightPageCenter;
    private int pageIndex = 0;
    private int stateSnapshot;

    private final List<BookPage> pages = new ArrayList<>();
    private final List<Quest> actives = new ArrayList<>();

    private record BookPage(int tier, List<Quest> entries, boolean locked) {
    }

    public GrimoireScreen() {
        super(Text.literal("Grimoire of Bargains"));
    }

    private int oathCardY(int index) {
        return bookTop + 34 + index * (OATH_CARD_HEIGHT + 8);
    }

    private int offerEntryY(int index) {
        return bookTop + 38 + index * OFFER_ENTRY_HEIGHT;
    }

    @Override
    protected void init() {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);
        this.stateSnapshot = snapshot(progress);
        this.bookLeft = (this.width - BOOK_WIDTH) / 2;
        this.bookTop = (this.height - BOOK_HEIGHT) / 2;
        this.leftPageCenter = bookLeft + PAGE_WIDTH / 2;
        this.rightPageLeft = bookLeft + PAGE_WIDTH;
        this.rightPageCenter = rightPageLeft + PAGE_WIDTH / 2;

        buildActives(progress);
        buildPages(progress);
        if (pageIndex >= pages.size()) pageIndex = pages.size() - 1;
        if (pageIndex < 0) pageIndex = 0;

        for (int i = 0; i < actives.size(); i++) {
            final String id = actives.get(i).id();
            this.addDrawableChild(ButtonWidget.builder(Text.literal("Turn in"), b -> {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(id);
                ClientPlayNetworking.send(ModNetworking.TURN_IN_QUEST, buf);
            }).dimensions(bookLeft + 14, oathCardY(i) + 30, PAGE_WIDTH - 28, 16).build());
        }

        BookPage page = pages.get(pageIndex);
        if (!page.locked()) {
            boolean atCap = actives.size() >= MAX_OATHS;

            for (int i = 0; i < page.entries().size(); i++) {
                Quest quest = page.entries().get(i);
                final String id = quest.id();
                boolean done = progress.hasCompleted(id);
                boolean sworn = progress.isActive(id);

                if (sworn) continue;

                ButtonWidget button = ButtonWidget.builder(
                        Text.literal(done ? "Done" : "Accept"), b -> {
                            PacketByteBuf buf = PacketByteBufs.create();
                            buf.writeString(id);
                            ClientPlayNetworking.send(ModNetworking.ACCEPT_QUEST, buf);
                        }).dimensions(rightPageLeft + 136, offerEntryY(i), 52, 16).build();

                button.active = !done && !atCap;
                this.addDrawableChild(button);
            }
        }

        this.addDrawableChild(ButtonWidget.builder(Text.literal("<"), b -> {
            pageIndex--;
            this.clearAndInit();
        }).dimensions(rightPageLeft + 8, bookTop + 8, 18, 14).build())
                .active = pageIndex > 0;

        this.addDrawableChild(ButtonWidget.builder(Text.literal(">"), b -> {
            pageIndex++;
            this.clearAndInit();
        }).dimensions(bookLeft + BOOK_WIDTH - 26, bookTop + 8, 18, 14).build())
                .active = pageIndex < pages.size() - 1;

        int barY = bookTop + BOOK_HEIGHT - 24;

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Reroll"), b ->
                ClientPlayNetworking.send(ModNetworking.REROLL, PacketByteBufs.create())
        ).dimensions(rightPageLeft + 8, barY, 48, 18).build());

        this.addDrawableChild(ButtonWidget.builder(Text.literal("Close"), b -> this.close())
                .dimensions(bookLeft + BOOK_WIDTH - 56, barY, 48, 18).build());
    }

    private void buildActives(QuestProgressComponent progress) {
        actives.clear();
        for (Quest quest : ClientQuestCache.QUESTS) {
            if (progress.isActive(quest.id())) {
                actives.add(quest);
            }
        }
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
            for (String id : progress.getOffered(tier)) {
                Quest quest = ClientQuestCache.byId(id);
                if (quest != null) {
                    entries.add(quest);
                }
            }
            pages.add(new BookPage(tier, entries, false));
        }

        if (pages.isEmpty()) {
            pages.add(new BookPage(1, List.of(), false));
        }
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

    // live reroll snapshot

    private int snapshot(QuestProgressComponent progress) {
        int snap = 0;

        // fold counts to preserve detection here's stupid math idk
        snap = snap * 31 + progress.getCompletedCount();
        snap = snap * 31 + progress.getActiveCount();

        // folding identities yeehaw !!!
        for (int tier: ClientQuestCache.TIERS.keySet()) {
            snap = snap * 31 + progress.getOffered(tier).hashCode();
        }


        return snap;
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

        drawLeftPage(context);
        drawRightPage(context, progress);

        super.render(context, mouseX, mouseY, delta);
    }

    private void drawLeftPage(DrawContext context) {
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal("Accepted Bargains"), leftPageCenter, bookTop + 8, 0xE0C468);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(actives.size() + " of " + MAX_OATHS + " accepted"),
                leftPageCenter, bookTop + 20, 0x888888);

        for (int i = 0; i < MAX_OATHS; i++) {
            int y = oathCardY(i);

            if (i < actives.size()) {
                drawOathCard(context, actives.get(i), y);
            } else {
                context.fill(bookLeft + 12, y, bookLeft + PAGE_WIDTH - 12, y + OATH_CARD_HEIGHT, 0x22000000);
                drawScaledText(context, "-- empty bargain --", true,
                        bookLeft + 44, y + 22, PAGE_WIDTH - 88, 0x555555);
            }
        }
    }

    private void drawOathCard(DrawContext context, Quest quest, int y) {
        context.fill(bookLeft + 12, y, bookLeft + PAGE_WIDTH - 12, y + OATH_CARD_HEIGHT, 0x44000000);

        ItemStack required = new ItemStack(quest.requiredItem(), Math.min(quest.requiredCount(), 64));
        context.drawItem(required, bookLeft + 16, y + 6);
        context.drawItemInSlot(this.textRenderer, required, bookLeft + 16, y + 6);

        String title = quest.title() + " · T" + quest.tier();
        drawScaledText(context, title, false,
                bookLeft + 38, y + 4, PAGE_WIDTH - 54, 0xF0E6D2);

        int held = this.client.player.getInventory().count(quest.requiredItem());
        int shown = Math.min(held, quest.requiredCount());
        int color = shown >= quest.requiredCount() ? 0x7FD98A : 0x9A9186;
        drawScaledText(context, shown + "/" + quest.requiredCount() + " gathered", false,
                bookLeft + 38, y + 16, PAGE_WIDTH - 54, color);
    }

    private void drawRightPage(DrawContext context, QuestProgressComponent progress) {
        BookPage page = pages.get(pageIndex);
        TierConfig config = ClientQuestCache.TIERS.get(page.tier());
        String tierName = config != null ? config.name() : "Tier " + page.tier();

        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(tierName), rightPageCenter, bookTop + 11, 0xE0C468);

        String sub;
        if (page.locked()) {
            sub = "Tier " + page.tier() + " · sealed";
        } else if (config != null && ClientQuestCache.TIERS.containsKey(page.tier() + 1)) {
            sub = "Tier " + page.tier() + " · "
                    + progress.getCompletions(page.tier()) + "/" + config.completionsToUnlockNext()
                    + " to next rank";
        } else {
            sub = "Tier " + page.tier() + " · " + progress.getCompletions(page.tier()) + " fulfilled";
        }
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(sub), rightPageCenter, bookTop + 25, 0x888888);

        if (page.locked()) {
            TierConfig prev = ClientQuestCache.TIERS.get(page.tier() - 1);
            int need = prev != null ? prev.completionsToUnlockNext() : 0;
            int have = progress.getCompletions(page.tier() - 1);

            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("This page is sealed.").formatted(Formatting.ITALIC),
                    rightPageCenter, bookTop + 95, 0x888888);
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("Fulfill " + Math.max(0, need - have) + " more of the prior rank."),
                    rightPageCenter, bookTop + 110, 0x888888);
            return;
        }

        if (page.entries().isEmpty()) {
            context.drawCenteredTextWithShadow(this.textRenderer,
                    Text.literal("The pages are blank until tomorrow...").formatted(Formatting.ITALIC),
                    rightPageCenter, bookTop + 100, 0x888888);
            return;
        }

        for (int i = 0; i < page.entries().size(); i++) {
            drawOfferEntry(context, progress, page.entries().get(i), offerEntryY(i));
        }
    }

    private void drawOfferEntry(DrawContext context, QuestProgressComponent progress, Quest quest, int y) {
        int x = rightPageLeft + 10;
        boolean done = progress.hasCompleted(quest.id());
        boolean sworn = progress.isActive(quest.id());
        boolean dimmed = done || sworn;

        int titleColor = dimmed ? 0x777777 : 0xF0E6D2;
        int bodyColor = dimmed ? 0x666666 : 0x9A9186;
        int reqColor = dimmed ? 0x666666 : 0xC8BFA8;

        drawScaledText(context, quest.title(), false, x, y + 3, 122, titleColor);

        if (sworn) {
            drawScaledText(context, "accepted", true, rightPageLeft + 148, y + 4, 40, 0xE0C468);
        }

        ItemStack required = new ItemStack(quest.requiredItem(), Math.min(quest.requiredCount(), 64));
        context.drawItem(required, x + 2, y + 18);
        context.drawItemInSlot(this.textRenderer, required, x + 2, y + 18);

        int textMax = PAGE_WIDTH - 56;
        drawScaledText(context, quest.lore(), true, x + 24, y + 18, textMax, bodyColor);

        String req = quest.requiredCount() + " × " + quest.requiredItem().getName().getString()
                + " → " + quest.rewardCount() + " × " + quest.rewardItem().getName().getString();
        drawScaledText(context, req, false, x + 24, y + 29, textMax, reqColor);

        context.fill(rightPageLeft + 8, y + OFFER_ENTRY_HEIGHT - 8,
                bookLeft + BOOK_WIDTH - 8, y + OFFER_ENTRY_HEIGHT - 7, 0x22FFFFFF);
    }

    private void drawScaledText(DrawContext context, String text, boolean italic,
                                int x, int y, int maxWidth, int color) {
        if (text == null || text.isEmpty()) return;

        Text styled = italic ? Text.literal(text).formatted(Formatting.ITALIC) : Text.literal(text);
        int width = this.textRenderer.getWidth(styled);

        if (width <= maxWidth) {
            context.drawTextWithShadow(this.textRenderer, styled, x, y, color);
            return;
        }

        float scale = Math.max(0.65f, (float) maxWidth / width);

        if (width * scale > maxWidth) {
            String trimmed = this.textRenderer.trimToWidth(text, (int) (maxWidth / scale) - 8) + "…";
            styled = italic ? Text.literal(trimmed).formatted(Formatting.ITALIC) : Text.literal(trimmed);
        }

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawTextWithShadow(this.textRenderer, styled, 0, 0, color);
        context.getMatrices().pop();
    }

    private void drawBookBackground(DrawContext context) {
        if (USE_TEXTURE) {
            // args: texture, x, y, u, v, drawWidth, drawHeight, textureFileWidth, textureFileHeight
            context.drawTexture(BOOK_TEXTURE, bookLeft, bookTop, 0, 0,
                    BOOK_WIDTH, BOOK_HEIGHT, BOOK_WIDTH, BOOK_HEIGHT);
            return;
        }

        // fallback: original placeholder fills
        context.fill(bookLeft, bookTop, bookLeft + BOOK_WIDTH, bookTop + BOOK_HEIGHT, 0xF2211A14);
        context.fill(bookLeft + 4, bookTop + 4, bookLeft + BOOK_WIDTH - 4, bookTop + BOOK_HEIGHT - 4, 0xFF2E2620);
        context.fill(rightPageLeft - 1, bookTop + 4, rightPageLeft + 1, bookTop + BOOK_HEIGHT - 4, 0x66000000);
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