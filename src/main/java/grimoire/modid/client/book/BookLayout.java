package grimoire.modid.client.book;

import grimoire.modid.Grimoire;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.util.Identifier;


final class BookLayout {

    private BookLayout() {
    }


    // shared layout types

    record Point(int x, int y) {
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

    record Size(int width, int height) {
    }

    // turn-in arrows now use arrow sprite but i misnamed it sorry
    record Oath(Rect2i title, Rect2i info, Point icon, Rect2i chevron, Rect2i cancel) {
        private static final Size TEXT_SIZE = new Size(108, 10);

        private static final Point INFO_OFFSET = new Point(0, 14);

        private static final Point ICON_OFFSET = new Point(-18, 16);

        private static final Point CHEVRON_OFFSET = new Point(66, 25);
        private static final Size CHEVRON_SIZE = new Size(44, 12);

        private static final Point CANCEL_OFFSET = new Point(-40, -5);   // PLACEHOLDER — you'll tune
        private static final Size CANCEL_SIZE = new Size(16, 16);     // must match icon_x.png


        static Oath at(Point title) {
            Point info = title.plus(INFO_OFFSET);
            Point icon = title.plus(ICON_OFFSET);
            Point chevron = title.plus(CHEVRON_OFFSET);
            Point cancel = title.plus(CANCEL_OFFSET);
            return new Oath(
                    title.rect(TEXT_SIZE),
                    info.rect(TEXT_SIZE),
                    icon,
                    chevron.rect(CHEVRON_SIZE),
                    cancel.rect(CANCEL_SIZE));
        }
    }

    record Offer(Rect2i title, Rect2i desc, Point icon, Rect2i accept, Rect2i tag, Rect2i info) {
        private static final Size TITLE_SIZE = new Size(82, 10);

        private static final Point DESC_OFFSET = new Point(0, 12);
        private static final Size DESC_SIZE = new Size(125, 10);

        private static final Point ICON_OFFSET = new Point(-14, 12);

        private static final Point ACCEPT_OFFSET = new Point(83, 28);
        private static final Size ACCEPT_SIZE = new Size(45, 12);

        private static final Point TAG_OFFSET = new Point(86, 0);
        private static final Size TAG_SIZE = new Size(38, 10);

        private static final Point INFO_OFFSET = new Point(-26, 33);
        private static final Size INFO_SIZE = new Size(106, 10);   // trade line

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

    static final int BOOK_WIDTH = 420;
    static final int BOOK_HEIGHT = 234;
    static final int PAGE_SPLIT = BOOK_WIDTH / 2;

    // banner
    static final Point BANNER_COUNT = new Point(109, 19);

    // left page - bargain cards, still called oath in code. habits hard to break
    static final Oath[] OATHS = {   // x=66..174
            Oath.at(new Point(68, 54)),
            Oath.at(new Point(68, 108)),
            Oath.at(new Point(68, 163)),
    };

    // right page - header
    static final int RIGHT_CX = 314;                       // header/title center
    static final int RIGHT_TITLE_Y = 24;
    static final int RIGHT_SUB_Y = 32;
    static final int RIGHT_HEADER_W = 120;                 // safe 251..380

    // right page - offer cards
    static final Offer[] OFFERS = {
            Offer.at(new Point(268, 55)),
            Offer.at(new Point(268, 108)),
            Offer.at(new Point(268, 163)),
    };

    // controls for buttons
    static final Rect2i DICE = new Rect2i(299, 201, 18, 16);      // reroll
    static final Rect2i HELP = new Rect2i(158, 10, 43, 43);       // "?" glyph (center 173,30)
    static final Rect2i NAV_L = new Rect2i(239, 22, 11, 16);
    static final Rect2i NAV_R = new Rect2i(381, 22, 11, 16);

    // back/accept buttons shared by detail and help modes
    static final Rect2i DETAIL_BACK = new Rect2i(234, 195, 45, 12);
    static final Rect2i DETAIL_ACCEPT = new Rect2i(349, 195, 45, 12);
    static final Rect2i HELP_BACK = new Rect2i(30, 195, 45, 12);


    // PALETTE - adjust carefully

    static final int INK_TITLE = 0xFF4A2E1A;
    static final int INK_BODY = 0xFF5C4732;
    static final int INK_DIM = 0xFF8A785F;
    static final int INK_READY = 0xFF2F6B2F;               // gathered-complete green
    static final int INK_TAG = 0xFF6E3B1F;                 // accepted tag
    static final int BANNER_INK = 0xFFEAF6F6;              // light on blue banner

    static final Identifier BOOK_TEXTURE =
            new Identifier(Grimoire.MOD_ID, "textures/gui/grimoire_book.png");
    static final boolean USE_TEXTURE = true;

    // button sprites
    static final Identifier SPRITE_TURNIN =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/accept_arrow.png");
    static final Identifier SPRITE_ACCEPT =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/accept_arrow.png");
    static final Identifier SPRITE_DICE =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/dice.png");
    static final Identifier SPRITE_NAV_L =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/nav_left.png");
    static final Identifier SPRITE_NAV_R =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/nav_right.png");
    static final Identifier SPRITE_BACK =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/back_arrow.png");
    static final Identifier SPRITE_CANCEL =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/icon_x.png");
    static final Identifier SPRITE_HELP =
            new Identifier(Grimoire.MOD_ID, "textures/gui/sprites/sigil_help.png");

    // mode backgrounds
    static final Identifier TEXTURE_DETAIL =
            new Identifier(Grimoire.MOD_ID, "textures/gui/grimoire_book_detail.png");
    static final Identifier TEXTURE_HELP =
            new Identifier(Grimoire.MOD_ID, "textures/gui/grimoire_book_help.png");


    // codex rows (right page) - placeholders to refine
    static final Rect2i CODEX_ROW = new Rect2i(241, 56, 140, 10);   // row 0
    static final int CODEX_ROW_PITCH = 12;
    static final int CODEX_ROWS_PER_PAGE = 11;

    // codex paging - placeholders
    static final Rect2i CODEX_NAV_L = new Rect2i(255, 201, 11, 16);
    static final Rect2i CODEX_NAV_R = new Rect2i(360, 201, 11, 16);
    static final Point  CODEX_PAGE_LABEL = new Point(313, 205);   // "2 / 5", centered between navs

    // help page layout, text edits in HelpText java
    static final Point HELP_HEADER = new Point(109, 30);
    static final Rect2i HELP_L = new Rect2i(38, 56, 145, 145);
    static final Rect2i HELP_R = new Rect2i(241, 33, 145, 210);

    static final int INDEX_ROW_W = 140;
    static final int INDEX_ROW_H = 10;   // clickable height per row
    static final Rect2i[] INDEX_ROWS = {
            new Rect2i(241, 60, INDEX_ROW_W, INDEX_ROW_H),
            new Rect2i(241, 80, INDEX_ROW_W, INDEX_ROW_H),
    };
}
