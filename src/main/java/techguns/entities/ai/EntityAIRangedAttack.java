package techguns.entities.ai;

import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.material.Material;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.EntityLiving;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.IRangedAttackMob;
import net.minecraft.entity.ai.EntityAIBase;
import net.minecraft.entity.ai.EntityAIOpenDoor;
import net.minecraft.pathfinding.Path;
import net.minecraft.pathfinding.PathNavigateGround;
import net.minecraft.pathfinding.PathPoint;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import techguns.TGBlocks;
import techguns.blocks.BlockTGDoor2x1;
import techguns.blocks.BlockTGDoor3x3;

import static net.minecraft.block.BlockDoor.OPEN;

public class EntityAIRangedAttack extends EntityAIBase
{
    /** The entity the AI instance has been applied to */
    private final EntityLiving entityHost;
    /** The entity (as a RangedAttackMob) the AI instance has been applied to. */
    private final IRangedAttackMob rangedAttackEntityHost;
    private EntityLivingBase attackTarget;
    /**
     * A decrementing tick that spawns a ranged attack once this value reaches 0. It is then set back to the
     * maxRangedAttackTime.
     */
    private int rangedAttackTime;
    private double entityMoveSpeed;
    private int ticksTargetSeen;
    private int attackTimeVariance;
    /** The maximum time the AI has to wait before peforming another ranged attack. */
    private int maxRangedAttackTime;
    private float attackRange;
    private float attackRange_2;
    private final EntityAIOpenTGDoor doorOpenAI;
    
    //GUN HANDLING:
    private int maxBurstCount; //Total number of shots in burst.
    private int burstCount; //shots left in current burst.
    private int shotDelay; //delay between shots in burst.
    

//    public EntityAIRangedAttack(IRangedAttackMob p_i1649_1_, double p_i1649_2_, int p_i1649_4_, float p_i1649_5_)
//    {
//        this(p_i1649_1_, p_i1649_2_, p_i1649_4_, p_i1649_4_, p_i1649_5_);
//    }

    public EntityAIRangedAttack(IRangedAttackMob shooter, double moveSpeed, int attackTimeVariance, int attackTime, float attackRange, int maxBurstCount, int shotDelay)
    {
        this.rangedAttackTime = -1;

        if (!(shooter instanceof EntityLivingBase))
        {
            throw new IllegalArgumentException("ArrowAttackGoal requires Mob implements RangedAttackMob");
        }
        else
        {
            this.rangedAttackEntityHost = shooter;
            this.entityHost = (EntityLiving)shooter;
            this.entityMoveSpeed = moveSpeed;
            this.attackTimeVariance = attackTimeVariance;
            this.maxRangedAttackTime = attackTime;
            this.attackRange = attackRange;
            this.attackRange_2 = attackRange * attackRange;
            this.setMutexBits(3);
            
            this.maxBurstCount = maxBurstCount;
            this.burstCount = maxBurstCount;
            this.shotDelay = shotDelay;
        }

        if (this.entityHost.getNavigator() instanceof PathNavigateGround) {
            ((PathNavigateGround) this.entityHost.getNavigator()).setBreakDoors(true);
            ((PathNavigateGround) this.entityHost.getNavigator()).setEnterDoors(true);
        }
        this.doorOpenAI = new EntityAIOpenTGDoor(this.entityHost, true);
        this.entityHost.tasks.addTask(1, this.doorOpenAI);
    }

    /**
     * Returns whether the EntityAIBase should begin execution.
     */
    public boolean shouldExecute()
    {
        EntityLivingBase entitylivingbase = this.entityHost.getAttackTarget();

        if (entitylivingbase == null)
        {
            return false;
        }
        else
        {
            this.attackTarget = entitylivingbase;
            return true;
        }
    }

    /**
     * Returns whether an in-progress EntityAIBase should continue executing
     */
    public boolean continueExecuting()
    {
        return this.shouldExecute() || !this.entityHost.getNavigator().noPath();
    }

    /**
     * Resets the task
     */
    public void resetTask()
    {
        this.attackTarget = null;
        this.ticksTargetSeen = 0;
        this.rangedAttackTime = -1;
    }

    /**
     * Updates the task
     */
    public void updateTask()
    {
        double d0 = this.entityHost.getDistanceSq(this.attackTarget.posX, this.attackTarget.posY/*this.attackTarget.boundingBox.minY TODO??*/, this.attackTarget.posZ);
        boolean targetInSight = this.entityHost.getEntitySenses().canSee(this.attackTarget);

        if (targetInSight)
        {
            ++this.ticksTargetSeen;
        }
        else
        {
            this.ticksTargetSeen = 0;
        }

        if (d0 <= (double)this.attackRange_2 && this.ticksTargetSeen >= 20)
        {
            this.entityHost.getNavigator().clearPath();
        }
        else
        {
            this.entityHost.getNavigator().tryMoveToEntityLiving(this.attackTarget, this.entityMoveSpeed);
        }

        this.entityHost.getLookHelper().setLookPositionWithEntity(this.attackTarget, 30.0F, 55.0F);
        float f;

        if (--this.rangedAttackTime == 0)
        {
            if (d0 > (double)this.attackRange_2 || !targetInSight)
            {
                return;
            }

            f = MathHelper.sqrt(d0) / this.attackRange;
            
            float f1 = f;

            if (f < 0.1F)
            {
                f1 = 0.1F;
            }

            if (f1 > 1.0F)
            {
                f1 = 1.0F;
            }

            this.rangedAttackEntityHost.attackEntityWithRangedAttack(this.attackTarget, f1);
            
            if (maxBurstCount > 0) burstCount--;
            if (burstCount > 0) {
            	this.rangedAttackTime = shotDelay;
            }else {
            	burstCount = maxBurstCount;
            	this.rangedAttackTime = MathHelper.floor(f * (float)(this.maxRangedAttackTime - this.attackTimeVariance) + (float)this.attackTimeVariance);
            }
        }
        else if (this.rangedAttackTime < 0)
        {
            f = MathHelper.sqrt(d0) / this.attackRange;
            this.rangedAttackTime = MathHelper.floor(f * (float)(this.maxRangedAttackTime - this.attackTimeVariance) + (float)this.attackTimeVariance);
        }

        if (this.entityHost.getNavigator().getPath() != null) {
            this.doorOpenAI.updateTask();
        }
    }

