package de.niklas.miniblock.world;

import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

/** The retired 0.1.0 material IDs, used only when importing old saved containers. */
public enum MiniMaterial {
    STONE(1, "stone", Blocks.STONE),
    COBBLESTONE(2, "cobblestone", Blocks.COBBLESTONE),
    OAK_PLANKS(3, "oak_planks", Blocks.OAK_PLANKS),
    BRICKS(4, "bricks", Blocks.BRICKS),
    DIRT(5, "dirt", Blocks.DIRT),
    WHITE_CONCRETE(6, "white_concrete", Blocks.CONCRETE.pick(DyeColor.WHITE)),
    RED_CONCRETE(7, "red_concrete", Blocks.CONCRETE.pick(DyeColor.RED)),
    BLUE_CONCRETE(8, "blue_concrete", Blocks.CONCRETE.pick(DyeColor.BLUE));

    public final int id;
    public final String path;
    private final Block block;

    MiniMaterial(int id, String path, Block block) {
        this.id = id;
        this.path = path;
        this.block = block;
    }

    public BlockState state() { return block.defaultBlockState(); }
    public ItemStack stack(int count) { return new ItemStack(block.asItem(), count); }
    public static MiniMaterial byId(int id) {
        for (var material : values()) if (material.id == id) return material;
        return null;
    }
}
