package de.niklas.miniblock.player;

import de.niklas.miniblock.MiniBlockMod;
import de.niklas.miniblock.world.MiniBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ColorParticleOption;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import io.netty.buffer.Unpooled;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTest;
import net.minecraftforge.gametest.GameTestNamespace;

/** Native player-dimension and safe-growth checks in a real server collision world. */
@GameTestNamespace(MiniBlockMod.MODID)
public final class PlayerScaleGameTests {
    private static final BlockPos TUNNEL = new BlockPos(1, 1, 1);

    @GameTest
    public static void microFootParticlesUseContactMaterialAndPartialSurface(GameTestHelper helper) {
        helper.setBlock(TUNNEL, MiniBlockMod.MINI_BLOCK.get().defaultBlockState());
        var mini = helper.getBlockEntity(TUNNEL, MiniBlockEntity.class);
        mini.setCell(4, 3, 4, Blocks.DIRT.defaultBlockState());
        mini.setCell(5, 3, 4, Blocks.CONCRETE_POWDER.blue().defaultBlockState());
        mini.setCell(4, 3, 5, Blocks.OAK_SLAB.defaultBlockState());
        var player = helper.makeMockPlayer(GameType.SURVIVAL);
        PlayerScale.setMini(player, true);
        int[][] cells = {{4, 3, 4}, {5, 3, 4}, {4, 3, 5}};
        for (int[] cell : cells) {
            double height = cell[2] == 5 ? 3.5 / 16.0 : 4.0 / 16.0;
            Vec3 feet = helper.absoluteVec(new Vec3(1 + (cell[0] + 0.5) / 16.0,
                    1 + height, 1 + (cell[2] + 0.5) / 16.0));
            player.setPos(feet.x, feet.y, feet.z);
            var surface = MiniSurfaceEffects.microSurface(player);
            helper.assertTrue(surface != null, "Micro walking dust must find a real support face inside the invisible container");
            helper.assertValueEqual(surface.state(), mini.stateAt(cell[0], cell[1], cell[2]),
                    "Walking dust must use the actual dirt/powder/slab palette material under the feet");
            helper.assertTrue(Math.abs(surface.point().y - feet.y) < 1.0E-7,
                    "Dust must originate at the actual partial-block top rather than the storage-block top");
            helper.assertTrue(surface.sourceScale() == 1.0F / 16.0F,
                    "Native dust must carry the real microcell scale regardless of observer size");
        }
        Vec3 empty = helper.absoluteVec(new Vec3(1.5, 1.25, 1.5));
        player.setPos(empty.x, empty.y, empty.z);
        helper.assertTrue(MiniSurfaceEffects.microSurface(player) == null,
                "Empty space inside the same container must not generate material dust");
        helper.succeed();
    }

    @GameTest
    public static void removedCellAndPotionBurstsKeepNativeOptionsAndSourceSize(GameTestHelper helper) {
        helper.setBlock(TUNNEL, MiniBlockMod.MINI_BLOCK.get().defaultBlockState());
        var mini = helper.getBlockEntity(TUNNEL, MiniBlockEntity.class);
        var material = Blocks.CONCRETE_POWDER.blue().defaultBlockState();
        mini.setCell(4, 3, 4, material);
        var pos = helper.absolutePos(TUNNEL);
        Vec3 origin = helper.absoluteVec(new Vec3(1 + 4.5 / 16.0, 1 + 3.5 / 16.0, 1 + 4.5 / 16.0));
        var burst = MiniSurfaceEffects.blockBurst(material, pos, origin, 1.0F / 16.0F, 8, new Vec3(0.01, 0.01, 0.01));
        mini.removeCell(4, 3, 4);
        helper.assertTrue(helper.getLevel().getBlockEntity(pos) == null,
                "The regression must remove the final source cell before receiving its particle burst");
        var decoded = roundTrip(helper, burst);
        helper.assertTrue(decoded.options() instanceof BlockParticleOption,
                "The removed-cell burst must still select the native block particle provider");
        helper.assertValueEqual(((BlockParticleOption) decoded.options()).getState(), material,
                "Removed-cell break dust must preserve its native material across the network");
        helper.assertValueEqual(decoded.origin(), origin, "The exact removed microcell origin must survive networking");
        helper.assertTrue(decoded.sourceScale() == 1.0F / 16.0F,
                "Microdust must remain small even when its source is gone and the viewer grows");

        var potionOptions = new MobEffectInstance(MobEffects.SPEED, 100).getParticleOptions();
        var potionBurst = new MiniParticleBurst(potionOptions, pos, origin, Vec3.ZERO, new Vec3(1.0, 1.0, 1.0),
                1, 1.0F / 16.0F, 0.0);
        var decodedPotion = roundTrip(helper, potionBurst);
        helper.assertValueEqual(decodedPotion.options().getType(), potionOptions.getType(),
                "A potion source must keep its native effect particle provider");
        if (potionOptions instanceof ColorParticleOption color) {
            helper.assertTrue(decodedPotion.options() instanceof ColorParticleOption, "Potion color options must survive transport");
            var restored = (ColorParticleOption) decodedPotion.options();
            helper.assertTrue(color.getRed() == restored.getRed() && color.getGreen() == restored.getGreen()
                            && color.getBlue() == restored.getBlue() && color.getAlpha() == restored.getAlpha(),
                    "Potion particle color and visibility must not be replaced by generic terrain dust");
        }
        helper.assertTrue(decodedPotion.sourceScale() == 1.0F / 16.0F,
                "Colored potion effects must transport their intrinsic source scale independently of the viewer");
        helper.succeed();
    }

