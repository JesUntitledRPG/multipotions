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
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.item.component.Tool;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.common.Tags;
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
                )
                .durability(71) // 71 uses since the Liebig condenser which substitutes retorts in most standard lab usage was made in 1771
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
    public @NotNull InteractionResult interactLivingEntity(@NotNull ItemStack stack, Player player, @NotNull LivingEntity interactionTarget, @NotNull InteractionHand usedHand) {
        // We're working IN a level. I'm guessing this won't crash, then?
        Level pLevel = player.level();
        if (!pLevel.isClientSide() & !player.getCooldowns().isOnCooldown(this.asItem())) {
            if (usedHand == InteractionHand.MAIN_HAND) {
                Inventory inv = player.getInventory();
                if(inv.contains(new ItemStack(Items.GLASS_BOTTLE))) {
                    if(!(interactionTarget.getType() == (EntityType.PLAYER))) {
                        if(!interactionTarget.getActiveEffects().isEmpty()) { // for SOME reason we need this
                            if(!Config.FREE_WATER.getAsBoolean() | Config.FREE_WATER.getAsBoolean() & inv.contains(new ItemStack(Items.WATER_BUCKET))) { // Free water's sorta weird. TODO: Allow more sources of water.
                                pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_BELL, SoundSource.PLAYERS, 1f, 1f, 0);
                                pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.ITEM_PICKUP, SoundSource.PLAYERS, 1f, 1f, 0);
                                ArrayList<MobEffectInstance> effects = new ArrayList<>(interactionTarget.getActiveEffects()); // IDEA compressed a ton of lines into one
                                PotionContents outContents = new PotionContents(Optional.of(Potions.MUNDANE), Optional.empty(), effects);
                                ItemStack output = new ItemStack(Items.POTION);
                                output.set(DataComponents.POTION_CONTENTS, outContents);
                                int slot = inv.findSlotMatchingItem(new ItemStack(Items.GLASS_BOTTLE));
                                JessMultipotions.LOGGER.info(String.valueOf(slot));
                                if (slot == -1) { // we don't have a slot... either our inventory is full or we only have glass bottles offhand
                                    boolean inOffhand = (player.getOffhandItem().getItem() == Items.GLASS_BOTTLE);
                                    if (!inOffhand | (player.getOffhandItem().getCount() > 1)) {
                                        player.drop(output, true, false); // yeah, just toss it on the floor ig
                                        player.getOffhandItem().shrink(1); // THIS! This is how you do it- oh my god. Why does inv.remove not work, but this does? ISTFG.
                                    } else {
                                        player.setItemInHand(InteractionHand.OFF_HAND, output); // thank god
                                    }
                                }
                                if (!(slot == -1)) {
                                    inv.setItem(slot, output); // only executing if slot != -1, we use the method above in other cases
                                }
                                interactionTarget.removeAllEffects(); // AND WE'RE STILL GOING
                                player.getCooldowns().addCooldown(this.asItem(), 60); // I did have to look this up through AI again because all the docs I found were 1.21.2+. Which IMO is a way better way of doing cooldowns.
                                // I LOOKED AT THE JAVADOC THIS TIME BTW.
                                // So like. I tried. Like, ItemCooldowns being associated to the player makes sense, but, again...
                                // It's not even in the docs I searched... I think.
                            } else {
                                pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
                                player.displayClientMessage(Component.translatable("item.jes_multipots.claved_retort.eject.noWater"),true);
                            }
                        } else {
                            pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
                            player.displayClientMessage(Component.translatable("item.jes_multipots.claved_retort.eject.empty"),true);
                        }
                    } else {
                        pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
                        player.displayClientMessage(Component.translatable("item.jes_multipots.claved_retort.eject.useOnPlayer"),true);
                    }
                } else {
                    pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
                    player.displayClientMessage(Component.translatable("item.jes_multipots.claved_retort.eject.noBottle"),true);
                }
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean onLeftClickEntity(@NotNull ItemStack stack, Player player, @NotNull Entity entity) {
        if (!player.getCooldowns().isOnCooldown(this.asItem())) {
            Level pLevel = player.level();
            if (!(player.getOffhandItem().isEmpty())) {
                if (player.getMainHandItem().getItem() == JessMultipotions.CLAVED_RETORT.asItem()) {
                    ItemStack otherHand = player.getOffhandItem();
                    if (entity instanceof LivingEntity livingEntity) { // Not gonna lie. I had to ask AI how to do this. Apparently instanceof can cast new objects...?
                        if (!(entity.getType() == (EntityType.PLAYER))) {
                            try {
                                pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_PLING, SoundSource.PLAYERS, 1f, 1.0f, 0);
                                otherHand.finishUsingItem(player.level(), livingEntity);    // Also this. Question to anyone who knows this; I get _why_ an ItemStack would have this method, but _why_ is there an Item.use() method then? This confused me for like... 1 hour and 15 minutes.
                                // No, really. Was this in the 26.1 docs or something and I missed it because I only look at the 1.21.1 docs? And no, I couldn't have used the tutorial series because it doesn't do true custom tools.
                                // Every time I've had to use AI for this I just get pissed. Why is it this hard to find what a method does at first. I get that after you know once, it's simple and understandable, but...
                                // Same thing with the class extensions! Now that I know that Item implements the extension, it's cool; but... it's super easy to miss if you don't know it does.
                                // All I see in the Javadoc is "all known implementing classes" and a ton of classes. I'll take the blame for not checking every single class that implements it, but... come on...
                                // Also taking the blame on me not seeing the "most places where you'd expect an Item actually use an ItemStack instead" that one's like fully on me

                                // TODO: Prevent items that can't do anything from being used. (This would be easier in 1.21.2+, but alas...)
                                player.getInventory().removeItem(otherHand);
                                player.getCooldowns().addCooldown(this.asItem(), 60);
                                // There's probably a WAY better way to do this...
                                // If someone knows, PLEASE tell me.
                                if(otherHand.getItem() == Items.POTION | otherHand.getTags().anyMatch(e -> e == Tags.Items.DRINK_CONTAINING_BOTTLE)) {
                                    if(otherHand.getCount() == 1) {
                                        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.GLASS_BOTTLE));
                                    } else {
                                        player.getInventory().setItem(player.getInventory().findSlotMatchingItem(new ItemStack(Items.GLASS_BOTTLE)), new ItemStack(Items.GLASS_BOTTLE));
                                    }
                                } else if (otherHand.getTags().anyMatch(e -> e == Tags.Items.BUCKETS | otherHand.getTags().anyMatch(f -> f == Tags.Items.DRINK_CONTAINING_BUCKET))) {
                                    if(otherHand.getCount() == 1) {
                                        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BUCKET));
                                    } else {
                                        player.getInventory().setItem(player.getInventory().findSlotMatchingItem(new ItemStack(Items.BUCKET)), new ItemStack(Items.BUCKET));
                                    }
                                } else if (otherHand.getTags().anyMatch(e -> e == Tags.Items.FOODS_SOUP)) {
                                    if(otherHand.getCount() == 1) {
                                        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.BOWL));
                                    } else {
                                        player.getInventory().setItem(player.getInventory().findSlotMatchingItem(new ItemStack(Items.BOWL)), new ItemStack(Items.BOWL));
                                    }
                                }
                            } catch (Exception ignored) {
                                player.displayClientMessage(Component.translatable("item.jes_multipots.claved_retort.inject.itemError"),true);
                                pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
                            }
                        } else {
                            player.displayClientMessage(Component.translatable("item.jes_multipots.claved_retort.inject.useOnPlayer"),true);
                            pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
                        }
                    } else {
                        player.displayClientMessage(Component.translatable("item.jes_multipots.claved_retort.inject.notLiving"),true);
                        pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
                    }
                } else {
                    player.displayClientMessage(Component.translatable("item.jes_multipots.claved_retort.inject.offhand"),true);
                    pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
                }
            } else {
                player.displayClientMessage(Component.translatable("item.jes_multipots.claved_retort.inject.empty"),true);
                pLevel.playSeededSound(null, player.getX(), player.getY(), player.getZ(), SoundEvents.NOTE_BLOCK_HAT, SoundSource.PLAYERS, 1f, 0.5f, 0);
            }
        }
        return true;
    }
}
