package grimoire.modid.client.book;

import grimoire.modid.client.ClientQuestCache;
import grimoire.modid.client.IndexText;
import grimoire.modid.data.ModComponents;
import grimoire.modid.data.QuestProgressComponent;
import grimoire.modid.network.ModNetworking;
import grimoire.modid.quest.Quest;
import grimoire.modid.quest.TierConfig;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import static grimoire.modid.client.book.BookLayout.*;
import grimoire.modid.client.book.BookPages.BookPage;

import java.util.ArrayList;
import java.util.List;

public class GrimoireScreen extends Screen {

    private enum Mode { BOARD, DETAIL, HELP, INDEX, CODEX }
    private Point bookTopLeft;
    private int pageIndex = 0;
    private Mode mode = Mode.BOARD;            // single source of truth for mode
    private Quest detailQuest = null;
    private int stateSnapshot;

    private List<BookPage> pages = new ArrayList<>();
    private List<Quest> actives = new ArrayList<>();

    public GrimoireScreen() {
        super(Text.literal("Book of Bargains"));
    }


    // INIT - widgets only. right page change mode

    @Override
    protected void init() {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);
        this.stateSnapshot = BookPages.snapshot(progress);
        this.bookTopLeft = new Point((this.width - BOOK_WIDTH) / 2, (this.height - BOOK_HEIGHT) / 2);

        this.actives = BookPages.buildActives(progress);
        this.pages = BookPages.buildPages(progress);
        if (pageIndex >= pages.size()) pageIndex = pages.size() - 1;
        if (pageIndex < 0) pageIndex = 0;

        switch (mode) {
            case INDEX -> { initIndexMode(); return; }
            case HELP  -> { initHelpMode();  return; }
            case CODEX -> { initCodexMode(); return; }
            default -> { }   // BOARD or DETAIL continue below
        }

        initHelpButton();
        initOathHitboxes();

