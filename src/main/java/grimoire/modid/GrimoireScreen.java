package grimoire.modid;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
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
        // Turn In button for our hardcoded iron quest
        this.addDrawableChild(
                ButtonWidget.builder(Text.literal("Turn In: 10 Iron → 1 Diamond"), button -> {
                            ClientPlayNetworking.send(ModNetworking.TURN_IN_QUEST, PacketByteBufs.create());
                        })
                        .dimensions(this.width / 2 - 75, 90, 150, 20)
                        .build()
        );

        // Close button
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