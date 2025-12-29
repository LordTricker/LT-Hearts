package pl.lordtricker.lth.client.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.PlayerEntityRenderer;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.lth.client.render.ClientDamageTracker;
import pl.lordtricker.lth.core.HeartsState;
import pl.lordtricker.lth.core.damage.DamageIndicator;
import pl.lordtricker.lth.core.damage.DamageIndicatorTracker;
import pl.lordtricker.lth.core.render.HeartSlot;
import pl.lordtricker.lth.core.render.HeartSlotType;
import pl.lordtricker.lth.core.render.HeartsDisplayLayout;
import pl.lordtricker.lth.core.render.HeartsRenderLogic;

import java.util.List;

@Mixin(PlayerEntityRenderer.class)
public abstract class PlayerEntityRendererMixin
        extends LivingEntityRenderer<AbstractClientPlayerEntity, PlayerEntityModel<AbstractClientPlayerEntity>> {

    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier HEART_EMPTY = Identifier.ofVanilla("hud/heart/container");
    private static final int HEART_SIZE = 9;
    private static final int DAMAGE_HEART_SIZE = 14;
    private static final int HEART_ROW_SPACING = 2;

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx,
                                     PlayerEntityModel<AbstractClientPlayerEntity> model,
                                     float shadowRadius) {
        super(ctx, model, shadowRadius);
    }

    @Inject(method = "render(Lnet/minecraft/client/network/AbstractClientPlayerEntity;FFLnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    private void lth_renderHearts(AbstractClientPlayerEntity player,
                                  float entityYaw,
                                  float tickDelta,
                                  MatrixStack matrices,
                                  VertexConsumerProvider vertexConsumers,
                                  int light,
                                  CallbackInfo ci) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || player.getId() == client.player.getId()) {
            return;
        }
        if (!HeartsState.getSettings().showHearts && !HeartsState.getSettings().showDamageAnimation) {
            return;
        }

        int maxDistBlocks = HeartsRenderLogic.clampDistance(HeartsState.getSettings().maxRenderDistanceBlocks);
        double distSq = this.dispatcher.getSquaredDistanceToCamera(player);
        double maxDistSq = (double) maxDistBlocks * (double) maxDistBlocks;
        if (distSq > maxDistSq) {
            return;
        }

        TextRenderer tr = this.getTextRenderer();
        int padding = 2;
        float scale = 0.02666667F * 0.65F;
        float extraYOffset = HeartsRenderLogic.computeExtraYOffset(
                scale,
                tr.fontHeight,
                padding,
                this.hasLabel(player),
                HeartsState.getSettings().extraYOffsetPixels
        );

        double baseline = player.getHeight() + 0.69 + extraYOffset;
        matrices.push();
        matrices.translate(0.0, baseline, 0.0);

        Camera camera = client.gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.scale(-scale, -scale, scale);

        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        RenderSystem.depthMask(true);

        if (HeartsState.getSettings().showHearts) {
            HeartsDisplayLayout layout = HeartsDisplayLayout.build(player.getHealth(), player.getMaxHealth());
            renderHearts(layout, matrices, vertexConsumers, tr, light);
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
                            2.0f,
                            DAMAGE_HEART_SIZE * 3.0f
                    );
                    renderDamageLine(dmgText, lineOffset, matrices, vertexConsumers, tr, light, color, alpha, indicator.isCritical());
                    index++;
                }
            }
        }

        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.enableDepthTest();
        RenderSystem.disableBlend();

        matrices.pop();
    }

    private void renderHearts(HeartsDisplayLayout layout,
                              MatrixStack matrices,
                              VertexConsumerProvider vertexConsumers,
                              TextRenderer tr,
                              int light) {
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        if (layout == null || layout.getRows().isEmpty()) {
            return;
        }
        for (int rowIndex = 0; rowIndex < layout.getRows().size(); rowIndex++) {
            List<HeartSlot> row = layout.getRows().get(rowIndex);
            if (row.isEmpty()) {
                continue;
            }
            boolean overflowRow = false;
            for (HeartSlot slot : row) {
                HeartSlotType type = slot.getType();
                if (type == HeartSlotType.ELLIPSIS || type == HeartSlotType.BONUS) {
                    overflowRow = true;
                    break;
                }
            }
            int heartSpacing = HEART_SIZE;
            int rowWidth = row.size() * HEART_SIZE;
            int bonusTextWidth = 0;
            if (overflowRow) {
                for (HeartSlot slot : row) {
                    if (slot.getType() == HeartSlotType.BONUS) {
                        String text = "+" + slot.getBonusHearts();
                        bonusTextWidth = tr.getWidth(text);
                        break;
                    }
                }
            }
            if (overflowRow) {
                int overlap = 2;
                heartSpacing = HEART_SIZE - overlap;
                rowWidth = 8 * heartSpacing + HEART_SIZE + 2 + bonusTextWidth + 1 + HEART_SIZE;
            }
            int startX = -rowWidth / 2;
            float y = -rowIndex * (HEART_SIZE + HEART_ROW_SPACING);
            for (int i = 0; i < row.size(); i++) {
                HeartSlot slot = row.get(i);
                int x = startX + i * HEART_SIZE;
                if (overflowRow) {
                    if (i < 8) {
                        x = startX + i * heartSpacing;
                    } else {
                        x = startX + 8 * heartSpacing + (i - 8) * HEART_SIZE;
                    }
                }
                HeartSlotType type = slot.getType();
                if (type == HeartSlotType.BONUS) {
                    String text = "+" + slot.getBonusHearts();
                    int bonusColor = 0xFFFF1313;
                    int textX = x + 2;
                    if (overflowRow) {
                        textX = startX + 8 * heartSpacing + HEART_SIZE - 1;
                        x = textX + bonusTextWidth + 1;
                    }
                    drawInlineTextShadow(tr, matrices, vertexConsumers, text, textX, (int) y, light, bonusColor);
                    drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_EMPTY, HEART_SIZE);
                    drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_FULL, HEART_SIZE);
                    continue;
                }
                if (type == HeartSlotType.ELLIPSIS) {
                    int textWidth = tr.getWidth("...");
                    int textX = x + (HEART_SIZE - textWidth) / 2;
                    if (overflowRow) {
                        x = startX + 8 * heartSpacing;
                        textX = x + (HEART_SIZE - textWidth) / 2;
                    }
                    drawInlineText(tr, matrices, vertexConsumers, "...", textX, (int) y, light, 0xFF000000);
                    continue;
                }
                drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_EMPTY, HEART_SIZE);
                if (type == HeartSlotType.FULL) {
                    drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_FULL, HEART_SIZE);
                } else if (type == HeartSlotType.HALF) {
                    drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_HALF, HEART_SIZE);
                }
            }
        }
    }

    private void renderDamageLine(String text,
                                  float lineOffset,
                                  MatrixStack matrices,
                                  VertexConsumerProvider vertexConsumers,
                                  TextRenderer tr,
                                  int light,
                                  int color,
                                  int alpha,
                                  boolean critical) {
        RenderSystem.depthFunc(GL11.GL_ALWAYS);
        int textWidth = tr.getWidth(text);
        int critWidth = critical ? tr.getWidth("CRIT") + 3 : 0;
        int totalWidth = textWidth + 1 + DAMAGE_HEART_SIZE + critWidth;
        float startX = -totalWidth / 2f;
        OrderedText ordered = Text.literal(text).asOrderedText();
        tr.draw(ordered, startX, lineOffset, color,
                false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextLayerType.NORMAL, 0, light);
        int heartX = (int) (startX + textWidth + 1);
        drawHeartSprite(matrices, vertexConsumers, heartX, (int) lineOffset - 2, HEART_EMPTY, DAMAGE_HEART_SIZE);
        drawHeartSprite(matrices, vertexConsumers, heartX, (int) lineOffset - 2, HEART_FULL, DAMAGE_HEART_SIZE);
        if (critical) {
            int critX = heartX + DAMAGE_HEART_SIZE + 3;
            int critColor = (alpha << 24) | 0xFFFF9900;
            drawInlineText(tr, matrices, vertexConsumers, "CRIT", critX, (int) lineOffset, light, critColor);
        }
    }

    private void drawHeartSprite(MatrixStack matrices,
                                 VertexConsumerProvider vertexConsumers,        
                                 int x,
                                 int y,
                                 Identifier sprite,
                                 int size) {
        MinecraftClient client = MinecraftClient.getInstance();
        VertexConsumerProvider.Immediate immediate = null;
        boolean flushImmediate = false;
        if (vertexConsumers instanceof VertexConsumerProvider.Immediate provided) {
            immediate = provided;
        } else if (client != null) {
            immediate = client.getBufferBuilders().getEntityVertexConsumers();
            flushImmediate = true;
        }
        if (immediate == null) {
            return;
        }
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        net.minecraft.client.gui.DrawContext context = new net.minecraft.client.gui.DrawContext(client, immediate);
        context.getMatrices().push();
        context.getMatrices().multiplyPositionMatrix(matrices.peek().getPositionMatrix());
        context.drawGuiTexture(sprite, x, y, size, size);
        context.getMatrices().pop();
        if (flushImmediate) {
            immediate.draw();
        }
    }

    private void drawInlineText(TextRenderer tr,
                                MatrixStack matrices,
                                VertexConsumerProvider vertexConsumers,
                                String text,
                                int x,
                                int y,
                                int light,
                                int color) {
        OrderedText ordered = Text.literal(text).asOrderedText();
        tr.draw(ordered, x, y + 1, color, false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextLayerType.NORMAL, 0, light);
    }

    private void drawInlineTextBold(TextRenderer tr,
                                    MatrixStack matrices,
                                    VertexConsumerProvider vertexConsumers,     
                                    String text,
                                    int x,
                                    int y,
                                    int light,
                                    int color) {
        OrderedText ordered = Text.literal(text).asOrderedText();
        tr.draw(ordered, x, y + 1, color, false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextLayerType.NORMAL, 0, light);
        tr.draw(ordered, x + 1, y + 1, color, false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextLayerType.NORMAL, 0, light);
    }

    private void drawInlineTextShadow(TextRenderer tr,
                                      MatrixStack matrices,
                                      VertexConsumerProvider vertexConsumers,
                                      String text,
                                      int x,
                                      int y,
                                      int light,
                                      int color) {
        int shadowColor = 0xFF000000;
        OrderedText ordered = Text.literal(text).asOrderedText();
        tr.draw(ordered, x, y + 2, shadowColor, false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextLayerType.NORMAL, 0, light);
        tr.draw(ordered, x, y + 1, color, false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextLayerType.NORMAL, 0, light);
    }

}
