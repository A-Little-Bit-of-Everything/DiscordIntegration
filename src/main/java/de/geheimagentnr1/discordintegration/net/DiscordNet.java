package de.geheimagentnr1.discordintegration.net;

import de.geheimagentnr1.discordintegration.config.ServerConfig;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordCommandHandler;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordEventHandler;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.exceptions.InvalidTokenException;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.minecraft.command.CommandSource;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.text.ITextComponent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.slf4j.simple.SimpleServiceProvider;

import java.util.Arrays;
import java.util.List;


public class DiscordNet {
	
	
	private static final Logger LOGGER = LogManager.getLogger( DiscordNet.class );
	
	static {
		//SLF4J 2 (needed by JDA) doesn't find the bundled provider over the service loader in Forge,
		//so it is set directly. The class name is the relocated one in the mod jar.
		if( System.getProperty( "slf4j.provider" ) == null ) {
			System.setProperty( "slf4j.provider", SimpleServiceProvider.class.getName() );
		}
	}
	
	private static final String WEBHOOK_NAME = "MinecraftChatBridge";
	
	public static final String FEEDBACK_START = "**```";
	
	public static final String FEEDBACK_END = "```**";
	
	private static JDA jda;
	
	private static TextChannel channel;
	
	private static Webhook webhook;
	
	private static final List<GatewayIntent> INTENTS = Arrays.asList( GatewayIntent.GUILD_MESSAGES, GatewayIntent.MESSAGE_CONTENT );
	
	public static synchronized void init() {
		
		stop();
		if( ServerConfig.getActive() ) {
			try {
				jda = JDABuilder.create( ServerConfig.getBotToken(), INTENTS )
					.addEventListeners( new DiscordEventHandler() )
					.setAutoReconnect( true )
					.build();
				jda.awaitReady();
				channel = jda.getTextChannelById( ServerConfig.getChannelId() );
				if( channel == null ) {
					LOGGER.error( "Discord Text Channel {} not found", ServerConfig.getChannelId() );
				} else {
					initWebhook();
					DiscordCommandHandler.registerSlashCommands( channel.getGuild() );
				}
			} catch( InvalidTokenException | InterruptedException exception ) {
				LOGGER.error( "Login to Discord failed", exception );
			}
		}
	}
	
	private static void initWebhook() {
		
		if( channel == null ) {
			return;
		}
		try {
			List<Webhook> webhooks = channel.retrieveWebhooks().complete();
			long selfUserId = jda.getSelfUser().getIdLong();
			webhook = webhooks.stream()
				.filter( wh -> WEBHOOK_NAME.equals( wh.getName() ) &&
					wh.getOwnerAsUser() != null &&
					wh.getOwnerAsUser().getIdLong() == selfUserId &&
					wh.getToken() != null )
				.findFirst()
				.orElseGet( () -> channel.createWebhook( WEBHOOK_NAME ).complete() );
		} catch( Exception exception ) {
			LOGGER.warn(
				"Could not initialize webhook for Discord Text Channel {} (missing MANAGE_WEBHOOKS permission?), falling back to bot messages",
				channel.getIdLong(),
				exception
			);
			webhook = null;
		}
	}
	
	public static synchronized void stop() {
		
		if( isJdaInitialized() ) {
			jda.shutdown();
			channel = null;
			webhook = null;
			jda = null;
		}
	}
	
	private static synchronized boolean isJdaInitialized() {
		
		return jda != null;
	}
	
	public static synchronized boolean isInitialized() {
		
		return isJdaInitialized() && channel != null;
	}
	
	public static synchronized boolean feedBackAllowed( TextChannel _channel, User author ) {
		
		return _channel.getIdLong() == ServerConfig.getChannelId() && _channel.getIdLong() == channel.getIdLong() &&
			author.getIdLong() != jda.getSelfUser().getIdLong() &&
			( webhook == null || author.getIdLong() != webhook.getIdLong() );
	}
	
	public static void sendPlayerEventMessage( PlayerEntity player, String message, boolean usePlayerWebhook ) {
		
		String formatted = String.format( "**%s** %s", getPlayerName( player ), message );
		if( usePlayerWebhook ) {
			sendPlayerWebhookOrFallback( player, formatted, formatted );
		} else {
			sendServerMessage( formatted );
		}
	}
	
	public static void sendPlayerRawOrServerMessage( PlayerEntity player, String rawMessage, boolean usePlayerWebhook ) {
		
		if( usePlayerWebhook ) {
			sendPlayerWebhookOrFallback( player, rawMessage, rawMessage );
		} else {
			sendServerMessage( rawMessage );
		}
	}
	
