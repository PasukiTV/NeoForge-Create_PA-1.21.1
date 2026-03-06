package com.cosmolego527.create_pp.entity.programmable_pal;

import com.cosmolego527.create_pp.entity.ModEntities;
import com.cosmolego527.create_pp.entity.programmable_pal.ProgrammablePalVariant;
import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import com.cosmolego527.create_pp.entity.programmable_pal.menu.ProgrammablePalMenu;
import com.cosmolego527.create_pp.item.ModItems;
import com.mojang.authlib.GameProfile;
import com.simibubi.create.content.logistics.filter.FilterItemStack;
import com.simibubi.create.content.trains.schedule.Schedule;
import com.simibubi.create.content.trains.schedule.ScheduleEntry;
import com.simibubi.create.content.trains.schedule.destination.ScheduleInstruction;
import net.createmod.ponder.api.level.PonderLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.SwordItem;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.util.FakePlayer;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.entity.IEntityWithComplexSpawn;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayDeque;
import com.cosmolego527.create_pp.ModMenuTypes;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class ProgrammablePalEntity extends PathfinderMob implements IEntityWithComplexSpawn {
    public final AnimationState idleAnimationState = new AnimationState();
    private int idleAnimationTimeout = 0;

    public ItemStack itemStack = ItemStack.EMPTY;

    private final SimpleContainer inventory = new SimpleContainer(ProgrammablePalMenu.PAL_SLOT_COUNT) {
        @Override
        public void setChanged() {
            super.setChanged();
            syncStateFromInventory();
        }

        @Override
        public void startOpen(Player player) {
            super.startOpen(player);
            openMenuCount++;
        }

        @Override
        public void stopOpen(Player player) {
            super.stopOpen(player);
            openMenuCount = Math.max(0, openMenuCount - 1);
            if (openMenuCount == 0 && getInstructionTape().isEmpty()) {
                programStartPos = null;
                programActiveLastTick = false;
            }
        }
    };


    public int insertionDelay;
    private int openMenuCount = 0;

    public Vec3 clientPosition, vec2 = Vec3.ZERO, vec3 = Vec3.ZERO;

    private int instructionPointer = 0;
    private int instructionCooldown = 0;
    private int queuedMoveSteps = 0;
    private int queuedMoveDirectionIndex = 0;
    private CompoundTag queuedStepCheckInstructionData = null;
    private boolean skipNextStandaloneCheckInstruction = false;
    private ItemStack lastInstructionTapeSnapshot = ItemStack.EMPTY;
    private CompoundTag activeRunTapeProgramTag = null;
    private int activeRunTapeInstructionPointer = 0;
    private int activeRunTapeRemainingRuns = 0;
    private int runTapeDepth = 0;

    private final ArrayDeque<BlockPos> pendingChopTargets = new ArrayDeque<>();
    private final Set<BlockPos> queuedChopTargets = new HashSet<>();
    private final Set<BlockPos> pendingLeafRemoval = new HashSet<>();
    private BlockPos currentChopTarget = null;
    private float currentChopProgress = 0f;
    private int chopCooldown = 0;

    private UUID commanderUUID = null;
    private boolean followCommander = true;
    private BlockPos programStartPos = null;
    private boolean programActiveLastTick = false;

    private static final EntityDataAccessor<Integer> DOME_COLOR =
            SynchedEntityData.defineId(ProgrammablePalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK =
            SynchedEntityData.defineId(ProgrammablePalEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_HELD_TOOL =
            SynchedEntityData.defineId(ProgrammablePalEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final int TAPE_SLOT = ProgrammablePalMenu.TAPE_SLOT_INDEX;
    private static final int TOOL_SLOT_START = ProgrammablePalMenu.PAL_STORAGE_START;
    private static final int TOOL_SLOT_END = ProgrammablePalMenu.PAL_SLOT_COUNT;

    private static final String ACTION_KEY_TAG = "PalActionKey";
    private static final String CHECK_BLOCK_TARGET_INDEX_TAG = "PalCheckBlockTargetIndex";
    private static final String MOVE_DISTANCE_INDEX_TAG = "PalMoveDistanceIndex";
    private static final String ROTATE_OPTION_INDEX_TAG = "PalRotateOptionIndex";
    private static final String MOVE_STEP_CHECK_LINK_TAG = "PalMoveStepCheckLink";
    private static final String CHECK_BLOCK_MATCH_ACTION_KEY_TAG = "PalCheckBlockMatchActionKey";
    private static final String CHECK_BLOCK_MATCH_ITEM_TAG = "PalCheckBlockMatchItem";
    private static final String HAS_ITEM_TARGET_INDEX_TAG = "PalHasItemTargetIndex";
    private static final String HAS_ITEM_ACTION_KEY_TAG = "PalHasItemActionKey";
    private static final String HAS_ITEM_MATCH_ITEM_TAG = "PalHasItemMatchItem";
    private static final String RUN_TAPE_ITEM_TAG = "PalRunTapeItem";
    private static final String RUN_TAPE_REPEAT_COUNT_TAG = "PalRunTapeRepeatCount";
    private static final String INTERACT_TARGET_KEY_TAG = "PalInteractTargetKey";
    private static final String INTERACT_MODE_KEY_TAG = "PalInteractModeKey";
    private static final String INTERACT_FILTER_ITEM_TAG = "PalInteractFilterItem";
    private static final String INTERACT_KEEP_ITEM_TAG = "PalInteractKeepItem";
    private static final String COMMANDER_UUID_TAG = "CommanderUUID";
    private static final String FOLLOW_COMMANDER_TAG = "FollowCommander";
    private static final String PROGRAM_START_POS_TAG = "ProgramStartPos";
    private static final String INSTRUCTION_POINTER_TAG = "InstructionPointer";
    private static final String INSTRUCTION_COOLDOWN_TAG = "InstructionCooldown";
    private static final String QUEUED_MOVE_STEPS_TAG = "QueuedMoveSteps";
    private static final String QUEUED_MOVE_DIRECTION_INDEX_TAG = "QueuedMoveDirectionIndex";
    private static final String SKIP_NEXT_STANDALONE_CHECK_TAG = "SkipNextStandaloneCheckInstruction";
    private static final String QUEUED_STEP_CHECK_DATA_TAG = "QueuedStepCheckInstructionData";
    private static final String ACTIVE_RUN_TAPE_PROGRAM_TAG = "ActiveRunTapeProgramTag";
    private static final String ACTIVE_RUN_TAPE_POINTER_TAG = "ActiveRunTapeInstructionPointer";
    private static final String ACTIVE_RUN_TAPE_REMAINING_TAG = "ActiveRunTapeRemainingRuns";
    private static final String RUN_TAPE_DEPTH_TAG = "RunTapeDepth";
    private static final String PENDING_CHOP_TARGETS_TAG = "PendingChopTargets";
    private static final String QUEUED_CHOP_TARGETS_TAG = "QueuedChopTargets";
    private static final String PENDING_LEAF_REMOVAL_TAG = "PendingLeafRemoval";
    private static final String CURRENT_CHOP_TARGET_TAG = "CurrentChopTarget";
    private static final String CURRENT_CHOP_PROGRESS_TAG = "CurrentChopProgress";
    private static final String CHOP_COOLDOWN_TAG = "ChopCooldown";

    private static final GameProfile PAL_FAKE_PLAYER_PROFILE =
            new GameProfile(UUID.fromString("f27f13ee-1d56-4f31-a322-a4c32701793f"), "create_pp_pal");


    /**
     * Implements ProgrammablePalEntity behavior for the programmable pal feature.
     */
    public ProgrammablePalEntity(EntityType<? extends PathfinderMob> entityTypeIn, Level level) {
        super(entityTypeIn, level);
        setPersistenceRequired();
        insertionDelay = 30;
    }
    /**
     * Implements ProgrammablePalEntity behavior for the programmable pal feature.
     */
    public ProgrammablePalEntity(Level worldIn, Vec3 pos) {
        this(ModEntities.PROGRAMMABLE_PAL_ENTITY.get(), worldIn);
        this.setPos(pos);
        this.refreshDimensions();
    }

    @Override
    public boolean removeWhenFarAway(double distanceToClosestPlayer) {
        return false;
    }

    @Override
    /**
     * Implements dropAllDeathLoot behavior for the programmable pal feature.
     */
    protected void dropAllDeathLoot(ServerLevel p_level, DamageSource damageSource) {
        super.dropAllDeathLoot(p_level, damageSource);
        ItemEntity entityIn = new ItemEntity(level(),getX(),getY(),getZ(), itemStack);
        p_level.addFreshEntity(entityIn);
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty())
                p_level.addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), stack.copy()));
        }
    }

    /**
     * Implements build behavior for the programmable pal feature.
     */
    public static EntityType.Builder<?> build(EntityType.Builder<?> builder){
        @SuppressWarnings("unchecked")
        EntityType.Builder<ProgrammablePalEntity> palBuilder = (EntityType.Builder<ProgrammablePalEntity>) builder;
        return palBuilder.sized(1,1);
    }

    /**
     * Updates internal state through setItem.
     */
    public void setItem(ItemStack item){
        this.itemStack = item;
        refreshDimensions();
    }

    /**
     * Returns data needed by getItem.
     */
    public ItemStack getItem(){
        return this.itemStack;
    }

    /**
     * Updates internal state through setItemStack.
     */
    public void setItemStack(ItemStack itemStack){
        if(itemStack == null) return;
        this.entityData.set(DATA_ITEM_STACK, itemStack);
    }
    /**
     * Returns data needed by getItemStack.
     */
    public ItemStack getItemStack(){
        return this.entityData.get(DATA_ITEM_STACK);
    }

    /**
     * Updates internal state through setHeldTool.
     */
    public void setHeldTool(ItemStack tool) {
        if (tool == null || tool.isEmpty()) {
            this.entityData.set(DATA_HELD_TOOL, ItemStack.EMPTY);
            return;
        }
        ItemStack normalized = tool.copyWithCount(1);
        this.entityData.set(DATA_HELD_TOOL, normalized);
    }

    /**
     * Returns data needed by getHeldTool.
     */
    public ItemStack getHeldTool() {
        return this.entityData.get(DATA_HELD_TOOL);
    }


    public Container getInventory() {
        return inventory;
    }

    private ItemStack getInstructionTape() {
        return inventory.getItem(TAPE_SLOT);
    }

    private ItemStack normalizeTapeSnapshot(ItemStack stack) {
        if (stack.isEmpty())
            return ItemStack.EMPTY;
        return stack.copyWithCount(1);
    }

    private void clearQueuedMoveState() {
        queuedMoveSteps = 0;
        queuedMoveDirectionIndex = 0;
        queuedStepCheckInstructionData = null;
        skipNextStandaloneCheckInstruction = false;
    }

    private void clearActiveRunTapeState() {
        activeRunTapeProgramTag = null;
        activeRunTapeInstructionPointer = 0;
        activeRunTapeRemainingRuns = 0;
    }

    private void resetProgramRuntimeState() {
        instructionPointer = 0;
        instructionCooldown = 0;
        clearQueuedMoveState();
        clearActiveRunTapeState();
        runTapeDepth = 0;
        clearChopTask();
    }

    private Schedule getScheduleFromInstructionTape() {
        ItemStack instructions = getInstructionTape();
        if (instructions.isEmpty())
            return null;
        CompoundTag programTag = instructions.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        if (programTag == null || programTag.isEmpty())
            return null;
        return Schedule.fromTag(level().registryAccess(), programTag);
    }

    private void sanitizeLoadedProgramRuntimeState() {
        Schedule schedule = getScheduleFromInstructionTape();
        if (schedule == null || schedule.entries.isEmpty()) {
            resetProgramRuntimeState();
            return;
        }

        instructionPointer = Mth.clamp(instructionPointer, 0, schedule.entries.size() - 1);
        instructionCooldown = Math.max(0, instructionCooldown);
        queuedMoveSteps = Math.max(0, queuedMoveSteps);

        if (queuedMoveSteps <= 0) {
            queuedStepCheckInstructionData = null;
            skipNextStandaloneCheckInstruction = false;
        }

        if (activeRunTapeProgramTag != null) {
            Schedule nestedSchedule = Schedule.fromTag(level().registryAccess(), activeRunTapeProgramTag);
            if (nestedSchedule.entries.isEmpty()) {
                clearActiveRunTapeState();
            } else {
                activeRunTapeInstructionPointer = Mth.clamp(activeRunTapeInstructionPointer, 0,
                        nestedSchedule.entries.size());
                activeRunTapeRemainingRuns = Math.max(1, activeRunTapeRemainingRuns);
            }
        }

        runTapeDepth = Mth.clamp(runTapeDepth, 0, 4);
        chopCooldown = Math.max(0, chopCooldown);
        currentChopProgress = Math.max(0f, currentChopProgress);
        if (currentChopTarget != null)
            queuedChopTargets.add(currentChopTarget.immutable());
    }

    private long[] toLongArray(Iterable<BlockPos> positions, int size) {
        long[] values = new long[size];
        int index = 0;
        for (BlockPos pos : positions)
            values[index++] = pos.asLong();
        return values;
    }

    private void loadQueuedPositions(long[] packedPositions, ArrayDeque<BlockPos> outQueue) {
        outQueue.clear();
        for (long packed : packedPositions)
            outQueue.addLast(BlockPos.of(packed).immutable());
    }

    private void loadUniquePositions(long[] packedPositions, Set<BlockPos> outSet) {
        outSet.clear();
        for (long packed : packedPositions)
            outSet.add(BlockPos.of(packed).immutable());
    }

    private void captureProgramStartIfNeeded() {
        if (programStartPos == null)
            programStartPos = blockPosition();
        programActiveLastTick = true;
    }

    private void markProgramInactive() {
        programActiveLastTick = false;
    }

    public void resetToProgramStart() {
        resetProgramRuntimeState();
        setTarget(null);
        getNavigation().stop();
        setDeltaMovement(Vec3.ZERO);
        if (programStartPos != null)
            teleportTo(programStartPos.getX() + 0.5D, programStartPos.getY(), programStartPos.getZ() + 0.5D);
    }

    private void syncStateFromInventory() {
        ItemStack tape = getInstructionTape();
        ItemStack normalizedTape = normalizeTapeSnapshot(tape);
        if (!ItemStack.isSameItemSameComponents(lastInstructionTapeSnapshot, normalizedTape)) {
            boolean wasEmpty = lastInstructionTapeSnapshot.isEmpty();
            boolean nowEmpty = normalizedTape.isEmpty();

            resetProgramRuntimeState();

            if (nowEmpty) {
                if (openMenuCount == 0) {
                    programStartPos = null;
                    programActiveLastTick = false;
                }
            } else if (wasEmpty) {
                // Tape was reinserted after being removed: start a fresh anchor even while GUI is open.
                programStartPos = blockPosition();
                programActiveLastTick = false;
            }

            lastInstructionTapeSnapshot = normalizedTape;
        }

        ItemStack equippedTool = getHeldTool();
        if (!equippedTool.isEmpty()) {
            boolean foundMatch = false;
            for (int slot = TOOL_SLOT_START; slot < TOOL_SLOT_END; slot++) {
                ItemStack stack = inventory.getItem(slot);
                if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, equippedTool)) {
                    foundMatch = true;
                    break;
                }
            }
            if (!foundMatch)
                setHeldTool(ItemStack.EMPTY);
        }
    }

    private int findToolSlot(ItemStack target) {
        for (int slot = TOOL_SLOT_START; slot < TOOL_SLOT_END; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (!stack.isEmpty() && ItemStack.isSameItemSameComponents(stack, target))
                return slot;
        }
        return -1;
    }

    private ItemStack firstAxeInInventory() {
        for (int slot = TOOL_SLOT_START; slot < TOOL_SLOT_END; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof AxeItem)
                return stack;
        }
        return ItemStack.EMPTY;
    }

    private ItemStack bestCombatToolInInventory() {
        for (int slot = TOOL_SLOT_START; slot < TOOL_SLOT_END; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof SwordItem)
                return stack;
        }

        for (int slot = TOOL_SLOT_START; slot < TOOL_SLOT_END; slot++) {
            ItemStack stack = inventory.getItem(slot);
            if (stack.getItem() instanceof AxeItem)
                return stack;
        }

        return ItemStack.EMPTY;
    }

    private void openPalMenu(Player player) {
        if (level().isClientSide)
            return;
        MenuProvider provider = new SimpleMenuProvider(
                (id, inv, p) -> new ProgrammablePalMenu(ModMenuTypes.PROGRAMMABLE_PAL_MENU.get(), id, inv, this),
                Component.literal("Programmable Pal")
        );
        player.openMenu(provider, buf -> buf.writeInt(getId()));
    }


    /**
     * Implements registerGoals behavior for the programmable pal feature.
     */



    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2D, true) {
            @Override
            public boolean canUse() {
                return isFightTapeActive() && getTarget() != null && super.canUse();
            }

            @Override
            public boolean canContinueToUse() {
                return isFightTapeActive() && super.canContinueToUse();
            }
        });

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this) {
            @Override
            public boolean canUse() {
                return isFightTapeActive() && super.canUse();
            }
        });
