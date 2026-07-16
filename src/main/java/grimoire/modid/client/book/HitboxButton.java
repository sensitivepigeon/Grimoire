package grimoire.modid.client.book;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.util.math.Rect2i;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

/**
 * A click hitbox for the art, you have to redo this yourself if you change the art.
 * Labels for screen reading.
 */
public class HitboxButton extends ButtonWidget {

    private Identifier sprite = null;
    private boolean drawLabel = false;
    private int labelColor = 0xFF2F2A1E;

    public HitboxButton(Rect2i rect, Text label, PressAction onPress) {
        super(rect.getX(), rect.getY(), rect.getWidth(), rect.getHeight(), label, onPress, DEFAULT_NARRATION_SUPPLIER);
    }


    public HitboxButton withSprite(Identifier idle) {
        this.sprite = idle;
        return this;
    }

    public HitboxButton withLabel(int color) {
        this.drawLabel = true;
        this.labelColor = color;
        return this;
    }

    @Override
    protected void renderButton(DrawContext context, int mouseX, int mouseY, float delta) {
        if (sprite != null) {
            // texture, x, y, u, v, drawW, drawH, fileW, fileH - file dims = button dims
            context.drawTexture(sprite, getX(), getY(), 0, 0, width, height, width, height);
        }
        if (drawLabel) {
            var tr = net.minecraft.client.MinecraftClient.getInstance().textRenderer;
            int tw = tr.getWidth(this.getMessage());
            context.drawText(tr, this.getMessage(),
                    getX() + (width - tw) / 2, getY() + (height - 8) / 2, labelColor, false);
        }
        if (this.isHovered() && this.active) {
            context.fill(getX(), getY(), getX() + width, getY() + height, 0x30FFFFFF);
        }
    }
}