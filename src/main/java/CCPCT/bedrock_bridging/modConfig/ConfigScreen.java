package CCPCT.bedrock_bridging.modConfig;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;


public class ConfigScreen extends Screen {

    protected ConfigScreen() {
        super(Component.translatable("easiercrafting.config.title"));
    }

    public static Screen getConfigScreen(Screen parent) {
        ModConfig.load();
        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Component.translatable("easiercrafting.config.title"))
                .setSavingRunnable(ModConfig::save);

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory generalTab = builder.getOrCreateCategory(Component.literal("general"));


        // General settings
        generalTab.addEntry(entryBuilder.startBooleanToggle(Component.literal("Enable Mod"), ModConfig.get().modEnabled)
                .setDefaultValue(true)
                .setSaveConsumer(newValue -> ModConfig.get().modEnabled = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startFloatField(Component.literal("Block reach"), ModConfig.get().reach)
                .setTooltip(Component.literal("adjust distance u can reach a block\n-1 or 4.5= no change"))
                .setDefaultValue(-1f)
                .setSaveConsumer(newValue -> ModConfig.get().reach = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startIntField(Component.literal("Placement interval"), ModConfig.get().placementInterval)
                .setTooltip(Component.literal("delay between place blocks in ticks: 6 for bedrock, 4 for java"))
                .setDefaultValue(4)
                .setSaveConsumer(newValue -> ModConfig.get().placementInterval = newValue)
                .build());

        generalTab.addEntry(entryBuilder.startBooleanToggle(Component.literal("Debug"), ModConfig.get().debug)
                .setTooltip(Component.literal("ru a dev?"))
                .setDefaultValue(false)
                .setSaveConsumer(newValue -> ModConfig.get().debug = newValue)
                .build());

        return builder.build();
    }
}
