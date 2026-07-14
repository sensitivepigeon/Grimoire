package grimoire.modid.event;




import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.server.network.ServerPlayerEntity;

// this file has announcements for events within the mod
// useful for kubejs scripts and other mods asking quest tome what's up
// reroll and rotation currently are player-specific sorry
// global timer added later
// wired to modnetworking and bountyboard
// tier unlocks added later

public class QuestTomeEvents {

    @FunctionalInterface
    public interface BargainAccepted {
        void onAccepted(ServerPlayerEntity player, String questId, int tier, String patron);
    }

    @FunctionalInterface
    public interface BargainCompleted {
        void onCompleted(ServerPlayerEntity player, String questId, int tier, String patron);
    }

    @FunctionalInterface
    public interface BargainRerolled {
        void onRerolled(ServerPlayerEntity player);
    }

    @FunctionalInterface
    public interface RotationRolled {
        void onRolled(ServerPlayerEntity player);
    }
// modnetworking
    public static final Event<BargainAccepted> BARGAIN_ACCEPTED =
            EventFactory.createArrayBacked(BargainAccepted.class,
                    listeners -> (player, questId, tier, patron) -> {
                        for (BargainAccepted l : listeners) l.onAccepted(player, questId, tier, patron);
                    });

    public static final Event<BargainCompleted> BARGAIN_COMPLETED =
            EventFactory.createArrayBacked(BargainCompleted.class,
                    listeners -> (player, questId, tier, patron) -> {
                        for (BargainCompleted l : listeners) l.onCompleted(player, questId, tier, patron);
                    });
// bounty board
    public static final Event<BargainRerolled> BARGAIN_REROLLED =
            EventFactory.createArrayBacked(BargainRerolled.class,
                    listeners -> (player) -> {
                        for (BargainRerolled l : listeners) l.onRerolled(player);
                    });

    public static final Event<RotationRolled> ROTATION_ROLLED =
            EventFactory.createArrayBacked(RotationRolled.class,
                    listeners -> (player) -> {
                        for (RotationRolled l : listeners) l.onRolled(player);
                    });
}