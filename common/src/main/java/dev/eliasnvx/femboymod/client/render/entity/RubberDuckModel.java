package dev.eliasnvx.femboymod.client.render.entity;

import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.util.Mth;

/** Rubber Duck: round body with a perky tail, head with a flat beak, stubby wings and feet; waddles. Texture 32x32. */
public final class RubberDuckModel extends EntityModel<LivingEntityRenderState> {

    private static final int TEXTURE_SIZE = 32;
    private static final float WADDLE = 0.14F;
    private static final float WADDLE_SPEED = 0.9F;
    private static final float FOOT_SWING = 0.8F;
    private static final float TAIL_TILT = -0.6F;

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart rightFoot;
    private final ModelPart leftFoot;

    public RubberDuckModel(ModelPart root) {
        super(root);
        this.body = root.getChild("body");
        this.head = root.getChild("head");
        this.rightFoot = root.getChild("right_foot");
        this.leftFoot = root.getChild("left_foot");
    }

    public static LayerDefinition createLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        PartDefinition body = root.addOrReplaceChild("body", CubeListBuilder.create().texOffs(0, 0).addBox(-2.5F, -2.0F, -3.0F, 5, 4, 6),
                PartPose.offset(0.0F, 21.0F, 0.0F));
        body.addOrReplaceChild("tail", CubeListBuilder.create().texOffs(16, 10).addBox(-1.5F, -2.0F, 0.0F, 3, 2, 1),
                PartPose.offsetAndRotation(0.0F, -1.0F, 3.0F, TAIL_TILT, 0.0F, 0.0F));
        body.addOrReplaceChild("right_wing", CubeListBuilder.create().texOffs(0, 18).addBox(-1.0F, -1.0F, -1.5F, 1, 2, 3),
                PartPose.offset(-2.5F, 0.0F, 0.5F));
        body.addOrReplaceChild("left_wing", CubeListBuilder.create().texOffs(0, 18).mirror().addBox(0.0F, -1.0F, -1.5F, 1, 2, 3),
                PartPose.offset(2.5F, 0.0F, 0.5F));
        PartDefinition head = root.addOrReplaceChild("head", CubeListBuilder.create().texOffs(0, 10).addBox(-2.0F, -4.0F, -2.0F, 4, 4, 4),
                PartPose.offset(0.0F, 20.0F, -2.0F));
        head.addOrReplaceChild("beak", CubeListBuilder.create().texOffs(16, 14).addBox(-1.0F, -1.8F, -3.5F, 2, 1, 2),
                PartPose.ZERO);
        root.addOrReplaceChild("right_foot", CubeListBuilder.create().texOffs(10, 18).addBox(-0.5F, 0.0F, -1.5F, 1, 1, 2),
                PartPose.offset(-1.2F, 23.0F, 0.0F));
        root.addOrReplaceChild("left_foot", CubeListBuilder.create().texOffs(10, 18).mirror().addBox(-0.5F, 0.0F, -1.5F, 1, 1, 2),
                PartPose.offset(1.2F, 23.0F, 0.0F));
        return LayerDefinition.create(mesh, TEXTURE_SIZE, TEXTURE_SIZE);
    }

    @Override
    public void setupAnim(LivingEntityRenderState state) {
        super.setupAnim(state);
        head.xRot = state.xRot * Mth.DEG_TO_RAD;
        head.yRot = state.yRot * Mth.DEG_TO_RAD;
        float walk = state.walkAnimationPos * WADDLE_SPEED;
        float amount = Math.min(1.0F, state.walkAnimationSpeed);
        body.zRot = Mth.sin(walk) * WADDLE * amount;
        rightFoot.xRot = Mth.cos(walk) * FOOT_SWING * amount;
        leftFoot.xRot = -Mth.cos(walk) * FOOT_SWING * amount;
    }
}
