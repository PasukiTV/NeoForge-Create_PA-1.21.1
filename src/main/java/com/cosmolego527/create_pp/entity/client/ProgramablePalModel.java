package com.cosmolego527.create_pp.entity.client;

import com.cosmolego527.create_pp.CreatePP;
import com.cosmolego527.create_pp.entity.custom.ProgrammablePalEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.*;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;

public class ProgramablePalModel<T extends ProgrammablePalEntity> extends HierarchicalModel<T> {



    // This layer location should be baked with EntityRendererProvider.Context in the entity renderer and passed into this model's constructor
    public static final ModelLayerLocation LAYER_LOCATION =
            new ModelLayerLocation(ResourceLocation.fromNamespaceAndPath(CreatePP.MOD_ID, "programmable_pal"), "main");

    private final ModelPart body;
    private final ModelPart head;
    private final ModelPart armR;

    public ProgramablePalModel(ModelPart root) {
        super(RenderType::entityTranslucent);
        this.body = root.getChild("Body");
        this.head = this.body.getChild("Head");
        this.armR = this.body.getChild("ArmR");
    }

    public static LayerDefinition createBodyLayer() {
        MeshDefinition meshdefinition = new MeshDefinition();
        PartDefinition partdefinition = meshdefinition.getRoot();

        PartDefinition Body = partdefinition.addOrReplaceChild("Body", CubeListBuilder.create().texOffs(0, 26).addBox(-3.0F, -4.0F, -2.0F, 6.0F, 4.0F, 4.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, 20.0F, 0.0F));

        PartDefinition LegL = Body.addOrReplaceChild("LegL", CubeListBuilder.create().texOffs(28, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(2.0F, 0.0F, 0.0F));

        PartDefinition LegR = Body.addOrReplaceChild("LegR", CubeListBuilder.create().texOffs(28, 31).addBox(-1.0F, 0.0F, -1.0F, 2.0F, 4.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-2.0F, 0.0F, 0.0F));

        PartDefinition ArmR = Body.addOrReplaceChild("ArmR", CubeListBuilder.create().texOffs(0, 18).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offset(-4.0F, -3.0F, 0.0F));

        PartDefinition ArmL = Body.addOrReplaceChild("ArmL", CubeListBuilder.create(), PartPose.offset(4.0F, -3.0F, 0.0F));

        PartDefinition cube_r1 = ArmL.addOrReplaceChild("cube_r1", CubeListBuilder.create().texOffs(0, 18).addBox(-1.0F, -1.0F, -1.0F, 2.0F, 5.0F, 2.0F, new CubeDeformation(0.0F)), PartPose.offsetAndRotation(0.0F, 0.0F, 0.0F, 0.0F, 3.1416F, 0.0F));

        PartDefinition Head = Body.addOrReplaceChild("Head", CubeListBuilder.create().texOffs(20, 0).addBox(-3.0F, -1.5F, -3.0F, 6.0F, 2.0F, 6.0F, new CubeDeformation(0.0F))
                .texOffs(28, 26).addBox(-2.0F, -2.5F, -2.0F, 4.0F, 1.0F, 4.0F, new CubeDeformation(0.0F))
                .texOffs(0, 35).addBox(1.0F, -2.5F, 2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(0, 35).addBox(-2.0F, -2.5F, 2.0F, 1.0F, 1.0F, 2.0F, new CubeDeformation(0.0F))
                .texOffs(40, 36).addBox(-3.0F, -5.5F, -3.0F, 6.0F, 4.0F, 6.0F, new CubeDeformation(0.0F)), PartPose.offset(0.0F, -4.5F, 0.0F));

        return LayerDefinition.create(meshdefinition, 64, 64);
    }

    @Override
    public void setupAnim(T entity, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch) {
        this.root().getAllParts().forEach(ModelPart::resetPose);
        this.applyHeadRotation(netHeadYaw, headPitch);

        this.animateWalk(ProgrammabelPalAnimations.ANIM_PP_WALK2, limbSwing, limbSwingAmount, 2f, 2.5f);
        this.animate(entity.idleAnimationState, ProgrammabelPalAnimations.ANIM_PP_IDLE, ageInTicks, 1f);
    }

    private void applyHeadRotation(float headYaw, float headPitch){
        headYaw = Mth.clamp(headYaw, -30f,30f);
        headPitch = Mth.clamp(headPitch, -25f, 45);

        this.head.yRot = headYaw * ((float)Math.PI/180f);
        this.head.xRot = headPitch * ((float)Math.PI/180f);
    }

    /**
     * Applies model transforms to the right hand so render layers can attach held items.
     */
    public void translateToRightHand(PoseStack poseStack) {
        this.body.translateAndRotate(poseStack);
        this.armR.translateAndRotate(poseStack);
        poseStack.translate(0.0D, 0.15D, -0.1D);
    }

    @Override
    public void renderToBuffer(PoseStack poseStack, VertexConsumer vertexConsumer, int packedLight, int packedOverlay, int color) {

        body.render(poseStack, vertexConsumer, packedLight, packedOverlay, color);
    }

    @Override
    public ModelPart root() {
        return body;
    }
}
