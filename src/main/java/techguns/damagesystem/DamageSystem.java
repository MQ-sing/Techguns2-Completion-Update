package techguns.damagesystem;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.init.MobEffects;
import net.minecraft.inventory.EntityEquipmentSlot;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemArmor;
import net.minecraft.item.ItemShield;
import net.minecraft.item.ItemStack;
import net.minecraft.util.CombatRules;
import net.minecraft.util.DamageSource;
import net.minecraft.util.EntityDamageSource;
import net.minecraft.util.EnumHand;
import net.minecraft.util.SoundEvent;
import net.minecraft.util.math.MathHelper;
import net.minecraftforge.fml.common.FMLCommonHandler;
import net.minecraftforge.fml.relauncher.ReflectionHelper;
import techguns.TGConfig;
import techguns.api.damagesystem.DamageType;
import techguns.api.npc.INpcTGDamageSystem;
import techguns.entities.npcs.NPCTurret;
import techguns.items.armors.GenericArmor;
import techguns.util.MathUtil;

public class DamageSystem {

	public static float getDamageFactor(EntityLivingBase attacker, EntityLivingBase target) {
		if(attacker instanceof EntityPlayer){
            if(target instanceof EntityPlayer){
                if (FMLCommonHandler.instance().getMinecraftServerInstance().isPVPEnabled()){
                    return TGConfig.damagePvP;
                } else {
                    return 0.0f;
                }
            }
            return TGConfig.damagePlayerAttack;
        } else if (target instanceof EntityPlayer){
			if (attacker instanceof NPCTurret){
				return TGConfig.damageTurretToPlayer;
			} else {
				return TGConfig.damageFactorNPC;
			}
		}
		
		return TGConfig.damageFactorNPC;
	}
	public static float getTotalArmorAgainstType(EntityPlayer ply, DamageType type){
		float value=0.0f;
		
		for(int i=0;i<4; i++){
			ItemStack armor = ply.inventory.armorInventory.get(i);//ply.inventory.armorInventory[i];
			if(armor!=null){
				Item item = armor.getItem();
				
				if(item instanceof GenericArmor){
					value+=((GenericArmor)item).getArmorValue(armor, type);				
				} else if (item instanceof ItemArmor){
					if(type==DamageType.PHYSICAL){
						value += ((ItemArmor) item).getArmorMaterial().getDamageReductionAmount(((ItemArmor)item).armorType);
					}
				}
				
			}
			
		}
		
		return value;
	}
	
	/**
	 * Default behavior when unspecified
	 */
	public static float getArmorAgainstDamageTypeDefault(EntityLivingBase elb, float armor, DamageType damageType){
		switch(damageType){
			case PHYSICAL:
			case PROJECTILE:
				return armor;
				
			case EXPLOSION:
			case ENERGY:
			case ICE:
			case LIGHTNING:
			case DARK:
				return armor*0.5f;
			case FIRE:
				if(elb.isImmuneToFire()){
					return armor*2;
				} else {
					return armor*0.5f;
				}
				
			case POISON:
				return 0;
			case RADIATION:
				return 0;
			case UNRESISTABLE:
			default:
				return 0;
		}
		
	}
	