        switch (mode) {
            case DETAIL -> initDetailMode(progress);
            default     -> initBoardMode(progress);   // BOARD
        }
    }

    private void initIndexMode() {
        this.addDrawableChild(new HitboxButton(
                bookTopLeft.plus(HELP_BACK),
                Text.literal("Back"), b -> {
            this.mode = Mode.BOARD;
            this.clearAndInit();
        }).withSprite(SPRITE_BACK).withLabel(0xFF2F3D1A));

        // row 0 -> Help
        this.addDrawableChild(new HitboxButton(
                bookTopLeft.plus(INDEX_ROWS[0]),
                Text.literal("Help"), b -> {
            this.mode = Mode.HELP;
            this.clearAndInit();
        }));

        // row 1 -> Codex
        this.addDrawableChild(new HitboxButton(
                bookTopLeft.plus(INDEX_ROWS[1]),
                Text.literal("The Codex: Bargains"), b -> {
            this.mode = Mode.CODEX;
            this.clearAndInit();
        }));
    }

    private void initCodexMode() {
        this.addDrawableChild(new HitboxButton(
                bookTopLeft.plus(HELP_BACK),
                Text.literal("Back"), b -> {
            this.mode = Mode.INDEX;   // Codex Back returns to the hub
            this.clearAndInit();
        }).withSprite(SPRITE_BACK).withLabel(0xFF2F3D1A));
    }

    private void initHelpMode() {
        this.addDrawableChild(new HitboxButton(
                bookTopLeft.plus(HELP_BACK),
                Text.literal("Back"), b -> {
            this.mode = Mode.INDEX;   // was Mode.BOARD — Help is reached via Index now
            this.detailQuest = null;
            this.clearAndInit();
        }).withSprite(SPRITE_BACK).withLabel(0xFF2F3D1A));
    }

    private void initHelpButton() {
        this.addDrawableChild(new HitboxButton(
                bookTopLeft.plus(HELP),
                Text.literal("?"), b -> {
            this.mode = Mode.INDEX;   // was Mode.HELP
            this.detailQuest = null;
            this.clearAndInit();
        }));
    }

    // left page turn in hitboxes
    private void initOathHitboxes() {
        for (int i = 0; i < actives.size(); i++) {
            Quest quest = actives.get(i);

            if (!quest.description().isEmpty()) {
                final Quest q = quest;
                this.addDrawableChild(new HitboxButton(
                        bookTopLeft.plus(OATHS[i].title()),
                        Text.literal("More"), b -> {
                    this.detailQuest = q;
                    this.mode = Mode.DETAIL;
                    this.clearAndInit();
                }));
            }

            final String id = actives.get(i).id();
            this.addDrawableChild(new HitboxButton(
                    bookTopLeft.plus(OATHS[i].chevron()),
                    Text.literal("Turn in"), b -> {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(id);
                ClientPlayNetworking.send(ModNetworking.TURN_IN_QUEST, buf);
            }).withSprite(SPRITE_TURNIN).withLabel(0xFF2F3D1A));


            final String cancelId = actives.get(i).id();
            this.addDrawableChild(new HitboxButton(
                    bookTopLeft.plus(OATHS[i].cancel()),
                    Text.literal("Cancel"), b -> {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(cancelId);
                ClientPlayNetworking.send(ModNetworking.CANCEL_QUEST, buf);
            }).withSprite(SPRITE_CANCEL));
        }
    }

    private void initDetailMode(QuestProgressComponent progress) {
        this.addDrawableChild(new HitboxButton(
                bookTopLeft.plus(DETAIL_BACK),
                Text.literal("Back"), b -> {
            this.mode = Mode.BOARD;
            this.detailQuest = null;
            this.clearAndInit();
        }).withSprite(SPRITE_BACK).withLabel(0xFF2F3D1A));

        boolean done = progress.hasCompleted(detailQuest.id());
        boolean sworn = progress.isActive(detailQuest.id());
        boolean atCap = progress.getActiveCount() >= OATHS.length;

        if (!sworn && !done) {
            final String id = detailQuest.id();
            HitboxButton accept = new HitboxButton(
                    bookTopLeft.plus(DETAIL_ACCEPT),
                    Text.literal("Accept"), b -> {
                PacketByteBuf buf = PacketByteBufs.create();
                buf.writeString(id);
                ClientPlayNetworking.send(ModNetworking.ACCEPT_QUEST, buf);
            }).withSprite(SPRITE_ACCEPT).withLabel(0xFF2F3D1A);
            accept.active = !done && !atCap;
            this.addDrawableChild(accept);
        }
    }

    private void initBoardMode(QuestProgressComponent progress) {
        BookPage page = pages.get(pageIndex);
        if (!page.locked()) {
            boolean atCap = progress.getActiveCount() >= OATHS.length;

            for (int i = 0; i < page.entries().size(); i++) {
                Quest quest = page.entries().get(i);
                final String id = quest.id();
                boolean done = progress.hasCompleted(id);
                boolean sworn = progress.isActive(id);

                // detail access: invisible hitbox over title
                if (!quest.description().isEmpty()) {
                    final Quest q = quest;
                    this.addDrawableChild(new HitboxButton(
                            bookTopLeft.plus(OFFERS[i].title()),
                            Text.literal("More"), b -> {
                        this.detailQuest = q;
                        this.mode = Mode.DETAIL;
                        this.clearAndInit();
                    }));
                }


                if (sworn || done) continue;   // accepted bargains: tag instead of arrow

                HitboxButton accept = new HitboxButton(
                        bookTopLeft.plus(OFFERS[i].accept()),
                        Text.literal("Accept"), b -> {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeString(id);
                    ClientPlayNetworking.send(ModNetworking.ACCEPT_QUEST, buf);
                }).withSprite(SPRITE_ACCEPT).withLabel(0xFF2F3D1A);
                accept.active = !atCap;
                this.addDrawableChild(accept);
            }
        }

        // page-turn chevrons (painted art)
        HitboxButton navL = new HitboxButton(bookTopLeft.plus(NAV_L),
                Text.literal("Previous tier"), b -> {
            pageIndex--;
            this.clearAndInit();
        }).withSprite(SPRITE_NAV_L);
        navL.active = pageIndex > 0;
        this.addDrawableChild(navL);

        HitboxButton navR = new HitboxButton(bookTopLeft.plus(NAV_R),
                Text.literal("Next tier"), b -> {
            pageIndex++;
            this.clearAndInit();
        }).withSprite(SPRITE_NAV_R);
        navR.active = pageIndex < pages.size() - 1;
        this.addDrawableChild(navR);

        // dice = reroll (sprite)
        this.addDrawableChild(new HitboxButton(bookTopLeft.plus(DICE),
                Text.literal("Reroll"), b ->
                ClientPlayNetworking.send(ModNetworking.REROLL, PacketByteBufs.create()))
                .withSprite(SPRITE_DICE));
    }

    // live reroll snapshot: counts + per-tier offer identities


    @Override
    public void tick() {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);
        if (BookPages.snapshot(progress) != this.stateSnapshot) {
            this.clearAndInit();
        }
    }

    private void drawOathCard(DrawContext context, Oath oath, Quest quest) {
        ItemStack required = new ItemStack(quest.requiredItem(), Math.min(quest.requiredCount(), 64));
        Point icon = bookTopLeft.plus(oath.icon());
        BookText.drawItemCentered(context, this.textRenderer, required, icon.x(), icon.y());

        BookText.drawScaledText(context, this.textRenderer, quest.title() + " · T" + quest.tier(), false,
                bookTopLeft.plus(oath.title()), INK_TITLE);

        int held = this.client.player.getInventory().count(quest.requiredItem());
        int shown = Math.min(held, quest.requiredCount());
        int color = shown >= quest.requiredCount() ? INK_READY : INK_BODY;
        BookText.drawScaledText(context, this.textRenderer, shown + "/" + quest.requiredCount() + " gathered", false,
                bookTopLeft.plus(oath.info()), color);
    }

    private void drawOfferCard(DrawContext context, Offer offer, QuestProgressComponent progress, Quest quest) {
        boolean done = progress.hasCompleted(quest.id());
        boolean sworn = progress.isActive(quest.id());
        boolean dimmed = done || sworn;

        int titleColor = dimmed ? INK_DIM : INK_TITLE;
        int bodyColor = dimmed ? INK_DIM : INK_BODY;

        ItemStack required = new ItemStack(quest.requiredItem(), Math.min(quest.requiredCount(), 64));
        Point icon = bookTopLeft.plus(offer.icon());
        BookText.drawItemCentered(context, this.textRenderer, required, icon.x(), icon.y());

        BookText.drawScaledText(context, this.textRenderer, quest.title(), false,
                bookTopLeft.plus(offer.title()), titleColor);

        if (sworn || done) {
            BookText.drawScaledText(context, this.textRenderer, done ? "done" : "accepted", true,
                    bookTopLeft.plus(offer.tag()), INK_TAG);
        }

        BookText.drawScaledText(context, this.textRenderer, quest.lore(), true,
                bookTopLeft.plus(offer.desc()), bodyColor);

        String req = quest.requiredCount() + " × " + quest.requiredItem().getName().getString()
                + " → " + quest.rewards().get(0).count() + " × " + quest.rewards().get(0).item().getName().getString();
        BookText.drawScaledText(context, this.textRenderer, req, false,
                bookTopLeft.plus(offer.info()), bodyColor);
    }
    // render - background, left page, right page (mode-branched)

   @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        drawBookBackground(context);

        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);

       if (mode == Mode.BOARD || mode == Mode.DETAIL) {
            drawLeftPage(context);
        }
        drawRightPage(context, progress);

        super.render(context, mouseX, mouseY, delta);
    }

    // left page banner and oaths
    private void drawLeftPage(DrawContext context) {
        Point bannerCount = bookTopLeft.plus(BANNER_COUNT);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(actives.size() + " of " + OATHS.length + " accepted"),
                bannerCount.x(), bannerCount.y(), BANNER_INK);

        Rect2i help = bookTopLeft.plus(HELP);
        context.drawText(this.textRenderer,
                Text.literal("?"),
                help.getX() + 6,
                help.getY() + 8,
                INK_TITLE,
                false);

        for (int i = 0; i < OATHS.length; i++) {
            if (i < actives.size()) {
                drawOathCard(context, OATHS[i], actives.get(i));
            } else {
                BookText.drawScaledText(context, this.textRenderer, "-- empty bargain --", true,
                        bookTopLeft.plus(OATHS[i].info()), INK_DIM);
            }
        }
    }

    // right page mode switching
    private void drawRightPage(DrawContext context, QuestProgressComponent progress) {
        switch (mode) {
            case HELP  -> { drawHelpPage(context); return; }
            case DETAIL -> { drawDetailPage(context, progress); return; }
            case INDEX -> { drawIndexPage(context); return; }
            case CODEX -> { drawCodexPage(context); return; }
            default -> { }   // BOARD falls through
        }

        BookPage page = pages.get(pageIndex);
        TierConfig config = ClientQuestCache.TIERS.get(page.tier());
        String tierName = config != null ? config.name() : "Tier " + page.tier();

        BookText.drawCenteredNoShadow(context, this.textRenderer, tierName,
                bookTopLeft.x() + RIGHT_CX, bookTopLeft.y() + RIGHT_TITLE_Y, INK_TITLE);

        String sub;
        if (page.locked()) {
            sub = "Tier " + page.tier() + " · sealed";
        } else if (config != null && ClientQuestCache.TIERS.containsKey(page.tier() + 1)) {
            int needed = config.completionsToUnlockNext();
            int shown = Math.min(progress.getCompletions(page.tier()), needed);

            sub = "Tier " + page.tier() + " · "
                    + shown + "/" + needed
                    + " to next rank";
        } else {
            sub = "Tier " + page.tier() + " · " + progress.getCompletions(page.tier()) + " fulfilled";
        }
        BookText.drawCenteredNoShadow(context, this.textRenderer, sub,
                bookTopLeft.x() + RIGHT_CX, bookTopLeft.y() + RIGHT_SUB_Y, INK_DIM);

        if (page.locked()) {
            TierConfig prev = ClientQuestCache.TIERS.get(page.tier() - 1);
            int need = prev != null ? prev.completionsToUnlockNext() : 0;
            int have = progress.getCompletions(page.tier() - 1);

            BookText.drawCenteredNoShadow(context, this.textRenderer, "This page is sealed.",
                    bookTopLeft.x() + RIGHT_CX, bookTopLeft.y() + 100, INK_DIM);
            BookText.drawCenteredNoShadow(context, this.textRenderer,
                    "Fulfill " + Math.max(0, need - have) + " more of the prior rank.",
                    bookTopLeft.x() + RIGHT_CX, bookTopLeft.y() + 114, INK_DIM);
            return;
        }

        if (page.entries().isEmpty()) {
            BookText.drawCenteredNoShadow(context, this.textRenderer, "The pages are blank until tomorrow...",
                    bookTopLeft.x() + RIGHT_CX, bookTopLeft.y() + 105, INK_DIM);
            return;
        }

        for (int i = 0; i < page.entries().size(); i++) {
            drawOfferCard(context, OFFERS[i], progress, page.entries().get(i));

        }
    }
    private void drawIndexPage(DrawContext context) {
        Point header = bookTopLeft.plus(HELP_HEADER);
        BookText.drawCenteredNoShadow(context, this.textRenderer, "The Book of Bargains",
                header.x(), header.y(), INK_TITLE);

        Rect2i left = bookTopLeft.plus(HELP_L);
        BookText.drawWrappedTextScaledToFit(context, this.textRenderer, IndexText.LEFT,
                left.getX(), left.getY(), left.getWidth(), left.getHeight(), INK_BODY);

        // numbered bold list, right page
        String[] rows = { "Help", "The Codex: Bargains" };
        for (int i = 0; i < rows.length; i++) {
            Rect2i row = bookTopLeft.plus(INDEX_ROWS[i]);
            BookText.drawScaledText(context, this.textRenderer,
                    "§l" + (i + 1) + ". " + rows[i], false,
                    row.getX(), row.getY(), row.getWidth(), INK_TITLE);
        }
    }

    private void drawCodexPage(DrawContext context) {
        Point header = bookTopLeft.plus(HELP_HEADER);
        BookText.drawCenteredNoShadow(context, this.textRenderer, "The Codex",
                header.x(), header.y(), INK_TITLE);

        Rect2i left = bookTopLeft.plus(HELP_L);
        BookText.drawWrappedTextScaledToFit(context, this.textRenderer,
                "Codex coming soon.",
                left.getX(), left.getY(), left.getWidth(), left.getHeight(), INK_BODY);
    }
    // detail mode layout
    private void drawDetailPage(DrawContext context, QuestProgressComponent progress) {
        Quest quest = detailQuest;
        int x = bookTopLeft.x() + 234;
        int textWidth = 155;

        BookText.drawCenteredNoShadow(context, this.textRenderer, quest.title() + " · T" + quest.tier(),
                bookTopLeft.x() + RIGHT_CX, bookTopLeft.y() + RIGHT_TITLE_Y, INK_TITLE);

        boolean sworn = progress.isActive(quest.id());
        boolean done = progress.hasCompleted(quest.id());
        if (sworn || done) {
            BookText.drawCenteredNoShadow(context, this.textRenderer, done ? "fulfilled today" : "accepted",
                    bookTopLeft.x() + RIGHT_CX, bookTopLeft.y() + RIGHT_SUB_Y, INK_DIM);
        }

        int tradeY = bookTopLeft.y() + 46;
        ItemStack required = new ItemStack(quest.requiredItem(), Math.min(quest.requiredCount(), 64));
        context.drawItem(required, x, tradeY);
        context.drawItemInSlot(this.textRenderer, required, x, tradeY);

        //show full soon
        String req = quest.requiredCount() + " × " + quest.requiredItem().getName().getString()
                + " → " + quest.rewards().get(0).count() + " × " + quest.rewards().get(0).item().getName().getString();
        BookText.drawScaledText(context, this.textRenderer, req, false,
                x + 22, tradeY + 4, textWidth - 22, INK_BODY);

        BookText.drawWrappedText(context, this.textRenderer, quest.description(),
                x, tradeY + 26, textWidth, INK_BODY);
    }



    public void drawHelpPage(DrawContext context) {
        // header sits above the painted line at y=58
        Point header = bookTopLeft.plus(HELP_HEADER);
        BookText.drawCenteredNoShadow(context, this.textRenderer, "The Book of Bargains",
                header.x(), header.y(), INK_TITLE);

        Rect2i left = bookTopLeft.plus(HELP_L);
        BookText.drawWrappedTextScaledToFit(context, this.textRenderer, HelpText.LEFT,
                left.getX(), left.getY(), left.getWidth(), left.getHeight(), INK_BODY);

        Rect2i right = bookTopLeft.plus(HELP_R);
        BookText.drawWrappedTextScaledToFit(context, this.textRenderer, HelpText.RIGHT,
                right.getX(), right.getY(), right.getWidth(), right.getHeight(), INK_BODY);
    }

    private void drawBookBackground(DrawContext context) {
        if (USE_TEXTURE) {
            boolean sealedBoard = mode == Mode.BOARD
                    && !pages.isEmpty() && pages.get(pageIndex).locked();

            Identifier tex = switch (mode) {
                case HELP  -> TEXTURE_HELP;
                case INDEX -> TEXTURE_HELP;   // borrowing help spread for now
                case CODEX -> TEXTURE_HELP;   // borrowing help spread for now
                case DETAIL -> TEXTURE_DETAIL;
                default -> sealedBoard ? TEXTURE_DETAIL : BOOK_TEXTURE;   // BOARD
            };

            context.drawTexture(tex, bookTopLeft.x(), bookTopLeft.y(), 0, 0,
                    BOOK_WIDTH, BOOK_HEIGHT, BOOK_WIDTH, BOOK_HEIGHT);
            return;
        }

        // fallback: placeholder fills
        context.fill(bookTopLeft.x(), bookTopLeft.y(), bookTopLeft.x() + BOOK_WIDTH, bookTopLeft.y() + BOOK_HEIGHT, 0xF2211A14);
        context.fill(bookTopLeft.x() + 4, bookTopLeft.y() + 4, bookTopLeft.x() + BOOK_WIDTH - 4, bookTopLeft.y() + BOOK_HEIGHT - 4, 0xFF2E2620);
        context.fill(bookTopLeft.x() + PAGE_SPLIT - 1, bookTopLeft.y() + 4, bookTopLeft.x() + PAGE_SPLIT + 1, bookTopLeft.y() + BOOK_HEIGHT - 4, 0x66000000);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}