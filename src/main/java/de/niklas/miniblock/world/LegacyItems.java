package de.niklas.miniblock.world;

import de.niklas.miniblock.MiniBlockMod;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Items;
import net.minecraftforge.registries.MissingMappingsEvent;

/** Existing inventories keep usable vanilla items when the obsolete mini items disappear. */
public final class LegacyItems {
    private LegacyItems() {}

    public static void register() {
        MissingMappingsEvent.BUS.addListener(event -> {
            for (var mapping : event.getMappings(Registries.ITEM, MiniBlockMod.MODID)) {
                String path = mapping.getKey().getPath();
                if (path.equals("chisel")) {
                    mapping.remap(Items.IRON_PICKAXE);
                } else {
                    for (MiniMaterial material : MiniMaterial.values()) {
                        if (path.equals("mini_" + material.path)) {
                            mapping.remap(material.state().getBlock().asItem());
                            break;
                        }
                    }
                }
            }
        });
    }
}
