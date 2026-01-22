package pl.lordtricker.lth.client.mixin;

import com.mojang.blaze3d.opengl.GlStateManager;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.font.TextRenderer.TextLayerType;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.render.Camera;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.client.render.entity.EntityRendererFactory;
import net.minecraft.client.render.entity.EntityRenderer;
import net.minecraft.client.render.entity.LivingEntityRenderer;
import net.minecraft.client.render.entity.state.LivingEntityRenderState;
import net.minecraft.client.render.entity.state.PlayerEntityRenderState;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.LivingEntity;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.text.OrderedText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import pl.lordtricker.lth.client.render.ClientCombatTracker;
import pl.lordtricker.lth.client.render.ClientHeartsVisibility;
import pl.lordtricker.lth.client.render.ClientDamageTracker;
import pl.lordtricker.lth.core.HeartsState;
import pl.lordtricker.lth.core.config.HeartsConfigLimits;
import pl.lordtricker.lth.core.damage.DamageIndicator;
import pl.lordtricker.lth.core.damage.DamageIndicatorTracker;
import pl.lordtricker.lth.core.render.HeartSlot;
import pl.lordtricker.lth.core.render.HeartSlotType;
import pl.lordtricker.lth.core.render.HeartsDisplayLayout;
import pl.lordtricker.lth.core.render.HeartsRenderLogic;

import java.util.List;