	public static void sendPlayerMessage( PlayerEntity player, String message ) {
		
		sendPlayerEventMessage( player, message, true );
	}
	
	public static void sendChatMessage( PlayerEntity player, String message ) {
		
		sendPlayerWebhookOrFallback(
			player,
			message,
			String.format( "**[%s]** %s", getPlayerName( player ), message )
		);
	}
	
	private static String getPlayerName( PlayerEntity player ) {
		
		return player.getDisplayName().getString();
	}
	
	public static void sendChatMessage( CommandSource source, ITextComponent message ) {
		
		sendCommandChatMessage( source, message.getString() );
	}
	
	public static void sendMeChatMessage( CommandSource source, String action ) {
		
		sendCommandChatMessage( source, String.format( "*%s*", action ) );
	}
	
	private static void sendCommandChatMessage( CommandSource source, String message ) {
		
		String fallback = String.format( "**[%s]** %s", source.getDisplayName().getString(), message );
		if( source.getEntity() instanceof PlayerEntity ) {
			sendPlayerWebhookOrFallback( (PlayerEntity)source.getEntity(), message, fallback );
		} else {
			sendServerWebhookOrFallback( message, fallback );
		}
	}
	
	private static void sendChatMessage( String name, String message ) {
		
		sendMessage( String.format( "**[%s]** %s", name, message ) );
	}
	
	public static void sendServerMessage( String message ) {
		
		sendServerWebhookOrFallback( message, message );
	}
	
	private static synchronized void sendPlayerWebhookOrFallback(
		PlayerEntity player,
		String webhookMessage,
		String fallbackMessage ) {
		
		if( isInitialized() ) {
			if( webhook != null ) {
				sendWebhookMessage( getPlayerName( player ), getPlayerAvatarUrl( player ), webhookMessage );
			} else {
				sendMessage( fallbackMessage );
			}
		}
	}
	
	private static synchronized void sendServerWebhookOrFallback(
		String webhookMessage,
		String fallbackMessage ) {
		
		if( isInitialized() ) {
			if( webhook != null ) {
				sendWebhookMessage( getServerWebhookName(), getServerAvatarUrl(), webhookMessage );
			} else {
				sendMessage( fallbackMessage );
			}
		}
	}
	
	private static void sendWebhookMessage( String username, String avatarUrl, String message ) {
		
		try {
			for( int start = 0; start < message.length(); start += 2000 ) {
				String part = message.substring( start, Math.min( message.length(), start + 2000 ) );
				webhook.sendMessage( part )
					.setUsername( username )
					.setAvatarUrl( avatarUrl )
					.queue();
			}
		} catch( Exception exception ) {
			LOGGER.error( "Webhook message could not be sent", exception );
		}
	}
	
	private static String getPlayerAvatarUrl( PlayerEntity player ) {
		
		String profileName = player.getGameProfile().getName();
		if( profileName != null && !profileName.trim().isEmpty() ) {
			return "https://mc-heads.net/avatar/" + profileName + "/128.png";
		}
		return "https://mc-heads.net/avatar/" + player.getUniqueID() + "/128.png";
	}
	
	private static String getServerWebhookName() {
		
		String configured = ServerConfig.getServerWebhookName();
		if( configured != null && !configured.trim().isEmpty() ) {
			return configured;
		}
		return jda.getSelfUser().getEffectiveName();
	}
	
	private static String getServerAvatarUrl() {
		
		String configured = ServerConfig.getServerWebhookAvatarUrl();
		if( configured != null && !configured.trim().isEmpty() ) {
			return configured;
		}
		return jda.getSelfUser().getEffectiveAvatarUrl();
	}
	
	public static void sendFeedbackMessage( String message ) {
		
		for( int start = 0; start <= message.length(); start += 1990 ) {
			sendMessage( FEEDBACK_START + message.substring( start, Math.min( message.length(), start + 1990 ) ) +
				FEEDBACK_END );
		}
	}
	
	public static synchronized void sendMessage( String message ) {
		
		if( isInitialized() ) {
			try {
				for( int start = 0; start < message.length(); start += 2000 ) {
					channel.sendMessage( message.substring( start, Math.min( message.length(), start + 2000 ) ) )
						.queue();
				}
			} catch( Exception exception ) {
				LOGGER.error( "Message could not be send", exception );
			}
		}
	}
}
