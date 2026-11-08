package de.geheimagentnr1.discordintegration.elements.discord;

import de.geheimagentnr1.discordintegration.config.ServerConfig;
import de.geheimagentnr1.discordintegration.net.DiscordNet;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.util.text.event.HoverEvent;

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
						String displayName = member != null ? member.getEffectiveName() : author.getEffectiveName();
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
				server.getPlayerList().sendMessage( new StringTextComponent( message ), true );
			}
		}
	}
	
	private void handleUserMessage( String author, String hoverHandle, String message ) {
		
		if( ServerConfig.getMaxCharCount() == -1 || message.length() <= ServerConfig.getMaxCharCount() ) {
			ITextComponent nameComponent = new StringTextComponent( author );
			nameComponent.getStyle().setHoverEvent(
				new HoverEvent( HoverEvent.Action.SHOW_TEXT, new StringTextComponent( hoverHandle ) )
			);
			ITextComponent fullMessage = new StringTextComponent( "[" )
				.appendSibling( nameComponent )
				.appendText( "] " + message );
			server.getPlayerList().sendMessage( fullMessage, true );
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
