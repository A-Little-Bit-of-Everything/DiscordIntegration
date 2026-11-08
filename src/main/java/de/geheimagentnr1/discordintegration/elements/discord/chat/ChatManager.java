package de.geheimagentnr1.discordintegration.elements.discord.chat;

import de.geheimagentnr1.discordintegration.config.ServerConfig;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordManager;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordMessageBuilder;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordMessageSender;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;

import java.util.List;


@SuppressWarnings( { "SynchronizeOnThis", "NestedSynchronizedStatement" } )
@Log4j2
public class ChatManager {
	
	
	private static final String WEBHOOK_NAME = "MinecraftChatBridge";
	
	private static TextChannel channel;
	
	private static Webhook webhook;
	
	public static void init() {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				stop();
				if( shouldInitialize() ) {
					long channelId = ServerConfig.CHAT_CONFIG.getChannelId();
					JDA jda = DiscordManager.getJda();
					channel = jda.getTextChannelById( channelId );
					if( channel == null ) {
						log.error( "Discord Chat Text Channel {} not found", channelId );
					} else {
						initWebhook();
					}
				}
			}
		}
	}
	
	private static void initWebhook() {
		
		if( channel == null ) {
			return;
		}
		try {
			List<Webhook> webhooks = channel.retrieveWebhooks().complete();
			long selfUserId = DiscordManager.getSelfUser().getIdLong();
			webhook = webhooks.stream()
				.filter( wh -> WEBHOOK_NAME.equals( wh.getName() ) &&
					wh.getOwnerAsUser() != null &&
					wh.getOwnerAsUser().getIdLong() == selfUserId &&
					wh.getToken() != null )
				.findFirst()
				.orElseGet( () -> channel.createWebhook( WEBHOOK_NAME ).complete() );
		} catch( Exception exception ) {
			log.warn(
				"Could not initialize webhook for Discord Chat Text Channel {} (missing MANAGE_WEBHOOKS permission?), falling back to bot messages",
				channel.getIdLong(),
				exception
			);
			webhook = null;
		}
	}
	
	public static synchronized void stop() {
		
		channel = null;
		webhook = null;
	}
	
	private static boolean shouldInitialize() {
		
		return DiscordManager.isInitialized() && ServerConfig.CHAT_CONFIG.isEnabled();
	}
	
	private static boolean isInitialized() {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				return shouldInitialize() && channel != null;
			}
		}
	}
	
	//package-private
	static boolean isCorrectChannel( long channelId ) {
		
		return isInitialized() && ServerConfig.CHAT_CONFIG.getChannelId() == channelId;
	}
	
	//package-private
	static boolean isOwnWebhook( long webhookOrAuthorId ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				return webhook != null && webhook.getIdLong() == webhookOrAuthorId;
			}
		}
	}
	
	public static void sendMeChatMessage( CommandSourceStack source, String action ) {
		
		if( source.getEntity() instanceof Player player ) {
			sendPlayerMessage(
				player,
				DiscordMessageBuilder.buildWebhookMeChatMessage( action ),
				DiscordMessageBuilder.buildMeChatMessage( source, action )
			);
		} else {
			sendServerMessage(
				DiscordMessageBuilder.buildWebhookMeChatMessage( action ),
				DiscordMessageBuilder.buildMeChatMessage( source, action )
			);
		}
	}
	
	public static void sendChatMessage( CommandSourceStack source, Component message ) {
		
		if( !ServerConfig.CHAT_CONFIG.suppressServerMessages() ||
			!"Server".equals( source.getTextName() ) ||
			source.getEntity() != null ) {
			if( source.getEntity() instanceof Player player ) {
				sendPlayerMessage(
					player,
					DiscordMessageBuilder.buildWebhookChatMessage( message.getString() ),
					DiscordMessageBuilder.buildChatMessage( source, message )
				);
			} else {
				sendServerMessage(
					DiscordMessageBuilder.buildWebhookChatMessage( message.getString() ),
					DiscordMessageBuilder.buildChatMessage( source, message )
				);
			}
		}
	}
	
	public static void sendChatMessage( ServerPlayer player, String message ) {
		
		sendPlayerMessage(
			player,
			DiscordMessageBuilder.buildWebhookChatMessage( message ),
			DiscordMessageBuilder.buildChatMessage( player, message )
		);
	}
	
	public static void sendPlayerMessage( Player player, String message ) {
		
		sendPlayerMessage( player, message, message );
	}
	
	public static void sendPlayerMessage(
		Player player,
		String webhookMessage,
		String fallbackMessage ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				if( isInitialized() ) {
					if( webhook != null ) {
						String username = DiscordMessageBuilder.getEntityName( player );
						String avatarUrl = getPlayerAvatarUrl( player );
						DiscordMessageSender.sendWebhookMessage( webhook, username, avatarUrl, webhookMessage );
					} else {
						DiscordMessageSender.sendMessage( channel, fallbackMessage );
					}
				}
			}
		}
	}
	
	public static void sendServerMessage( String message ) {
		
		sendServerMessage( message, message );
	}
	
	public static void sendServerMessage( String webhookMessage, String fallbackMessage ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				if( isInitialized() ) {
					if( webhook != null ) {
						String username = getServerWebhookName();
						String avatarUrl = getServerAvatarUrl();
						DiscordMessageSender.sendWebhookMessage( webhook, username, avatarUrl, webhookMessage );
					} else {
						DiscordMessageSender.sendMessage( channel, fallbackMessage );
					}
				}
			}
		}
	}
	
	private static String getPlayerAvatarUrl( Player player ) {
		
		String profileName = player.getGameProfile().getName();
		if( profileName != null && !profileName.isBlank() ) {
			return "https://mc-heads.net/avatar/" + profileName + "/128.png";
		}
		return "https://mc-heads.net/avatar/" + player.getUUID() + "/128.png";
	}
	
	private static String getServerWebhookName() {
		
		String configuredName = ServerConfig.CHAT_CONFIG.getServerWebhookName();
		if( !configuredName.isBlank() ) {
			return configuredName;
		}
		return DiscordManager.getSelfUser().getEffectiveName();
	}
	
	private static String getServerAvatarUrl() {
		
		String configuredAvatarUrl = ServerConfig.CHAT_CONFIG.getServerWebhookAvatarUrl();
		if( !configuredAvatarUrl.isBlank() ) {
			return configuredAvatarUrl;
		}
		return DiscordManager.getSelfUser().getEffectiveAvatarUrl();
	}
	
	public static void sendMessage( String message ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				if( isInitialized() ) {
					DiscordMessageSender.sendMessage( channel, message );
				}
			}
		}
	}
	
	//package-private
	static void sendFeedbackMessage( String message ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				if( isInitialized() ) {
					for( String messagePart : DiscordMessageBuilder.buildFeedbackMessage( message ) ) {
						DiscordMessageSender.sendMessage( channel, messagePart );
					}
				}
			}
		}
	}
}