    private class EntityAIOpenTGDoor extends EntityAIBase
    {
        private final EntityLiving entity;
        private BlockPos doorPosition;
        private boolean hasStoppedDoorInteraction;
        private float entityPositionX;
        private float entityPositionZ;

        public EntityAIOpenTGDoor(EntityLiving entityIn, boolean shouldClose)
        {
            this.entity = entityIn;
            this.setMutexBits(3);
        }

        @Override
        public boolean shouldExecute()
        {
            if (!this.entity.collidedHorizontally)
            {
                return false;
            }
            else
            {
                PathNavigateGround pathnavigateground = (PathNavigateGround)this.entity.getNavigator();
                Path path = pathnavigateground.getPath();

                if (path != null && !path.isFinished() && pathnavigateground.getEnterDoors())
                {
                    for (int i = 0; i < Math.min(path.getCurrentPathIndex() + 2, path.getCurrentPathLength()); ++i)
                    {
                        PathPoint pathpoint = path.getPathPointFromIndex(i);
                        BlockPos pos = new BlockPos(pathpoint.x, pathpoint.y, pathpoint.z);

                        if (this.entity.getDistanceSq(pos.getX(), this.entity.posY, pos.getZ()) <= 2.25D)
                        {
                            this.doorPosition = this.getDoorPosition(pos);

                            if (this.doorPosition != null)
                            {
                                return true;
                            }
                        }
                    }

                    this.doorPosition = this.getDoorPosition(new BlockPos(this.entity).up());
                    return this.doorPosition != null;
                }
                else
                {
                    return false;
                }
            }
        }

        @Override
        public void startExecuting()
        {
            this.hasStoppedDoorInteraction = false;
            this.entityPositionX = (float)((double)this.doorPosition.getX() + 0.5D - this.entity.posX);
            this.entityPositionZ = (float)((double)this.doorPosition.getZ() + 0.5D - this.entity.posZ);
        }

        @Override
        public boolean shouldContinueExecuting()
        {
            return !this.hasStoppedDoorInteraction;
        }

        @Override
        public void updateTask()
        {
            if (this.doorPosition != null)
            {
                double distanceSq = this.entity.getDistanceSq(
                        this.doorPosition.getX() + 0.5D,
                        this.doorPosition.getY() + 0.5D,
                        this.doorPosition.getZ() + 0.5D
                );

                boolean nearDoor = distanceSq < 2.25D;

                if (nearDoor)
                {
                    this.interactWithDoor(this.doorPosition, true);
                }

                float f = (float)((double)this.doorPosition.getX() + 0.5D - this.entity.posX);
                float f1 = (float)((double)this.doorPosition.getZ() + 0.5D - this.entity.posZ);
                float f2 = this.entityPositionX * f + this.entityPositionZ * f1;

                if (f2 < 0.0F || !nearDoor)
                {
                    this.hasStoppedDoorInteraction = true;
                }
            }
            else
            {
                this.hasStoppedDoorInteraction = true;
            }
        }

        private BlockPos getDoorPosition(BlockPos pos)
        {
            IBlockState iblockstate = this.entity.world.getBlockState(pos);
            Block block = iblockstate.getBlock();

            if (block instanceof BlockDoor && iblockstate.getMaterial() == Material.WOOD)
            {
                return pos;
            }
            else if (block instanceof BlockTGDoor2x1)
            {
                return pos;
            }

            return null;
        }

        private void interactWithDoor(BlockPos pos, boolean open) {
            IBlockState state = this.entity.world.getBlockState(pos);
            Block block = state.getBlock();
            if (block instanceof BlockTGDoor2x1) {
                if (!((Boolean) state.getValue(OPEN)).booleanValue()) {
                    EnumFacing facing = EnumFacing.fromAngle(this.entity.rotationYaw);
                    float hitX = 0.5f;
                    float hitY = 0.5f;
                    float hitZ = 0.5f;

                    block.onBlockActivated(this.entity.world, pos, state, null, EnumHand.MAIN_HAND, facing, hitX, hitY, hitZ);
                }
            } else if (block instanceof BlockDoor) {
                if (!((Boolean) state.getValue(OPEN)).booleanValue()) {
                    EnumFacing facing = EnumFacing.fromAngle(this.entity.rotationYaw);
                    float hitX = 0.5f;
                    float hitY = 0.5f;
                    float hitZ = 0.5f;

                    block.onBlockActivated(this.entity.world, pos, state, null, EnumHand.MAIN_HAND, facing, hitX, hitY, hitZ);
                }
            }
        }
    }
}