    private static MiniParticleBurst roundTrip(GameTestHelper helper, MiniParticleBurst burst) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), helper.getLevel().registryAccess());
        try {
            MiniParticleBurst.STREAM_CODEC.encode(buffer, burst);
            var decoded = MiniParticleBurst.STREAM_CODEC.decode(buffer);
            helper.assertTrue(buffer.readableBytes() == 0, "The real particle payload must consume its complete wire representation");
            return decoded;
        } finally {
            buffer.release();
        }
    }

    @GameTest
    public static void miniaturePlayerCanGrowOnlyWhenCurrentPoseFits(GameTestHelper helper) {
        helper.setBlock(TUNNEL, MiniBlockMod.MINI_BLOCK.get().defaultBlockState());
        MiniBlockEntity container = helper.getBlockEntity(TUNNEL, MiniBlockEntity.class);
        container.grid().fill(container.paletteId(Blocks.STONE.defaultBlockState()));
        for (int x = 2; x < 14; x++) {
            for (int y = 0; y < 10; y++) {
                for (int z = 0; z < 16; z++) {
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
        PlayerScale.setMini(player, true);

        helper.assertTrue(PlayerScale.isMini(player), "The permanent mini modifier must mark the player as small");
        helper.assertTrue(Math.abs(player.getDimensions(Pose.STANDING).width() - 0.0375) < 1.0E-6,
                "The actual standing width must be one sixteenth of the native player width");
        helper.assertTrue(Math.abs(player.getDimensions(Pose.STANDING).height() - 0.1125) < 1.0E-6,
                "The actual standing height must be one sixteenth of the native player height");
        helper.assertTrue(Math.abs(player.getEyeHeight() - 0.10125) < 1.0E-6,
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
        PlayerScale.setMini(player, false);
        helper.assertTrue(Math.abs(player.getBoundingBox().getXsize() - 0.6) < 1.0E-6,
                "After growing, the real crawling width must return to the native size");
        helper.assertTrue(Math.abs(player.getBoundingBox().getYsize() - 0.6) < 1.0E-6,
                "After growing, the real crawling height must return to the native size");
        helper.assertTrue(helper.getLevel().noCollision(player, player.getBoundingBox()),
                "The grown crawling player must physically fit in the same unchanged tunnel");
        helper.assertValueEqual(player.position(), position, "Growing in crawl pose must not push the player");

        PlayerScale.setMini(player, true);
        player.setPose(Pose.STANDING);
        Vec3 outside = helper.absoluteVec(new Vec3(2.5, 1.01, 1.5));
        player.setPos(outside.x, outside.y, outside.z);
        helper.assertTrue(PlayerScale.canGrow(player), "Standing growth must be permitted outside the tunnel");
        helper.succeed();
    }

    @GameTest
    public static void miniatureJumpPreservesNativeArcAndSprintControl(GameTestHelper helper) {
        var normal = helper.makeMockPlayer(GameType.SURVIVAL);
        var miniature = helper.makeMockPlayer(GameType.SURVIVAL);
        // Keep the entire comparison above nearby test structures so only player physics is measured.
        Vec3 start = helper.absoluteVec(new Vec3(1.5, 20.0, 1.5));
        normal.setPos(start.x, start.y, start.z);
        miniature.setPos(start.x, start.y, start.z);
        // LivingEntity construction chooses a random yaw; both trajectories need the same heading.
        normal.setYRot(0.0F);
        miniature.setYRot(0.0F);
        normal.setDeltaMovement(Vec3.ZERO);
        miniature.setDeltaMovement(Vec3.ZERO);
        PlayerScale.setMini(miniature, true);
        helper.assertTrue(Math.abs(miniature.getAttributeValue(Attributes.GRAVITY) - 0.005) < 1.0E-10,
                "Gravity must scale with the jump impulse, preserving a smooth jump duration");

        normal.setSprinting(true);
        miniature.setSprinting(true);
        normal.jumpFromGround();
        miniature.jumpFromGround();
        helper.assertTrue(Math.abs(miniature.getDeltaMovement().z - normal.getDeltaMovement().z / 16.0) < 1.0E-8,
                "The native sprint-jump impulse must also shrink instead of launching the miniature player: normal="
                        + normal.getDeltaMovement() + ", miniature=" + miniature.getDeltaMovement());
        double peak = 0.0;
        for (int tick = 0; tick < 18; tick++) {
            normal.aiStep();
            miniature.aiStep();
            double normalHeight = normal.getY() - start.y;
            double miniatureHeight = miniature.getY() - start.y;
            peak = Math.max(peak, miniatureHeight);
            helper.assertTrue(Math.abs(miniatureHeight - normalHeight / 16.0) < 1.0E-6,
                    "Every actual ascent/descent tick must match the native arc at one-sixteenth scale: tick " + tick);
            helper.assertTrue(Math.abs(miniature.getDeltaMovement().y - normal.getDeltaMovement().y / 16.0) < 1.0E-7,
                    "The apex must keep native timing instead of snapping tiny velocities to zero: tick " + tick);
        }
        helper.assertTrue(peak > 1.0 / 16.0 && peak < 1.5 / 16.0,
                "A complete jump must clear one miniature block with a native-looking scaled arc");

        normal.setDeltaMovement(Vec3.ZERO);
        miniature.setDeltaMovement(Vec3.ZERO);
        normal.setOnGround(false);
        miniature.setOnGround(false);
        normal.travel(new Vec3(1.0, 0.0, 0.0));
        miniature.travel(new Vec3(1.0, 0.0, 0.0));
        helper.assertTrue(Math.abs(miniature.getDeltaMovement().x - normal.getDeltaMovement().x / 16.0) < 1.0E-7,
                "Airborne steering must have the same miniature scale as ground movement");

        PlayerScale.setMini(miniature, false);
        helper.assertTrue(Math.abs(miniature.getAttributeValue(Attributes.GRAVITY) - normal.getAttributeValue(Attributes.GRAVITY)) < 1.0E-10,
                "Growing must restore ordinary gravity without leaving a miniature modifier behind");
        helper.assertTrue(Math.abs(miniature.getAttributeValue(Attributes.JUMP_STRENGTH) - normal.getAttributeValue(Attributes.JUMP_STRENGTH)) < 1.0E-10,
                "Growing must restore the ordinary jump impulse");
        helper.succeed();
    }

    @GameTest
    public static void creativeFlightPreservesScaleAndNativeAbilitySettings(GameTestHelper helper) {
        var normal = helper.makeMockPlayer(GameType.CREATIVE);
        var miniature = helper.makeMockPlayer(GameType.CREATIVE);
        Vec3 start = helper.absoluteVec(new Vec3(1.5, 20.0, 1.5));
        normal.setYRot(0.0F);
        miniature.setYRot(0.0F);
        normal.getAbilities().flying = true;
        miniature.getAbilities().flying = true;
        // Exercise a custom ability speed too: resizing must preserve settings from servers and other mods.
        float abilitySpeed = 0.085F;
        normal.getAbilities().setFlyingSpeed(abilitySpeed);
        miniature.getAbilities().setFlyingSpeed(abilitySpeed);
        PlayerScale.setMini(miniature, true);

        double ordinaryDistance = 0.0;
        for (boolean sprinting : new boolean[] {false, true}) {
            normal.setPos(start.x, start.y, start.z);
            miniature.setPos(start.x, start.y, start.z);
            normal.setDeltaMovement(Vec3.ZERO);
            miniature.setDeltaMovement(Vec3.ZERO);
            normal.setOnGround(false);
            miniature.setOnGround(false);
            normal.setSprinting(sprinting);
            miniature.setSprinting(sprinting);
            for (int tick = 0; tick < 12; tick++) {
                normal.travel(new Vec3(1.0, 0.0, 0.0));
                miniature.travel(new Vec3(1.0, 0.0, 0.0));
                helper.assertTrue(Math.abs(miniature.getDeltaMovement().x
                                - normal.getDeltaMovement().x / 16.0) < 1.0E-7,
                        "Actual Creative flight velocity must shrink once, including sprint flight: tick " + tick
                                + ", sprinting=" + sprinting);
                helper.assertTrue(Math.abs((miniature.getX() - start.x)
                                - (normal.getX() - start.x) / 16.0) < 1.0E-6,
                        "Every actual Creative flight position must advance at one-sixteenth speed: tick " + tick
                                + ", sprinting=" + sprinting);
            }
            double distance = normal.getX() - start.x;
            if (!sprinting) {
                ordinaryDistance = distance;
            } else {
                helper.assertTrue(Math.abs(distance - ordinaryDistance * 2.0) < 1.0E-6,
                        "Vanilla's double-speed sprint flight must survive miniature scaling");
            }
            helper.assertTrue(miniature.getAbilities().getFlyingSpeed() == abilitySpeed,
                    "Resizing and moving must not mutate the saved or networked vanilla ability speed");
        }

        PlayerScale.setMini(miniature, false);
        normal.setDeltaMovement(Vec3.ZERO);
        miniature.setDeltaMovement(Vec3.ZERO);
        normal.setOnGround(false);
        miniature.setOnGround(false);
        normal.travel(new Vec3(1.0, 0.0, 0.0));
        miniature.travel(new Vec3(1.0, 0.0, 0.0));
        helper.assertTrue(Math.abs(miniature.getDeltaMovement().x - normal.getDeltaMovement().x) < 1.0E-7,
                "Growing must immediately restore native Creative flight speed");
        helper.assertTrue(miniature.getAbilities().getFlyingSpeed() == abilitySpeed,
                "Growing must retain the original custom ability speed");
        helper.succeed();
    }
}
