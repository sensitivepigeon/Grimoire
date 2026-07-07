package grimoire.modid;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class GrimoireScreen extends Screen {

    public GrimoireScreen() {
        super(Text.literal("Grimoire"));
    }

    @Override
    protected void init() {
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Close"), button -> this.close())
                        .dimensions(this.width / 2 - 50, this.height - 40, 100, 20)
                        .build()
        );
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(
                this.textRenderer,
                Text.literal("Bounties will appear here..."),
                this.width / 2,
                60,
                0xFFFFFF
        );
    }

    @Override
    public boolean shouldPause() {
        return false;
    }
}