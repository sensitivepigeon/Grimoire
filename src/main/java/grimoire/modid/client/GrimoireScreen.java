package grimoire.modid.client;

import grimoire.modid.Grimoire;
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

import java.util.ArrayList;
import java.util.List;

public class GrimoireScreen extends Screen {

    // shared layout types

    private record Point(int x, int y) {
        Point plus(Point p) {
            return new Point(x + p.x, y + p.y);
        }

        Rect2i plus(Rect2i r) {
            return new Rect2i(x + r.getX(), y + r.getY(), r.getWidth(), r.getHeight());
        }

        Rect2i rect(Size size) {
            return new Rect2i(x, y, size.width(), size.height());
        }
    }

    private record Size(int width, int height) {
    }

    // turn-in arrows now use arrow sprite but i misnamed it sorry
    private record Oath(Rect2i title, Rect2i info, Point icon, Rect2i chevron) {
        private static final Size  TEXT_SIZE      = new Size(108, 10);

        private static final Point INFO_OFFSET    = new Point(0, 14);

        private static final Point ICON_OFFSET    = new Point(-18, 16);

        private static final Point CHEVRON_OFFSET = new Point(66, 25);
        private static final Size  CHEVRON_SIZE   = new Size(44, 12);

        static Oath at(Point title) {
            Point info = title.plus(INFO_OFFSET);
            Point icon = title.plus(ICON_OFFSET);
            Point chevron = title.plus(CHEVRON_OFFSET);
            return new Oath(
                    title.rect(TEXT_SIZE),
                    info.rect(TEXT_SIZE),
                    icon,
                    chevron.rect(CHEVRON_SIZE));
        }
    }

    private record Offer(Rect2i title, Rect2i desc, Point icon, Rect2i accept, Rect2i tag, Rect2i info) {
        private static final Size  TITLE_SIZE   = new Size(82, 10);

        private static final Point DESC_OFFSET  = new Point(0, 12);
        private static final Size  DESC_SIZE    = new Size(125, 10);

        private static final Point ICON_OFFSET  = new Point(-14, 12);

        private static final Point ACCEPT_OFFSET = new Point(83, 28);
        private static final Size  ACCEPT_SIZE   = new Size(45, 12);

        private static final Point TAG_OFFSET   = new Point(86, 0);
        private static final Size  TAG_SIZE     = new Size(38, 10);

        private static final Point INFO_OFFSET  = new Point(-26, 33);
        private static final Size  INFO_SIZE    = new Size(106, 10);   // trade line

        static Offer at(Point title) {
            Point desc = title.plus(DESC_OFFSET);
            Point icon = title.plus(ICON_OFFSET);
            Point accept = title.plus(ACCEPT_OFFSET);
            Point tag = title.plus(TAG_OFFSET);
            Point info = title.plus(INFO_OFFSET);
            return new Offer(
                    title.rect(TITLE_SIZE),
                    desc.rect(DESC_SIZE),
                    icon,
                    accept.rect(ACCEPT_SIZE),
                    tag.rect(TAG_SIZE),
                    info.rect(INFO_SIZE));
        }
    }

    // layout

    private static final int BOOK_WIDTH = 420;
    private static final int BOOK_HEIGHT = 234;
    private static final int PAGE_SPLIT = BOOK_WIDTH / 2;

    // banner
    private static final Point BANNER_COUNT = new Point(109, 19);

    // left page - bargain cards, still called oath in code. habits hard to break
    private static final Oath[] OATHS = {   // x=66..174
            Oath.at(new Point(66, 52)),
            Oath.at(new Point(66, 106)),
            Oath.at(new Point(66, 161)),
    };

    // right page - header
    private static final int RIGHT_CX = 314;                       // header/title center
    private static final int RIGHT_TITLE_Y = 22;
    private static final int RIGHT_SUB_Y = 30;
    private static final int RIGHT_HEADER_W = 120;                 // safe 251..380

    // right page - offer cards
    private static final Offer[] OFFERS = {
            Offer.at(new Point(268, 55)),
            Offer.at(new Point(268, 108)),
            Offer.at(new Point(268, 163)),
    };