@Mixin(LivingEntityRenderer.class)
public abstract class PlayerEntityRendererMixin
        extends EntityRenderer<AbstractClientPlayerEntity, PlayerEntityRenderState> {

    private static final Identifier HEART_FULL = Identifier.ofVanilla("hud/heart/full");
    private static final Identifier HEART_HALF = Identifier.ofVanilla("hud/heart/half");
    private static final Identifier HEART_EMPTY = Identifier.ofVanilla("hud/heart/container");
    private static final int HEART_SIZE = 9;
    private static final int DAMAGE_HEART_SIZE = 14;
    private static final int HEART_ROW_SPACING = 2;

    public PlayerEntityRendererMixin(EntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Shadow
    protected abstract boolean hasLabel(LivingEntity entity, double squaredDistance);

    @Inject(method = "render(Lnet/minecraft/client/render/entity/state/LivingEntityRenderState;Lnet/minecraft/client/util/math/MatrixStack;Lnet/minecraft/client/render/VertexConsumerProvider;I)V", at = @At("TAIL"))
    private void lth_renderHearts(LivingEntityRenderState state,
                                  MatrixStack matrices,
                                  VertexConsumerProvider vertexConsumers,
                                  int light,
                                  CallbackInfo ci) {
        if (!(state instanceof PlayerEntityRenderState playerState)) {
            return;
        }
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null || client.world == null) {
            return;
        }
        if (!(client.world.getEntityById(playerState.id) instanceof AbstractClientPlayerEntity player)) {
            return;
        }
        if (player.getId() == client.player.getId()) {
            return;
        }
        if (!HeartsState.getSettings().showHearts && !HeartsState.getSettings().showDamageAnimation) {
            return;
        }

        int maxDistBlocks = HeartsRenderLogic.clampDistance(HeartsState.getSettings().maxRenderDistanceBlocks);
        double distSq = this.dispatcher.getSquaredDistanceToCamera(player);
        double maxDistSq = (double) maxDistBlocks * (double) maxDistBlocks;
        long worldTime = client.world.getTime();
        int combatSeconds = HeartsConfigLimits.clampCombatMemorySeconds(
                HeartsState.getSettings().combatMemorySeconds
        );
        int combatTicks = combatSeconds * 20;
        boolean inCombat = ClientCombatTracker.isActive(player.getId(), worldTime, combatTicks);
        boolean canRenderHearts = ClientHeartsVisibility.shouldRender(player.getId(), distSq, maxDistSq);
        boolean renderHearts = HeartsState.getSettings().showHearts && (inCombat || canRenderHearts);
        List<DamageIndicator> indicators = List.of();
        if (HeartsState.getSettings().showDamageAnimation) {
            indicators = ClientDamageTracker.getIndicators(player.getId());
        }
        boolean renderDamage = HeartsState.getSettings().showDamageAnimation
                && !indicators.isEmpty()
                && (inCombat || distSq <= maxDistSq);
        if (!renderHearts && !renderDamage) {
            return;
        }

        TextRenderer tr = this.getTextRenderer();
        int padding = 2;
        float scale = 0.02666667F * 0.65F;
        float extraYOffset = HeartsRenderLogic.computeExtraYOffset(
                scale,
                tr.fontHeight,
                padding,
                this.hasLabel(player, distSq),
                HeartsState.getSettings().extraYOffsetPixels
        );

        double baseline = player.getHeight() + 0.69 + extraYOffset;
        matrices.push();
        matrices.translate(0.0, baseline, 0.0);

        Camera camera = client.gameRenderer.getCamera();
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-camera.getYaw()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(camera.getPitch()));
        matrices.scale(-scale, -scale, scale);

        GlStateManager._enableBlend();
        GlStateManager._blendFuncSeparate(
                GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE,
                GL11.GL_ONE_MINUS_SRC_ALPHA
        );
        GlStateManager._enableDepthTest();
        GlStateManager._depthFunc(GL11.GL_ALWAYS);
        GlStateManager._depthMask(true);

        int fullBright = LightmapTextureManager.MAX_LIGHT_COORDINATE;
        if (renderHearts) {
            HeartsDisplayLayout layout = HeartsDisplayLayout.build(player.getHealth(), player.getMaxHealth());
            renderHearts(layout, matrices, vertexConsumers, tr, fullBright);
        }

        if (renderDamage) {
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
                    renderDamageLine(dmgText, lineOffset, matrices, vertexConsumers, tr, fullBright, color, alpha, indicator.isCritical());
                    index++;
                }
            }
        }

        GlStateManager._depthFunc(GL11.GL_LEQUAL);
        GlStateManager._depthMask(true);
        GlStateManager._enableDepthTest();
        GlStateManager._disableBlend();

        matrices.pop();
    }

    private void renderHearts(HeartsDisplayLayout layout,
                              MatrixStack matrices,
                              VertexConsumerProvider vertexConsumers,
                              TextRenderer tr,
                              int light) {
        GlStateManager._depthFunc(GL11.GL_ALWAYS);
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
                    drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_EMPTY, HEART_SIZE, light);
                    drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_FULL, HEART_SIZE, light);
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
                drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_EMPTY, HEART_SIZE, light);
                if (type == HeartSlotType.FULL) {
                    drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_FULL, HEART_SIZE, light);
                } else if (type == HeartSlotType.HALF) {
                    drawHeartSprite(matrices, vertexConsumers, x, (int) y, HEART_HALF, HEART_SIZE, light);
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
        GlStateManager._depthFunc(GL11.GL_ALWAYS);
        int textWidth = tr.getWidth(text);
        int critWidth = critical ? tr.getWidth("CRIT") + 3 : 0;
        int totalWidth = textWidth + 1 + DAMAGE_HEART_SIZE + critWidth;
        float startX = -totalWidth / 2f;
        OrderedText ordered = Text.literal(text).asOrderedText();
        tr.draw(ordered, startX, lineOffset, color,
                false, matrices.peek().getPositionMatrix(),
                vertexConsumers, TextLayerType.NORMAL, 0, light);
        int heartX = (int) (startX + textWidth + 1);
        drawHeartSprite(matrices, vertexConsumers, heartX, (int) lineOffset - 2, HEART_EMPTY, DAMAGE_HEART_SIZE, light);
        drawHeartSprite(matrices, vertexConsumers, heartX, (int) lineOffset - 2, HEART_FULL, DAMAGE_HEART_SIZE, light);
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
                                 int size,
                                 int light) {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client == null) {
            return;
        }
        Sprite guiSprite = client.getGuiAtlasManager().getSprite(sprite);
        RenderLayer layer = RenderLayer.getEntityTranslucent(guiSprite.getAtlasId());
        VertexConsumer consumer = guiSprite.getTextureSpecificVertexConsumer(vertexConsumers.getBuffer(layer));

        float x0 = x;
        float x1 = x + size;
        float y0 = y;
        float y1 = y + size;
        float minU = guiSprite.getMinU();
        float maxU = guiSprite.getMaxU();
        float minV = guiSprite.getMinV();
        float maxV = guiSprite.getMaxV();
        MatrixStack.Entry entry = matrices.peek();

        consumer.vertex(entry, x0, y1, 0.0f)
                .color(255, 255, 255, 255)
                .texture(minU, maxV)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0f, 0.0f, 1.0f);
        consumer.vertex(entry, x1, y1, 0.0f)
                .color(255, 255, 255, 255)
                .texture(maxU, maxV)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0f, 0.0f, 1.0f);
        consumer.vertex(entry, x1, y0, 0.0f)
                .color(255, 255, 255, 255)
                .texture(maxU, minV)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0f, 0.0f, 1.0f);
        consumer.vertex(entry, x0, y0, 0.0f)
                .color(255, 255, 255, 255)
                .texture(minU, minV)
                .overlay(OverlayTexture.DEFAULT_UV)
                .light(light)
                .normal(entry, 0.0f, 0.0f, 1.0f);
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
