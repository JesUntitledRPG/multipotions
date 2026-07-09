package com.jes.multipotions.merger;

import com.jes.multipotions.JessMultipotions;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.SlotItemHandler;

public class MergerMenu extends AbstractContainerMenu {
    public final MergerBlockEntity blockEntity;
    private final Level level;
    private final ContainerData data;

    public MergerMenu(int pContainerId, Inventory inv, FriendlyByteBuf extraData) {
        this(pContainerId, inv, inv.player.level().getBlockEntity(extraData.readBlockPos()), new SimpleContainerData(4));
    }

    public MergerMenu(int pContainerId, Inventory inv, BlockEntity entity, ContainerData data) {
        super(JessMultipotions.MERGER_MENU.get(), pContainerId);
        this.blockEntity = ((MergerBlockEntity) entity);
        this.level = inv.player.level();
        this.data = data;

        addPlayerInventory(inv);
        addPlayerHotbar(inv);

        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, 0, 54, 19));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, 1, 54, 39));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, 2, 77, 51));
        this.addSlot(new SlotItemHandler(blockEntity.itemHandler, 3, 104, 29));

        addDataSlots(data);
    }

    public boolean isCrafting() {
      return data.get(0) > 0; // Index zero corresponds to "progress".
    };

    public int getScaledArrowProgress() {
        int progress = this.data.get(0);
        int maxProgress = this.data.get(1);
        // the arrow width in pixels is 24
        // TODO: no really what the #### does the question mark do or the colon sign
        // seems that it could be clamping or smth idfk
        return maxProgress != 0 && progress != 0 ? progress * 24 / maxProgress : 0;
    };

    // The tutorial defines some constants here. I'm copying them over.
    // Apparently someone else made the tutorial's function! Crediting them right now.
    // Credit goes to diesieben07 | https://github.com/diesieben07/SevenCommons

    private static final int VANILLA_SLOT_COUNT = 36;
    private static final int VANILLA_FIRST_SLOT_INDEX = 0;
    private static final int ENTITY_FIRST_SLOT_INDEX = 36; // as we know indexes start at zero so vanilla has slots 0-35 so we get our 4 slots at index 36
    private static final int ENTITY_INVENTORY_SLOT_COUNT = 4; // this has to be redefined if we change the slot count

    @Override
    public ItemStack quickMoveStack(Player player, int i) { // TODO: Recheck this.
        Slot sourceSlot = slots.get(i);
        if (sourceSlot == null || !sourceSlot.hasItem()) return  ItemStack.EMPTY;
        ItemStack sourceStack = sourceSlot.getItem();
        ItemStack sStackCopy = sourceStack.copy();

        if (i < VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT) {
            if(!moveItemStackTo(sourceStack, ENTITY_FIRST_SLOT_INDEX, ENTITY_FIRST_SLOT_INDEX + ENTITY_INVENTORY_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else if (i < ENTITY_FIRST_SLOT_INDEX + ENTITY_INVENTORY_SLOT_COUNT) {
            if (!moveItemStackTo(sourceStack, VANILLA_FIRST_SLOT_INDEX, VANILLA_FIRST_SLOT_INDEX + VANILLA_SLOT_COUNT, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            System.out.println("Multipots: Invalid slot index for merger:" + i);
            return ItemStack.EMPTY;
        }

        if (sourceStack.getCount() == 0) {
            sourceSlot.set(ItemStack.EMPTY);
        } else {
            sourceSlot.setChanged();
        }
        sourceSlot.onTake(player, sourceStack);
        return sStackCopy;
    }

    @Override
    public boolean stillValid(Player player) {
        return stillValid(ContainerLevelAccess.create(level, blockEntity.getBlockPos()), player, JessMultipotions.MERGER.get());
    }

    private void addPlayerInventory(Inventory pInv) {
        for (int i=0; i<3; ++i) {
            for (int j=0; j<9; ++j) {
                // Seems that coords are hardcoded...
                this.addSlot(new Slot(pInv, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
            }
        }
    }

    private void addPlayerHotbar(Inventory pInv) {
        for (int i=0; i<9; ++i) {
            // TODO: Study where we can place these to make a good screen layout.
            this.addSlot(new Slot(pInv, i, 8 + i * 18, 142));
        }
    }

}
