package de.geheimagentnr1.discordintegration.handlers;

import com.mojang.brigadier.CommandDispatcher;
import de.geheimagentnr1.discordintegration.DiscordIntegration;
import de.geheimagentnr1.discordintegration.config.ServerConfig;
import de.geheimagentnr1.discordintegration.elements.commands.DiscordCommand;
import de.geheimagentnr1.discordintegration.elements.commands.MeToDiscordCommand;
import de.geheimagentnr1.discordintegration.elements.commands.SayToDiscordCommand;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordEventHandler;
import de.geheimagentnr1.discordintegration.net.DiscordNet;
import net.minecraft.advancements.DisplayInfo;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.passive.TameableEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.server.FMLServerStartedEvent;
import net.minecraftforge.fml.event.server.FMLServerStartingEvent;
import net.minecraftforge.fml.event.server.FMLServerStoppedEvent;


@Mod.EventBusSubscriber( modid = DiscordIntegration.MODID,
	bus = Mod.EventBusSubscriber.Bus.FORGE,
	value = Dist.DEDICATED_SERVER )
public class ForgeEventHandler {
	
	
	@SubscribeEvent
	public static void handleServerStartingEvent( FMLServerStartingEvent event ) {
		
		DiscordEventHandler.setServer( event.getServer() );
	}
	
	@SubscribeEvent
	public static void handlerRegisterCommandsEvent( RegisterCommandsEvent event ) {
		
		CommandDispatcher<CommandSource> dispatcher = event.getDispatcher();
		DiscordCommand.register( dispatcher );
		MeToDiscordCommand.register( dispatcher );
		SayToDiscordCommand.register( dispatcher );
	}
	
	@SubscribeEvent
	public static void handleServerStartedEvent( FMLServerStartedEvent event ) {
		
		DiscordNet.sendServerMessage( ServerConfig.getStartMessage() );
	}
	
	@SubscribeEvent
	public static void handleServerStoppedEvent( FMLServerStoppedEvent event ) {
		
		if( event.getServer().isServerRunning() ) {
			DiscordNet.sendServerMessage( "Server crashed" );
		} else {
			DiscordNet.sendServerMessage( ServerConfig.getStopMessage() );
		}
		DiscordNet.stop();
	}
	
	@SubscribeEvent
	public static void handlePlayerLoggedInEvent( PlayerEvent.PlayerLoggedInEvent event ) {
		
		DiscordNet.sendPlayerEventMessage(
			event.getPlayer(),
			"joined the game.",
			ServerConfig.getPlayerJoinedUsePlayerWebhook()
		);
	}
	
	@SubscribeEvent
	public static void handlePlayerLoggedOutEvent( PlayerEvent.PlayerLoggedOutEvent event ) {
		
		DiscordNet.sendPlayerEventMessage(
			event.getPlayer(),
			"disconnected.",
			ServerConfig.getPlayerLeftUsePlayerWebhook()
		);
	}
	
	@SubscribeEvent
	public static void handleServerChatEvent( ServerChatEvent event ) {
		
		DiscordNet.sendChatMessage( event.getPlayer(), event.getMessage() );
	}
	
	@SubscribeEvent
	public static void handleLivingDeathEvent( LivingDeathEvent event ) {
		
		LivingEntity entity = event.getEntityLiving();
		
		if( entity instanceof PlayerEntity ) {
			String name = entity.getDisplayName().getString();
			String deathMessage = event.getSource()
				.getDeathMessage( entity )
				.getString()
				.replace( name, "**" + name + "**" );
			DiscordNet.sendPlayerRawOrServerMessage(
				(PlayerEntity)entity,
				deathMessage,
				ServerConfig.getPlayerDiedUsePlayerWebhook()
			);
		} else if( entity instanceof TameableEntity && ( (TameableEntity)entity ).getOwnerId() != null ) {
			String name = entity.getDisplayName().getString();
			DiscordNet.sendServerMessage(
				event.getSource()
					.getDeathMessage( entity )
					.getString()
					.replace( name, "**" + name + "**" )
			);
		}
	}
	
	@SubscribeEvent
	public static void handleAdvancementEvent( AdvancementEvent event ) {
		
		DisplayInfo displayInfo = event.getAdvancement().getDisplay();
		
		if( displayInfo != null && displayInfo.shouldAnnounceToChat() ) {
			DiscordNet.sendPlayerEventMessage(
				event.getPlayer(),
				String.format(
					"has made the advancement **%s**%n*%s*",
					displayInfo.getTitle().getString(),
					displayInfo.getDescription().getString()
				),
				ServerConfig.getPlayerGotAdvancementUsePlayerWebhook()
			);
		}
	}
}
