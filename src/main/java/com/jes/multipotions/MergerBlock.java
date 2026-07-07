package com.jes.multipotions;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

;

public class MergerBlock extends BaseEntityBlock {
    public static final MapCodec<MergerBlock> CODEC = simpleCodec(MergerBlock::new);

    public MergerBlock(Properties properties) {
        super(properties);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos blockPos, BlockState blockState) {
        return new MergerBlockEntity(blockPos, blockState); // Adds the BE to the Block
    }


    // This method makes sure we render the model.
    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }


    // This method drops anything inside the merger when it's broken. TODO: It'd be funny to make it so it only drops complete mixtures, right?
    @Override
    public void onRemove(BlockState pState, Level pLevel, BlockPos pPos, BlockState pNewState, boolean pIsMoving) {
        if (pState.getBlock() != pNewState.getBlock()) {
            BlockEntity blockEntity = pLevel.getBlockEntity(pPos);
            if (blockEntity instanceof MergerBlockEntity mergerBlockEntity) {
                mergerBlockEntity.drops();
            }
        }

        super.onRemove(pState, pLevel, pPos, pNewState, pIsMoving);
    };

    // This one is for when we interact with the merger. It's probably a good idea to tinker with this further than I'm gonna do.
    @Override
    protected ItemInteractionResult useItemOn(ItemStack pStack, BlockState pState, Level pLevel, BlockPos pPos, Player pPlayer, InteractionHand pHand, BlockHitResult pHitResult) {
        if (!pLevel.isClientSide()) {
            BlockEntity entity = pLevel.getBlockEntity(pPos);
            if(entity instanceof MergerBlockEntity mergerBlockEntity) {
                ((ServerPlayer) pPlayer).openMenu(new SimpleMenuProvider(mergerBlockEntity, Component.literal("Merger")), pPos); // As it turns out I fucked up the parameters earlier whoops
            } else {
                throw new IllegalStateException("We don't have a provider for our container!");
            }
        }

        return ItemInteractionResult.sidedSuccess(pLevel.isClientSide());
    };

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> blockEntityType) {
      if(level.isClientSide()){
          return null;
      }

      // We tick inside the entity itself to make sure we tick EVERY tick.
      // Notice; the ENTITY. MERGER =/= MERGER_BE. One's the block, the other's the entity.
      // By the way; levelR stands for Returning Level; AKA the level we're gonna return.
      return createTickerHelper(blockEntityType, JessMultipotions.MERGER_BE.get(), (levelR, blockPos, blockState, blockEntity) -> blockEntity.tick(levelR, blockPos, blockState));
    };
};
