package grimoire.modid.item;

import grimoire.modid.Grimoire;
import net.fabricmc.fabric.api.item.v1.FabricItemSettings;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.util.Identifier;

public class ModItems {

    public static final Item GRIMOIRE_TOME = new TomeItem(new FabricItemSettings().maxCount(1));

    public static void register() {
        Registry.register(Registries.ITEM, new Identifier(Grimoire.MOD_ID, "grimoire_tome"), GRIMOIRE_TOME);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS)
                .register(entries -> entries.add(GRIMOIRE_TOME));
    }
}