    /**
     * Static copy of EntityLivingBase.attackEntityFrom with some changes
     * @throws IllegalAccessException 
     * @throws IllegalArgumentException 
     * @throws InvocationTargetException 
     */
    public static boolean attackEntityFrom(EntityLivingBase ent, DamageSource source, float amount)
    {
        //if (!net.minecraftforge.common.ForgeHooks.onLivingAttack(this, source, amount)) return false;
    	
    	TGDamageSource dmgsrc = TGDamageSource.getFromGenericDamageSource(source);
    	
        if (ent.isEntityInvulnerable(source))
        {
            return false;
        }
        else if (ent.world.isRemote)
        {
            return false;
        }
        else
        {
            ent.idleTime = 0;

            if (ent.getHealth() <= 0.0F)
            {
                return false;
            }
            else if (source.isFireDamage() && ent.isPotionActive(MobEffects.FIRE_RESISTANCE))
            {
                return false;
            }
            else
            {
                float f = amount;

                if ((source == DamageSource.ANVIL || source == DamageSource.FALLING_BLOCK) && !ent.getItemStackFromSlot(EntityEquipmentSlot.HEAD).isEmpty())
                {
                    ent.getItemStackFromSlot(EntityEquipmentSlot.HEAD).damageItem((int)(amount * 4.0F + (ent.rand).nextFloat() * amount * 2.0F), ent);
                    amount *= 0.75F;
                }

                boolean flag = false;

                //ELB_canBlockDamageSource.invoke(ent, source);
                
                if (amount > 0.0F && ent.canBlockDamageSource(source))
                {
                    ent.damageShield(amount);

                    //amount = 0.0F;
                	/**SHIELD DAMAGE HOOK**/
                	amount = calculateShieldDamage(ent, amount, dmgsrc);
                	ShieldStats.playBlockSound(ent, dmgsrc);
                	/**END**/

                    //if (!source.isProjectile())
                	if(dmgsrc.knockbackOnShieldBlock())
                    {
                        Entity entity = source.getImmediateSource();

                        if (entity instanceof EntityLivingBase)
                        {
                            ent.blockUsingShield((EntityLivingBase)entity);
                        }
                    }

                    flag = true;
                }

                ent.limbSwingAmount = 1.5F;
                boolean flag1 = true;

                if (!dmgsrc.ignoreHurtresistTime && ((float)ent.hurtResistantTime > (float)ent.maxHurtResistantTime / 2.0F))
                {
                    if (amount <= ent.lastDamage)
                    {
                        return false;
                    }

                    ent.damageEntity(source, amount -ent.lastDamage);
                    ent.lastDamage = amount;
                    flag1 = false;
                }
                else
                {
                	if (!dmgsrc.ignoreHurtresistTime) {
	                    ent.lastDamage=amount;
	                    ent.hurtResistantTime = ent.maxHurtResistantTime;
                	}
                    ent.damageEntity(source, amount);
                    if(!dmgsrc.ignoreHurtresistTime) {
	                    ent.maxHurtTime = 10;
	                    ent.hurtTime = ent.maxHurtTime;
                    }
                }

                ent.attackedAtYaw = 0.0F;
                Entity entity1 = source.getTrueSource();

                if (entity1 != null)
                {
                    if (entity1 instanceof EntityLivingBase)
                    {
                        ent.setRevengeTarget((EntityLivingBase)entity1);
                    }

                    if (entity1 instanceof EntityPlayer)
                    {
                        ent.recentlyHit = 100;
                        ent.attackingPlayer = (EntityPlayer)entity1;
                    }
                    else if (entity1 instanceof net.minecraft.entity.passive.EntityTameable)
                    {
                        net.minecraft.entity.passive.EntityTameable entitywolf = (net.minecraft.entity.passive.EntityTameable)entity1;

                        if (entitywolf.isTamed())
                        {
                            ent.recentlyHit = 100;
                            ent.attackingPlayer = null;
                        }
                    }
                }

                if (flag1)
                {
                    if (flag)
                    {
                        ent.world.setEntityState(ent, (byte)29);
                    }
                    else if (source instanceof EntityDamageSource && ((EntityDamageSource)source).getIsThornsDamage())
                    {
                        ent.world.setEntityState(ent, (byte)33);
                    }
                    else
                    {
                        byte b0;

                        if (source == DamageSource.DROWN)
                        {
                            b0 = 36;
                        }
                        else if (source.isFireDamage())
                        {
                            b0 = 37;
                        }
                        else
                        {
                            b0 = 2;
                        }

                        ent.world.setEntityState(ent, b0);
                    }

                    if (source != DamageSource.DROWN && (!flag || amount > 0.0F))
                    {
                        ent.markVelocityChanged();
                    }

                    if (entity1 != null)
                    {
                        double d1 = entity1.posX - ent.posX;
                        double d0;

                        for (d0 = entity1.posZ - ent.posZ; d1 * d1 + d0 * d0 < 1.0E-4D; d0 = (Math.random() - Math.random()) * 0.01D)
                        {
                            d1 = (Math.random() - Math.random()) * 0.01D;
                        }

                        ent.attackedAtYaw = (float)(MathHelper.atan2(d0, d1) * (180D / Math.PI) - (double)ent.rotationYaw);
                        float knockback_strength = 0.4F*dmgsrc.knockbackMultiplier;
                        if (knockback_strength>0) {
                        	ent.knockBack(entity1, knockback_strength, d1, d0);
                        }
                    }
                    else
                    {
                        ent.attackedAtYaw = (float)((int)(Math.random() * 2.0D) * 180);
                    }
                }

                if (ent.getHealth() <= 0.0F)
                {
                    if (!ent.checkTotemDeathProtection(source))
                    {
                        SoundEvent soundevent = ent.getDeathSound();

                        if (flag1 && soundevent != null)
                        {
                            ent.playSound(soundevent, ent.getSoundVolume(), ent.getSoundPitch());
                        }

                        ent.onDeath(source);
                    }
                }
                else if (flag1)
                {
                    ent.playHurtSound(source);
                }

                boolean flag2 = !flag || amount > 0.0F;

                if (flag2)
                {
                    ent.lastDamageSource = source;
                    ent.lastDamageStamp = ent.world.getTotalWorldTime();
                }

                if (ent instanceof EntityPlayerMP)
                {
                    CriteriaTriggers.ENTITY_HURT_PLAYER.trigger((EntityPlayerMP)ent, source, f, amount, flag);
                }

                if (entity1 instanceof EntityPlayerMP)
                {
                    CriteriaTriggers.PLAYER_HURT_ENTITY.trigger((EntityPlayerMP)entity1, ent, source, f, amount, flag);
                }

                //return parameter workaround
                if(flag2) {
                	dmgsrc.setAttackSuccessful();
                }
                return flag2;
            }
        }
    }

