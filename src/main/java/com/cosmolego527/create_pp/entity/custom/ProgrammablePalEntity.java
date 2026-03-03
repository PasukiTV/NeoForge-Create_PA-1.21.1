package com.cosmolego527.create_pp.entity.custom;

import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import com.cosmolego527.create_pp.entity.ModEntities;
import com.cosmolego527.create_pp.entity.ProgrammablePalVariant;
import com.cosmolego527.create_pp.util.ModTags;
import com.simibubi.create.AllItems;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;
public class ProgrammablePalEntity extends PathfinderMob implements IEntityWithComplexSpawn {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    public ItemStack itemStack = ItemStack.EMPTY;

    public ItemStack instructions = ItemStack.EMPTY;

    public int insertionDelay;

    public Vec3 clientPosition, vec2 = Vec3.ZERO, vec3 = Vec3.ZERO;

    private int instructionPointer = 0;
    private int instructionCooldown = 0;

    private static final EntityDataAccessor<Integer> DOME_COLOR =
            SynchedEntityData.defineId(ProgrammablePalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK =
            SynchedEntityData.defineId(ProgrammablePalEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final String ACTION_KEY_TAG = "PalActionKey";
    private static final String ACTION_INDEX_TAG = "PalActionIndex";
    private static final String CHECK_BLOCK_TARGET_INDEX_TAG = "PalCheckBlockTargetIndex";
    private static final String MOVE_DIRECTION_INDEX_TAG = "PalMoveDirectionIndex";



    public ProgrammablePalEntity(EntityType<? extends PathfinderMob> entityTypeIn, Level level) {
        super(entityTypeIn, level);
        insertionDelay = 30;
    }
    public ProgrammablePalEntity(Level worldIn, Vec3 pos) {
        this(ModEntities.PROGRAMMABLE_PAL_ENTITY.get(), worldIn);
        this.setPos(pos);
        this.refreshDimensions();
    }

    @Override
    protected void dropAllDeathLoot(ServerLevel p_level, DamageSource damageSource) {
        super.dropAllDeathLoot(p_level, damageSource);
        ItemEntity entityIn = new ItemEntity(level(),getX(),getY(),getZ(), itemStack);
        p_level.addFreshEntity(entityIn);
    }

    public static EntityType.Builder<?> build(EntityType.Builder<?> builder){
        @SuppressWarnings("unchecked")
        EntityType.Builder<ProgrammablePalEntity> palBuilder = (EntityType.Builder<ProgrammablePalEntity>) builder;
        return palBuilder.sized(1,1);
    }

    public void setItem(ItemStack item){
        this.itemStack = item;
        refreshDimensions();
    }

    public ItemStack getItem(){
        return this.itemStack;
    }

    public void setItemStack(ItemStack itemStack){
        if(itemStack == null) return;
        this.entityData.set(DATA_ITEM_STACK, itemStack);
    }
    public ItemStack getItemStack(){
        return this.entityData.get(DATA_ITEM_STACK);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this){
            @Override
            public boolean canUse() {
                return !hasActiveInstructionTape() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !hasActiveInstructionTape() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(1, new TemptGoal(this,1.0, stack -> stack.is(AllItems.ANDESITE_ALLOY), false){
            @Override
            public boolean canUse() {
                return !hasActiveInstructionTape() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !hasActiveInstructionTape() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0){
            @Override
            public boolean canUse() {
                return !hasActiveInstructionTape() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !hasActiveInstructionTape() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10f){
            @Override
            public boolean canUse() {
                return !hasActiveInstructionTape() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !hasActiveInstructionTape() && super.canContinueToUse();
            }
        });
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this){
            @Override
            public boolean canUse() {
                return !hasActiveInstructionTape() && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return !hasActiveInstructionTape() && super.canContinueToUse();
            }
        });
    }

    @Override
    public @Nullable ItemStack getPickedResult(HitResult target) {
        return itemStack.copy();
    }

    public static AttributeSupplier.Builder createPalAttributes(){
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D);
    }


    private void setupAnimationStates() {
        if(this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 100;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }
    }



    @Override
    protected void customServerAiStep() {
        if (hasActiveInstructionTape())
            return;
        super.customServerAiStep();
    }

    @Override
    public void tick() {
        if (level() instanceof PonderLevel) {
            setDeltaMovement(getDeltaMovement().add(0, -0.06, 0));
            if (position().y < 0.125)
                discard();
        }

        if (!level().isClientSide)
            runProgramTick();

        super.tick();
    }

    /* VARIANT */
    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DOME_COLOR, 0);
        builder.define(DATA_ITEM_STACK, ItemStack.EMPTY);
    }

    private int getTypeVariant() {
        return this.entityData.get(DOME_COLOR);
    }

    public ProgrammablePalVariant getVariant() {
        return ProgrammablePalVariant.byId(this.getTypeVariant() & 255);
    }

