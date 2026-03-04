package com.cosmolego527.create_pp.entity.custom;

import com.cosmolego527.create_pp.entity.ModEntities;
import com.cosmolego527.create_pp.entity.ProgrammablePalVariant;
import com.cosmolego527.create_pp.component.ModDataComponentTypes;
import com.cosmolego527.create_pp.entity.menu.ProgrammablePalMenu;
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
import net.minecraft.world.level.block.state.BlockState;
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
    };


    public int insertionDelay;

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
    private static final String MOVE_DIRECTION_INDEX_TAG = "PalMoveDirectionIndex";
    private static final String MOVE_DISTANCE_INDEX_TAG = "PalMoveDistanceIndex";
    private static final String MOVE_STEP_CHECK_LINK_TAG = "PalMoveStepCheckLink";
    private static final String CHECK_BLOCK_MATCH_ACTION_KEY_TAG = "PalCheckBlockMatchActionKey";
    private static final String CHECK_BLOCK_MATCH_ITEM_TAG = "PalCheckBlockMatchItem";
    private static final String HAS_ITEM_TARGET_INDEX_TAG = "PalHasItemTargetIndex";
    private static final String HAS_ITEM_ACTION_KEY_TAG = "PalHasItemActionKey";
    private static final String HAS_ITEM_MATCH_ITEM_TAG = "PalHasItemMatchItem";
    private static final String RUN_TAPE_ITEM_TAG = "PalRunTapeItem";
    private static final String COMMANDER_UUID_TAG = "CommanderUUID";
    private static final String FOLLOW_COMMANDER_TAG = "FollowCommander";
    private static final String PROGRAM_START_POS_TAG = "ProgramStartPos";

    private static final GameProfile PAL_FAKE_PLAYER_PROFILE =
            new GameProfile(UUID.fromString("f27f13ee-1d56-4f31-a322-a4c32701793f"), "create_pp_pal");


    /**
     * Implements ProgrammablePalEntity behavior for the programmable pal feature.
     */
    public ProgrammablePalEntity(EntityType<? extends PathfinderMob> entityTypeIn, Level level) {
        super(entityTypeIn, level);
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

    private void resetProgramRuntimeState() {
        instructionPointer = 0;
        instructionCooldown = 0;
        queuedMoveSteps = 0;
        queuedMoveDirectionIndex = 0;
        queuedStepCheckInstructionData = null;
        skipNextStandaloneCheckInstruction = false;
        activeRunTapeProgramTag = null;
        activeRunTapeInstructionPointer = 0;
        runTapeDepth = 0;
        clearChopTask();
    }

    private void captureProgramStartIfNeeded() {
        if (!programActiveLastTick)
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
            resetProgramRuntimeState();
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
        compound.putInt("InstructionPointer", instructionPointer);
        compound.putInt("InstructionCooldown", instructionCooldown);
        compound.putInt("QueuedMoveSteps", queuedMoveSteps);
        compound.putInt("QueuedMoveDirectionIndex", queuedMoveDirectionIndex);
        if (commanderUUID != null)
            compound.putUUID(COMMANDER_UUID_TAG, commanderUUID);
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
        instructionPointer = compound.getInt("InstructionPointer");
        instructionCooldown = compound.getInt("InstructionCooldown");
        queuedMoveSteps = compound.getInt("QueuedMoveSteps");
        queuedMoveDirectionIndex = compound.getInt("QueuedMoveDirectionIndex");
        commanderUUID = compound.hasUUID(COMMANDER_UUID_TAG) ? compound.getUUID(COMMANDER_UUID_TAG) : null;
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
            queuedMoveSteps = 0;
            queuedStepCheckInstructionData = null;
            skipNextStandaloneCheckInstruction = false;
            clearChopTask();
            runFightTapeBehavior();
            return;
        }

        if (!hasActiveInstructionTape()) {
            markProgramInactive();
            queuedMoveSteps = 0;
            queuedStepCheckInstructionData = null;
            skipNextStandaloneCheckInstruction = false;
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
                instructionCooldown = 20;
            } else {
                // Keep move queued and retry until the obstacle is gone.
                instructionCooldown = 20;
            }

            if (queuedMoveSteps <= 0)
                queuedStepCheckInstructionData = null;
            return;
        }

        if (activeRunTapeProgramTag != null) {
            boolean finishedSubTape = executeActiveRunTapeStep();
            if (finishedSubTape)
                advanceMainInstructionPointer();
            instructionCooldown = 20;
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

        instructionCooldown = 20;
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

        executeMoveForward(data, schedule);
        return true;
    }

    /**
     * Executes runtime logic for executeMoveForward.
     */
    private void executeMoveForward(CompoundTag data, Schedule schedule) {
        queuedMoveDirectionIndex = data.getInt(MOVE_DIRECTION_INDEX_TAG);
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
     * Executes runtime logic for executeQueuedMoveStep.
     */
    private boolean executeQueuedMoveStep() {
        getNavigation().stop();

        Direction direction = switch (queuedMoveDirectionIndex) {
            case 1 -> Direction.EAST;
            case 2 -> Direction.SOUTH;
            case 3 -> Direction.WEST;
            default -> Direction.NORTH;
        };

        setYRot(direction.toYRot());
        setYHeadRot(direction.toYRot());
        yBodyRot = direction.toYRot();

        BlockPos current = blockPosition();
        BlockPos next = current.relative(direction);

        if (canMoveThrough(next)) {
            setPos(next.getX() + 0.5D, getY(), next.getZ() + 0.5D);
            return true;
        }

        // Allow only tiny step-ups (e.g. farmland -> full block), but not full 1-block climbs.
        BlockState nextState = level().getBlockState(next);
        BlockPos steppedFeet = next.above();
        double nextCollisionTop = nextState.getCollisionShape(level(), next).max(Direction.Axis.Y);
        double stepHeight = (next.getY() + nextCollisionTop) - getY();

        boolean canSmallStep = !canMoveThrough(next)
                && canMoveThrough(steppedFeet)
                && nextCollisionTop > 0.0D
                && nextCollisionTop <= 1.0D
                && stepHeight > 0.0D
                && stepHeight <= 0.2D;

        if (!canSmallStep)
            return false;

        setPos(next.getX() + 0.5D, getY() + stepHeight, next.getZ() + 0.5D);
        return true;
    }

    private void startRunTape(CompoundTag data) {
        if (!data.contains(RUN_TAPE_ITEM_TAG)) {
            activeRunTapeProgramTag = null;
            return;
        }

        ItemStack nestedTape = ItemStack.parseOptional(level().registryAccess(), data.getCompound(RUN_TAPE_ITEM_TAG));
        if (nestedTape.isEmpty() || nestedTape.getItem() != ModItems.PROGRAMMABLE_TAPE.get()) {
            activeRunTapeProgramTag = null;
            return;
        }

        CompoundTag nestedProgramTag = nestedTape.get(ModDataComponentTypes.VOID_FUNCTION_DATA);
        if (nestedProgramTag == null || nestedProgramTag.isEmpty()) {
            activeRunTapeProgramTag = null;
            return;
        }

        Schedule nestedSchedule = Schedule.fromTag(level().registryAccess(), nestedProgramTag);
        if (nestedSchedule.entries.isEmpty()) {
            activeRunTapeProgramTag = null;
            return;
        }

        activeRunTapeProgramTag = nestedProgramTag.copy();
        activeRunTapeInstructionPointer = 0;
    }

    private boolean executeActiveRunTapeStep() {
        if (activeRunTapeProgramTag == null)
            return true;

        Schedule nestedSchedule = Schedule.fromTag(level().registryAccess(), activeRunTapeProgramTag);
        if (nestedSchedule.entries.isEmpty()) {
            activeRunTapeProgramTag = null;
            return true;
        }

        if (activeRunTapeInstructionPointer >= nestedSchedule.entries.size()) {
            activeRunTapeProgramTag = null;
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
            activeRunTapeProgramTag = null;
            activeRunTapeInstructionPointer = 0;
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

        runTapeDepth++;
        try {
            for (ScheduleEntry entry : nestedSchedule.entries)
                executeInstruction(entry.instruction, nestedSchedule);
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

        Vec3 hitCenter = Vec3.atCenterOf(targetPos);
        BlockHitResult hitResult = new BlockHitResult(hitCenter, face, targetPos, false);
        UseOnContext context = new UseOnContext(fakePlayer, InteractionHand.MAIN_HAND, hitResult);

        InteractionResult result = handStack.useOn(context);
        if (result == InteractionResult.PASS)
            handStack.use(serverLevel, fakePlayer, InteractionHand.MAIN_HAND);

        ItemStack updated = fakePlayer.getItemInHand(InteractionHand.MAIN_HAND);
        inventory.setItem(inventorySlot, updated.copy());
        setHeldTool(updated);
    }

    /**
     * Executes runtime logic for executeCheckBlock.
     */
    private void executeCheckBlock(CompoundTag data) {
        int targetIndex = data.getInt(CHECK_BLOCK_TARGET_INDEX_TAG);
        BlockPos checkPos = getCheckTargetPosition(targetIndex);
        BlockState state = level().getBlockState(checkPos);

        broadcastCheckResult(targetIndex, state);

        if (!matchesConfiguredCheckBlockItem(data, state))
            return;

        applyCheckBlockMatchAction(data.getString(CHECK_BLOCK_MATCH_ACTION_KEY_TAG), checkPos, state);
    }

    /**
     * Resolves the block position used by check-block based on target index.
     */
    private BlockPos getCheckTargetPosition(int targetIndex) {
        return switch (targetIndex) {
            case 1 -> blockPosition().above();
            case 2 -> blockPosition().relative(getDirection());
            default -> blockPosition().below();
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

        if (state.getBlock() instanceof CropBlock cropBlock && cropBlock.isMaxAge(state)) {
            breakBlockAndStoreDrops(checkPos, state, ItemStack.EMPTY);
            return;
        }

        if (!state.isAir())
            breakBlockAndStoreDrops(checkPos, state, ItemStack.EMPTY);
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
            if (level().getBlockState(leafPos).is(BlockTags.LEAVES))
                level().setBlock(leafPos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
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
    private boolean matchesConfiguredCheckBlockItem(CompoundTag data, BlockState state) {
        if (!data.contains(CHECK_BLOCK_MATCH_ITEM_TAG) || state.isAir())
            return false;

        ItemStack configured = ItemStack.parseOptional(level().registryAccess(), data.getCompound(CHECK_BLOCK_MATCH_ITEM_TAG));
        if (configured.isEmpty())
            return false;

        ItemStack target = new ItemStack(state.getBlock().asItem());
        if (target.isEmpty())
            return false;

        FilterItemStack configuredFilter = FilterItemStack.of(configured.copy());
        return configuredFilter.test(level(), target);
    }
}