    public static float calculateShieldDamage(EntityLivingBase ent, float amount, TGDamageSource source) {
    	
    	//ItemStack offHand = ent.getHeldItem(EnumHand.OFF_HAND);
    	
    	ItemStack active = ent.getActiveItemStack();
    	
		ShieldStats s = ShieldStats.getStats(active, ent);
    	
    	/*if(offHand.getItem()==Items.SHIELD) {
    		float amountNew = ShieldStats.VANILLA_SHIELD.getAmount(amount, source);
    		//System.out.println("HIT_SHIELD: "+amount +"->"+amountNew );
    		return amountNew;
    	} else if (offHand.getItem() instanceof ItemShield) {
    		return ShieldStats.DEFAULT_STATS.getAmount(amount, source);
    	}*/
    	if(s!=null) {
    		return s.getAmount(amount, source);
    	}
		return amount;
	}
    
    public static void livingHurt(EntityLivingBase elb, DamageSource damageSrc, float damageAmount) {
    	damageAmount = ELB_applyArmorCalculations(elb,damageSrc, damageAmount);
        damageAmount = elb.applyPotionDamageCalculations(damageSrc, damageAmount);
        float f = damageAmount;
        damageAmount = Math.max(damageAmount - elb.getAbsorptionAmount(), 0.0F);
        elb.setAbsorptionAmount(elb.getAbsorptionAmount() - (f - damageAmount));
        damageAmount = net.minecraftforge.common.ForgeHooks.onLivingDamage(elb, damageSrc, damageAmount);

        if (damageAmount != 0.0F)
        {
            float f1 = elb.getHealth();
            elb.setHealth(f1 - damageAmount);
            elb.getCombatTracker().trackDamage(damageSrc, f1, damageAmount);
            elb.setAbsorptionAmount(elb.getAbsorptionAmount() - damageAmount);
        }
    }
    
    /**
     * Reduces damage, depending on armor
     */
    public static float ELB_applyArmorCalculations(EntityLivingBase elb, DamageSource source, float damage)
    {
        if (!source.isUnblockable())
        {
            elb.damageArmor(damage);
            TGDamageSource dmgsrc = TGDamageSource.getFromGenericDamageSource(source);
            INpcTGDamageSystem tg = (INpcTGDamageSystem) elb;
            
            //float toughness = tg.getToughnessAfterPentration(elb, dmgsrc);
            
            float toughness = (float)elb.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue();
            
           // System.out.println("DamageBefore:"+damage);
           // System.out.println("Armor:"+tg.getTotalArmorAgainstType(dmgsrc));
           // System.out.println("Pen(x4):"+dmgsrc.armorPenetration*4);
           // System.out.println("Toughness:"+toughness);
            
            //damage = CombatRules.getDamageAfterAbsorb(damage, tg.getTotalArmorAgainstType(dmgsrc), toughness);
            damage = (float) getDamageAfterAbsorb_TGFormula(damage, tg.getTotalArmorAgainstType(dmgsrc), toughness, dmgsrc.armorPenetration*4);
        }

        //System.out.println("DamageAfter:"+damage);
        return damage;
    }
    
    /**
     * based on old 1.7 damage formula
     * @return
     */
    public static double getDamageAfterAbsorb_TGFormula(float damage, float totalArmor, float toughnessAttribute, float penetration)
    {
    	//*New Formula*
        //float f = 2.0F + toughnessAttribute / 4.0F;
        //float f1 = MathHelper.clamp(totalArmor - damage / f, totalArmor * 0.2F, 20.0F);
        //return damage * (1.0F - f1 / 25.0F);
    	
    	//use toughness to reduce penetration

    	float pen = Math.max((penetration)-toughnessAttribute, 0);
    	
    	double armor = MathUtil.clamp(totalArmor-pen, 0.0,24.0);
    	
    	/*System.out.println("***********************************");
    	System.out.println("DAMAGE:"+damage);
    	System.out.println("Penetration:"+penetration);
    	System.out.println("Toughness:"+toughnessAttribute);
    	System.out.println("PEN:"+pen);
    	System.out.println("TotalArmor"+totalArmor);
    	System.out.println("ArmorFinal:"+armor);
    	System.out.println("DamageFinal:"+(damage * (1.0-armor/25.0)));*/
    	
    	return damage * (1.0-armor/25.0);
    }
}
