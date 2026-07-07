package grimoire.modid.data;

import dev.onyxstudios.cca.api.v3.component.ComponentKey;
import dev.onyxstudios.cca.api.v3.component.ComponentRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentFactoryRegistry;
import dev.onyxstudios.cca.api.v3.entity.EntityComponentInitializer;
import dev.onyxstudios.cca.api.v3.entity.RespawnCopyStrategy;
import grimoire.modid.Grimoire;
import net.minecraft.util.Identifier;

public class ModComponents implements EntityComponentInitializer {

    public static final ComponentKey<QuestProgressComponent> QUEST_PROGRESS =
            ComponentRegistry.getOrCreate(
                    new Identifier(Grimoire.MOD_ID, "quest_progress"),
                    QuestProgressComponent.class
            );

    @Override
    public void registerEntityComponentFactories(EntityComponentFactoryRegistry registry) {
        registry.registerForPlayers(
                QUEST_PROGRESS,
                player -> new QuestProgressComponent(),
                RespawnCopyStrategy.ALWAYS_COPY
        );
    }
}