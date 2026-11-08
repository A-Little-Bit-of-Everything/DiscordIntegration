package de.geheimagentnr1.discordintegration.elements.discord;

import de.geheimagentnr1.discordintegration.config.ServerConfig;
import de.geheimagentnr1.discordintegration.net.DiscordNet;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.Util;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;

import javax.annotation.Nonnull;


public class DiscordEventHandler extends ListenerAdapter {
	
	
	private static MinecraftServer server;
	
	public static void setServer( MinecraftServer _server ) {
		
		server = _server;
	}
	
	@Override
	public void onSlashCommandInteraction( @Nonnull SlashCommandInteractionEvent event ) {
		
		if( !event.isFromGuild() || event.getChannelType() != ChannelType.TEXT ) {
			return;
		}
		if( isInitialized() && event.getChannel().getIdLong() == ServerConfig.getChannelId() ) {
			DiscordCommandHandler.handleSlashCommandInteraction( event, server );
		}
	}
	
	@Override
	public void onMessageReceived( @Nonnull MessageReceivedEvent event ) {
		
		if( !event.isFromGuild() || event.getChannelType() != ChannelType.TEXT ) {
			return;
		}
		User author = event.getAuthor();
		Member member = event.getMember();
		if( isInitialized() && DiscordNet.feedBackAllowed( event.getChannel().asTextChannel(), author ) ) {
			String message = event.getMessage().getContentDisplay();
			if( author.isBot() ) {
				handleBotMessage( message );
			} else {
				if( message.startsWith( ServerConfig.getCommandPrefix() ) ) {
					handleCommands( author, message );
				} else {
					if( beginnsNotWithOtherCommandPrefix( message ) ) {
						String displayName = ( ServerConfig.isUseNickname() && member != null ) ?
							member.getEffectiveName() :
							author.getEffectiveName();
						String hoverHandle = "@" + author.getName();
						handleUserMessage( displayName, hoverHandle, message );
					}
				}
			}
		}
	}
	
	private boolean isInitialized() {
		
		return server != null && DiscordNet.isInitialized();
	}
	
	private boolean beginnsNotWithOtherCommandPrefix( String message ) {
		
		for( String prefix : ServerConfig.getOtherBotsCommandPrefixes() ) {
			if( message.startsWith( prefix ) ) {
				return false;
			}
		}
		return true;
	}
	
	private void handleCommands( User author, String command ) {
		
		if( !author.isBot() ) {
			if( !DiscordCommandHandler.handleCommand( command, server ) ) {
				DiscordNet.sendFeedbackMessage( String.format( "%n%s%nError: Unknown Command", author.getName() ) );
			}
		}
	}
	
	private void handleBotMessage( String message ) {
		
		if( ServerConfig.isTransmitBotMessages() ) {
			if( !message.startsWith( DiscordNet.FEEDBACK_START ) || !message.endsWith( DiscordNet.FEEDBACK_END ) ) {
				server.getPlayerList().broadcastMessage(
					new TextComponent( message ),
					ChatType.CHAT,
					Util.NIL_UUID
				);
			}
		}
	}
	
	private void handleUserMessage( String author, String hoverHandle, String message ) {
		
		if( ServerConfig.getMaxCharCount() == -1 || message.length() <= ServerConfig.getMaxCharCount() ) {
			MutableComponent nameComponent = new TextComponent( author ).withStyle(
				style -> style.withHoverEvent(
					new HoverEvent( HoverEvent.Action.SHOW_TEXT, new TextComponent( hoverHandle ) )
				)
			);
			MutableComponent fullMessage = new TextComponent( "[" )
				.append( nameComponent )
				.append( new TextComponent( "] " + message ) );
			server.getPlayerList().broadcastMessage(
				fullMessage,
				ChatType.CHAT,
				Util.NIL_UUID
			);
		} else {
			DiscordNet.sendFeedbackMessage( String.format(
				"%n%s%nError: Message to long.%n" +
					"Messages can only be up to %d characters long.%n" +
					"Your message is %d characters long.",
				author,
				ServerConfig.getMaxCharCount(),
				message.length()
			) );
		}
	}
}