//        this.goalSelector.addGoal(0, new FloatGoal(this) {
//            @Override
//            public boolean canUse() {
//                return !hasActiveInstructionTape() && super.canUse();
//            }
//
//            @Override
//            public boolean canContinueToUse() {
//                return !hasActiveInstructionTape() && super.canContinueToUse();
//            }
//        });
//        this.goalSelector.addGoal(1, new TemptGoal(this, 1.0, stack -> stack.is(AllItems.ANDESITE_ALLOY), false) {
//            @Override
//            public boolean canUse() {
//                return !hasActiveInstructionTape() && super.canUse();
//            }
//
//            @Override
//            public boolean canContinueToUse() {
//                return !hasActiveInstructionTape() && super.canContinueToUse();
//            }
//        });
//        this.goalSelector.addGoal(2, new WaterAvoidingRandomStrollGoal(this, 1.0) {
//            @Override
//            public boolean canUse() {
//                return !hasActiveInstructionTape() && super.canUse();
//            }
//
//            @Override
//            public boolean canContinueToUse() {
//                return !hasActiveInstructionTape() && super.canContinueToUse();
//            }
//        });
//        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 10f) {
//            @Override
//            public boolean canUse() {
//                return !hasActiveInstructionTape() && super.canUse();
//            }
//
//            @Override
//            public boolean canContinueToUse() {
//                return !hasActiveInstructionTape() && super.canContinueToUse();
//            }
//        });
//        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this) {
//            @Override
//            public boolean canUse() {
//                return !hasActiveInstructionTape() && super.canUse();
//            }
//
//            @Override
//            public boolean canContinueToUse() {
//                return !hasActiveInstructionTape() && super.canContinueToUse();
//            }
//        });
    }

    @Override
    /**
     * Returns data needed by getPickedResult.
     */
    public @Nullable ItemStack getPickedResult(HitResult target) {
        return itemStack.copy();
    }

    @Override
    /**
     * Checks the state used by isPushable.
     */
    public boolean isPushable() {
        return !hasActiveInstructionTape();
    }

    @Override
    /**
     * Implements push behavior for the programmable pal feature.
     */
    public void push(Entity entity) {
        if (!hasActiveInstructionTape())
            super.push(entity);
    }

    /**
     * Implements createPalAttributes behavior for the programmable pal feature.
     */
    public static AttributeSupplier.Builder createPalAttributes(){
        return PathfinderMob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.2D)
                .add(Attributes.ATTACK_DAMAGE, 3.0D);
    }


    /**
     * Updates internal state through setupAnimationStates.
     */
    private void setupAnimationStates() {
        if(this.idleAnimationTimeout <= 0) {
            this.idleAnimationTimeout = 100;
            this.idleAnimationState.start(this.tickCount);
        } else {
            --this.idleAnimationTimeout;
        }
    }



    @Override
    /**
     * Implements customServerAiStep behavior for the programmable pal feature.
     */
    protected void customServerAiStep() {
        if (hasActiveInstructionTape())
            return;
        super.customServerAiStep();
    }

    @Override
    /**
     * Handles the tick lifecycle step for this screen/entity.
     */
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
    /**
     * Implements defineSynchedData behavior for the programmable pal feature.
     */
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DOME_COLOR, 0);
        builder.define(DATA_ITEM_STACK, ItemStack.EMPTY);
        builder.define(DATA_HELD_TOOL, ItemStack.EMPTY);
    }

    /**
     * Returns data needed by getTypeVariant.
     */
    private int getTypeVariant() {
        return this.entityData.get(DOME_COLOR);
    }

    /**
     * Returns data needed by getVariant.
     */
    public ProgrammablePalVariant getVariant() {
        return ProgrammablePalVariant.byId(this.getTypeVariant() & 255);
    }

    /**
     * Updates internal state through setVariant.
     */
    public void setVariant(ProgrammablePalVariant style) {
        this.entityData.set(DOME_COLOR, style.getId() & 255);
    }

    private void saveProgramRuntimeState(CompoundTag compound) {
        compound.putInt(INSTRUCTION_POINTER_TAG, instructionPointer);
        compound.putInt(INSTRUCTION_COOLDOWN_TAG, instructionCooldown);
        compound.putInt(QUEUED_MOVE_STEPS_TAG, queuedMoveSteps);
        compound.putInt(QUEUED_MOVE_DIRECTION_INDEX_TAG, queuedMoveDirectionIndex);
        compound.putBoolean(SKIP_NEXT_STANDALONE_CHECK_TAG, skipNextStandaloneCheckInstruction);
        if (queuedStepCheckInstructionData != null)
            compound.put(QUEUED_STEP_CHECK_DATA_TAG, queuedStepCheckInstructionData.copy());
        if (activeRunTapeProgramTag != null)
            compound.put(ACTIVE_RUN_TAPE_PROGRAM_TAG, activeRunTapeProgramTag.copy());
        compound.putInt(ACTIVE_RUN_TAPE_POINTER_TAG, activeRunTapeInstructionPointer);
        compound.putInt(ACTIVE_RUN_TAPE_REMAINING_TAG, activeRunTapeRemainingRuns);
        compound.putInt(RUN_TAPE_DEPTH_TAG, runTapeDepth);
    }

    private void loadProgramRuntimeState(CompoundTag compound) {
        instructionPointer = compound.getInt(INSTRUCTION_POINTER_TAG);
        instructionCooldown = compound.getInt(INSTRUCTION_COOLDOWN_TAG);
        queuedMoveSteps = compound.getInt(QUEUED_MOVE_STEPS_TAG);
        queuedMoveDirectionIndex = compound.getInt(QUEUED_MOVE_DIRECTION_INDEX_TAG);
        skipNextStandaloneCheckInstruction = compound.getBoolean(SKIP_NEXT_STANDALONE_CHECK_TAG);
        queuedStepCheckInstructionData = compound.contains(QUEUED_STEP_CHECK_DATA_TAG)
                ? compound.getCompound(QUEUED_STEP_CHECK_DATA_TAG).copy()
                : null;
        activeRunTapeProgramTag = compound.contains(ACTIVE_RUN_TAPE_PROGRAM_TAG)
                ? compound.getCompound(ACTIVE_RUN_TAPE_PROGRAM_TAG).copy()
                : null;
        activeRunTapeInstructionPointer = compound.getInt(ACTIVE_RUN_TAPE_POINTER_TAG);
        activeRunTapeRemainingRuns = compound.getInt(ACTIVE_RUN_TAPE_REMAINING_TAG);
        runTapeDepth = compound.getInt(RUN_TAPE_DEPTH_TAG);
    }

    private void saveChopState(CompoundTag compound) {
        compound.putLongArray(PENDING_CHOP_TARGETS_TAG, toLongArray(pendingChopTargets, pendingChopTargets.size()));
        compound.putLongArray(QUEUED_CHOP_TARGETS_TAG, toLongArray(queuedChopTargets, queuedChopTargets.size()));
        compound.putLongArray(PENDING_LEAF_REMOVAL_TAG, toLongArray(pendingLeafRemoval, pendingLeafRemoval.size()));
        if (currentChopTarget != null)
            compound.putLong(CURRENT_CHOP_TARGET_TAG, currentChopTarget.asLong());
        compound.putFloat(CURRENT_CHOP_PROGRESS_TAG, currentChopProgress);
        compound.putInt(CHOP_COOLDOWN_TAG, chopCooldown);
    }

    private void loadChopState(CompoundTag compound) {
        loadQueuedPositions(compound.getLongArray(PENDING_CHOP_TARGETS_TAG), pendingChopTargets);
        loadUniquePositions(compound.getLongArray(QUEUED_CHOP_TARGETS_TAG), queuedChopTargets);
        loadUniquePositions(compound.getLongArray(PENDING_LEAF_REMOVAL_TAG), pendingLeafRemoval);
        currentChopTarget = compound.contains(CURRENT_CHOP_TARGET_TAG)
                ? BlockPos.of(compound.getLong(CURRENT_CHOP_TARGET_TAG)).immutable()
                : null;
        currentChopProgress = compound.getFloat(CURRENT_CHOP_PROGRESS_TAG);
        chopCooldown = compound.getInt(CHOP_COOLDOWN_TAG);
    }

    private void saveCommanderAndProgramStartState(CompoundTag compound) {
        if (commanderUUID != null)
            compound.putUUID(COMMANDER_UUID_TAG, commanderUUID);
        compound.putBoolean(FOLLOW_COMMANDER_TAG, followCommander);
        if (!getInstructionTape().isEmpty() && programStartPos != null)
            compound.putLong(PROGRAM_START_POS_TAG, programStartPos.asLong());
    }

    private void loadCommanderAndProgramStartState(CompoundTag compound) {
        commanderUUID = compound.hasUUID(COMMANDER_UUID_TAG) ? compound.getUUID(COMMANDER_UUID_TAG) : null;
        followCommander = !compound.contains(FOLLOW_COMMANDER_TAG) || compound.getBoolean(FOLLOW_COMMANDER_TAG);
        programStartPos = compound.contains(PROGRAM_START_POS_TAG)
                ? BlockPos.of(compound.getLong(PROGRAM_START_POS_TAG))
                : null;
    }

    @Override
    /**
     * Implements addAdditionalSaveData behavior for the programmable pal feature.
     */
    public void addAdditionalSaveData(CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putInt("Variant", this.getTypeVariant());
        compound.put("item", itemStack.saveOptional(level().registryAccess()));
        ContainerHelper.saveAllItems(compound, inventory.getItems(), level().registryAccess());
        compound.put("HeldTool", getHeldTool().saveOptional(level().registryAccess()));
        saveProgramRuntimeState(compound);
        saveChopState(compound);
        saveCommanderAndProgramStartState(compound);
    }

    @Override
    /**
     * Implements readAdditionalSaveData behavior for the programmable pal feature.
     */
    public void readAdditionalSaveData(CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(DOME_COLOR, compound.getInt("Variant"));
        itemStack = ItemStack.parseOptional(level().registryAccess(), compound.getCompound("item"));
        ContainerHelper.loadAllItems(compound, inventory.getItems(), level().registryAccess());
        setHeldTool(ItemStack.parseOptional(level().registryAccess(), compound.getCompound("HeldTool")));
        lastInstructionTapeSnapshot = normalizeTapeSnapshot(getInstructionTape());
        syncStateFromInventory();
        loadProgramRuntimeState(compound);
        loadChopState(compound);
        loadCommanderAndProgramStartState(compound);
        sanitizeLoadedProgramRuntimeState();
        if (getInstructionTape().isEmpty()) {
            programStartPos = null;
            programActiveLastTick = false;
        }
    }



    @Override
    /**
     * Implements writeSpawnData behavior for the programmable pal feature.
     */
    public void writeSpawnData(RegistryFriendlyByteBuf buffer) {
        ItemStack.STREAM_CODEC.encode(buffer, getItem());
        Vec3 motion = getDeltaMovement();
        buffer.writeFloat((float) motion.x);
        buffer.writeFloat((float) motion.y);
        buffer.writeFloat((float) motion.z);
    }

    @Override
    /**
     * Implements readSpawnData behavior for the programmable pal feature.
     */
    public void readSpawnData(RegistryFriendlyByteBuf additionalData) {
        setItem(ItemStack.STREAM_CODEC.decode(additionalData));
        setDeltaMovement(
                additionalData.readFloat(),
                additionalData.readFloat(),
                additionalData.readFloat());
    }


    @Override
    /**
     * Implements mobInteract behavior for the programmable pal feature.
     */
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (!level().isClientSide) {
            if (player.isShiftKeyDown() && isFightTapeActive()) {
                if (commanderUUID == null || commanderUUID.equals(player.getUUID())) {
                    commanderUUID = player.getUUID();
                    followCommander = !followCommander;
                    player.displayClientMessage(Component.literal(followCommander
                            ? "Pal now follows you."
                            : "Pal no longer follows you."), true);
                }
                return InteractionResult.SUCCESS;
            }

            commanderUUID = player.getUUID();
            openPalMenu(player);
        }
        return InteractionResult.sidedSuccess(level().isClientSide);
    }

    private boolean isFightTapeActive() {
        ItemStack tape = getInstructionTape();
        if (tape.isEmpty())
            return false;
        return "fight_tape".equals(BuiltInRegistries.ITEM.getKey(tape.getItem()).getPath());
    }

    private void runFightTapeBehavior() {
        if (!(level() instanceof ServerLevel serverLevel))
            return;

        ItemStack combatTool = bestCombatToolInInventory();
        if (!combatTool.isEmpty())
            setHeldTool(combatTool);
        else
            setHeldTool(ItemStack.EMPTY);

        Player commander = commanderUUID == null ? null : serverLevel.getPlayerByUUID(commanderUUID);
        if (commander == null)
            return;

        LivingEntity target = commander.getLastHurtMob();
        if (target == null || !target.isAlive() || target.distanceToSqr(this) > 256.0D)
            target = commander.getLastHurtByMob();

        if (target != null && target.isAlive() && target != this) {
            setTarget(target);
        } else if (followCommander) {
            double distSq = distanceToSqr(commander);
            if (distSq > 9.0D)
                getNavigation().moveTo(commander, 1.1D);
            else if (distSq < 4.0D)
                getNavigation().stop();
        }
    }

    /**
     * Checks the state used by hasActiveInstructionTape.
     */
    private boolean hasActiveInstructionTape() {
        if (isFightTapeActive())
            return false;
        ItemStack instructions = getInstructionTape();
        if (instructions.isEmpty())
            return false;
        CompoundTag programTag = instructions.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        return programTag != null && !programTag.isEmpty();
    }

    /**
     * Executes runtime logic for runProgramTick.
     */
    private void runProgramTick() {
        if (isFightTapeActive()) {
            markProgramInactive();
            clearQueuedMoveState();
            clearActiveRunTapeState();
            clearChopTask();
            runFightTapeBehavior();
            return;
        }

        if (!hasActiveInstructionTape()) {
            markProgramInactive();
            clearQueuedMoveState();
            clearActiveRunTapeState();
            clearChopTask();
            return;
        }

        captureProgramStartIfNeeded();

        if (tickChopTask())
            return;

        if (instructionCooldown > 0)
            instructionCooldown--;
        if (instructionCooldown > 0)
            return;

        if (queuedMoveSteps > 0) {
            if (executeQueuedMoveStep()) {
                queuedMoveSteps--;
                if (queuedStepCheckInstructionData != null)
                    executeCheckBlock(queuedStepCheckInstructionData);
                instructionCooldown = 10;
            } else {
                // Re-run the linked check when a move step gets blocked to react to world changes
                // (e.g. a sapling growing into a log between check and movement).
                if (queuedStepCheckInstructionData != null)
                    executeCheckBlock(queuedStepCheckInstructionData);
                instructionCooldown = 10;
            }

            if (queuedMoveSteps <= 0)
                queuedStepCheckInstructionData = null;
            return;
        }

        if (activeRunTapeProgramTag != null) {
            boolean finishedSubTape = executeActiveRunTapeStep();
            if (finishedSubTape)
                advanceMainInstructionPointer();
            instructionCooldown = 10;
            return;
        }

        ItemStack instructions = getInstructionTape();
        CompoundTag programTag = instructions.get(ModDataComponentTypes.VOID_FUNCTION_DATA);

        Schedule schedule = Schedule.fromTag(level().registryAccess(), programTag);
        if (schedule.entries.isEmpty())
            return;

        if (instructionPointer >= schedule.entries.size())
            instructionPointer = 0;

        ScheduleEntry entry = schedule.entries.get(instructionPointer);
        CompoundTag entryData = entry.instruction.getData();
        String actionKey = entryData.getString(ACTION_KEY_TAG);

        boolean advancePointer = true;
        if (skipNextStandaloneCheckInstruction && "check_block".equals(actionKey)) {
            skipNextStandaloneCheckInstruction = false;
        } else {
            advancePointer = executeInstruction(entry.instruction, schedule);
        }

        if (advancePointer)
            advanceMainInstructionPointer();

        instructionCooldown = 10;
    }

    /**
     * Advances the active chop job by one game tick using player-like break progress.
     */
    private boolean tickChopTask() {
        if (chopCooldown > 0) {
            chopCooldown--;
            return true;
        }

        if (!acquireNextChopTarget())
            return false;

        if (currentChopTarget == null)
            return false;

        ItemStack tool = getHeldTool();
        if (tool.isEmpty() || !(tool.getItem() instanceof AxeItem)) {
            tool = firstAxeInInventory();
            if (tool.isEmpty()) {
                clearChopTask();
                return false;
            }
            setHeldTool(tool);
        }

        int toolSlot = findToolSlot(tool);

        BlockState state = level().getBlockState(currentChopTarget);
        if (state.isAir() || state.getDestroySpeed(level(), currentChopTarget) < 0) {
            finishCurrentChopTarget();
            return true;
        }

        float hardness = Math.max(0.1f, state.getDestroySpeed(level(), currentChopTarget));
        float toolSpeed = Math.max(1f, tool.getDestroySpeed(state));
        currentChopProgress += toolSpeed / (hardness * 30f);

        int breakStage = Mth.clamp((int) (currentChopProgress * 10f) - 1, 0, 9);
        level().destroyBlockProgress(getId(), currentChopTarget, breakStage);

        if (currentChopProgress >= 1f) {
            breakBlockAndStoreDrops(currentChopTarget, state, tool);
            tool.hurtAndBreak(1, this, EquipmentSlot.MAINHAND);
            if (toolSlot >= 0)
                inventory.setItem(toolSlot, tool.copy());

            if (tool.isEmpty()) {
                setHeldTool(ItemStack.EMPTY);
                clearChopTask();
                return false;
            }

            setHeldTool(tool);
            finishCurrentChopTarget();
            chopCooldown = 4;
        }

        return true;
    }

    private boolean isAdjacentChopReach(BlockPos target) {
        BlockPos palPos = blockPosition();
        int dx = Math.abs(target.getX() - palPos.getX());
        int dy = Math.abs(target.getY() - palPos.getY());
        int dz = Math.abs(target.getZ() - palPos.getZ());
        return dx <= 1 && dy <= 1 && dz <= 1;
    }

    private boolean queueBlockingLeavesTowardsTarget(BlockPos target) {
        BlockPos palPos = blockPosition();
        int stepX = Integer.compare(target.getX(), palPos.getX());
        int stepZ = Integer.compare(target.getZ(), palPos.getZ());

        BlockPos cursor = palPos;
        boolean queuedAny = false;

        for (int i = 0; i < 3; i++) {
            cursor = cursor.offset(stepX, 0, stepZ);
            if (level().getBlockState(cursor).is(BlockTags.LEAVES)) {
                queueChopTarget(cursor);
                queuedAny = true;
            }

            BlockPos above = cursor.above();
            if (level().getBlockState(above).is(BlockTags.LEAVES)) {
                queueChopTarget(above);
                queuedAny = true;
            }
        }

        return queuedAny;
    }

    /**
     * Ensures there is an active target while skipping blocks that are already gone.
     */
    private boolean acquireNextChopTarget() {
        while (currentChopTarget == null && !pendingChopTargets.isEmpty()) {
            BlockPos candidate = pendingChopTargets.removeFirst();
            BlockState candidateState = level().getBlockState(candidate);
            if (candidateState.isAir()) {
                queuedChopTargets.remove(candidate);
                continue;
            }

            if (candidateState.is(BlockTags.LOGS) && !isAdjacentChopReach(candidate)) {
                boolean queuedLeaves = queueBlockingLeavesTowardsTarget(candidate);
                if (queuedLeaves) {
                    pendingChopTargets.addLast(candidate);
                    continue;
                }
            }

            currentChopTarget = candidate;
            currentChopProgress = 0f;
        }

        if (currentChopTarget != null)
            return true;

        removePendingLeaves();
        queuedChopTargets.clear();
        return false;
    }

    /**
     * Clears block-crack visuals and resets the current chopping target.
     */
    private void finishCurrentChopTarget() {
        if (currentChopTarget != null) {
            level().destroyBlockProgress(getId(), currentChopTarget, -1);
            queuedChopTargets.remove(currentChopTarget);
        }
        currentChopTarget = null;
        currentChopProgress = 0f;
    }

    /**
     * Clears any queued chop work when tape execution stops or changes context.
     */
    private void clearChopTask() {
        finishCurrentChopTarget();
        pendingChopTargets.clear();
        queuedChopTargets.clear();
        pendingLeafRemoval.clear();
        chopCooldown = 0;
    }

    private void advanceMainInstructionPointer() {
        ItemStack instructions = getInstructionTape();
        CompoundTag programTag = instructions.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        if (programTag == null || programTag.isEmpty()) {
            instructionPointer = 0;
            return;
        }

        Schedule schedule = Schedule.fromTag(level().registryAccess(), programTag);
        if (schedule.entries.isEmpty()) {
            instructionPointer = 0;
            return;
        }

        instructionPointer++;
        if (instructionPointer >= schedule.entries.size())
            instructionPointer = 0;
    }

    /**
     * Executes runtime logic for executeInstruction.
     */
    private boolean executeInstruction(ScheduleInstruction instruction, Schedule schedule) {
        CompoundTag data = instruction.getData();
        String action = data.getString(ACTION_KEY_TAG);


        if ("check_block".equals(action)) {
            executeCheckBlock(data);
            return true;
        }

        if ("has_item".equals(action)) {
            executeHasItem(data);
            return true;
        }

        if ("rotate".equals(action)) {
            executeRotate(data);
            return true;
        }

        if ("run_tape".equals(action)) {
            if (runTapeDepth > 0) {
                executeRunTapeImmediate(data);
                return true;
            }

            startRunTape(data);
            if (activeRunTapeProgramTag == null)
                return true;
            return executeActiveRunTapeStep();
        }

        if ("interact".equals(action)) {
            executeInteract(data);
            return true;
        }

        executeMoveForward(data, schedule);
        return true;
    }

    private void executeRotate(CompoundTag data) {
        int option = data.getInt(ROTATE_OPTION_INDEX_TAG);
        Direction target = switch (option) {
            case 0 -> getDirection().getClockWise();
            case 1 -> getDirection().getCounterClockWise();
            case 2 -> Direction.NORTH;
            case 3 -> Direction.EAST;
            case 4 -> Direction.SOUTH;
            case 5 -> Direction.WEST;
            default -> getDirection();
        };

        setYRot(target.toYRot());
        setYHeadRot(target.toYRot());
        yBodyRot = target.toYRot();
        getNavigation().stop();
    }

    /**
     * Executes runtime logic for executeMoveForward.
     */
    private void executeMoveForward(CompoundTag data, Schedule schedule) {
        queuedMoveSteps = Math.max(1, data.getInt(MOVE_DISTANCE_INDEX_TAG) + 1);
        queuedStepCheckInstructionData = null;
        skipNextStandaloneCheckInstruction = false;

        int nextIndex = instructionPointer + 1;
        if (nextIndex >= schedule.entries.size())
            nextIndex = 0;

        if (!schedule.entries.isEmpty()) {
            ScheduleInstruction nextInstruction = schedule.entries.get(nextIndex).instruction;
            CompoundTag nextData = nextInstruction.getData();
            if (data.getBoolean(MOVE_STEP_CHECK_LINK_TAG) && "check_block".equals(nextData.getString(ACTION_KEY_TAG))) {
                queuedStepCheckInstructionData = nextData.copy();
                skipNextStandaloneCheckInstruction = true;
            }
        }

        if (executeQueuedMoveStep()) {
            queuedMoveSteps--;
            if (queuedStepCheckInstructionData != null)
                executeCheckBlock(queuedStepCheckInstructionData);
        }
    }

    /**
     * Checks whether the Pal can occupy the given block position like a player would.
     */
    private boolean canMoveThrough(BlockPos pos) {
        BlockState state = level().getBlockState(pos);
        if (state.is(BlockTags.LEAVES))
            return true;
        return state.getCollisionShape(level(), pos).isEmpty();
    }

    /**
     * Resolves the block occupied by the Pal's feet for relative target checks/movement.
     */
    private BlockPos getFeetReferencePos() {
        BlockPos base = blockPosition();
        return canMoveThrough(base) ? base : base.above();
    }

    private boolean executeQueuedMoveStep() {
        getNavigation().stop();

        Direction direction = getDirection();

        setYRot(direction.toYRot());
        setYHeadRot(direction.toYRot());
        yBodyRot = direction.toYRot();

        BlockPos currentFeet = getFeetReferencePos();
        BlockPos nextFeet = currentFeet.relative(direction);

        if (canMoveThrough(nextFeet)) {
            setPos(nextFeet.getX() + 0.5D, nextFeet.getY(), nextFeet.getZ() + 0.5D);
            return true;
        }

        // Allow only tiny step-ups (e.g. farmland -> full block), but not full 1-block climbs.
        BlockPos nextGround = nextFeet.below();
        BlockState nextState = level().getBlockState(nextGround);
        BlockPos steppedFeet = nextGround.above();
        double nextCollisionTop = nextState.getCollisionShape(level(), nextGround).max(Direction.Axis.Y);
        double stepHeight = (nextGround.getY() + nextCollisionTop) - getY();

        boolean canSmallStep = !canMoveThrough(nextGround)
                && canMoveThrough(steppedFeet)
                && nextCollisionTop > 0.0D
                && nextCollisionTop <= 1.0D
                && stepHeight > 0.0D
                && stepHeight <= 0.2D;

        if (!canSmallStep)
            return false;

        setPos(nextGround.getX() + 0.5D, getY() + stepHeight, nextGround.getZ() + 0.5D);
        return true;
    }

    private void startRunTape(CompoundTag data) {
        if (!data.contains(RUN_TAPE_ITEM_TAG)) {
            clearActiveRunTapeState();
            return;
        }

        ItemStack nestedTape = ItemStack.parseOptional(level().registryAccess(), data.getCompound(RUN_TAPE_ITEM_TAG));
        if (nestedTape.isEmpty() || nestedTape.getItem() != ModItems.PROGRAMMABLE_TAPE.get()) {
            clearActiveRunTapeState();
            return;
        }

        CompoundTag nestedProgramTag = nestedTape.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        if (nestedProgramTag == null || nestedProgramTag.isEmpty()) {
            clearActiveRunTapeState();
            return;
        }

        Schedule nestedSchedule = Schedule.fromTag(level().registryAccess(), nestedProgramTag);
        if (nestedSchedule.entries.isEmpty()) {
            clearActiveRunTapeState();
            return;
        }

        activeRunTapeProgramTag = nestedProgramTag.copy();
        activeRunTapeInstructionPointer = 0;
        activeRunTapeRemainingRuns = Math.max(1, data.getInt(RUN_TAPE_REPEAT_COUNT_TAG) + 1);
    }

    private boolean executeActiveRunTapeStep() {
        if (activeRunTapeProgramTag == null)
            return true;

        Schedule nestedSchedule = Schedule.fromTag(level().registryAccess(), activeRunTapeProgramTag);
        if (nestedSchedule.entries.isEmpty()) {
            clearActiveRunTapeState();
            return true;
        }

        if (activeRunTapeInstructionPointer >= nestedSchedule.entries.size()) {
            clearActiveRunTapeState();
            return true;
        }

        ScheduleInstruction nestedInstruction = nestedSchedule.entries.get(activeRunTapeInstructionPointer).instruction;
        activeRunTapeInstructionPointer++;

        runTapeDepth++;
        try {
            executeInstruction(nestedInstruction, nestedSchedule);
        } finally {
            runTapeDepth--;
        }

        if (activeRunTapeInstructionPointer >= nestedSchedule.entries.size()) {
            if (activeRunTapeRemainingRuns > 1) {
                activeRunTapeRemainingRuns--;
                activeRunTapeInstructionPointer = 0;
                return false;
            }
            clearActiveRunTapeState();
            return true;
        }

        return false;
    }

    private void executeRunTapeImmediate(CompoundTag data) {
        if (runTapeDepth > 4 || !data.contains(RUN_TAPE_ITEM_TAG))
            return;

        ItemStack nestedTape = ItemStack.parseOptional(level().registryAccess(), data.getCompound(RUN_TAPE_ITEM_TAG));
        if (nestedTape.isEmpty() || nestedTape.getItem() != ModItems.PROGRAMMABLE_TAPE.get())
            return;

        CompoundTag nestedProgramTag = nestedTape.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        if (nestedProgramTag == null || nestedProgramTag.isEmpty())
            return;

        Schedule nestedSchedule = Schedule.fromTag(level().registryAccess(), nestedProgramTag);
        if (nestedSchedule.entries.isEmpty())
            return;

        int repeatCount = Math.max(1, data.getInt(RUN_TAPE_REPEAT_COUNT_TAG) + 1);

        runTapeDepth++;
        try {
            for (int run = 0; run < repeatCount; run++) {
                for (ScheduleEntry entry : nestedSchedule.entries)
                    executeInstruction(entry.instruction, nestedSchedule);
            }
        } finally {
            runTapeDepth--;
        }
    }

    private void executeHasItem(CompoundTag data) {
        if (!data.contains(HAS_ITEM_MATCH_ITEM_TAG))
            return;

        ItemStack configured = ItemStack.parseOptional(level().registryAccess(), data.getCompound(HAS_ITEM_MATCH_ITEM_TAG));
        if (configured.isEmpty())
            return;

        int inventorySlot = findMatchingInventorySlot(configured);
        if (inventorySlot < 0)
            return;

        ItemStack stackToUse = inventory.getItem(inventorySlot);
        if (stackToUse.isEmpty())
            return;

        if ("use".equals(data.getString(HAS_ITEM_ACTION_KEY_TAG))) {
            int targetIndex = data.getInt(HAS_ITEM_TARGET_INDEX_TAG);
            useInventoryItemOnTarget(inventorySlot, stackToUse, targetIndex);
        }
    }

    private void executeInteract(CompoundTag data) {
        String target = data.getString(INTERACT_TARGET_KEY_TAG);
        if (!target.isEmpty() && !"storage".equals(target))
            return;

        if (!data.contains(INTERACT_FILTER_ITEM_TAG))
            return;

        ItemStack filterItem = ItemStack.parseOptional(level().registryAccess(), data.getCompound(INTERACT_FILTER_ITEM_TAG));
        if (filterItem.isEmpty())
            return;

        Container storage = getFrontStorageContainer();
        if (storage == null)
            return;

        FilterItemStack transferFilter = FilterItemStack.of(filterItem.copy());
        String mode = data.getString(INTERACT_MODE_KEY_TAG);
        ItemStack keepItem = ItemStack.EMPTY;
        if (data.contains(INTERACT_KEEP_ITEM_TAG))
            keepItem = ItemStack.parseOptional(level().registryAccess(), data.getCompound(INTERACT_KEEP_ITEM_TAG));

        if ("pull".equals(mode)) {
            pullFilteredItemsFromStorage(storage, transferFilter, keepItem);
            return;
        }

        pushFilteredItemsToStorage(storage, transferFilter, keepItem);
    }

    private @Nullable Container getFrontStorageContainer() {
        BlockPos storagePos = blockPosition().relative(getDirection());
        BlockState storageState = level().getBlockState(storagePos);

        if (storageState.getBlock() instanceof ChestBlock chestBlock) {
            Container chestContainer = ChestBlock.getContainer(chestBlock, storageState, level(), storagePos, true);
            if (chestContainer != null)
                return chestContainer;
        }

        BlockEntity blockEntity = level().getBlockEntity(storagePos);
        if (blockEntity instanceof Container container)
            return container;
        return null;
    }

    private void pullFilteredItemsFromStorage(Container storage, FilterItemStack transferFilter, ItemStack keepItem) {
        FilterItemStack keepFilter = keepItem.isEmpty() ? null : FilterItemStack.of(keepItem.copy());
        int keepCurrentCount = keepFilter != null ? countPalItemsMatching(keepFilter) : 0;

        boolean changed = false;
        for (int slot = 0; slot < storage.getContainerSize(); slot++) {
            ItemStack source = storage.getItem(slot);
            if (source.isEmpty())
                continue;
            if (!transferFilter.test(level(), source))
                continue;

            boolean isKeepFilteredItem = keepFilter != null && keepFilter.test(level(), source);
            int movable = source.getCount();
            if (isKeepFilteredItem) {
                int keepGoalForMatchedItem = source.getMaxStackSize();
                int keepRemaining = Math.max(0, keepGoalForMatchedItem - keepCurrentCount);
                if (keepRemaining <= 0)
                    continue;
                movable = Math.min(movable, keepRemaining);
            }

            if (movable <= 0)
                continue;

            ItemStack toMove = source.copyWithCount(movable);
            ItemStack remainder = insertIntoPalStorage(toMove);
            int moved = movable - remainder.getCount();
            if (moved <= 0)
                continue;

            source.shrink(moved);
            storage.setItem(slot, source.isEmpty() ? ItemStack.EMPTY : source);
            changed = true;

            if (isKeepFilteredItem)
                keepCurrentCount += moved;
        }

        if (changed) {
            storage.setChanged();
            inventory.setChanged();
        }
    }

    private void pushFilteredItemsToStorage(Container storage, FilterItemStack transferFilter, ItemStack keepItem) {
        FilterItemStack keepFilter = keepItem.isEmpty() ? null : FilterItemStack.of(keepItem.copy());
        int keepCurrentCount = keepFilter != null ? countPalItemsMatching(keepFilter) : 0;

        boolean changed = false;
        for (int slot = TOOL_SLOT_START; slot < TOOL_SLOT_END; slot++) {
            ItemStack source = inventory.getItem(slot);
            if (source.isEmpty())
                continue;
            if (!transferFilter.test(level(), source))
                continue;

            boolean isKeepFilteredItem = keepFilter != null && keepFilter.test(level(), source);
            int movable = source.getCount();
            if (isKeepFilteredItem) {
                int keepGoalForMatchedItem = source.getMaxStackSize();
                int excess = Math.max(0, keepCurrentCount - keepGoalForMatchedItem);
                movable = Math.min(movable, excess);
            }

            if (movable <= 0)
                continue;

            ItemStack toMove = source.copyWithCount(movable);
            ItemStack remainder = insertIntoContainer(storage, toMove);
            int moved = movable - remainder.getCount();
            if (moved <= 0)
                continue;

            source.shrink(moved);
            inventory.setItem(slot, source.isEmpty() ? ItemStack.EMPTY : source);
            changed = true;

            if (isKeepFilteredItem)
                keepCurrentCount = Math.max(0, keepCurrentCount - moved);
        }

        if (changed) {
            storage.setChanged();
            inventory.setChanged();
        }
    }

    private ItemStack insertIntoPalStorage(ItemStack stack) {
        ItemStack remaining = stack.copy();

        for (int slot = TOOL_SLOT_START; slot < TOOL_SLOT_END && !remaining.isEmpty(); slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (existing.isEmpty())
                continue;
            if (!ItemStack.isSameItemSameComponents(existing, remaining))
                continue;

            int maxSize = Math.min(existing.getMaxStackSize(), inventory.getMaxStackSize());
            int free = maxSize - existing.getCount();
            if (free <= 0)
                continue;

            int moved = Math.min(free, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
            inventory.setItem(slot, existing);
        }

        for (int slot = TOOL_SLOT_START; slot < TOOL_SLOT_END && !remaining.isEmpty(); slot++) {
            ItemStack existing = inventory.getItem(slot);
            if (!existing.isEmpty())
                continue;

            int moved = Math.min(Math.min(remaining.getMaxStackSize(), inventory.getMaxStackSize()), remaining.getCount());
            ItemStack placed = remaining.copyWithCount(moved);
            inventory.setItem(slot, placed);
            remaining.shrink(moved);
        }

        return remaining;
    }

    private ItemStack insertIntoContainer(Container storage, ItemStack stack) {
        ItemStack remaining = stack.copy();

        for (int slot = 0; slot < storage.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = storage.getItem(slot);
            if (existing.isEmpty())
                continue;
            if (!ItemStack.isSameItemSameComponents(existing, remaining))
                continue;
            if (!storage.canPlaceItem(slot, remaining))
                continue;

            int maxSize = Math.min(existing.getMaxStackSize(), storage.getMaxStackSize());
            int free = maxSize - existing.getCount();
            if (free <= 0)
                continue;

            int moved = Math.min(free, remaining.getCount());
            existing.grow(moved);
            remaining.shrink(moved);
            storage.setItem(slot, existing);
        }

        for (int slot = 0; slot < storage.getContainerSize() && !remaining.isEmpty(); slot++) {
            ItemStack existing = storage.getItem(slot);
            if (!existing.isEmpty())
                continue;
            if (!storage.canPlaceItem(slot, remaining))
                continue;

            int moved = Math.min(Math.min(remaining.getMaxStackSize(), storage.getMaxStackSize()), remaining.getCount());
            ItemStack placed = remaining.copyWithCount(moved);
            storage.setItem(slot, placed);
            remaining.shrink(moved);
        }

        return remaining;
    }

    private int countPalItemsMatching(@Nullable FilterItemStack filter) {
        if (filter == null)
            return 0;

        int total = 0;
        for (int slot = TOOL_SLOT_START; slot < TOOL_SLOT_END; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty())
                continue;
            if (filter.test(level(), candidate))
                total += candidate.getCount();
        }
        return total;
    }
    private int findMatchingInventorySlot(ItemStack configured) {
        FilterItemStack configuredFilter = FilterItemStack.of(configured.copy());
        for (int slot = TOOL_SLOT_START; slot < TOOL_SLOT_END; slot++) {
            ItemStack candidate = inventory.getItem(slot);
            if (candidate.isEmpty())
                continue;
            if (configuredFilter.test(level(), candidate))
                return slot;
        }
        return -1;
    }

    private void useInventoryItemOnTarget(int inventorySlot, ItemStack stackToUse, int targetIndex) {
        if (!(level() instanceof ServerLevel serverLevel))
            return;

        BlockPos targetPos = getCheckTargetPosition(targetIndex);
        Direction face = switch (targetIndex) {
            case 1 -> Direction.DOWN;
            case 2 -> getDirection().getOpposite();
            default -> Direction.UP;
        };

        FakePlayer fakePlayer = FakePlayerFactory.get(serverLevel, PAL_FAKE_PLAYER_PROFILE);
        fakePlayer.moveTo(getX(), getY(), getZ(), getYRot(), getXRot());

        ItemStack handStack = stackToUse.copy();
        fakePlayer.setItemInHand(InteractionHand.MAIN_HAND, handStack);

        BlockPos attemptedTargetPos = targetPos;
        InteractionResult result = InteractionResult.PASS;

        BlockPos[] targetsToTry = face == Direction.UP
                ? new BlockPos[]{targetPos, targetPos.above()}
                : new BlockPos[]{targetPos};

        for (BlockPos candidateTargetPos : targetsToTry) {
            Vec3 hitCenter = getInteractionHitPosition(candidateTargetPos, face);
            BlockHitResult hitResult = new BlockHitResult(hitCenter, face, candidateTargetPos, false);
            UseOnContext context = new UseOnContext(fakePlayer, InteractionHand.MAIN_HAND, hitResult);

            result = fakePlayer.gameMode.useItemOn(fakePlayer, serverLevel, handStack,
                    InteractionHand.MAIN_HAND, hitResult);
            if (!result.consumesAction())
                result = handStack.useOn(context);

            attemptedTargetPos = candidateTargetPos;
            if (result.consumesAction())
                break;
        }

        if (!result.consumesAction())
            result = handStack.use(serverLevel, fakePlayer, InteractionHand.MAIN_HAND).getResult();

        if (!result.consumesAction()) {
            BlockState targetState = level().getBlockState(attemptedTargetPos);
            BlockState aboveState = level().getBlockState(attemptedTargetPos.above());
//            level().players().forEach(p -> p.displayClientMessage(Component.literal(
//                    "Pal Use failed: item=" + stackToUse.getHoverName().getString()
//                            + " target=" + targetState.getBlock().getName().getString()
//                            + " above=" + aboveState.getBlock().getName().getString()
//                            + " face=" + face), false));
        }

        ItemStack updated = fakePlayer.getItemInHand(InteractionHand.MAIN_HAND);
        inventory.setItem(inventorySlot, updated.copy());
        setHeldTool(updated);
    }
    private Vec3 getInteractionHitPosition(BlockPos targetPos, Direction face) {
        VoxelShape shape = level().getBlockState(targetPos).getShape(level(), targetPos);
        if (shape.isEmpty()) {
            return Vec3.atCenterOf(targetPos)
                    .add(face.getStepX() * 0.25D, face.getStepY() * 0.25D, face.getStepZ() * 0.25D);
        }

        double epsilon = 0.01D;
        double x = targetPos.getX() + 0.5D;
        double y = targetPos.getY() + 0.5D;
        double z = targetPos.getZ() + 0.5D;

        switch (face) {
            case UP -> y = targetPos.getY() + Math.min(1D - epsilon, shape.max(Direction.Axis.Y) + epsilon);
            case DOWN -> y = targetPos.getY() + Math.min(1D - epsilon, shape.min(Direction.Axis.Y) + epsilon);
            case NORTH -> z = targetPos.getZ() + Math.min(1D - epsilon, shape.min(Direction.Axis.Z) + epsilon);
            case SOUTH -> z = targetPos.getZ() + Math.max(epsilon, shape.max(Direction.Axis.Z) - epsilon);
            case WEST -> x = targetPos.getX() + Math.min(1D - epsilon, shape.min(Direction.Axis.X) + epsilon);
            case EAST -> x = targetPos.getX() + Math.max(epsilon, shape.max(Direction.Axis.X) - epsilon);
        }

        return new Vec3(x, y, z);
    }

    /**
     * Executes runtime logic for executeCheckBlock.
     */
    private void executeCheckBlock(CompoundTag data) {
        int targetIndex = data.getInt(CHECK_BLOCK_TARGET_INDEX_TAG);
        BlockPos checkPos = getCheckTargetPosition(targetIndex);
        BlockState state = level().getBlockState(checkPos);

        //broadcastCheckResult(targetIndex, state);

        if (!matchesConfiguredCheckBlockItem(data, checkPos, state))
            return;

        applyCheckBlockMatchAction(data.getString(CHECK_BLOCK_MATCH_ACTION_KEY_TAG), checkPos, state);
    }

    /**
     * Resolves the block position used by check-block based on target index.
     */
    private BlockPos getCheckTargetPosition(int targetIndex) {
        BlockPos feetPos = getFeetReferencePos();
        return switch (targetIndex) {
            case 1 -> feetPos.above();
            case 2 -> feetPos.relative(getDirection());
            default -> feetPos.below();
        };
    }

    /**
     * Sends a small debug message about what block was inspected.
     */
    private void broadcastCheckResult(int targetIndex, BlockState state) {
        String targetName = switch (targetIndex) {
            case 1 -> "Above";
            case 2 -> "Front";
            default -> "Below";
        };

        level().players().forEach(p -> p.displayClientMessage(
                Component.literal("Pal Check " + targetName + ": " + state.getBlock().getName().getString()), false));
    }

    private void collectBlockDropsToInventory(BlockPos blockPos, BlockState state, ItemStack tool) {
        if (!(level() instanceof ServerLevel serverLevel))
            return;

        LootParams.Builder lootBuilder = new LootParams.Builder(serverLevel)
                .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(blockPos))
                .withParameter(LootContextParams.TOOL, tool)
                .withOptionalParameter(LootContextParams.THIS_ENTITY, this);

        for (ItemStack drop : state.getDrops(lootBuilder)) {
            ItemStack remainder = inventory.addItem(drop.copy());
            if (!remainder.isEmpty())
                level().addFreshEntity(new ItemEntity(level(), getX(), getY(), getZ(), remainder));
        }
    }

    private void breakBlockAndStoreDrops(BlockPos blockPos, BlockState state, ItemStack tool) {
        if (state.isAir())
            return;
        collectBlockDropsToInventory(blockPos, state, tool);
        level().destroyBlock(blockPos, false, this);
    }

    /**
     * Applies the configured follow-up action after a successful check-block match.
     */
    private void applyCheckBlockMatchAction(String matchAction, BlockPos checkPos, BlockState state) {
        if ("chop".equals(matchAction)) {
            ItemStack tool = getHeldTool();
            if (!(tool.getItem() instanceof AxeItem))
                tool = firstAxeInInventory();
            if (!(tool.getItem() instanceof AxeItem)) {
                level().players().forEach(p -> p.displayClientMessage(Component.literal("Pal needs an axe in its inventory to chop."), true));
                return;
            }
            setHeldTool(tool);
            mineTreeOrBlock(checkPos, state);
            return;
        }

        if ("harvest".equals(matchAction)) {
            executeHarvestAction(checkPos, state);
            return;
        }
    }

    private void executeHarvestAction(BlockPos checkPos, BlockState state) {
        if (!isHarvestableCrop(state))
            return;

        breakBlockAndStoreDrops(checkPos, state, ItemStack.EMPTY);
    }

    private boolean isHarvestableCrop(BlockState state) {
        if (state.isAir() || !state.is(BlockTags.CROPS))
            return false;

        if (state.getBlock() instanceof CropBlock cropBlock)
            return cropBlock.isMaxAge(state);

        if (state.hasProperty(BlockStateProperties.AGE_1))
            return state.getValue(BlockStateProperties.AGE_1) >= 1;
        if (state.hasProperty(BlockStateProperties.AGE_2))
            return state.getValue(BlockStateProperties.AGE_2) >= 2;
        if (state.hasProperty(BlockStateProperties.AGE_3))
            return state.getValue(BlockStateProperties.AGE_3) >= 3;
        if (state.hasProperty(BlockStateProperties.AGE_4))
            return state.getValue(BlockStateProperties.AGE_4) >= 4;
        if (state.hasProperty(BlockStateProperties.AGE_5))
            return state.getValue(BlockStateProperties.AGE_5) >= 5;
        if (state.hasProperty(BlockStateProperties.AGE_7))
            return state.getValue(BlockStateProperties.AGE_7) >= 7;
        if (state.hasProperty(BlockStateProperties.AGE_15))
            return state.getValue(BlockStateProperties.AGE_15) >= 15;
        if (state.hasProperty(BlockStateProperties.AGE_25))
            return state.getValue(BlockStateProperties.AGE_25) >= 25;

        return true;
    }
    /**
     * Implements mineTreeOrBlock behavior for the programmable pal feature.
     */
    private void mineTreeOrBlock(BlockPos origin, BlockState originState) {
        clearChopTask();

        if (originState.isAir())
            return;

        if (!originState.is(BlockTags.LOGS)) {
            queueChopTarget(origin);
            return;
        }

        Set<BlockPos> visitedLogs = new HashSet<>();
        Set<BlockPos> visitedLeaves = new HashSet<>();
        ArrayDeque<BlockPos> logQueue = new ArrayDeque<>();
        ArrayDeque<BlockPos> leafQueue = new ArrayDeque<>();
        pendingLeafRemoval.clear();
        logQueue.add(origin);

        while (!logQueue.isEmpty()) {
            BlockPos current = logQueue.removeFirst();
            if (!visitedLogs.add(current))
                continue;

            BlockState currentState = level().getBlockState(current);
            if (!currentState.is(BlockTags.LOGS))
                continue;

            queueChopTarget(current);

            for (BlockPos neighbor : BlockPos.betweenClosed(current.offset(-1, -1, -1), current.offset(1, 1, 1))) {
                BlockPos immutableNeighbor = neighbor.immutable();
                BlockState neighborState = level().getBlockState(immutableNeighbor);
                if (neighborState.is(BlockTags.LOGS) && !visitedLogs.contains(immutableNeighbor))
                    logQueue.addLast(immutableNeighbor);
                if (neighborState.is(BlockTags.LEAVES) && !visitedLeaves.contains(immutableNeighbor))
                    leafQueue.addLast(immutableNeighbor);
            }
        }

        while (!leafQueue.isEmpty()) {
            BlockPos currentLeaf = leafQueue.removeFirst();
            if (!visitedLeaves.add(currentLeaf))
                continue;

            BlockState currentLeafState = level().getBlockState(currentLeaf);
            if (!currentLeafState.is(BlockTags.LEAVES))
                continue;

            pendingLeafRemoval.add(currentLeaf.immutable());

            for (BlockPos neighbor : BlockPos.betweenClosed(currentLeaf.offset(-1, -1, -1), currentLeaf.offset(1, 1, 1))) {
                BlockPos immutableNeighbor = neighbor.immutable();
                if (visitedLeaves.contains(immutableNeighbor))
                    continue;
                if (level().getBlockState(immutableNeighbor).is(BlockTags.LEAVES))
                    leafQueue.addLast(immutableNeighbor);
            }
        }
    }

    /**
     * Removes queued leaves instantly after all logs have been chopped.
     */
    private void removePendingLeaves() {
        for (BlockPos leafPos : pendingLeafRemoval) {
            BlockState leafState = level().getBlockState(leafPos);
            if (leafState.is(BlockTags.LEAVES))
                breakBlockAndStoreDrops(leafPos, leafState, ItemStack.EMPTY);
        }
        pendingLeafRemoval.clear();
    }

    /**
     * Queues one block for progressive chopping without duplicates.
     */
    private void queueChopTarget(BlockPos pos) {
        BlockPos immutablePos = pos.immutable();
        if (queuedChopTargets.add(immutablePos))
            pendingChopTargets.addLast(immutablePos);
    }

    /**
     * Implements matchesConfiguredCheckBlockItem behavior for the programmable pal feature.
     */
    private boolean matchesConfiguredCheckBlockItem(CompoundTag data, BlockPos checkPos, BlockState state) {
        if (state.isAir())
            return false;

        if (!data.contains(CHECK_BLOCK_MATCH_ITEM_TAG))
            return true;

        ItemStack configured = ItemStack.parseOptional(level().registryAccess(), data.getCompound(CHECK_BLOCK_MATCH_ITEM_TAG));
        if (configured.isEmpty())
            return true;

        FilterItemStack configuredFilter = FilterItemStack.of(configured.copy());

        ItemStack targetBlockItem = new ItemStack(state.getBlock().asItem());
        if (!targetBlockItem.isEmpty() && configuredFilter.test(level(), targetBlockItem))
            return true;

        if (level() instanceof ServerLevel serverLevel) {
            LootParams.Builder lootBuilder = new LootParams.Builder(serverLevel)
                    .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(checkPos))
                    .withParameter(LootContextParams.TOOL, ItemStack.EMPTY)
                    .withOptionalParameter(LootContextParams.THIS_ENTITY, this);

            for (ItemStack drop : state.getDrops(lootBuilder)) {
                if (configuredFilter.test(level(), drop))
                    return true;
            }
        }

        return false;
    }
}





