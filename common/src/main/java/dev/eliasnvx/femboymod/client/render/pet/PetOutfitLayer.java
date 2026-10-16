package dev.eliasnvx.femboymod.client.render.pet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.client.render.BuiltinCosmeticRenderers;
import dev.eliasnvx.femboymod.client.render.ColorwayClock;
import dev.eliasnvx.femboymod.client.render.model.Groups;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.EnumMap;
import java.util.Map;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a pet's outfit ({@link PetOutfitRenderState}) on its head: a bow or a hat on top, a collar or bandana around
 * the neck. Added to the cat, wolf and parrot renderers (and the Stray Cat's). Kittens and puppies have their own
 * models in 26.x, so they get their own anchors ({@link PetSpecies#baby()}).
 */
public final class PetOutfitLayer<S extends LivingEntityRenderState, M extends EntityModel<? super S>> extends RenderLayer<S, M> {

    private static final RenderType TYPE = RenderTypes.entityCutout(
            Identifier.fromNamespaceAndPath(FemboyMod.MOD_ID, "textures/entity/cosmetic/fabric.png"));
    private static final float PIXEL = 1.0F / 16.0F;
    private static final Groups[] GROUPS = {Groups.MAIN, Groups.ACCENT, Groups.DETAIL, Groups.DARK, Groups.METAL};
    private static final int BOW_PINK = 0xFF8FB8;
    private static final int WITCH_PURPLE = 0x5B3A8C;
    private static final int COLLAR_PINK = 0xF291BE;
    private static final int BANDANA_BLUE = 0x7EC8F0;
    private static final int DETAIL_WHITE = 0xF4F1EE;
    private static final int DARK = 0x2A2328;
    private static final int GOLD = 0xF2C94C;
    private static final float LIGHTER = 0.45F;

    private final Map<Groups, ModelPart> bow;
    private final Map<Groups, ModelPart> hat;
    private final Fit adult;
    private final @Nullable Fit baby;

    /** One model's anchors and its neck pieces (sized to the head). */
    private record Fit(PetSpecies species, Map<Groups, ModelPart> collar, Map<Groups, ModelPart> bandana) {

        static Fit of(EntityModelSet models, PetSpecies species) {
            return new Fit(species, bake(models, PetOutfitModels.collar(species)), bake(models, PetOutfitModels.bandana(species)));
        }
    }

    public PetOutfitLayer(RenderLayerParent<S, M> parent, EntityModelSet models, PetSpecies species) {
        super(parent);
        this.bow = bake(models, PetOutfitModels.BOW);
        this.hat = bake(models, PetOutfitModels.WITCH_HAT);
        this.adult = Fit.of(models, species);
        PetSpecies babySpecies = species.baby();
        this.baby = babySpecies == null ? null : Fit.of(models, babySpecies);
    }

    /** One baked copy per color group with only that group visible (visibility is read when drawn, later). */
    private static Map<Groups, ModelPart> bake(EntityModelSet models, ModelLayerLocation layer) {
        Map<Groups, ModelPart> parts = new EnumMap<>(Groups.class);
        for (Groups group : GROUPS) {
            ModelPart root = models.bakeLayer(layer);
            Groups.show(root, group, Groups.ALL_BANDS);
            parts.put(group, root);
        }
        return parts;
    }

    @Override
    public void submit(PoseStack pose, SubmitNodeCollector collector, int light, S state, float yRot, float xRot) {
        PetOutfitRenderState outfit = (PetOutfitRenderState) state;
        ItemStack head = outfit.femboymod$petHead();
        ItemStack neck = outfit.femboymod$petNeck();
        Fit fit = state.isBaby ? baby : adult;
        if (head.isEmpty() && neck.isEmpty() || state.isInvisible || fit == null) {
            return;
        }
        PetSpecies species = fit.species();
        ModelPart part = getParentModel().root();
        pose.pushPose();
        part.translateAndRotate(pose);
        for (String name : species.headPath()) {
            if (!part.hasChild(name)) {
                pose.popPose();
                return;
            }
            part = part.getChild(name);
            part.translateAndRotate(pose);
        }
        int overlay = LivingEntityRenderer.getOverlayCoords(state, 0.0F);
        if (!head.isEmpty()) {
            pose.pushPose();
            boolean bowPiece = head.is(FemboyItems.PET_BOW.get());
            if (bowPiece) {
                pose.translate(species.bowX() * PIXEL, species.bowY() * PIXEL, species.bowZ() * PIXEL);
                pose.rotate(Axis.ZP, species.bowTilt());
                float scale = species.hatScale() * PetSpecies.BOW_SCALE;
                pose.scale(scale, scale, scale);
            } else {
                pose.translate(species.hatX() * PIXEL, species.hatY() * PIXEL, species.hatZ() * PIXEL);
                pose.scale(species.hatScale(), species.hatScale(), species.hatScale());
            }
            submit(bowPiece ? bow : hat, head, bowPiece ? BOW_PINK : WITCH_PURPLE, pose, collector, light, overlay);
            pose.popPose();
        }
        if (!neck.isEmpty()) {
            pose.pushPose();
            pose.translate(species.ringX() * PIXEL, species.ringY() * PIXEL, species.ringZ() * PIXEL);
            boolean bandanaPiece = neck.is(FemboyItems.PET_BANDANA.get());
            submit(bandanaPiece ? fit.bandana() : fit.collar(), neck, bandanaPiece ? BANDANA_BLUE : COLLAR_PINK, pose, collector, light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }

    private static void submit(Map<Groups, ModelPart> model, ItemStack stack, int defaultColor, PoseStack pose,
                               SubmitNodeCollector collector, int light, int overlay) {
        Colorway colorway = Colorways.effective(stack).orElse(null);
        int main = colorway == null ? defaultColor : colorway.stripeColor(0, ColorwayClock.ticks());
        for (Groups group : GROUPS) {
            int rgb = switch (group) {
                case MAIN -> main;
                case ACCENT -> BuiltinCosmeticRenderers.shade(main, LIGHTER);
                case DETAIL -> DETAIL_WHITE;
                case DARK -> DARK;
                default -> GOLD;
            };
            collector.submitModelPart(model.get(group), pose, TYPE, light, overlay, null, ARGB.opaque(rgb));
        }
    }
}
