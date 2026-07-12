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
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class GrimoireScreen extends Screen {


    // ART LAYOUT - coords for sprites and text on 420x234 canvas.

    private record Point(int x, int y) {
        Point plus(Point p) {
            return new Point(x + p.x, y + p.y);
        }

        Rect2i plus(Rect2i r) {
            return new Rect2i(x + r.getX(), y + r.getY(), r.getWidth(), r.getHeight());
        }
    }

    private static final int BOOK_WIDTH = 420;
    private static final int BOOK_HEIGHT = 234;
    private static final int PAGE_SPLIT = 210;

    // banner
    private static final Point BANNER_COUNT = new Point(109, 19);

    // left page - bargain cards, still called oath in code. habits hard to break
    private static final Rect2i[] OATH_TITLE = {   // x=66..174
            new Rect2i(66, 52, 108, 10),
            new Rect2i(66, 106, 108, 10),
            new Rect2i(66, 161, 108, 10),
    };
    private static final Rect2i[] OATH_INFO = {
            new Rect2i(66, 65, 108, 10),
            new Rect2i(66, 120, 108, 10),
            new Rect2i(66, 175, 108, 10),
    };
    private static final Point[] OATH_ICON = {
            new Point(48, 67),
            new Point(48, 122),
            new Point(48, 177),
    };

    // turn-in arrows now use arrow sprite but i misnamed it sorry
    private static final Rect2i[] CHEVRON = {
            new Rect2i(132, 77, 44, 12),
            new Rect2i(132, 131, 44, 12),
            new Rect2i(132, 185, 44, 12),
    };

    // right page - header
    private static final int RIGHT_CX = 314;                       // header/title center
    private static final int RIGHT_TITLE_Y = 22;
    private static final int RIGHT_SUB_Y = 30;
    private static final int RIGHT_HEADER_W = 120;                 // safe 251..380

    // right page - offer cards
    private static final Point[] OFFER_ICON = {
            new Point(254, 66),
            new Point(254, 120),
            new Point(254, 175),
    };
    private static final Rect2i[] OFFER_TITLE = {
            new Rect2i(268, 55, 82, 10),
            new Rect2i(268, 108, 82, 10),
            new Rect2i(268, 163, 82, 10),
    };
    private static final Rect2i[] OFFER_DESC = {
            new Rect2i(268, 67, 125, 10),
            new Rect2i(268, 122, 125, 10),
            new Rect2i(268, 175, 125, 10),
    };
    private static final Rect2i[] INFO = {   // trade line; ARTIST-CONFIRM: note cut off; guessed from arrow band
            new Rect2i(242, 88, 106, 10),
            new Rect2i(242, 141, 106, 10),
            new Rect2i(242, 195, 106, 10),
    };
    private static final Rect2i[] TAG = {
            new Rect2i(354, 55, 38, 10),
            new Rect2i(354, 108, 38, 10),
            new Rect2i(354, 163, 38, 10),
    };

    // accept arrows (painted art of the design; left is hitboxes only. x351..396)
    private static final Rect2i[] ACCEPT = {
            new Rect2i(351, 83, 45, 12),
            new Rect2i(351, 136, 45, 12),
            new Rect2i(351, 191, 45, 12),
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
    private static final float HELP_TEXT_MIN_SCALE = 0.55f;

    private static final int MAX_OATHS = 3;    // mirrored in BountyBoard.MAX_ACTIVE_BOUNTIES

    private int bookLeft;
    private int bookTop;
    private Point origin;                      // (bookLeft, bookTop) - add to a Rect2i/Point to place it on screen
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
        this.bookLeft = (this.width - BOOK_WIDTH) / 2;
        this.bookTop = (this.height - BOOK_HEIGHT) / 2;
        this.origin = new Point(bookLeft, bookTop);

        buildActives(progress);
        buildPages(progress);
        if (pageIndex >= pages.size()) pageIndex = pages.size() - 1;
        if (pageIndex < 0) pageIndex = 0;

        if (!showHelp) {
            Rect2i helpBtn = origin.plus(HELP);
            this.addDrawableChild(new HitboxButton(
                    helpBtn.getX(), helpBtn.getY(), helpBtn.getWidth(), helpBtn.getHeight(),
                    Text.literal("?"), b -> {
                this.showHelp = true;
                this.detailQuest = null;
                this.clearAndInit();
            }));
        }


        // left page turn in hitboxes
        if (!showHelp) {
            for (int i = 0; i < actives.size(); i++) {
                Quest quest = actives.get(i);


                if (!quest.description().isEmpty()) {
                    final Quest q = quest;
                    Rect2i more = origin.plus(OATH_TITLE[i]);
                    this.addDrawableChild(new HitboxButton(
                            more.getX(), more.getY(), more.getWidth(), more.getHeight(),
                            Text.literal("More"), b -> {
                        this.detailQuest = q;
                        this.clearAndInit();
                    }));
                }

                final String id = actives.get(i).id();
                Rect2i chevron = origin.plus(CHEVRON[i]);
                this.addDrawableChild(new HitboxButton(
                        chevron.getX(), chevron.getY(), chevron.getWidth(), chevron.getHeight(),
                        Text.literal("Turn in"), b -> {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeString(id);
                    ClientPlayNetworking.send(ModNetworking.TURN_IN_QUEST, buf);
                }).withSprite(SPRITE_TURNIN).withLabel(0xFF2F3D1A));
            }
        }

        if (showHelp) {
           // help mode
            Rect2i helpBack = origin.plus(HELP_BACK);
            this.addDrawableChild(new HitboxButton(
                    helpBack.getX(), helpBack.getY(), helpBack.getWidth(), helpBack.getHeight(),
                    Text.literal("Back"), b -> {
                this.showHelp = false;
                this.clearAndInit();
            }).withSprite(SPRITE_BACK).withLabel(0xFF2F3D1A));

        } else if (detailQuest != null) {
            // detail mode
            Rect2i detailBack = origin.plus(DETAIL_BACK);
            this.addDrawableChild(new HitboxButton(
                    detailBack.getX(), detailBack.getY(), detailBack.getWidth(), detailBack.getHeight(),
                    Text.literal("Back"), b -> {
                this.detailQuest = null;
                this.clearAndInit();
            }).withSprite(SPRITE_BACK).withLabel(0xFF2F3D1A));

            boolean done = progress.hasCompleted(detailQuest.id());
            boolean sworn = progress.isActive(detailQuest.id());
            boolean atCap = progress.getActiveCount() >= MAX_OATHS;

            if (!sworn && !done) {
                final String id = detailQuest.id();
                Rect2i detailAccept = origin.plus(DETAIL_ACCEPT);
                HitboxButton accept = new HitboxButton(
                        detailAccept.getX(), detailAccept.getY(), detailAccept.getWidth(), detailAccept.getHeight(),
                        Text.literal("Accept"), b -> {
                    PacketByteBuf buf = PacketByteBufs.create();
                    buf.writeString(id);
                    ClientPlayNetworking.send(ModNetworking.ACCEPT_QUEST, buf);
                }).withSprite(SPRITE_ACCEPT).withLabel(0xFF2F3D1A);
                accept.active = !done && !atCap;
                this.addDrawableChild(accept);
            }

        } else {
            // board mode
            BookPage page = pages.get(pageIndex);
            if (!page.locked()) {
                boolean atCap = progress.getActiveCount() >= MAX_OATHS;

                for (int i = 0; i < page.entries().size(); i++) {
                    Quest quest = page.entries().get(i);
                    final String id = quest.id();
                    boolean done = progress.hasCompleted(id);
                    boolean sworn = progress.isActive(id);

                    // detail access: invisible hitbox over title
                    if (!quest.description().isEmpty()) {
                        final Quest q = quest;
                        Rect2i more = origin.plus(OFFER_TITLE[i]);
                        this.addDrawableChild(new HitboxButton(
                                more.getX(), more.getY(), more.getWidth(), more.getHeight(),
                                Text.literal("More"), b -> {
                            this.detailQuest = q;
                            this.clearAndInit();
                        }));
                    }



                    if (sworn || done) continue;   // accepted bargains: tag instead of arrow

                    Rect2i acceptRect = origin.plus(ACCEPT[i]);
                    HitboxButton accept = new HitboxButton(
                            acceptRect.getX(), acceptRect.getY(), acceptRect.getWidth(), acceptRect.getHeight(),
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
            Rect2i navLRect = origin.plus(NAV_L);
            HitboxButton navL = new HitboxButton(navLRect.getX(), navLRect.getY(),
                    navLRect.getWidth(), navLRect.getHeight(),
                    Text.literal("Previous tier"), b -> {
                pageIndex--;
                this.clearAndInit();
            }).withSprite(SPRITE_NAV_L);
            navL.active = pageIndex > 0;
            this.addDrawableChild(navL);

            Rect2i navRRect = origin.plus(NAV_R);
            HitboxButton navR = new HitboxButton(navRRect.getX(), navRRect.getY(),
                    navRRect.getWidth(), navRRect.getHeight(),
                    Text.literal("Next tier"), b -> {
                pageIndex++;
                this.clearAndInit();
            }).withSprite(SPRITE_NAV_R);
            navR.active = pageIndex < pages.size() - 1;
            this.addDrawableChild(navR);

            // dice = reroll (sprite)
            Rect2i dice = origin.plus(DICE);
            this.addDrawableChild(new HitboxButton(dice.getX(), dice.getY(),
                    dice.getWidth(), dice.getHeight(),
                    Text.literal("Reroll"), b ->
                    ClientPlayNetworking.send(ModNetworking.REROLL, PacketByteBufs.create()))
                    .withSprite(SPRITE_DICE));
        }

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
        Point bannerCount = origin.plus(BANNER_COUNT);
        context.drawCenteredTextWithShadow(this.textRenderer,
                Text.literal(actives.size() + " of " + MAX_OATHS + " accepted"),
                bannerCount.x(), bannerCount.y(), BANNER_INK);

        Rect2i help = origin.plus(HELP);
        context.drawText(this.textRenderer,
                Text.literal("?"),
                help.getX() + 6,
                help.getY() + 8,
                INK_TITLE,
                false);

        for (int i = 0; i < MAX_OATHS; i++) {
            if (i < actives.size()) {
                drawOathCard(context, actives.get(i), i);
            } else {
                Rect2i info = origin.plus(OATH_INFO[i]);
                drawScaledText(context, "-- empty bargain --", true,
                        info.getX(), info.getY(), info.getWidth(), INK_DIM);
            }
        }
    }

    private void drawOathCard(DrawContext context, Quest quest, int i) {
        ItemStack required = new ItemStack(quest.requiredItem(), Math.min(quest.requiredCount(), 64));
        Point icon = origin.plus(OATH_ICON[i]);
        drawItemCentered(context, required, icon.x(), icon.y());

        Rect2i title = origin.plus(OATH_TITLE[i]);
        drawScaledText(context, quest.title() + " · T" + quest.tier(), false,
                title.getX(), title.getY(), title.getWidth(), INK_TITLE);

        int held = this.client.player.getInventory().count(quest.requiredItem());
        int shown = Math.min(held, quest.requiredCount());
        int color = shown >= quest.requiredCount() ? INK_READY : INK_BODY;
        Rect2i info = origin.plus(OATH_INFO[i]);
        drawScaledText(context, shown + "/" + quest.requiredCount() + " gathered", false,
                info.getX(), info.getY(), info.getWidth(), color);
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

        drawCenteredNoShadow(context, tierName, bookLeft + RIGHT_CX, bookTop + RIGHT_TITLE_Y, INK_TITLE);

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
        drawCenteredNoShadow(context, sub, bookLeft + RIGHT_CX, bookTop + RIGHT_SUB_Y, INK_DIM);

        if (page.locked()) {
            TierConfig prev = ClientQuestCache.TIERS.get(page.tier() - 1);
            int need = prev != null ? prev.completionsToUnlockNext() : 0;
            int have = progress.getCompletions(page.tier() - 1);

            drawCenteredNoShadow(context, "This page is sealed.",
                    bookLeft + RIGHT_CX, bookTop + 100, INK_DIM);
            drawCenteredNoShadow(context, "Fulfill " + Math.max(0, need - have) + " more of the prior rank.",
                    bookLeft + RIGHT_CX, bookTop + 114, INK_DIM);
            return;
        }

        if (page.entries().isEmpty()) {
            drawCenteredNoShadow(context, "The pages are blank until tomorrow...",
                    bookLeft + RIGHT_CX, bookTop + 105, INK_DIM);
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
        Point icon = origin.plus(OFFER_ICON[i]);
        drawItemCentered(context, required, icon.x(), icon.y());

        Rect2i title = origin.plus(OFFER_TITLE[i]);
        drawScaledText(context, quest.title(), false,
                title.getX(), title.getY(), title.getWidth(), titleColor);

        if (sworn || done) {
            Rect2i tag = origin.plus(TAG[i]);
            drawScaledText(context, done ? "done" : "accepted", true,
                    tag.getX(), tag.getY(), tag.getWidth(), INK_TAG);
        }

        Rect2i desc = origin.plus(OFFER_DESC[i]);
        drawScaledText(context, quest.lore(), true,
                desc.getX(), desc.getY(), desc.getWidth(), bodyColor);

        String req = quest.requiredCount() + " × " + quest.requiredItem().getName().getString()
                + " → " + quest.rewardCount() + " × " + quest.rewardItem().getName().getString();
        Rect2i info = origin.plus(INFO[i]);
        drawScaledText(context, req, false,
                info.getX(), info.getY(), info.getWidth(), bodyColor);
    }

    // detail mode layout
    private void drawDetailPage(DrawContext context, QuestProgressComponent progress) {
        Quest quest = detailQuest;
        int x = bookLeft + 234;
        int textWidth = 155;

        drawCenteredNoShadow(context, quest.title() + " · T" + quest.tier(),
                bookLeft + RIGHT_CX, bookTop + RIGHT_TITLE_Y, INK_TITLE);

        boolean sworn = progress.isActive(quest.id());
        boolean done = progress.hasCompleted(quest.id());
        if (sworn || done) {
            drawCenteredNoShadow(context, done ? "fulfilled today" : "accepted",
                    bookLeft + RIGHT_CX, bookTop + RIGHT_SUB_Y, INK_DIM);
        }

        int tradeY = bookTop + 46;
        ItemStack required = new ItemStack(quest.requiredItem(), Math.min(quest.requiredCount(), 64));
        context.drawItem(required, x, tradeY);
        context.drawItemInSlot(this.textRenderer, required, x, tradeY);

        String req = quest.requiredCount() + " × " + quest.requiredItem().getName().getString()
                + " → " + quest.rewardCount() + " × " + quest.rewardItem().getName().getString();
        drawScaledText(context, req, false, x + 22, tradeY + 4, textWidth - 22, INK_BODY);

        drawWrappedText(context, quest.description(), x, tradeY + 26, textWidth, INK_BODY);
    }



    public void drawHelpPage(DrawContext context) {
        // header sits above the painted line at y=58
        Point header = origin.plus(HELP_HEADER);
        drawCenteredNoShadow(context, "The Book of Bargains",
                header.x(), header.y(), INK_TITLE);

        Rect2i left = origin.plus(HELP_L);
        drawWrappedTextScaledToFit(context, HelpText.LEFT,
                left.getX(), left.getY(), left.getWidth(), left.getHeight(), INK_BODY);

        Rect2i right = origin.plus(HELP_R);
        drawWrappedTextScaledToFit(context, HelpText.RIGHT,
                right.getX(), right.getY(), right.getWidth(), right.getHeight(), INK_BODY);
    }


    // draw help

    private void drawItemCentered(DrawContext context, ItemStack stack, int cx, int cy) {
        context.drawItem(stack, cx - 8, cy - 8);
        context.drawItemInSlot(this.textRenderer, stack, cx - 8, cy - 8);
    }

    private void drawCenteredNoShadow(DrawContext context, String text, int cx, int y, int color) {
        Text t = Text.literal(text);
        context.drawText(this.textRenderer, t, cx - this.textRenderer.getWidth(t) / 2, y, color, false);
    }

    // single line: shrink to 0.65x, then ellipsis. shadowless
    private void drawScaledText(DrawContext context, String text, boolean italic,
                                int x, int y, int maxWidth, int color) {
        if (text == null || text.isEmpty()) return;

        Text styled = italic ? Text.literal(text).formatted(Formatting.ITALIC) : Text.literal(text);
        int width = this.textRenderer.getWidth(styled);

        if (width <= maxWidth) {
            context.drawText(this.textRenderer, styled, x, y, color, false);
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
        context.drawText(this.textRenderer, styled, 0, 0, color, false);
        context.getMatrices().pop();
    }

    // long text: wrapped lines, \n = paragraph break. returns y after last line.
    private int drawWrappedText(DrawContext context, String text,
                                int x, int y, int maxWidth, int color) {
        if (text == null || text.isEmpty()) return y;

        int lineHeight = 10;
        String[] paragraphs = text.split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                y += lineHeight / 2;
                continue;
            }
            for (OrderedText line : this.textRenderer.wrapLines(
                    StringVisitable.plain(paragraph), maxWidth)) {
                context.drawText(this.textRenderer, line, x, y, color, false);
                y += lineHeight;
            }
            y += lineHeight / 2;
        }
        return y;
    }

    private void drawWrappedTextScaledToFit(DrawContext context, String text,
                                            int x, int y, int maxWidth, int maxHeight, int color) {
        if (text == null || text.isEmpty()) return;

        int baseLineHeight = 10;
        int textHeight = measureWrappedTextHeight(text, maxWidth, baseLineHeight);

        float scale = 1.0f;
        if (textHeight > maxHeight) {
            scale = Math.max(HELP_TEXT_MIN_SCALE, (float) maxHeight / textHeight);
        }

        int scaledWidth = Math.max(1, (int) (maxWidth / scale));
        int scaledTextHeight = (int) (textHeight * scale);
        int offset = Math.max(0, (maxHeight - scaledTextHeight) / 2);


        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        drawWrappedText(context, text, 0, 0, scaledWidth, color);

        context.getMatrices().pop();
    }

    private int measureWrappedTextHeight(String text, int maxWidth, int lineHeight) {
        int height = 0;
        String[] paragraphs = text.split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                height += lineHeight / 2;
                continue;
            }

            for (OrderedText ignored : this.textRenderer.wrapLines(
                    StringVisitable.plain(paragraph), maxWidth)) {
                height += lineHeight;
            }

            height += lineHeight / 2;
        }

        return height;
    }

    private void drawBookBackground(DrawContext context) {
        if (USE_TEXTURE) {
            boolean sealedBoard = !showHelp && detailQuest == null
                    && !pages.isEmpty() && pages.get(pageIndex).locked();

            Identifier tex = showHelp ? TEXTURE_HELP
                    : (detailQuest != null || sealedBoard) ? TEXTURE_DETAIL
                      : BOOK_TEXTURE;
            context.drawTexture(tex, bookLeft, bookTop, 0, 0,
                    BOOK_WIDTH, BOOK_HEIGHT, BOOK_WIDTH, BOOK_HEIGHT);
            return;
        }

        // fallback: placeholder fills
        context.fill(bookLeft, bookTop, bookLeft + BOOK_WIDTH, bookTop + BOOK_HEIGHT, 0xF2211A14);
        context.fill(bookLeft + 4, bookTop + 4, bookLeft + BOOK_WIDTH - 4, bookTop + BOOK_HEIGHT - 4, 0xFF2E2620);
        context.fill(bookLeft + PAGE_SPLIT - 1, bookTop + 4, bookLeft + PAGE_SPLIT + 1, bookTop + BOOK_HEIGHT - 4, 0x66000000);
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}
