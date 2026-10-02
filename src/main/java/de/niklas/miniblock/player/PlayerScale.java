package de.niklas.miniblock.player;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.core.Holder;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;

/** Server-owned size and crawling state. Geometry stays in ordinary world coordinates. */
public final class PlayerScale {
    public static final double MINI_SCALE = 1.0 / 16.0;
    private static final Identifier MODIFIER_ID = Identifier.fromNamespaceAndPath("miniblock", "mini_size");
    private static final List<ScaledAttribute> SCALED_ATTRIBUTES = List.of(
            new ScaledAttribute(Attributes.SCALE, MINI_SCALE),
            new ScaledAttribute(Attributes.MOVEMENT_SPEED, MINI_SCALE),
            new ScaledAttribute(Attributes.JUMP_STRENGTH, MINI_SCALE),
            new ScaledAttribute(Attributes.GRAVITY, MINI_SCALE),
            new ScaledAttribute(Attributes.STEP_HEIGHT, MINI_SCALE),
            new ScaledAttribute(Attributes.BLOCK_INTERACTION_RANGE, 0.25),
            new ScaledAttribute(Attributes.ENTITY_INTERACTION_RANGE, 0.25));
    private static final Map<UUID, CrawlState> CRAWLING = new HashMap<>();

    private PlayerScale() {}

    public static void register() {
        PlayerNetwork.register();
        TickEvent.PlayerTickEvent.Pre.BUS.addListener(PlayerScale::beforePlayerTick);
        PlayerEvent.Clone.BUS.addListener(PlayerScale::clonePlayer);
        PlayerEvent.PlayerLoggedInEvent.BUS.addListener(event -> {
            // Upgrade saved 0.1 players and apply the new gravity modifier as one consistent set.
            if (isMini(event.getEntity())) setMini(event.getEntity(), true);
            resetCrawling(event.getEntity());
        });
        PlayerEvent.PlayerRespawnEvent.BUS.addListener(event -> resetCrawling(event.getEntity()));
        PlayerEvent.PlayerChangedDimensionEvent.BUS.addListener(event -> resetCrawling(event.getEntity()));
        PlayerEvent.PlayerLoggedOutEvent.BUS.addListener(event -> CRAWLING.remove(event.getEntity().getUUID()));
    }

    public static boolean isMini(Player player) {
        AttributeInstance scale = player.getAttribute(Attributes.SCALE);
        return scale != null && scale.hasModifier(MODIFIER_ID);
    }

    /** Returns false without changing size when the larger, current-pose box would intersect anything. */
    public static boolean toggleSize(ServerPlayer player) {
        if (player.isDeadOrDying() || player.isSpectator() || player.isSleeping() || player.isPassenger()) {
            player.sendOverlayMessage(Component.translatable("message.miniblock.size.unavailable"));
            return false;
        }
        boolean grow = isMini(player);
        if (grow && !canGrow(player)) {
            player.sendOverlayMessage(Component.translatable("message.miniblock.size.no_room"));
            return false;
        }
        setMini(player, !grow);
        player.sendOverlayMessage(Component.translatable(grow
                ? "message.miniblock.size.normal" : "message.miniblock.size.small"));
        return true;
    }

    public static boolean canGrow(Player player) {
        AttributeInstance attribute = player.getAttribute(Attributes.SCALE);
        if (attribute == null || !attribute.hasModifier(MODIFIER_ID)) {
            return true;
        }
        float targetScale = (float) valueWithoutMiniModifier(attribute);
        AABB largerBox = player.getDimensions(player.getPose())
                .scale(targetScale / player.getScale()).makeBoundingBox(player.position());
        return player.level().noCollision(player, largerBox.deflate(1.0E-7));
    }

