package grimoire.modid.item;

import grimoire.modid.quest.BountyBoard;
import grimoire.modid.client.GrimoireModClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class TomeItem extends Item {

    public TomeItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            GrimoireModClient.openGrimoireScreen();
        } else {
            BountyBoard.ensureFreshRotation((ServerPlayerEntity) user);
        }

        return TypedActionResult.success(stack, world.isClient);
    }
}