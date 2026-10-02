package de.niklas.miniblock.player;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.listener.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = "miniblock", value = Dist.CLIENT)
public final class ClientCrawling {
    private static final KeyMapping CRAWL = new KeyMapping("key.miniblock.crawl", InputConstants.KEY_C,
            KeyMapping.Category.MOVEMENT);
    private static LocalPlayer currentPlayer;
    private static boolean enabled;
    private static boolean ownsForcedPose;

    private ClientCrawling() {}

    @SubscribeEvent
    public static void registerKeys(RegisterKeyMappingsEvent event) {
        event.register(CRAWL);
        PlayerNetwork.setClientReceiver(ClientCrawling::receiveState);
    }

    private static void receiveState(boolean crawling) {
        updatePlayer(Minecraft.getInstance().player);
        enabled = crawling;
    }

    private static void updatePlayer(LocalPlayer player) {
        if (player != currentPlayer) {
            if (currentPlayer != null) {
                PlayerScale.applyCrawlPose(currentPlayer, false, ownsForcedPose);
            }
            currentPlayer = player;
            enabled = false;
            ownsForcedPose = false;
        }
    }

    @SubscribeEvent
    public static void beforeClientTick(TickEvent.ClientTickEvent.Pre event) {
        Minecraft minecraft = Minecraft.getInstance();
        LocalPlayer player = minecraft.player;
        updatePlayer(player);
        if (player == null || minecraft.getConnection() == null) {
            enabled = false;
            if (player != null) {
                ownsForcedPose = PlayerScale.applyCrawlPose(player, false, ownsForcedPose);
            }
            while (CRAWL.consumeClick()) {}
            return;
        }
        while (CRAWL.consumeClick()) {
            if (minecraft.gui.screen() == null) {
                PlayerNetwork.requestCrawlToggle();
            }
        }
        ownsForcedPose = PlayerScale.applyCrawlPose(player, enabled && player.onGround()
                && PlayerScale.canCrawl(player) && PlayerScale.hasCrawlClearance(player), ownsForcedPose);
    }
}
