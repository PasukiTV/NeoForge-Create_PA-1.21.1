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
import net.minecraft.world.level.Level;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
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
    private boolean executingMainInstruction = false;
    private boolean repeatCurrentInstruction = false;

    private static final EntityDataAccessor<Integer> DOME_COLOR =
            SynchedEntityData.defineId(ProgrammablePalEntity.class, EntityDataSerializers.INT);
    private static final EntityDataAccessor<ItemStack> DATA_ITEM_STACK =
            SynchedEntityData.defineId(ProgrammablePalEntity.class, EntityDataSerializers.ITEM_STACK);
    private static final EntityDataAccessor<ItemStack> DATA_HELD_TOOL =
            SynchedEntityData.defineId(ProgrammablePalEntity.class, EntityDataSerializers.ITEM_STACK);

    private static final int TAPE_SLOT = ProgrammablePalMenu.TAPE_SLOT_INDEX;
    private static final int TOOL_SLOT_START = ProgrammablePalMenu.PAL_STORAGE_START;
    private static final int TOOL_SLOT_END = ProgrammablePalMenu.PAL_SLOT_COUNT;

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

    void clearActiveRunTapeState() {
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
        executingMainInstruction = false;
        repeatCurrentInstruction = false;
    }

    private Schedule getScheduleFromInstructionTape() {
        return PalScheduleRuntime.getScheduleFromTape(level(), getInstructionTape());
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
        return PalPersistenceRuntime.toLongArray(positions, size);
    }

    private void loadQueuedPositions(long[] packedPositions, ArrayDeque<BlockPos> outQueue) {
        PalPersistenceRuntime.loadQueuedPositions(packedPositions, outQueue);
    }

    private void loadUniquePositions(long[] packedPositions, Set<BlockPos> outSet) {
        PalPersistenceRuntime.loadUniquePositions(packedPositions, outSet);
    }

    void captureProgramStartIfNeeded() {
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

    int findToolSlot(ItemStack target) {
        return PalInventoryRuntime.findToolSlot(inventory, TOOL_SLOT_START, TOOL_SLOT_END, target);
    }

    ItemStack firstAxeInInventory() {
        return PalInventoryRuntime.firstAxeInInventory(inventory, TOOL_SLOT_START, TOOL_SLOT_END);
    }

    ItemStack bestCombatToolInInventory() {
        return PalInventoryRuntime.bestCombatToolInInventory(inventory, TOOL_SLOT_START, TOOL_SLOT_END);
    }



    int getToolSlotStart() {
        return TOOL_SLOT_START;
    }

    int getToolSlotEnd() {
        return TOOL_SLOT_END;
    }

    GameProfile getFakePlayerProfile() {
        return PAL_FAKE_PLAYER_PROFILE;
    }    UUID getCommanderUUID() {
        return commanderUUID;
    }

    boolean shouldFollowCommander() {
        return followCommander;
    }

    int getChopCooldown() {
        return chopCooldown;
    }

    void setChopCooldown(int value) {
        chopCooldown = value;
    }

    BlockPos getCurrentChopTarget() {
        return currentChopTarget;
    }

    float getCurrentChopProgress() {
        return currentChopProgress;
    }

    void setCurrentChopProgress(float value) {
        currentChopProgress = value;
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

    boolean isFightTapeActive() {
        ItemStack tape = getInstructionTape();
        if (tape.isEmpty())
            return false;
        return "fight_tape".equals(BuiltInRegistries.ITEM.getKey(tape.getItem()).getPath());
    }

    private void runFightTapeBehavior() {
        PalCombatRuntime.runFightTapeBehavior(this);
    }

    /**
     * Checks the state used by hasActiveInstructionTape.
     */
    boolean hasActiveInstructionTape() {
        if (isFightTapeActive())
            return false;
        return PalScheduleRuntime.hasProgramData(getInstructionTape());
    }

    /**
     * Executes runtime logic for runProgramTick.
     */
    private void runProgramTick() {
        PalProgramRuntime.runProgramTick(this);
    }

    void handleFightTapeProgramTick() {
        markProgramInactive();
        clearQueuedMoveState();
        clearActiveRunTapeState();
        clearChopTask();
        runFightTapeBehavior();
    }

    void handleNoInstructionTapeProgramTick() {
        markProgramInactive();
        clearQueuedMoveState();
        clearActiveRunTapeState();
        clearChopTask();
    }

    boolean consumeInstructionCooldown() {
        if (instructionCooldown > 0)
            instructionCooldown--;
        return instructionCooldown > 0;
    }

    boolean handleQueuedMoveProgramTick() {
        if (queuedMoveSteps <= 0)
            return false;

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
        return true;
    }

    boolean handleActiveRunTapeProgramTick() {
        if (activeRunTapeProgramTag == null)
            return false;

        boolean finishedSubTape = executeActiveRunTapeStep();
        if (finishedSubTape)
            advanceMainInstructionPointer();
        instructionCooldown = 10;
        return true;
    }

    void executeMainInstructionProgramTick() {
        Schedule schedule = PalScheduleRuntime.getScheduleFromTape(level(), getInstructionTape());
        if (schedule == null)
            return;

        if (instructionPointer >= schedule.entries.size())
            instructionPointer = 0;

        ScheduleEntry entry = schedule.entries.get(instructionPointer);
        CompoundTag entryData = entry.instruction.getData();
        String actionKey = entryData.getString(PalTagKeys.ACTION_KEY);

        boolean advancePointer = true;
        if (skipNextStandaloneCheckInstruction && PalTagKeys.ACTION_CHECK_BLOCK.equals(actionKey)) {
            skipNextStandaloneCheckInstruction = false;
        } else {
            executingMainInstruction = true;
            try {
                advancePointer = executeInstruction(entry.instruction, schedule);
            } finally {
                executingMainInstruction = false;
            }

            if (consumeRepeatCurrentInstruction()) {
                advancePointer = false;
            }
        }

        if (advancePointer)
            advanceMainInstructionPointer();

        instructionCooldown = 10;
    }
    /**
     * Advances the active chop job by one game tick using player-like break progress.
     */
    boolean tickChopTask() {
        return PalChopRuntime.tickChopTask(this);
    }

    private boolean isAdjacentChopReach(BlockPos target) {
        return PalChopRuntime.isAdjacentChopReach(blockPosition(), target);
    }

    private boolean queueBlockingLeavesTowardsTarget(BlockPos target) {
        return PalChopRuntime.queueBlockingLeavesTowardsTarget(level(), blockPosition(), target, this::queueChopTarget);
    }

    /**
     * Ensures there is an active target while skipping blocks that are already gone.
     */
    boolean acquireNextChopTarget() {
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
    void finishCurrentChopTarget() {
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
    void clearChopTask() {
        finishCurrentChopTarget();
        pendingChopTargets.clear();
        queuedChopTargets.clear();
        pendingLeafRemoval.clear();
        chopCooldown = 0;
    }

    private void advanceMainInstructionPointer() {
        instructionPointer = PalScheduleRuntime.advanceMainInstructionPointer(level(), getInstructionTape(),
                instructionPointer);
    }

    /**
     * Executes runtime logic for executeInstruction.
     */
    boolean executeInstruction(ScheduleInstruction instruction, Schedule schedule) {
        return PalProgramRuntime.executeInstruction(this, instruction, schedule);
    }


    boolean consumeRepeatCurrentInstruction() {
        if (!repeatCurrentInstruction)
            return false;
        repeatCurrentInstruction = false;
        return true;
    }    boolean isNestedRunTapeExecution() {
        return runTapeDepth > 0;
    }

    boolean hasActiveRunTapeProgram() {
        return activeRunTapeProgramTag != null;
    }

    void setActiveRunTapeProgramTag(CompoundTag tag) {
        activeRunTapeProgramTag = tag;
    }
    CompoundTag getActiveRunTapeProgramTag() {
        return activeRunTapeProgramTag;
    }

    int getActiveRunTapeInstructionPointer() {
        return activeRunTapeInstructionPointer;
    }

    void setActiveRunTapeInstructionPointer(int value) {
        activeRunTapeInstructionPointer = value;
    }

    int getActiveRunTapeRemainingRuns() {
        return activeRunTapeRemainingRuns;
    }

    void setActiveRunTapeRemainingRuns(int value) {
        activeRunTapeRemainingRuns = value;
    }

    void decrementActiveRunTapeRemainingRuns() {
        activeRunTapeRemainingRuns--;
    }

    void incrementRunTapeDepth() {
        runTapeDepth++;
    }

    int getRunTapeDepth() {
        return runTapeDepth;
    }
    void decrementRunTapeDepth() {
        runTapeDepth--;
    }

    void executeRotate(CompoundTag data) {
        Direction target = PalMovementRuntime.resolveRotateDirection(getDirection(),
                data.getInt(PalTagKeys.ROTATE_OPTION_INDEX));

        setYRot(target.toYRot());
        setYHeadRot(target.toYRot());
        yBodyRot = target.toYRot();
        getNavigation().stop();
    }

    /**
     * Executes runtime logic for executeMoveForward.
     */
    void executeMoveForward(CompoundTag data, Schedule schedule) {
        queuedMoveSteps = Math.max(1, data.getInt(PalTagKeys.MOVE_DISTANCE_INDEX) + 1);
        queuedStepCheckInstructionData = null;
        skipNextStandaloneCheckInstruction = false;

        int nextIndex = instructionPointer + 1;
        if (nextIndex >= schedule.entries.size())
            nextIndex = 0;

        if (!schedule.entries.isEmpty()) {
            ScheduleInstruction nextInstruction = schedule.entries.get(nextIndex).instruction;
            CompoundTag nextData = nextInstruction.getData();
            if (data.getBoolean(PalTagKeys.MOVE_STEP_CHECK_LINK) && PalTagKeys.ACTION_CHECK_BLOCK.equals(nextData.getString(PalTagKeys.ACTION_KEY))) {
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
    private boolean executeQueuedMoveStep() {
        return PalMovementRuntime.executeQueuedMoveStep(this);
    }

    void startRunTape(CompoundTag data) {
        PalRunTapeRuntime.startRunTape(this, data);
    }

    boolean executeActiveRunTapeStep() {
        return PalRunTapeRuntime.executeActiveRunTapeStep(this);
    }

    void executeRunTapeImmediate(CompoundTag data) {
        PalRunTapeRuntime.executeRunTapeImmediate(this, data);
    }

    void executeHasItem(CompoundTag data) {
        PalActionRuntime.executeHasItem(this, data);
    }

    void executeInteract(CompoundTag data) {
        PalInteractRuntime.executeInteract(level(), blockPosition(), getDirection(), inventory,
                TOOL_SLOT_START, TOOL_SLOT_END, data,
                PalTagKeys.INTERACT_TARGET_KEY, PalTagKeys.INTERACT_MODE_KEY, PalTagKeys.INTERACT_FILTER_ITEM, PalTagKeys.INTERACT_MAX_STACKS_INDEX);
    }

    void executeCheckBlock(CompoundTag data) {
        PalActionRuntime.executeCheckBlock(this, data);
    }

    /**
     * Resolves the block position used by check-block based on target index.
     */
    BlockPos getCheckTargetPosition(int targetIndex) {
        return PalMovementRuntime.getCheckTargetPosition(this, targetIndex);
    }    void breakBlockAndStoreDrops(BlockPos blockPos, BlockState state, ItemStack tool) {
        PalBlockBreakRuntime.breakBlockAndStoreDrops(level(), this, inventory, blockPos, state, tool,
                getX(), getY(), getZ());
    }

    /**
     * Applies the configured follow-up action after a successful check-block match.
     */
    void applyCheckBlockMatchAction(String matchAction, BlockPos checkPos, BlockState state) {
        if ("chop".equals(matchAction)) {
            ItemStack tool = getHeldTool();
            if (!(tool.getItem() instanceof AxeItem))
                tool = firstAxeInInventory();
            if (!(tool.getItem() instanceof AxeItem)) {
                level().players().forEach(p -> p.displayClientMessage(Component.literal("Pal needs an axe in its inventory to chop."), true));
                repeatCurrentInstruction = true;
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
        if (!PalCheckBlockRuntime.isHarvestableCrop(state))
            return;

        breakBlockAndStoreDrops(checkPos, state, ItemStack.EMPTY);
    }

    /**
     * Implements mineTreeOrBlock behavior for the programmable pal feature.
     */
    private void mineTreeOrBlock(BlockPos origin, BlockState originState) {
        PalChopRuntime.mineTreeOrBlock(level(), origin, originState, this::clearChopTask,
                pendingLeafRemoval, this::queueChopTarget);
    }

    /**
     * Removes queued leaves instantly after all logs have been chopped.
     */
    private void removePendingLeaves() {
        PalChopRuntime.removePendingLeaves(level(), pendingLeafRemoval,
                (pos, state) -> breakBlockAndStoreDrops(pos, state, ItemStack.EMPTY));
    }

    /**
     * Queues one block for progressive chopping without duplicates.
     */
    private void queueChopTarget(BlockPos pos) {
        PalChopRuntime.queueChopTarget(pos, queuedChopTargets, pendingChopTargets);
    }}

