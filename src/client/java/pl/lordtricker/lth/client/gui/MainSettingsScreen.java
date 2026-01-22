package pl.lordtricker.lth.client.gui;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.SliderWidget;
import net.minecraft.text.Text;
import pl.lordtricker.lth.client.config.ConfigLoader;
import pl.lordtricker.lth.core.HeartsState;
import pl.lordtricker.lth.core.config.HeartsSettings;
import pl.lordtricker.lth.core.config.HeartsConfigLimits;

public class MainSettingsScreen extends Screen {
    private SliderWidget offsetSlider;
    private SliderWidget distanceSlider;
    private SliderWidget combatMemorySlider;
    private ButtonWidget heartsToggleBtn;
    private ButtonWidget damageToggleBtn;
    private ButtonWidget saveButton;
    private int offsetPixels;
    private int distanceBlocks;
    private int combatMemorySeconds;
    private boolean showHearts;
    private boolean showDamageAnimation;

    public MainSettingsScreen() {
        super(Text.literal("LT-Hearts Settings"));
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int btnWidth = 170;
        int btnHeight = 20;
        int spacing = 5;
        int totalHeight = btnHeight * 6 + spacing * 5;
        int startY = (this.height - totalHeight) / 2;

        HeartsSettings settings = HeartsState.getSettings();
        offsetPixels = HeartsConfigLimits.clampOffset(settings.extraYOffsetPixels);
        distanceBlocks = HeartsConfigLimits.clampDistance(settings.maxRenderDistanceBlocks);
        combatMemorySeconds = HeartsConfigLimits.clampCombatMemorySeconds(settings.combatMemorySeconds);
        showHearts = settings.showHearts;
        showDamageAnimation = settings.showDamageAnimation;

        heartsToggleBtn = ButtonWidget.builder(
                Text.literal("Show hearts: " + (showHearts ? "ON" : "OFF")),
                btn -> {
                    showHearts = !showHearts;
                    btn.setMessage(Text.literal("Show hearts: " + (showHearts ? "ON" : "OFF")));
                }
        ).dimensions(centerX - btnWidth / 2, startY, btnWidth, btnHeight).build();
        addDrawableChild(heartsToggleBtn);

        damageToggleBtn = ButtonWidget.builder(
                Text.literal("Damage animation: " + (showDamageAnimation ? "ON" : "OFF")),
                btn -> {
                    showDamageAnimation = !showDamageAnimation;
                    btn.setMessage(Text.literal("Damage animation: " + (showDamageAnimation ? "ON" : "OFF")));
                }
        ).dimensions(centerX - btnWidth / 2, startY + btnHeight + spacing, btnWidth, btnHeight).build();
        addDrawableChild(damageToggleBtn);

        offsetSlider = new SliderWidget(
                centerX - btnWidth / 2, startY + 2 * (btnHeight + spacing), btnWidth, btnHeight,
                Text.literal(formatOffsetLabel(offsetPixels)), normalizeOffset(offsetPixels)
        ) {
            @Override
            protected void updateMessage() {
                int value = HeartsConfigLimits.denormalizeOffset(this.value);
                this.setMessage(Text.literal(formatOffsetLabel(value)));
            }

            @Override
            protected void applyValue() {
                offsetPixels = HeartsConfigLimits.denormalizeOffset(this.value);
            }
        };
        addDrawableChild(offsetSlider);

        distanceSlider = new SliderWidget(
                centerX - btnWidth / 2, startY + 3 * (btnHeight + spacing), btnWidth, btnHeight,
                Text.literal(formatDistanceLabel(distanceBlocks)), normalizeDistance(distanceBlocks)
        ) {
            @Override
            protected void updateMessage() {
                int value = HeartsConfigLimits.denormalizeDistance(this.value);
                this.setMessage(Text.literal(formatDistanceLabel(value)));
            }

            @Override
            protected void applyValue() {
                distanceBlocks = HeartsConfigLimits.denormalizeDistance(this.value);
            }
        };
        addDrawableChild(distanceSlider);

        combatMemorySlider = new SliderWidget(
                centerX - btnWidth / 2, startY + 4 * (btnHeight + spacing), btnWidth, btnHeight,
                Text.literal(formatCombatMemoryLabel(combatMemorySeconds)),
                normalizeCombatMemory(combatMemorySeconds)
        ) {
            @Override
            protected void updateMessage() {
                int value = HeartsConfigLimits.denormalizeCombatMemorySeconds(this.value);
                this.setMessage(Text.literal(formatCombatMemoryLabel(value)));
            }

            @Override
            protected void applyValue() {
                combatMemorySeconds = HeartsConfigLimits.denormalizeCombatMemorySeconds(this.value);
            }
        };
        addDrawableChild(combatMemorySlider);

        saveButton = ButtonWidget.builder(
                Text.literal("Save and Close"),
                btn -> {
                    HeartsSettings current = HeartsState.getSettings();
                    current.showHearts = showHearts;
                    current.showDamageAnimation = showDamageAnimation;
                    current.extraYOffsetPixels = offsetPixels;
                    current.maxRenderDistanceBlocks = distanceBlocks;
                    current.combatMemorySeconds = combatMemorySeconds;
                    ConfigLoader.saveConfig(HeartsState.getConfig());
                    if (this.client != null) {
                        this.client.setScreen(null);
                    }
                }
        ).dimensions(centerX - btnWidth / 2, startY + 5 * (btnHeight + spacing), btnWidth, btnHeight).build();
        addDrawableChild(saveButton);
    }

    @Override
    public void removed() {
        HeartsSettings current = HeartsState.getSettings();
        current.showHearts = showHearts;
        current.showDamageAnimation = showDamageAnimation;
        current.extraYOffsetPixels = offsetPixels;
        current.maxRenderDistanceBlocks = distanceBlocks;
        current.combatMemorySeconds = combatMemorySeconds;
        ConfigLoader.saveConfig(HeartsState.getConfig());
        super.removed();
    }

    private void drawCenteredText(DrawContext context, TextRenderer textRenderer, Text text, int x, int y, int color) {
        int textWidth = textRenderer.getWidth(text);
        context.drawText(textRenderer, text, x - textWidth / 2, y, color, false);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        try {
            this.applyBlur(context);
        } catch (IllegalStateException ignored) {
        }
        this.renderInGameBackground(context);
        super.render(context, mouseX, mouseY, delta);
        drawCenteredText(context, this.textRenderer, this.title, this.width / 2, 15, 0xFFFFFF);
    }

    private static String formatOffsetLabel(int value) {
        return "Offset: " + value + " px";
    }

    private static String formatDistanceLabel(int value) {
        return "Max distance: " + value;
    }

    private static String formatCombatMemoryLabel(int value) {
        return "Combat memory: " + value + "s";
    }

    private static double normalizeOffset(int value) {
        return HeartsConfigLimits.normalizeOffset(value);
    }

    private static double normalizeDistance(int value) {
        return HeartsConfigLimits.normalizeDistance(value);
    }

    private static double normalizeCombatMemory(int value) {
        return HeartsConfigLimits.normalizeCombatMemorySeconds(value);
    }
}
