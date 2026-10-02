package de.niklas.miniblock.player;

import de.niklas.miniblock.MiniBlockMod;
import de.niklas.miniblock.world.MiniBlockEntity;
import de.niklas.miniblock.world.MiniMaterial;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;

/** Native player-dimension and safe-growth checks in a real server collision world. */
@GameTestNamespace(MiniBlockMod.MODID)
public final class PlayerScaleGameTests {
    private static final Identifier SIZE_MODIFIER = Identifier.fromNamespaceAndPath("miniblock", "mini_size");
    private static final BlockPos TUNNEL = new BlockPos(1, 1, 1);

    @GameTest
    public static void miniaturePlayerCanGrowOnlyWhenCurrentPoseFits(GameTestHelper helper) {
        helper.setBlock(TUNNEL, MiniBlockMod.MINI_BLOCK.get().defaultBlockState());
        MiniBlockEntity container = helper.getBlockEntity(TUNNEL, MiniBlockEntity.class);
        container.grid().fill(MiniMaterial.STONE.id);
        for (int x = 1; x < 7; x++) {
            for (int y = 0; y < 5; y++) {
                for (int z = 0; z < 8; z++) {
                    container.grid().set(x, y, z, 0);
                }
            }
        }

        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        Vec3 position = helper.absoluteVec(new Vec3(1.5, 1.01, 1.5));
        player.setPos(position.x, position.y, position.z);
        player.setPose(Pose.STANDING);
        var scale = player.getAttribute(Attributes.SCALE);
        helper.assertTrue(scale != null, "A native player must support the vanilla scale attribute");
        scale.addOrReplacePermanentModifier(new AttributeModifier(SIZE_MODIFIER, -0.875,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        player.refreshDimensions();

        helper.assertTrue(PlayerScale.isMini(player), "The permanent mini modifier must mark the player as small");
        helper.assertTrue(Math.abs(player.getDimensions(Pose.STANDING).width() - 0.075) < 1.0E-6,
                "The actual standing width must be one eighth of the native player width");
        helper.assertTrue(Math.abs(player.getDimensions(Pose.STANDING).height() - 0.225) < 1.0E-6,
                "The actual standing height must be one eighth of the native player height");
        helper.assertTrue(Math.abs(player.getEyeHeight() - 0.2025) < 1.0E-6,
                "The native eye height must shrink with the collision box");
        helper.assertValueEqual(player.position(), position,
                "Refreshing a player's dimensions must preserve the feet position");
        helper.assertTrue(helper.getLevel().noCollision(player, player.getBoundingBox()),
                "The small standing player must physically fit inside the miniature tunnel");
        helper.assertFalse(PlayerScale.canGrow(player),
                "Standing growth must be rejected by the real mini-block ceiling");
        helper.assertTrue(PlayerScale.isMini(player), "A rejected growth check must retain the small size");
        helper.assertValueEqual(player.position(), position, "A rejected growth check must never move the player");

        player.setPose(Pose.SWIMMING);
        helper.assertTrue(PlayerScale.canGrow(player),
                "The same tunnel must permit growth into a normal crawling collision box");
        scale.removeModifier(SIZE_MODIFIER);
        player.refreshDimensions();
        helper.assertTrue(Math.abs(player.getBoundingBox().getXsize() - 0.6) < 1.0E-6,
                "After growing, the real crawling width must return to the native size");
        helper.assertTrue(Math.abs(player.getBoundingBox().getYsize() - 0.6) < 1.0E-6,
                "After growing, the real crawling height must return to the native size");
        helper.assertTrue(helper.getLevel().noCollision(player, player.getBoundingBox()),
                "The grown crawling player must physically fit in the same unchanged tunnel");
        helper.assertValueEqual(player.position(), position, "Growing in crawl pose must not push the player");

        scale.addOrReplacePermanentModifier(new AttributeModifier(SIZE_MODIFIER, -0.875,
                AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
        player.setPose(Pose.STANDING);
        Vec3 outside = helper.absoluteVec(new Vec3(2.5, 1.01, 1.5));
        player.setPos(outside.x, outside.y, outside.z);
        helper.assertTrue(PlayerScale.canGrow(player), "Standing growth must be permitted outside the tunnel");
        helper.succeed();
    }
}