    // controls for buttons
    private static final Rect2i DICE = new Rect2i(299, 201, 18, 16);      // reroll
    private static final Rect2i HELP = new Rect2i(164, 17, 18, 26);       // "?" glyph (center 173,30)
    private static final Rect2i NAV_L = new Rect2i(239, 22, 11, 16);
    private static final Rect2i NAV_R = new Rect2i(381, 22, 11, 16);

    // back/accept buttons shared by detail and help modes
    private static final Rect2i DETAIL_BACK = new Rect2i(234, 195, 45, 12);
    private static final Rect2i DETAIL_ACCEPT = new Rect2i(349, 195, 45, 12);
    private static final Rect2i HELP_BACK = new Rect2i(30, 195, 45, 12);


    // PALETTE - adjust carefully

    private static final int INK_TITLE = 0xFF4A2E1A;
    private static final int INK_BODY = 0xFF5C4732;
    private static final int INK_DIM = 0xFF8A785F;
    private static final int INK_READY = 0xFF2F6B2F;               // gathered-complete green
    private static final int INK_TAG = 0xFF6E3B1F;                 // accepted tag
    private static final int BANNER_INK = 0xFFEAF6F6;              // light on blue banner

    private static final Identifier BOOK_TEXTURE =
            new Identifier(Grimoire.MOD_ID, "textures/gui/grimoire_book.png");
    private static final boolean USE_TEXTURE = true;

    // button sprites
    private static final Identifier SPRITE_TURNIN =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/accept_arrow.png");
    private static final Identifier SPRITE_ACCEPT =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/accept_arrow.png");
    private static final Identifier SPRITE_DICE =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/dice.png");
    private static final Identifier SPRITE_NAV_L =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/nav_left.png");
    private static final Identifier SPRITE_NAV_R =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/nav_right.png");
    private static final Identifier SPRITE_BACK =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/back_arrow.png");

    // mode backgrounds
    private static final Identifier TEXTURE_DETAIL =
            new Identifier(Grimoire.MOD_ID, "textures/gui/grimoire_book_detail.png");
    private static final Identifier TEXTURE_HELP =
            new Identifier(Grimoire.MOD_ID, "textures/gui/grimoire_book_help.png");

    // help page layout, text edits in HelpText java
    private static final Point HELP_HEADER = new Point(109, 30);
    private static final Rect2i HELP_L = new Rect2i(38, 56, 145, 145);
    private static final Rect2i HELP_R = new Rect2i(241, 33, 145, 210);

    private Point bookTopLeft;
    private int pageIndex = 0;
    private Quest detailQuest = null;          // non-null = detail mode
    private boolean showHelp = false;          // true = help mode (beats detail)
    private int stateSnapshot;

    private final List<BookPage> pages = new ArrayList<>();
    private final List<Quest> actives = new ArrayList<>();

    private record BookPage(int tier, List<Quest> entries, boolean locked) {
    }

    public GrimoireScreen() {
        super(Text.literal("Book of Bargains"));
    }


    // INIT - widgets only. right page change mode

    @Override
    protected void init() {
        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);
        this.stateSnapshot = snapshot(progress);
        this.bookTopLeft = new Point((this.width - BOOK_WIDTH) / 2, (this.height - BOOK_HEIGHT) / 2);

        buildActives(progress);
        buildPages(progress);
        if (pageIndex >= pages.size()) pageIndex = pages.size() - 1;
        if (pageIndex < 0) pageIndex = 0;

        if (showHelp) {
            initHelpMode();
            return;
        }

        initHelpButton();
        initOathHitboxes();