    public static void setMini(Player player, boolean mini) {
        for (ScaledAttribute entry : SCALED_ATTRIBUTES) {
            AttributeInstance attribute = player.getAttribute(entry.attribute());
            if (attribute == null) {
                continue;
            }
            if (mini) {
                // Permanent modifiers are saved by vanilla and synchronized with all clients.
                attribute.addOrReplacePermanentModifier(new AttributeModifier(MODIFIER_ID,
                        entry.multiplier() - 1.0, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            } else {
                attribute.removeModifier(MODIFIER_ID);
            }
        }
        player.refreshDimensions();
    }

    private static double valueWithoutMiniModifier(AttributeInstance attribute) {
        double base = attribute.getBaseValue();
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (!modifier.id().equals(MODIFIER_ID)
                    && modifier.operation() == AttributeModifier.Operation.ADD_VALUE) {
                base += modifier.amount();
            }
        }
        double value = base;
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (!modifier.id().equals(MODIFIER_ID)
                    && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_BASE) {
                value += base * modifier.amount();
            }
        }
        for (AttributeModifier modifier : attribute.getModifiers()) {
            if (!modifier.id().equals(MODIFIER_ID)
                    && modifier.operation() == AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL) {
                value *= 1.0 + modifier.amount();
            }
        }
        return attribute.getAttribute().value().sanitizeValue(value);
    }

    private static void clonePlayer(PlayerEvent.Clone event) {
        if (isMini(event.getOriginal())) {
            setMini(event.getEntity(), true);
        }
        CRAWLING.remove(event.getEntity().getUUID());
    }

    static void toggleCrawling(ServerPlayer player) {
        CrawlState state = CRAWLING.computeIfAbsent(player.getUUID(), ignored -> new CrawlState());
        long now = player.level().getGameTime();
        if (now - state.lastToggle < 5) {
            return;
        }
        state.lastToggle = now;
        if (!state.enabled && (!canCrawl(player) || !player.onGround() || !hasCrawlClearance(player))) {
            player.sendOverlayMessage(Component.translatable("message.miniblock.crawl.unavailable"));
            PlayerNetwork.sendCrawlState(player, false);
            return;
        }
        state.enabled = !state.enabled;
        state.ownsForcedPose = applyCrawlPose(player, state.enabled && player.onGround(), state.ownsForcedPose);
        PlayerNetwork.sendCrawlState(player, state.enabled);
        player.sendOverlayMessage(Component.translatable(state.enabled
                ? "message.miniblock.crawl.on" : "message.miniblock.crawl.off"));
    }

    /** Shared clearance/eligibility checks keep local motion prediction consistent with the server. */
    public static boolean canCrawl(Player player) {
        return !player.isDeadOrDying() && !player.isSpectator() && !player.isPassenger()
                && !player.isSleeping() && !player.isFallFlying() && !player.isInWater()
                && !player.isInLava();
    }

    public static boolean hasCrawlClearance(Player player) {
        return player.level().noCollision(player,
                player.getDimensions(Pose.SWIMMING).makeBoundingBox(player.position()).deflate(1.0E-7));
    }

    public static boolean applyCrawlPose(Player player, boolean crawling, boolean ownsForcedPose) {
        if (crawling) {
            if (player.getForcedPose() == null) {
                player.setForcedPose(Pose.SWIMMING);
                ownsForcedPose = true;
            }
            if (ownsForcedPose && player.getForcedPose() == Pose.SWIMMING) {
                player.setPose(Pose.SWIMMING);
                return true;
            }
        } else if (ownsForcedPose && player.getForcedPose() == Pose.SWIMMING) {
            // Vanilla will keep the player lying down until standing/sneaking actually fits.
            player.setForcedPose(null);
        }
        return false;
    }

    private static void beforePlayerTick(TickEvent.PlayerTickEvent.Pre event) {
        if (!(event.player() instanceof ServerPlayer player)) {
            return;
        }
        CrawlState state = CRAWLING.get(player.getUUID());
        if (state == null) {
            return;
        }
        if (state.enabled && !canCrawl(player)) {
            state.enabled = false;
            PlayerNetwork.sendCrawlState(player, false);
        }
        state.ownsForcedPose = applyCrawlPose(player,
                state.enabled && player.onGround() && hasCrawlClearance(player), state.ownsForcedPose);
    }

    private static void resetCrawling(Player player) {
        CrawlState state = CRAWLING.remove(player.getUUID());
        if (state != null) {
            applyCrawlPose(player, false, state.ownsForcedPose);
        }
        if (player instanceof ServerPlayer serverPlayer) {
            PlayerNetwork.sendCrawlState(serverPlayer, false);
        }
    }

    private record ScaledAttribute(Holder<Attribute> attribute, double multiplier) {}

    private static final class CrawlState {
        private boolean enabled;
        private boolean ownsForcedPose;
        private long lastToggle = Long.MIN_VALUE / 2;
    }
}
