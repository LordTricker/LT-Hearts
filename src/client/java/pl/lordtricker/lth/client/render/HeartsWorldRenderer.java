package pl.lordtricker.lth.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderContext;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import pl.lordtricker.lth.core.HeartsState;
import pl.lordtricker.lth.core.damage.DamageIndicator;
import pl.lordtricker.lth.core.damage.DamageIndicatorTracker;
import pl.lordtricker.lth.core.render.HeartSlot;
import pl.lordtricker.lth.core.render.HeartSlotType;
import pl.lordtricker.lth.core.render.HeartsDisplayLayout;
import pl.lordtricker.lth.core.render.HeartsRenderLogic;

import java.util.List;

public final class HeartsWorldRenderer {
    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier HEART_EMPTY = Identifier.ofVanilla("hud/heart/container");
    private static final Identifier CRIT_ICON = Identifier.ofVanilla("textures/mob_effect/strength.png");
    private static final int HEART_SIZE = 9;
    private static final int DAMAGE_HEART_SIZE = 12;
    private static final int CRIT_ICON_SIZE = 10;
    private static final int CRIT_ICON_TEXTURE_SIZE = 18;
    private static final int HEART_ROW_SPACING = 2;

    private HeartsWorldRenderer() {}

    public static void render(WorldRenderContext context) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.world == null || client.player == null) {
            return;
        }
        if (!HeartsState.getSettings().showHearts && !HeartsState.getSettings().showDamageAnimation) {
            return;
        }

        VertexConsumerProvider consumers = context.consumers();
        if (!(consumers instanceof VertexConsumerProvider.Immediate)) {
            return;
        }

        Camera camera = context.camera();
        Vec3d cameraPos = camera.getPos();
        MatrixStack matrices = context.matrixStack();

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);

        for (AbstractClientPlayerEntity player : client.world.getPlayers()) {
            if (player.getId() == client.player.getId()) {
                continue;
            }
            int maxDistBlocks = HeartsRenderLogic.clampDistance(HeartsState.getSettings().maxRenderDistanceBlocks);
            double maxDistSq = (double) maxDistBlocks * (double) maxDistBlocks;
            if (player.squaredDistanceTo(cameraPos) > maxDistSq) {
                continue;
            }

            TextRenderer tr = client.textRenderer;
            int padding = 2;
            float scale = 0.02666667F * 0.65F;
            float extraYOffset = HeartsRenderLogic.computeExtraYOffset(
                    scale,
                    tr.fontHeight,
                    padding,
                    player.hasCustomName(),
                    HeartsState.getSettings().extraYOffsetPixels
            );

            double baseline = player.getHeight() + 0.69 + extraYOffset;
            matrices.push();
            matrices.translate(
                    player.getX() - cameraPos.x,
                    player.getY() - cameraPos.y + baseline,
                    player.getZ() - cameraPos.z
            );
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
            matrices.scale(-scale, -scale, scale);

            if (HeartsState.getSettings().showHearts) {
                HeartsDisplayLayout layout = HeartsDisplayLayout.build(player.getHealth(), player.getMaxHealth());
                renderHearts(layout, matrices, consumers, tr, 0xFFFFFFFF);
            }

            if (HeartsState.getSettings().showDamageAnimation) {
                List<DamageIndicator> indicators = ClientDamageTracker.getIndicators(player.getId());
                if (!indicators.isEmpty()) {
                    int fontHeight = tr.fontHeight;
                    int index = 0;
                    for (DamageIndicator indicator : indicators) {
                        int alpha = HeartsRenderLogic.computeFadeAlpha(
                                indicator.getAgeTicks(),
                                DamageIndicatorTracker.MAX_AGE_TICKS
                        );
                        int color = (alpha << 24) | 0xFF5555;
                        String dmgText = HeartsRenderLogic.buildDamageLabel(indicator.getAmountHearts());
                        float lineOffset = HeartsRenderLogic.computeDamageLineOffset(
                                index,
                                fontHeight,
                                padding,
                                indicator.getAgeTicks(),
                                2.0f
                        );
                        renderDamageLine(dmgText, lineOffset, matrices, consumers, tr, color, indicator.isCritical());
                        index++;
                    }
                }
            }

            matrices.pop();
        }

        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();
    }

    private static void renderHearts(HeartsDisplayLayout layout,
                                     MatrixStack matrices,
                                     VertexConsumerProvider vertexConsumers,
                                     TextRenderer tr,
                                     int color) {
        if (layout == null || layout.getRows().isEmpty()) {
            return;
        }
        for (int rowIndex = 0; rowIndex < layout.getRows().size(); rowIndex++) {
            List<HeartSlot> row = layout.getRows().get(rowIndex);
            if (row.isEmpty()) {
                continue;
            }
            int rowWidth = row.size() * HEART_SIZE;
            int startX = -rowWidth / 2;
            float y = -rowIndex * (HEART_SIZE + HEART_ROW_SPACING);
            for (int i = 0; i < row.size(); i++) {
                HeartSlot slot = row.get(i);
                int x = startX + i * HEART_SIZE;
                HeartSlotType type = slot.getType();
                drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_EMPTY, HEART_SIZE);
                if (type == HeartSlotType.FULL) {
                    drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_FULL, HEART_SIZE);
                } else if (type == HeartSlotType.HALF) {
                    drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_HALF, HEART_SIZE);
                } else if (type == HeartSlotType.ELLIPSIS) {
                    drawInlineText(tr, matrices, vertexConsumers, "...", x, (int) y, color);
                } else if (type == HeartSlotType.BONUS) {
                    String text = "+" + slot.getBonusHearts();
                    drawInlineText(tr, matrices, vertexConsumers, text, x, (int) y, color);
                    drawHeartSprite(matrices, vertexConsumers, x + tr.getWidth(text) + 1, (int) y, HEART_FULL, HEART_SIZE);
                }
            }
        }
    }

    private static void renderDamageLine(String text,
                                         float lineOffset,
                                         MatrixStack matrices,
                                         VertexConsumerProvider vertexConsumers,
                                         TextRenderer tr,
                                         int color,
                                         boolean critical) {
        int textWidth = tr.getWidth(text);
        int totalWidth = textWidth + 1 + DAMAGE_HEART_SIZE + (critical ? (1 + CRIT_ICON_SIZE) : 0);
        float startX = -totalWidth / 2f;
        OrderedText ordered = Text.literal(text).asOrderedText();
        tr.draw(ordered, startX, lineOffset, color,
                false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextLayerType.NORMAL, 0, 0xF000F0);
        int heartX = (int) (startX + textWidth + 1);
        drawHeartSprite(matrices, vertexConsumers, heartX, (int) lineOffset - 1, HEART_EMPTY, DAMAGE_HEART_SIZE);
        drawHeartSprite(matrices, vertexConsumers, heartX, (int) lineOffset - 1, HEART_FULL, DAMAGE_HEART_SIZE);
        if (critical) {
            int critX = heartX + DAMAGE_HEART_SIZE + 1;
            drawCriticalSprite(matrices, vertexConsumers, critX, (int) lineOffset - 1, CRIT_ICON_SIZE);
        }
    }

    private static void drawHeartSprite(MatrixStack matrices,
                                        VertexConsumerProvider vertexConsumers,
                                        int x,
                                        int y,
                                        Identifier sprite,
                                        int size) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(vertexConsumers instanceof VertexConsumerProvider.Immediate immediate)) {
            return;
        }
        net.minecraft.client.gui.DrawContext context = new net.minecraft.client.gui.DrawContext(client, immediate);
        context.getMatrices().push();
        context.getMatrices().multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        context.drawGuiTexture(sprite, x, y, size, size);
        context.getMatrices().pop();
    }

    private static void drawCriticalSprite(MatrixStack matrices,
                                           VertexConsumerProvider vertexConsumers,
                                           int x,
                                           int y,
                                           int size) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (!(vertexConsumers instanceof VertexConsumerProvider.Immediate immediate)) {
            return;
        }
        net.minecraft.client.gui.DrawContext context = new net.minecraft.client.gui.DrawContext(client, immediate);
        context.getMatrices().push();
        context.getMatrices().multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        context.drawTexture(CRIT_ICON, x, y, 0, 0, size, size, CRIT_ICON_TEXTURE_SIZE, CRIT_ICON_TEXTURE_SIZE);
        context.getMatrices().pop();
    }

    private static void drawInlineText(TextRenderer tr,
                                       MatrixStack matrices,
                                       VertexConsumerProvider vertexConsumers,
                                       String text,
                                       int x,
                                       int y,
                                       int color) {
        OrderedText ordered = Text.literal(text).asOrderedText();
        tr.draw(ordered, x, y + 1, color, false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextLayerType.NORMAL, 0, 0xF000F0);
    }
}
