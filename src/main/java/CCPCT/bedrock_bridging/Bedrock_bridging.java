package CCPCT.bedrock_bridging;

import CCPCT.bedrock_bridging.modConfig.ModConfig;
import CCPCT.bedrock_bridging.util.Chat;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;


public class Bedrock_bridging implements ClientModInitializer {
    public static final String MOD_ID = "bedrock_bridging";

    public static KeyMapping enableModKey;
    public static KeyMapping reverseKey;

    public static boolean magicSelect = false;
    public static Vec3i magicDirection;
    public static boolean prepareMagic = false;
    public static double magicY = 0;
    public static BlockPos lastPlacePos;
    public static Vec3 lastPlayerPos;
    public static int lastReverse = 0;

    public static boolean disablePosPacket = false;
    public static boolean recoverPosPacket = false;


    @Override
    public void onInitializeClient() {
        KeyMapping.Category keybindCat = KeyMapping.Category.register(Identifier.fromNamespaceAndPath(MOD_ID, "keys"));
        ModConfig.load();
        // Register the KeyMapping
        enableModKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key."+ MOD_ID +".enableMod",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_B,
                keybindCat
        ));

        reverseKey = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key."+ MOD_ID +".reverse",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_J,
                keybindCat
        ));

        ClientTickEvents.START_CLIENT_TICK.register(client -> {
            var player = client.player;
            if (player == null) return;

            if (enableModKey.consumeClick()) {
                ModConfig.get().modEnabled ^= true;
                Chat.overlay("BE placement " + (ModConfig.get().modEnabled ? "§aON" : "§cOFF"));
                ModConfig.save();
            }

            if (client.options.keyUse.isDown()) {
                // placing blocks
                disablePosPacket = reverseKey.isDown();


            } else {
                lastPlacePos = null;
                lastPlayerPos = null;
                prepareMagic = false;
                magicDirection = null;
                lastReverse = 0;
                disablePosPacket = false;

            }

            if (!reverseKey.isDown()) {
                disablePosPacket = false;
            }

        });
    }
}