    public void setVariant(ProgrammablePalVariant style) {
        this.entityData.set(DOME_COLOR, style.getId() & 255);
    }

    @Override
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Variant", this.getTypeVariant());
        compound.put("item", itemStack.saveOptional(level().registryAccess()));
        compound.put("InstructionTape", instructions.saveOptional(level().registryAccess()));
        compound.putInt("InstructionPointer", instructionPointer);
        compound.putInt("InstructionCooldown", instructionCooldown);
    }



    @Override
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(DOME_COLOR, compound.getInt("Variant"));
        itemStack = ItemStack.parseOptional(level().registryAccess(), compound.getCompound("item"));
        instructions = ItemStack.parseOptional(level().registryAccess(), compound.getCompound("InstructionTape"));
        instructionPointer = compound.getInt("InstructionPointer");
        instructionCooldown = compound.getInt("InstructionCooldown");
    }



    @Override
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        ItemStack.STREAM_CODEC.encode(buffer, getItem());
        Vec3 motion = getDeltaMovement();
        buffer.writeFloat((float) motion.x);
        buffer.writeFloat((float) motion.y);
        buffer.writeFloat((float) motion.z);
    }

    @Override
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        setItem(ItemStack.STREAM_CODEC.decode(additionalData));
        setDeltaMovement(
                additionalData.readFloat(),
                additionalData.readFloat(),
                additionalData.readFloat());
    }


    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (ModTags.AllItemTags.PROGRAMMABLE_INSTRUCTION_ITEM.matches(held)) {
            if (!level().isClientSide) {
                instructions = held.copyWithCount(1);
                instructionPointer = 0;
                instructionCooldown = 0;
                player.displayClientMessage(Component.literal("Tape assigned to Pal."), true);
                if (!player.getAbilities().instabuild)
                    held.shrink(1);
            }
            return InteractionResult.sidedSuccess(level().isClientSide);
        }

        return super.mobInteract(player, hand);
    }

    private boolean hasActiveInstructionTape() {
        if (instructions.isEmpty())
            return false;
        CompoundTag programTag = instructions.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        return programTag != null && !programTag.isEmpty();
    }

    private void runProgramTick() {
        if (instructionCooldown > 0)
            instructionCooldown--;
        if (instructionCooldown > 0)
            return;

        if (!hasActiveInstructionTape())
            return;

        CompoundTag programTag = instructions.get(ModDataComponentTypes.VOID_FUNCTION_DATA);

        Schedule schedule = Schedule.fromTag(level().registryAccess(), programTag);
        if (schedule.entries.isEmpty())
            return;

        if (instructionPointer >= schedule.entries.size())
            instructionPointer = 0;

        ScheduleEntry entry = schedule.entries.get(instructionPointer);
        executeInstruction(entry.instruction);

        instructionPointer++;
        if (instructionPointer >= schedule.entries.size())
            instructionPointer = 0;

        instructionCooldown = 20;
    }

    private void executeInstruction(ScheduleInstruction instruction) {
        CompoundTag data = instruction.getData();
        String action = data.getString(ACTION_KEY_TAG);

        if (action.isBlank()) {
            int actionIndex = data.getInt(ACTION_INDEX_TAG);
            action = actionIndex == 1 ? "check_block" : "move";
        }

        if ("check_block".equals(action)) {
            executeCheckBlock(data);
            return;
        }

        executeMoveForward(data);
    }

    private void executeMoveForward(CompoundTag data) {
        getNavigation().stop();

        int directionIndex = data.getInt(MOVE_DIRECTION_INDEX_TAG);
        Direction direction = switch (directionIndex) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };

        setYRot(direction.toYRot());
        setYHeadRot(direction.toYRot());
        yBodyRot = direction.toYRot();

        BlockPos target = blockPosition().relative(direction);

        BlockState stateAtFeet = level().getBlockState(target);
        BlockState stateAtHead = level().getBlockState(target.above());
        if (!stateAtFeet.isAir() || !stateAtHead.isAir())
            return;

        setPos(target.getX() + 0.5D, getY(), target.getZ() + 0.5D);
    }

    private void executeCheckBlock(CompoundTag data) {
        int targetIndex = data.getInt(CHECK_BLOCK_TARGET_INDEX_TAG);
        BlockPos checkPos = switch (targetIndex) {
            case 1 -> blockPosition().above();
            case 2 -> blockPosition().relative(getDirection());
            default -> blockPosition().below();
        };

        BlockState state = level().getBlockState(checkPos);
        String targetName = switch (targetIndex) {
            case 1 -> "Above";
            case 2 -> "Front";
            default -> "Below";
        };
        level().players().forEach(p -> p.displayClientMessage(Component.literal("Pal Check " + targetName + ": " + state.getBlock().getName().getString()), false));
    }
}