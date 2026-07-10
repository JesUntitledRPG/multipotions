package com.jes.multipotions;

import com.jes.multipotions.merger.MergerBlock;
import com.jes.multipotions.merger.MergerBlockEntity;
import com.jes.multipotions.merger.MergerMenu;
import com.jes.multipotions.merger.MergerScreen;
import com.jes.multipotions.retort.ClavedRetortItem;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.data.PackOutput;
import net.minecraft.data.loot.BlockLootSubProvider;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.item.alchemy.PotionBrewing;
import net.minecraft.world.item.alchemy.Potions;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
import net.neoforged.neoforge.client.model.generators.BlockStateProvider;
import net.neoforged.neoforge.client.model.generators.ModelFile;
import net.neoforged.neoforge.common.data.ExistingFileHelper;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.event.brewing.RegisterBrewingRecipesEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import org.slf4j.Logger;

import com.mojang.logging.LogUtils;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.alchemy.Potion;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;
import net.neoforged.neoforge.registries.DeferredBlock;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.Set;
import java.util.function.Supplier;

// The value here should match an entry in the META-INF/neoforge.mods.toml file
@Mod(JessMultipotions.MODID)
public class JessMultipotions {
    // Define mod id in a common place for everything to reference
    public static final String MODID = "jes_multipots";
    // Directly reference a slf4j logger
    public static final Logger LOGGER = LogUtils.getLogger();
    // Create a Deferred Register to hold Blocks which will all be registered under the "jes_multipots" namespace
    public static final DeferredRegister.Blocks BLOCKS = DeferredRegister.createBlocks(MODID);
    // Create a Deferred Register to hold Items which will all be registered under the "jes_multipots" namespace
    public static final DeferredRegister.Items ITEMS = DeferredRegister.createItems(MODID);
    // Create a Deferred Register to hold CreativeModeTabs which will all be registered under the "jes_multipots" namespace
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS = DeferredRegister.create(Registries.CREATIVE_MODE_TAB, MODID);
    // Create... guess what. It's one of those for potions.
    public static final DeferredRegister<Potion> POTIONS = DeferredRegister.create(BuiltInRegistries.POTION, MODID);
    // One of those Deffered Registers for Block Entities. We don't know the type of them so they're ? type.
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(BuiltInRegistries.BLOCK_ENTITY_TYPE, MODID);
    // menu register woo
    public static final DeferredRegister<MenuType<?>> MENUS = DeferredRegister.create(Registries.MENU, MODID);

    // Creates a new Block with the id "jes_multipots:example_block", combining the namespace and path
    //public static final DeferredBlock<Block> EXAMPLE_BLOCK = BLOCKS.registerSimpleBlock("example_block", BlockBehaviour.Properties.of().mapColor(MapColor.STONE));
    // Creates a new BlockItem with the id "jes_multipots:example_block", combining the namespace and path
    //public static final DeferredItem<BlockItem> EXAMPLE_BLOCK_ITEM = ITEMS.registerSimpleBlockItem("example_block", EXAMPLE_BLOCK);

    // Creates a new HOLDER for potions with the id "jes_multipots:failed_mixture"
    // TODO: Make the Merger output a Failed Mixture.
    public static final Holder<Potion> DISGUSTING_MIX = POTIONS.register("failed_mixture",
            () -> new Potion(new MobEffectInstance(MobEffects.CONFUSION, 300, 1, true, false, true)));

    public static final Holder<Potion> WITHER_POTION = POTIONS.register("wither",
            () -> new Potion(new MobEffectInstance(MobEffects.WITHER, 200, 0, false, true, true)));
    // I think you can't add potions to mod-specific creative tabs, oops

    // Then let's make an item as the "heart of the machine" to have a tab icon
    public static final DeferredItem<Item> MERGER_HEART = ITEMS.registerItem("merger_heart", Item::new, new Item.Properties().stacksTo(1).rarity(Rarity.RARE));

    // We're also adding the machine's tanks.
    public static final DeferredItem<Item> MERGER_TANK = ITEMS.registerItem("merger_tank", Item::new, new Item.Properties().stacksTo(8).rarity(Rarity.UNCOMMON));

    // The first iteration of the machine itself! This is the block itself, NOT the item that places it - we gotta make that later.
    public static final DeferredBlock<MergerBlock> MERGER = BLOCKS.register("merger",
            () -> new MergerBlock(BlockBehaviour.Properties.of()
                    .destroyTime(7.5f)
                    .explosionResistance(10.0f)
                    .sound(SoundType.ANVIL)
                    .lightLevel(state -> 5)
                    .noOcclusion() // finding this property took THREE HOURS. THREE. STRAIGHT. HOURS.
                    .requiresCorrectToolForDrops()
            ));

    // BlockItem for the merger, coming RIGHT up!
    public static final Supplier<BlockItem> MERGER_ITEM = ITEMS.registerSimpleBlockItem(
            "merger", MERGER
    );

    // Here's the Block Entity for the Merger - previously referred to as "the machine".
    public static final Supplier<BlockEntityType<MergerBlockEntity>> MERGER_BE = BLOCK_ENTITIES.register("merger_be", () -> BlockEntityType.Builder.of(
            MergerBlockEntity::new, MERGER.get()
    ).build(null));

    // its menu has given me 8 headaches
    public static final DeferredHolder<MenuType<?>, MenuType<MergerMenu>> MERGER_MENU = MENUS.register("merger_menu", () -> IMenuTypeExtension.create(MergerMenu::new));

    // Adding the Claved Retort; a tool that's meant to extract potion effects from an Armor Stand to place them
    // TODO: Add a recipe and the base components

