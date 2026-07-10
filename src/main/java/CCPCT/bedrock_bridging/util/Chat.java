package CCPCT.bedrock_bridging.util;

import CCPCT.bedrock_bridging.modConfig.ModConfig;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;

public class Chat {
    public static void system(String message) {
        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.sendSystemMessage(Component.literal(message));
    }

    public static void debug(String message) {
        if (ModConfig.get().debug) {
            system(message);
        }
    }

    public static void overlay(String message) {
        assert Minecraft.getInstance().player != null;
        Minecraft.getInstance().player.sendOverlayMessage(Component.literal(message));
    }
}
