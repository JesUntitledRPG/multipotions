package com.jes.multipotions;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.common.Tags;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static java.lang.Integer.max;
import static java.lang.Integer.valueOf;
import static java.lang.Math.round;
import static java.util.Objects.requireNonNull;

// Apparently all public classes should go in separate files. Huh. Did... not know that.

public class MergerBlockEntity extends BlockEntity implements MenuProvider {

    public final ItemStackHandler itemHandler = new ItemStackHandler(4) {

        @Override
        protected void onContentsChanged(int slot) {
          setChanged();
          if(!level.isClientSide()) {
              level.sendBlockUpdated(getBlockPos(), getBlockState(), getBlockState(), 3);
          }
        };

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            if(slot == 0 | slot == 1) {
                if(stack.getTags().noneMatch(e -> e == Tags.Items.POTIONS)){
                    return false;
                }
            }
            else if(slot == 2) {
                if(stack.getItem() != Items.BLAZE_POWDER) {
                    return false;
                }
            }
            else {
                return false;
            }
            return super.isItemValid(slot, stack);
        }
    };

    // This is the only part of this codebase made with AI. This is because oh my god I have NO IDEA on how this shit is meant to work normally
    // Please someone improve the NeoForge documentation please pwease I googled for like 4 hours before I caved in and couldn't understand shit
    // Well I mean the boilerplate was made by IDEA so like
    // And some bugfixes were done by IDEA and by myself because AI does not get public classes vs private classes apparently
    // Comments are done by hand so I can refer to this later on and get what this does instead of dealing with AI again >:C

    public final IItemHandler automationWrapper = new IItemHandler() {
        // We're mostly just linking back to the itemHandler
        @Override
        public int getSlots() {
            return itemHandler.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int i) {
            return itemHandler.getStackInSlot(i);
        }

        @Override
        public @NotNull ItemStack insertItem(int i, ItemStack itemStack, boolean b) {
            // Linkback to the itemHandler if slot != 3
            if (!(i==3)) {
                // the AI had this as longer code but like why would I check for the 3 inputs individually i just wanna go outputting
                return itemHandler.insertItem(i, itemStack, b);
            }
            return itemStack;
        }

        @Override
        public @NotNull ItemStack extractItem(int i, int count, boolean b) {
            // Same thing NGL
            if(i==3) {
                return itemHandler.extractItem(i, count, b);
            }
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int i) {
            return itemHandler.getSlotLimit(i);
        }

        @Override
        public boolean isItemValid(int i, @NotNull ItemStack itemStack) {
            if(!(i==3)) {
                return itemHandler.isItemValid(i, itemStack);
            }
            return false; // AI originally had the opposite order here, but I appreciate consistency in my codebase, so I changed everything to be consistent
        }
    };

    // END OF AI GENERATED CODE, back to tutorials and human ingenuity AS INTENDED
    // yes i am STILL SORTA PISSY ABOUT THE DOCUMENTATION STUFF please update it

    // Technically not necessary...? But helpful.

    private static final int INPUT_POTION_ONE = 0;
    private static final int INPUT_POTION_TWO = 1;
    private static final int INPUT_FUEL = 2;
    private static final int OUTPUT_POTION = 3;

    protected final ContainerData data;
    private int progress = 0;
    private int maxProgress = 200; // TODO: Make variable times for potion mixing based on how concentrated it'd be
                                   // 10 seconds for max progress seems OK for now tho; maybe add a config multiplier for duration and crap

    public MergerBlockEntity(BlockPos pos, BlockState blockState) {
        super(JessMultipotions.MERGER_BE.get(), pos, blockState);
        data = new ContainerData() {
            @Override
            public int get(int i) {
                return switch (i) {
                    case 0 -> MergerBlockEntity.this.progress;
                    case 1 -> MergerBlockEntity.this.maxProgress;
                    default -> 0;
                };
            }

            @Override
            public void set(int i, int value) {
                switch (i) {
                    case 0: MergerBlockEntity.this.progress = value;
                    case 1: MergerBlockEntity.this.maxProgress = value;
                }
            }

            @Override
            public int getCount() {
                return 2;
            }
        };
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.jes_multipots.merger");
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int i, Inventory inventory, Player player) {
        return new MergerMenu(i, inventory, this, this.data);
    }

    // Drops
    // Basically "make an inventory and when broken drop EVERYTHING where we once were"

    public void drops() {
        SimpleContainer inventory = new SimpleContainer(itemHandler.getSlots());
        for (int i=0; i < itemHandler.getSlots(); i++) {
            inventory.setItem(i, itemHandler.getStackInSlot(i));
        }

        assert this.level != null;
        Containers.dropContents(this.level, this.worldPosition, inventory);
    }

    // Saving-loading methods for the data inside

    @Override
    protected void saveAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        pTag.put("inventory", itemHandler.serializeNBT(pRegistries)); // IDK what serializing the NBT does. is it making many tags into one? that sounds like it
        pTag.putInt("merger.progress", progress);
        pTag.putInt("merger.maxProgress", maxProgress);

        super.saveAdditional(pTag,pRegistries); // TODO: search what a "super" is. is it just a "self" method
    }

    @Override
    protected void loadAdditional(CompoundTag pTag, HolderLookup.Provider pRegistries) {
        super.loadAdditional(pTag,pRegistries);

        itemHandler.deserializeNBT(pRegistries, pTag.getCompound("inventory")); // that or it's a way to put nested tags which also sounds usegul
        progress = pTag.getInt("merger.progress");
        maxProgress = pTag.getInt("merger.maxProgress");
    }

    // The Tick Method

    public void tick(Level level, BlockPos blockPos, BlockState blockState) {
        // This has the logic inside. AKA here's where I can make ANYTHING I want! This is where the module becomes custom!
        // TODO: Change max progress depending on potion complexity.
        if (validInputs() && outputReady()) {
            progress++;
            setChanged(level, blockPos, blockState);

            if (progress >= maxProgress) {
                mergePotions();
                progress = 0;
                maxProgress = 144;
            }
        }
        else if(progress > 0) {
            progress = 0; // It's quicker to read than to write, as said in the URPG source code.
        }
    }

    private void mergePotions() {
        // TODO: MERGE.
        /*
         * Jotting this down here. This is a multistep process:
         * 1. Gather the Potion effects and place them on a list.
         * - TODO: Get how the effect data is saved inside the Potions.
         *    They seem to have 3 components that we care about; the effect itself, the duration in ticks and their level/amplifier.
         *
         * 2. Based on the strength/number of effects of the Potion, alter their tick duration.
         * - Example: A strength potion that lasts 1.2 minutes would last 1 minute when coupled with Regeneration.
         * - TODO: When Reagents are added, alter the formula; with the proper reagent that potion above should last the minute and 12 seconds.
         *
         * 3. Average out the color of the two potions to get the new color.
         *
         * 4. Based on the settings, either turn the output Potion into a failed mixture or not.
         *
         * 5. Mix the effects into the output Potion. Return said potion.
         *
         * 6. Delete everything in the slots.
         *
         * TODO: Add sounds.
         * */
        ItemStack output = new ItemStack(itemHandler.getStackInSlot(0).getItem()); // The potion on top sets if it's a splash potion or a lingering potion or stuff.
        ItemStack inputOne = itemHandler.getStackInSlot(0).copy(); // Apparently working with ItemStacks is what we should do?
        ItemStack inputTwo = itemHandler.getStackInSlot(1).copy();
        ArrayList<MobEffectInstance> effects = new ArrayList<MobEffectInstance>(); // IDK if having the type be explicit here's good or not but like sure I got time
        inputOne.get(DataComponents.POTION_CONTENTS).getAllEffects().forEach(effects::add);
        inputTwo.get(DataComponents.POTION_CONTENTS).getAllEffects().forEach(effects::add);

        if(!effects.isEmpty()) {
            ArrayList<Holder<MobEffect>> effectTypes = new ArrayList<>();
            ArrayList<Holder<MobEffect>> dupeList = new ArrayList<>();
            for(MobEffectInstance effect: effects) {
                effectTypes.add(effect.getEffect());
                if (Collections.frequency(effectTypes, effect.getEffect()) > 1) {
                    // WE HAVE A DUPE! We'll deal with it later once we can interact with ModEffectInstances.
                    dupeList.add(effect.getEffect());
                }
            }

            // TODO: Edit this so we can have an n-amount of duplicates instead of just two.
            for(Holder<MobEffect> dupe: dupeList) {
                List<MobEffectInstance> duplicates = effects.stream().filter(effect -> effect.is(dupe)).toList();
                MobEffectInstance fused = getFused(duplicates);
                effects.add(fused);
                for(MobEffectInstance duplicateEffect: duplicates) {
                    effects.remove(duplicateEffect); // java vs python list stuff still messes w my head, ik this is sus so it's going at the end of the loop
                }
            }

            boolean isValid = true;

            for (MobEffectInstance effect: effects) {
                if (effect.getAmplifier() > Config.MAX_EFFECT_LEVEL.getAsInt()) {
                    isValid = false;
                }
            }
            if (effects.size() > Config.MAX_EFFECTS_ON_MERGING.getAsInt()) {
                isValid = false;
            }

            if(isValid) {
                PotionContents outContents = new PotionContents(Optional.of(Potions.MUNDANE), Optional.empty(), effects); // note to self: apparently Optionals can just be empty

                output.set(DataComponents.POTION_CONTENTS, outContents); // this should do the trick >:3
                try {
                    assert this.level != null;
                    this.level.playSeededSound(null, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), SoundEvents.BREWING_STAND_BREW, SoundSource.BLOCKS, 1f, 1f, 0);
                    // someday I'd like to add particles to this, but it'll probably be a bit of a pain >_<
                } catch (Exception ignored) {
                }

            } else {
                output = new ItemStack(Items.COAL, 1);
                try {
                    assert this.level != null;
                    this.level.playSeededSound(null, this.worldPosition.getX(), this.worldPosition.getY(), this.worldPosition.getZ(), SoundEvents.GENERIC_EXTINGUISH_FIRE, SoundSource.BLOCKS, 1f, 0.4f, 0);
                } catch (Exception ignored) {
                }
            }


        } else {
            output = new ItemStack(Items.GLASS_BOTTLE);
        }


        // Remember - those are numbers. Let's go fast.
        for (int i = 0; i <= 2; i++) { // Corresponds to the inputs.
            itemHandler.extractItem(i, 1, false);
        }
        itemHandler.setStackInSlot(OUTPUT_POTION, output); // Once again -- it's one by one.
    }

    private static @NotNull MobEffectInstance getFused(List<MobEffectInstance> duplicates) {
        int newAmplifier = max(duplicates.getFirst().getAmplifier(), duplicates.getLast().getAmplifier());
        if (duplicates.getFirst().getAmplifier() == duplicates.getLast().getAmplifier()) {
            newAmplifier++;
        }
        return new MobEffectInstance(duplicates.getFirst().getEffect(),
                (
                        round((float) (duplicates.getFirst().getDuration() + duplicates.getLast().getDuration()) / 2)
                ), newAmplifier);
    }

    private boolean validInputs() {
        boolean validity = false;
        // Hardcoded right now.
        // TODO: Add the special edge cases for special potion types.
        if (itemHandler.getStackInSlot(INPUT_POTION_ONE).is(Tags.Items.POTIONS) &&
                itemHandler.getStackInSlot(INPUT_POTION_TWO).is(Tags.Items.POTIONS) &&
                itemHandler.getStackInSlot(INPUT_FUEL).is(Items.BLAZE_POWDER)) {
          validity = true;
        };
        return validity;
    }

    private boolean outputReady() {
        return itemHandler.getStackInSlot(OUTPUT_POTION).isEmpty(); // This also saves me the effort of checking in validInputs for output shennanigans. It's either nothing or something.
    }

    // Some general block entity stuff apparently

    @Override
    public CompoundTag getUpdateTag(HolderLookup.Provider pRegistries) {
        return saveWithoutMetadata(pRegistries); // does adding metadata change anything???
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() { // tbh it does make sense to have this here tho
        return ClientboundBlockEntityDataPacket.create(this);
    }
}
