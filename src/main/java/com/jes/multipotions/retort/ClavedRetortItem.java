package com.jes.multipotions.retort;

import com.jes.multipotions.Config;
import com.jes.multipotions.JessMultipotions;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class ClavedRetortItem extends Item {
    public ClavedRetortItem(Properties properties) {
        super(properties.component(
                        DataComponents.TOOL, new Tool(
                                new ArrayList<>(), 3.0f, 1
                        )
                ).durability(71) // 71 uses since the Liebig condenser which substitutes retorts in most standard lab usage was made in 1771
        );
    }

    @Override
    public void appendHoverText(@NotNull ItemStack stack, @NotNull TooltipContext context, @NotNull List<Component> tooltipComponents, @NotNull TooltipFlag tooltipFlag) {
        if(Config.FREE_WATER.getAsBoolean()) {
            tooltipComponents.add(Component.translatable("tooltip.jes_multipots.retort.bottle.notfree"));
        } else {
            tooltipComponents.add(Component.translatable("tooltip.jes_multipots.retort.bottle"));
        }
        super.appendHoverText(stack, context, tooltipComponents, tooltipFlag);
    }

    @Override
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, Player player, LivingEntity interactionTarget, @NotNull InteractionHand usedHand) {
        Inventory inv = player.getInventory();
        // We're working IN a level. I'm guessing this won't crash, then?
        Level pLevel = player.level();
        if(inv.contains(new ItemStack(Items.GLASS_BOTTLE)) & !interactionTarget.getActiveEffects().isEmpty()) {
            if(!Config.FREE_WATER.getAsBoolean() | Config.FREE_WATER.getAsBoolean() & inv.contains(new ItemStack(Items.WATER_BUCKET))) { // Free water's sorta weird. TODO: Allow more sources of water.
                pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1f, 1f, 0);
                pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1f, 1f, 0);
                int slot = inv.findSlotMatchingItem(new ItemStack(Items.GLASS_BOTTLE));
                ArrayList<MobEffectInstance> effects = new ArrayList<>(interactionTarget.getActiveEffects()); // IDEA compressed a ton of lines into one
                PotionContents outContents = new PotionContents(Optional.of(Potions.MUNDANE), Optional.empty(), effects);
                ItemStack output = new ItemStack(Items.POTION);
                output.set(DataComponents.POTION_CONTENTS, outContents);
                inv.setItem(slot, output); // god that is a LOT of things we have to do
                interactionTarget.removeAllEffects(); // AND WE'RE STILL GOING
            } else {
                pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
            }
        } else {
            pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
        }
        return super.interactLivingEntity(stack, player, interactionTarget, usedHand);
    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, Player player, @NotNull Entity entity) {
        Level pLevel = player.level();
        if (player.getMainHandItem().getItem() == JessMultipotions.CLAVED_RETORT.asItem() & !(player.getOffhandItem().isEmpty())) {
            ItemStack otherHand = player.getOffhandItem();
            if (entity instanceof LivingEntity livingEntity) { // Not gonna lie. I had to ask AI how to do this. Apparently instanceof can cast new objects...?
                if (!(entity.getType() == (EntityType.PLAYER))) {
                    try {
                        pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1f, 1.0f, 0);
                        otherHand.finishUsingItem(player.level(), livingEntity);    // Also this. Question to anyone who knows this; I get _why_ an ItemStack would have this method, but _why_ is there an Item.use() method then? This confused me for like... 1 hour and 15 minutes.
                        player.getInventory().removeItem(otherHand);
                        // No, really. Was this in the 26.1 docs or something and I missed it because I only look at the 1.21.1 docs? And no, I couldn't have used the tutorial series because it doesn't do true custom tools.
                        // Every time I've had to use AI for this I just get pissed. Why is it this hard to find what a method does at first. I get that after you know once, it's simple and understandable, but...
                        // Same thing with the class extensions! Now that I know that Item implements the extension, it's cool; but... it's super easy to miss if you don't know it does.
                        // All I see in the Javadoc is "all known implementing classes" and a ton of classes. I'll take the blame for not checking every single class that implements it, but... come on...
                        // Also taking the blame on me not seeing the "most places where you'd expect an Item actually use an ItemStack instead" that one's like fully on me
                    } catch (Exception ignored) {
                        pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
                    }
                } else {
                    pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
                }
            } else {
                pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
            }
        } else {
            pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
        }
        return super.onLeftClickEntity(stack, player, entity);
    }
}
