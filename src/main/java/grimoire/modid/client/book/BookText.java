package grimoire.modid.client.book;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.item.ItemStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public final class BookText {

    private BookText() {}   // toolbox, not a thing. no instances.


    static final float HELP_TEXT_MIN_SCALE = 0.55f;

    static void drawItemCentered(DrawContext context, TextRenderer tr, ItemStack stack, int cx, int cy) {
        context.drawItem(stack, cx - 8, cy - 8);
        context.drawItemInSlot(tr, stack, cx - 8, cy - 8);
    }

    static void drawCenteredNoShadow(DrawContext context, TextRenderer tr, String text, int cx, int y, int color) {
        Text t = Text.literal(text);
        context.drawText(tr, t, cx - tr.getWidth(t) / 2, y, color, false);
    }


    static void drawScaledText(DrawContext context, TextRenderer tr, String text, boolean italic,
                               int x, int y, int maxWidth, int color) {
        if (text == null || text.isEmpty()) return;

        Text styled = italic ? Text.literal(text).formatted(Formatting.ITALIC) : Text.literal(text);
        int width = tr.getWidth(styled);

        if (width <= maxWidth) {
            context.drawText(tr, styled, x, y, color, false);
            return;
        }

        float scale = Math.max(0.65f, (float) maxWidth / width);

        if (width * scale > maxWidth) {
            String trimmed = tr.trimToWidth(text, (int) (maxWidth / scale) - 8) + "…";
            styled = italic ? Text.literal(trimmed).formatted(Formatting.ITALIC) : Text.literal(trimmed);
        }

        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);
        context.drawText(tr, styled, 0, 0, color, false);
        context.getMatrices().pop();
    }


    static void drawScaledText(DrawContext context, TextRenderer tr, String text, boolean italic,
                               Rect2i rect, int color) {
        drawScaledText(context, tr, text, italic, rect.getX(), rect.getY(), rect.getWidth(), color);
    }


    static int drawWrappedText(DrawContext context, TextRenderer tr, String text,
                               int x, int y, int maxWidth, int color) {
        if (text == null || text.isEmpty()) return y;

        int lineHeight = 10;
        String[] paragraphs = text.split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                y += lineHeight / 2;
                continue;
            }
            for (OrderedText line : tr.wrapLines(
                    StringVisitable.plain(paragraph), maxWidth)) {
                context.drawText(tr, line, x, y, color, false);
                y += lineHeight;
            }
            y += lineHeight / 2;
        }
        return y;
    }

    static void drawWrappedTextScaledToFit(DrawContext context, TextRenderer tr, String text,
                                           int x, int y, int maxWidth, int maxHeight, int color) {
        if (text == null || text.isEmpty()) return;

        int baseLineHeight = 10;
        int textHeight = measureWrappedTextHeight(tr, text, maxWidth, baseLineHeight);

        float scale = 1.0f;
        if (textHeight > maxHeight) {
            scale = Math.max(HELP_TEXT_MIN_SCALE, (float) maxHeight / textHeight);
        }

        int scaledWidth = Math.max(1, (int) (maxWidth / scale));


        context.getMatrices().push();
        context.getMatrices().translate(x, y, 0);
        context.getMatrices().scale(scale, scale, 1.0f);

        drawWrappedText(context, tr, text, 0, 0, scaledWidth, color);

        context.getMatrices().pop();
    }


    static int measureWrappedTextHeight(TextRenderer tr, String text, int maxWidth, int lineHeight) {
        int height = 0;
        String[] paragraphs = text.split("\n");

        for (String paragraph : paragraphs) {
            if (paragraph.isEmpty()) {
                height += lineHeight / 2;
                continue;
            }

            for (OrderedText ignored : tr.wrapLines(
                    StringVisitable.plain(paragraph), maxWidth)) {
                height += lineHeight;
            }

            height += lineHeight / 2;
        }

        return height;
    }
}