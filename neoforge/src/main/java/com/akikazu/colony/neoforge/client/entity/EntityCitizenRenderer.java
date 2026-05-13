package com.akikazu.colony.neoforge.client.entity;

import com.akikazu.colony.common.citizen.entity.EntityCitizen;

import net.minecraft.client.model.VillagerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

/**
 * Placeholder renderer for {@link EntityCitizen}. Re-uses the vanilla villager model and texture so the entity is
 * visible during V1 development; a Colony-specific model and texture replace this in a later milestone.
 *
 * <p>
 * Cannot extend {@code VillagerRenderer} directly because that class is parameterised over {@code Villager} and the
 * design pillars forbid {@link EntityCitizen} from subclassing {@code Villager}. We extend {@link MobRenderer} bound to
 * the villager model instead.
 */
@OnlyIn(Dist.CLIENT)
public class EntityCitizenRenderer extends MobRenderer<EntityCitizen, VillagerModel<EntityCitizen>>
{
    private static final ResourceLocation VILLAGER_TEXTURE = ResourceLocation
            .withDefaultNamespace("textures/entity/villager/villager.png");

    public EntityCitizenRenderer(EntityRendererProvider.Context context)
    {
        super(context, new VillagerModel<>(context.bakeLayer(ModelLayers.VILLAGER)), 0.5F);
    }

    @Override
    public ResourceLocation getTextureLocation(EntityCitizen entity)
    {
        return VILLAGER_TEXTURE;
    }
}