        if (detailQuest != null) {
            initDetailMode(progress);
        } else {
            initBoardMode(progress);
        }
    }

    private void initHelpMode() {
        this.addDrawableChild(new HitboxButton(
                bookTopLeft.plus(HELP_BACK),
                Text.literal("Back"), b -> {
            this.showHelp = false;
            this.clearAndInit();
        }).withSprite(SPRITE_BACK).withLabel(0xFF2F3D1A));
    }

    private void initHelpButton() {
        this.addDrawableChild(new HitboxButton(
                bookTopLeft.plus(HELP),
                Text.literal("?"), b -> {
            this.showHelp = true;
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
        }
    }

    private void initDetailMode(QuestProgressComponent progress) {
        this.addDrawableChild(new HitboxButton(
                bookTopLeft.plus(DETAIL_BACK),
                Text.literal("Back"), b -> {
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


    // data builders

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

    // live reroll snapshot: counts + per-tier offer identities
    private int snapshot(QuestProgressComponent progress) {
        int snap = 0;
        snap = snap * 31 + progress.getCompletedCount();
        snap = snap * 31 + progress.getActiveCount();
        for (int tier : ClientQuestCache.TIERS.keySet()) {
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


    // render - background, left page, right page (mode-branched)

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        drawBookBackground(context);

        QuestProgressComponent progress = ModComponents.QUEST_PROGRESS.get(this.client.player);

        if (!showHelp) {
            drawLeftPage(context);   // help mode owns the whole spread
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
                drawOathCard(context, actives.get(i), i);
            } else {
                BookText.drawScaledText(context, this.textRenderer, "-- empty bargain --", true,
                        bookTopLeft.plus(OATHS[i].info()), INK_DIM);
            }
        }
    }

    private void drawOathCard(DrawContext context, Quest quest, int i) {
        ItemStack required = new ItemStack(quest.requiredItem(), Math.min(quest.requiredCount(), 64));
        Point icon = bookTopLeft.plus(OATHS[i].icon());
        BookText.drawItemCentered(context, this.textRenderer, required, icon.x(), icon.y());

        BookText.drawScaledText(context, this.textRenderer, quest.title() + " · T" + quest.tier(), false,
                bookTopLeft.plus(OATHS[i].title()), INK_TITLE);

        int held = this.client.player.getInventory().count(quest.requiredItem());
        int shown = Math.min(held, quest.requiredCount());
        int color = shown >= quest.requiredCount() ? INK_READY : INK_BODY;
        BookText.drawScaledText(context, this.textRenderer, shown + "/" + quest.requiredCount() + " gathered", false,
                bookTopLeft.plus(OATHS[i].info()), color);
    }

    // right page mode switching
    private void drawRightPage(DrawContext context, QuestProgressComponent progress) {
        if (showHelp) {
            drawHelpPage(context);
            return;
        }
        if (detailQuest != null) {
            drawDetailPage(context, progress);
            return;
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
            drawOfferEntry(context, progress, page.entries().get(i), i);
        }
    }

    private void drawOfferEntry(DrawContext context, QuestProgressComponent progress, Quest quest, int i) {
        boolean done = progress.hasCompleted(quest.id());
        boolean sworn = progress.isActive(quest.id());
        boolean dimmed = done || sworn;

        int titleColor = dimmed ? INK_DIM : INK_TITLE;
        int bodyColor = dimmed ? INK_DIM : INK_BODY;

        ItemStack required = new ItemStack(quest.requiredItem(), Math.min(quest.requiredCount(), 64));
        Point icon = bookTopLeft.plus(OFFERS[i].icon());
        BookText.drawItemCentered(context, this.textRenderer, required, icon.x(), icon.y());

        BookText.drawScaledText(context, this.textRenderer, quest.title(), false,
                bookTopLeft.plus(OFFERS[i].title()), titleColor);

        if (sworn || done) {
            BookText.drawScaledText(context, this.textRenderer, done ? "done" : "accepted", true,
                    bookTopLeft.plus(OFFERS[i].tag()), INK_TAG);
        }

        BookText.drawScaledText(context, this.textRenderer, quest.lore(), true,
                bookTopLeft.plus(OFFERS[i].desc()), bodyColor);

        String req = quest.requiredCount() + " × " + quest.requiredItem().getName().getString()
                + " → " + quest.rewardCount() + " × " + quest.rewardItem().getName().getString();
        BookText.drawScaledText(context, this.textRenderer, req, false,
                bookTopLeft.plus(OFFERS[i].info()), bodyColor);
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

        String req = quest.requiredCount() + " × " + quest.requiredItem().getName().getString()
                + " → " + quest.rewardCount() + " × " + quest.rewardItem().getName().getString();
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
            boolean sealedBoard = !showHelp && detailQuest == null
                    && !pages.isEmpty() && pages.get(pageIndex).locked();

            Identifier tex = showHelp ? TEXTURE_HELP
                    : (detailQuest != null || sealedBoard) ? TEXTURE_DETAIL
                      : BOOK_TEXTURE;
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