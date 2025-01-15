package techguns.client.models.guns;

import java.lang.reflect.InvocationTargetException;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.RenderItem;
import net.minecraft.client.renderer.block.model.IBakedModel;
import net.minecraft.client.renderer.block.model.ItemCameraTransforms.TransformType;
import net.minecraft.client.renderer.block.model.ModelResourceLocation;
import net.minecraft.entity.Entity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;

public class ModelBaseBakedGrenadeLauncher extends ModelBaseBaked {
	
	public ModelBaseBakedGrenadeLauncher(ResourceLocation texture_loc, ModelResourceLocation... modellocs) {
		super(texture_loc, modellocs);
	}

	public ModelBaseBakedGrenadeLauncher(IBakedModel... bakedModels) {
		super(bakedModels);
	}

	public ModelBaseBakedGrenadeLauncher(Item item, int parts) {
		super(item, parts);
	}

	@Override
	public void render(Entity entityIn, float limbSwing, float limbSwingAmount, float ageInTicks, float netHeadYaw, float headPitch, float scale, int ammoLeft,
			float reloadProgress, TransformType transformType, int part, float fireProgress, float chargeProgress) {
		RenderItem renderItem=Minecraft.getMinecraft().getRenderItem();
		renderItem.renderModel(bakedModels.get(0), ItemStack.EMPTY);
		GlStateManager.pushMatrix();
		GlStateManager.translate(0, -2f, 0);
		GlStateManager.rotate(60.0f*fireProgress, 1, 0, 0);
		GlStateManager.translate(0, 2f, 0);
		renderItem.renderModel(bakedModels.get(1), ItemStack.EMPTY);
		GlStateManager.popMatrix();
	}
	
}
