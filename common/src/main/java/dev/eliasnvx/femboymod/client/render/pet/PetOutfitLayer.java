package dev.eliasnvx.femboymod.client.render.pet;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.eliasnvx.femboymod.FemboyMod;
import dev.eliasnvx.femboymod.api.colorway.Colorway;
import dev.eliasnvx.femboymod.client.render.BuiltinCosmeticRenderers;
import dev.eliasnvx.femboymod.client.render.ColorwayClock;
import dev.eliasnvx.femboymod.client.render.ModelColors;
import dev.eliasnvx.femboymod.client.render.model.Groups;
import dev.eliasnvx.femboymod.cosmetic.Colorways;
import dev.eliasnvx.femboymod.mixin.client.AgeableListModelAccessor;
import dev.eliasnvx.femboymod.pet.PetOutfitItem;
import dev.eliasnvx.femboymod.pet.PetOutfits;
import dev.eliasnvx.femboymod.registry.FemboyItems;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Draws a pet's outfit ({@link PetOutfits}) on its head: a bow or a hat on top, a collar or bandana around the neck.
 * Added to the cat, wolf and parrot renderers (and the Stray Cat's). 1.20.1 has no render states: the layer reads the
 * pieces from the pet's equipment. Kittens and puppies are drawn with the adult model, whose head the model scales and
 * moves for young animals ({@code AgeableListModel#renderToBuffer}); the same transform is applied here before the
 * adult anchors, so the pieces fit the small head (only a kitten's neck piece hangs further forward,
 * {@link PetSpecies#babyRingZ}). Nothing is attached to the body.
 */
public final class PetOutfitLayer<T extends LivingEntity, M extends EntityModel<T>> extends RenderLayer<T, M> {

    private static final RenderType TYPE = RenderType.entityCutout(
            new ResourceLocation(FemboyMod.MOD_ID, "textures/entity/cosmetic/fabric.png"));
    private static final float PIXEL = 1.0F / 16.0F;
    /** {@code AgeableListModel#renderToBuffer} scales a young model's head by this over its baby head scale. */
    private static final float BABY_HEAD_SCALE_BASE = 1.5F;
    private static final Groups[] GROUPS = {Groups.MAIN, Groups.ACCENT, Groups.DETAIL, Groups.DARK, Groups.METAL};
    private static final ModelPart[] NO_HEAD = new ModelPart[0];
    private static final int BOW_PINK = 0xFF8FB8;
    private static final int WITCH_PURPLE = 0x5B3A8C;
    private static final int COLLAR_PINK = 0xF291BE;
    private static final int BANDANA_BLUE = 0x7EC8F0;
    private static final int DETAIL_WHITE = 0xF4F1EE;
    private static final int DARK = 0x2A2328;
    private static final int GOLD = 0xF2C94C;
    private static final float LIGHTER = 0.45F;

    private final PetSpecies species;
    private final Map<Groups, ModelPart> bow;
    private final Map<Groups, ModelPart> hat;
    private final Map<Groups, ModelPart> collar;
    private final Map<Groups, ModelPart> bandana;
    /** The parent model the head chain was found in; found again if another mod swaps the model. */
    private @Nullable EntityModel<?> chainModel;
    /** Parts to apply, in order, to reach the head (the root first on hierarchical models); empty if not found. */
    private ModelPart[] headChain = NO_HEAD;

    public PetOutfitLayer(RenderLayerParent<T, M> parent, EntityModelSet models, PetSpecies species) {
        super(parent);
        this.species = species;
        this.bow = bake(models, PetOutfitModels.BOW);
        this.hat = bake(models, PetOutfitModels.WITCH_HAT);
        this.collar = bake(models, PetOutfitModels.collar(species));
        this.bandana = bake(models, PetOutfitModels.bandana(species));
    }

    /** One baked copy per color group with only that group visible. */
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
    public void render(PoseStack pose, MultiBufferSource buffers, int light, T pet, float limbSwing, float limbSwingAmount,
                       float partialTick, float ageInTicks, float netHeadYaw, float headPitch) {
        ItemStack head = PetOutfits.worn(pet, PetOutfitItem.Slot.HEAD);
        ItemStack neck = PetOutfits.worn(pet, PetOutfitItem.Slot.NECK);
        if (head.isEmpty() && neck.isEmpty() || pet.isInvisible()) {
            return;
        }
        M model = getParentModel();
        ModelPart[] chain = headChain(model);
        if (chain.length == 0) {
            return;
        }
        pose.pushPose();
        boolean young = model.young && model instanceof AgeableListModel<?>;
        if (young) {
            babyHead((AgeableListModelAccessor) model, pose);
        }
        for (ModelPart part : chain) {
            part.translateAndRotate(pose);
        }
        VertexConsumer buffer = buffers.getBuffer(TYPE);
        int overlay = LivingEntityRenderer.getOverlayCoords(pet, 0.0F);
        if (!head.isEmpty()) {
            pose.pushPose();
            boolean bowPiece = head.is(FemboyItems.PET_BOW.get());
            if (bowPiece) {
                pose.translate(species.bowX() * PIXEL, species.bowY() * PIXEL, species.bowZ() * PIXEL);
                pose.mulPose(Axis.ZP.rotation(species.bowTilt()));
                float scale = species.hatScale() * PetSpecies.BOW_SCALE;
                pose.scale(scale, scale, scale);
            } else {
                pose.translate(species.hatX() * PIXEL, species.hatY() * PIXEL, species.hatZ() * PIXEL);
                pose.scale(species.hatScale(), species.hatScale(), species.hatScale());
            }
            draw(bowPiece ? bow : hat, head, bowPiece ? BOW_PINK : WITCH_PURPLE, pose, buffer, light, overlay);
            pose.popPose();
        }
        if (!neck.isEmpty()) {
            pose.pushPose();
            float ringZ = young ? species.babyRingZ() : species.ringZ();
            pose.translate(species.ringX() * PIXEL, species.ringY() * PIXEL, ringZ * PIXEL);
            boolean bandanaPiece = neck.is(FemboyItems.PET_BANDANA.get());
            draw(bandanaPiece ? bandana : collar, neck, bandanaPiece ? BANDANA_BLUE : COLLAR_PINK, pose, buffer, light, overlay);
            pose.popPose();
        }
        pose.popPose();
    }

    /** The young model's own head transform: scaled (cats) and moved, as {@code AgeableListModel} draws its head parts. */
    private static void babyHead(AgeableListModelAccessor model, PoseStack pose) {
        if (model.femboymod$scaleHead()) {
            float scale = BABY_HEAD_SCALE_BASE / model.femboymod$babyHeadScale();
            pose.scale(scale, scale, scale);
        }
        pose.translate(0.0F, model.femboymod$babyYHeadOffset() * PIXEL, model.femboymod$babyZHeadOffset() * PIXEL);
    }

    /**
     * The parts from the model down to its head, found once per model (no allocation per frame). Hierarchical models
     * (parrot) start at their root; ageable list models (cat, wolf) have no root, so the chain starts at their first
     * head part, which is the root's {@code headPath[0]} child on the vanilla models.
     */
    private ModelPart[] headChain(EntityModel<?> model) {
        if (model == chainModel) {
            return headChain;
        }
        chainModel = model;
        headChain = NO_HEAD;
        String[] path = species.headPath();
        ModelPart[] chain;
        ModelPart part;
        int index;
        if (model instanceof HierarchicalModel<?> hierarchical) {
            chain = new ModelPart[path.length + 1];
            part = hierarchical.root();
            index = 0;
        } else if (model instanceof AgeableListModel<?> ageable) {
            Iterator<ModelPart> heads = ((AgeableListModelAccessor) ageable).femboymod$headParts().iterator();
            if (!heads.hasNext()) {
                return headChain;
            }
            chain = new ModelPart[path.length];
            part = heads.next();
            index = 1;
        } else {
            return headChain; // a model another mod put in; no known head
        }
        int filled = 0;
        chain[filled++] = part;
        for (; index < path.length; index++) {
            if (!part.hasChild(path[index])) {
                return headChain;
            }
            part = part.getChild(path[index]);
            chain[filled++] = part;
        }
        headChain = chain;
        return headChain;
    }

    private static void draw(Map<Groups, ModelPart> model, ItemStack stack, int defaultColor, PoseStack pose,
                             VertexConsumer buffer, int light, int overlay) {
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
            ModelColors.render(model.get(group), pose, buffer, light, overlay, ModelColors.opaque(rgb));
        }
    }
}