    public static final DeferredItem<Item> CLAVED_RETORT = ITEMS.registerItem(
            "claved_retort",
            ClavedRetortItem::new,
            new Item.Properties()
    );

    // Creates a creative tab with the id "jes_multipots:example_tab" for the example item, that is placed after the combat tab
    public static final DeferredHolder<CreativeModeTab, CreativeModeTab> MULTIPOTS_TAB = CREATIVE_MODE_TABS.register("multipots_tab", () -> CreativeModeTab.builder()
            .title(Component.translatable("itemGroup.jes_multipots")) //The language key for the title of your CreativeModeTab
            .withTabsBefore(CreativeModeTabs.COMBAT)
            .icon(() -> MERGER_HEART.get().getDefaultInstance())
            .displayItems((parameters, output) -> {
                output.accept(MERGER_HEART.get()); // Add the Heart of the Machine to the tab. For your own tabs, this method is preferred over the event
                output.accept(MERGER_TANK.get());
                output.accept(MERGER_ITEM.get());
                output.accept(CLAVED_RETORT.get());
            }).build());

    // The constructor for the mod class is the first code that is run when your mod is loaded.
    // FML will recognize some parameter types like IEventBus or ModContainer and pass them in automatically.
    public JessMultipotions(IEventBus modEventBus, ModContainer modContainer) {
        // Register the commonSetup method for modloading
        modEventBus.addListener(this::commonSetup);

        // Register the Deferred Register to the mod event bus so blocks get registered
        BLOCKS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so items get registered
        ITEMS.register(modEventBus);
        // Register the Deferred Register to the mod event bus so tabs get registered
        CREATIVE_MODE_TABS.register(modEventBus);
        // You know what's happenning.
        POTIONS.register(modEventBus);
        // Block entity time.
        BLOCK_ENTITIES.register(modEventBus);
        // Menus.
        MENUS.register(modEventBus);

        // Register ourselves for server and other game events we are interested in.
        // Note that this is necessary if and only if we want *this* class (JessMultipotions) to respond directly to events.
        // Do not add this line if there are no @SubscribeEvent-annotated functions in this class, like onServerStarting() below.
        NeoForge.EVENT_BUS.register(this);

        // Register the item to a creative tab
        //modEventBus.addListener(this::addCreative);

        // Register our mod's ModConfigSpec so that FML can create and load the config file for us
        modContainer.registerConfig(ModConfig.Type.COMMON, Config.SPEC);
    }

    private void commonSetup(FMLCommonSetupEvent event) {
        // Some common setup code
        LOGGER.info("Hi from Jes's Multipotions at common setup!");
        /* Some example code that I'm commenting out
        if (Config.LOG_DIRT_BLOCK.getAsBoolean()) {
            LOGGER.info("DIRT BLOCK >> {}", BuiltInRegistries.BLOCK.getKey(Blocks.DIRT));
        }

        LOGGER.info("{}{}", Config.MAGIC_NUMBER_INTRODUCTION.get(), Config.MAGIC_NUMBER.getAsInt());

        Config.ITEM_STRINGS.get().forEach((item) -> LOGGER.info("ITEM >> {}", item));
         */
    }


    // You can use SubscribeEvent and let the Event Bus discover methods to call
    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        // Do something when the server starts
        LOGGER.info("Hi again from Jes's Multipotions at server start!");
    }

    @SubscribeEvent
    public void onServerReady(ServerStartedEvent event) {
        // In case we need to do something when the server's started up
    }

    public void onClientSetup(FMLClientSetupEvent event) {

    }

    @EventBusSubscriber
    public static class onModBus {
        // Here's the merger menu.
        // Registering screens here ONLY FOR THE CLIENT!
        // We need to do this on the Mod Event Bus. This is why the onModBus class was made.
        // Everything here will get called into the Mod Event Bus.
        @SubscribeEvent
        public static void registerScreens(RegisterMenuScreensEvent event) {
            event.register(MERGER_MENU.get(), MergerScreen::new);
        }
        @SubscribeEvent
        public static void registerCapabilities(RegisterCapabilitiesEvent event) {
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    MERGER_BE.get(),
                    (MergerBlockEntity, NORTH) -> {
                        return MergerBlockEntity.automationWrapper;
                    }
            );
        }
    }

    // Some datagen here.
    public static class MultipotsBlockStateProvider extends BlockStateProvider {
        public MultipotsBlockStateProvider(PackOutput output, ExistingFileHelper existingFileHelper) {
            super(output, MODID, existingFileHelper);
        }

        ModelFile mergerModelFile = models().withExistingParent("merger_model", this.mcLoc("block/merger"));

        @Override
        protected void registerStatesAndModels() {
            simpleBlockWithItem(MERGER.get(), mergerModelFile);
        }
    }

    public static class MultipotsBlockLootTableSubprovider extends BlockLootSubProvider {
        public MultipotsBlockLootTableSubprovider(HolderLookup.Provider lookupProvider) {
            super(Set.of(), FeatureFlags.DEFAULT_FLAGS, lookupProvider);
        } // Seems that when we extend something we have to add its argument constructor.

        // We need to add a validation Iterable.
        @Override
        protected Iterable<Block> getKnownBlocks() {
            return BLOCKS.getEntries().stream()
                    // Apparently Java complains sometimes if we don't cast a block to itself.
                    .map(e -> (Block) e.value())
                    .toList();
        }

        @Override
        protected void generate() {
            dropSelf(MERGER.get());
        }
    }

    @SubscribeEvent
    public void registerBrewingRecipes(RegisterBrewingRecipesEvent event) {
        PotionBrewing.Builder builder = event.getBuilder();

        builder.addMix(
                Potions.AWKWARD,
                Items.WITHER_ROSE,
                WITHER_POTION
        );
    }
}
