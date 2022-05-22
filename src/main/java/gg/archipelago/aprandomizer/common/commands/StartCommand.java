package gg.archipelago.aprandomizer.common.commands;import com.mojang.brigadier.CommandDispatcher;import com.mojang.brigadier.context.CommandContext;import gg.archipelago.APClient.ClientStatus;import gg.archipelago.aprandomizer.APRandomizer;import gg.archipelago.aprandomizer.common.Utils.Utils;import net.minecraft.commands.CommandSourceStack;import net.minecraft.commands.Commands;import net.minecraft.core.BlockPos;import net.minecraft.network.chat.TextComponent;import net.minecraft.resources.ResourceLocation;import net.minecraft.server.MinecraftServer;import net.minecraft.server.level.ServerLevel;import net.minecraft.server.level.ServerPlayer;import net.minecraft.stats.Stats;import net.minecraft.world.Clearable;import net.minecraft.world.item.ItemStack;import net.minecraft.world.level.GameRules;import net.minecraft.world.level.Level;import net.minecraft.world.level.block.Block;import net.minecraft.world.level.block.Blocks;import net.minecraft.world.level.block.state.BlockState;import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;import net.minecraftforge.event.RegisterCommandsEvent;import net.minecraftforge.eventbus.api.SubscribeEvent;import net.minecraftforge.fml.common.Mod;import org.apache.logging.log4j.LogManager;import org.apache.logging.log4j.Logger;@Mod.EventBusSubscriberpublic class StartCommand {    // Directly reference a log4j logger.    private static final Logger LOGGER = LogManager.getLogger();    //build our command structure and submit it    public static void Register(CommandDispatcher<CommandSourceStack> dispatcher) {        dispatcher.register(                Commands.literal("start") //base slash command is "start"                .executes(context -> Start(context,false))                        .then(                                Commands.literal("force")                                        .executes(context -> Start(context,true))                        )        );    }    private static int Start(CommandContext<CommandSourceStack> commandSourceCommandContext, boolean force) {        if(!APRandomizer.getAP().isConnected() && !force) {            commandSourceCommandContext.getSource().sendFailure(new TextComponent("Please connect to the Archipelago server before starting."));            return 1;        }        if(!APRandomizer.isJailPlayers()) {            commandSourceCommandContext.getSource().sendFailure(new TextComponent("The game has already started! what are you doing? START PLAYING!"));            return 1;        }        Utils.sendMessageToAll("GO!");        if (APRandomizer.isConnected()) {            APRandomizer.getAP().setGameState(ClientStatus.CLIENT_PLAYING);        }        APRandomizer.setJailPlayers(false);        MinecraftServer server = APRandomizer.getServer();        ServerLevel overworld = server.getLevel(Level.OVERWORLD);        BlockPos spawn = overworld.getSharedSpawnPos();        // alter the spawn box position, so it doesn't interfere with spawning        StructureTemplate jailStruct = overworld.getStructureManager().get(new ResourceLocation(APRandomizer.MODID,"spawnjail")).get();        BlockPos jailPos = new BlockPos(spawn.getX(), 300, spawn.getZ());        for (BlockPos blockPos : BlockPos.betweenClosed(jailPos, jailPos.offset(jailStruct.getSize()))) {            overworld.setBlock(blockPos, Blocks.AIR.defaultBlockState(), Block.UPDATE_CLIENTS);        }        server.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(true, server);        server.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(true, server);        server.getGameRules().getRule(GameRules.RULE_DOFIRETICK).set(true, server);        server.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).value = 3;        server.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).onChanged(server);        server.getGameRules().getRule(GameRules.RULE_DO_PATROL_SPAWNING).set(true,server);        server.getGameRules().getRule(GameRules.RULE_DO_TRADER_SPAWNING).set(true,server);        server.getGameRules().getRule(GameRules.RULE_MOBGRIEFING).set(true,server);        server.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(true,server);        server.getGameRules().getRule(GameRules.RULE_DO_IMMEDIATE_RESPAWN).set(false,server);        server.getGameRules().getRule(GameRules.RULE_DOMOBLOOT).set(true,server);        server.getGameRules().getRule(GameRules.RULE_DOENTITYDROPS).set(true,server);        server.execute(() -> {            for (ServerPlayer player : server.getPlayerList().getPlayers()) {                player.getFoodData().eat(20,20);                player.setHealth(20);                player.getInventory().clearContent();                player.resetStat(Stats.CUSTOM.get(Stats.TIME_SINCE_REST));                player.teleportTo(spawn.getX(),spawn.getY(),spawn.getZ());                APRandomizer.getItemManager().catchUpPlayer(player);                for (ItemStack iStack : APRandomizer.getAP().getSlotData().startingItemStacks) {                    Utils.giveItemToPlayer(player, iStack.copy());                }            }        });        return 1;    }    //wait for register commands event then register ourself as a command.    @SubscribeEvent    static void onRegisterCommandsEvent(RegisterCommandsEvent event) {        StartCommand.Register(event.getDispatcher());    }}