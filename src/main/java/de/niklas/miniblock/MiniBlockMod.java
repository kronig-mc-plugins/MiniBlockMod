package de.niklas.miniblock;

import de.niklas.miniblock.player.PlayerScale;
import de.niklas.miniblock.player.ShrinkDeviceItem;
import de.niklas.miniblock.world.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.Set;

@Mod(MiniBlockMod.MODID)
public final class MiniBlockMod {
    public static final String MODID = "miniblock";
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, MODID);
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, MODID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, MODID);
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);

    public static final RegistryObject<MiniBlock> MINI_BLOCK = BLOCKS.register("mini_block", () -> new MiniBlock(
            BlockBehaviour.Properties.of().setId(BLOCKS.key("mini_block")).strength(0.5F)
                    .dynamicShape().noOcclusion().pushReaction(PushReaction.IMMOVEABLE)
                    .isSuffocating((state, level, pos) -> false)
                    .isViewBlocking((state, level, pos, box) -> false)
                    .isRedstoneConductor((state, level, pos) -> false)));
    public static final RegistryObject<BlockEntityType<MiniBlockEntity>> MINI_BLOCK_ENTITY = BLOCK_ENTITIES.register(
            "mini_block", () -> new BlockEntityType<>(MiniBlockEntity::new, Set.of(MINI_BLOCK.get())));
    public static final RegistryObject<Item> SHRINK_DEVICE = ITEMS.register("shrink_device", () -> new ShrinkDeviceItem(
            new Item.Properties().setId(ITEMS.key("shrink_device")).stacksTo(1)));
    public static final RegistryObject<CreativeModeTab> MINI_TAB = TABS.register("miniblock", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.miniblock"))
            .withTabsBefore(CreativeModeTabs.BUILDING_BLOCKS)
            .icon(() -> SHRINK_DEVICE.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(SHRINK_DEVICE.get());
            }).build());

    public MiniBlockMod(FMLJavaModLoadingContext context) {
        var bus = context.getModBusGroup();
        BLOCKS.register(bus);
        ITEMS.register(bus);
        BLOCK_ENTITIES.register(bus);
        TABS.register(bus);
        PlayerScale.register();
        LegacyItems.register();
    }
}
