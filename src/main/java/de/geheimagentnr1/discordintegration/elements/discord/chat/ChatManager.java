package de.geheimagentnr1.discordintegration.elements.discord.chat;

import de.geheimagentnr1.discordintegration.DiscordIntegration;
import de.geheimagentnr1.discordintegration.elements.discord.AbstractDiscordIntegrationServiceProvider;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordManager;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


@SuppressWarnings( { "SynchronizeOnThis", "NestedSynchronizedStatement" } )
@Log4j2
@RequiredArgsConstructor
public class ChatManager extends AbstractDiscordIntegrationServiceProvider {
	
	
	@NotNull
	private static final String WEBHOOK_NAME = "MinecraftChatBridge";
	
	@Getter( AccessLevel.PROTECTED )
	@NotNull
	private final DiscordIntegration discordIntegration;
	
	@Nullable
	private TextChannel channel;
	
	@Nullable
	private Webhook webhook;
	
	public void init() {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				stop();
				if( shouldInitialize() ) {
					long channelId = serverConfig().getChatConfig().getChannelId();
					JDA jda = discordManager().getJda();
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
	
	private void initWebhook() {
		
		if( channel == null ) {
			return;
		}
		try {
			List<Webhook> webhooks = channel.retrieveWebhooks().complete();
			long selfUserId = discordManager().getSelfUser().getIdLong();
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
	
	public void stop() {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				channel = null;
				webhook = null;
			}
		}
	}
	
	private boolean shouldInitialize() {
		
		return discordManager().isInitialized() && serverConfig().getChatConfig().isEnabled();
	}
	
	private boolean isInitialized() {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				return shouldInitialize() && channel != null;
			}
		}
	}
	
	//package-private
	boolean isCorrectChannel( long channelId ) {
		
		return isInitialized() && serverConfig().getChatConfig().getChannelId() == channelId;
	}
	
	//package-private
	boolean isOwnWebhook( long webhookOrAuthorId ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				return webhook != null && webhook.getIdLong() == webhookOrAuthorId;
			}
		}
	}
	
	public void sendEmoteChatMessage( CommandSourceStack source, Component action ) {
		
		if( source.getEntity() instanceof Player player ) {
			sendPlayerMessage(
				player,
				discordMessageBuilder().buildWebhookMeChatMessage( action.getString() ),
				discordMessageBuilder().buildMeChatMessage( source, action.getString() )
			);
		} else {
			sendServerMessage(
				discordMessageBuilder().buildWebhookMeChatMessage( action.getString() ),
				discordMessageBuilder().buildMeChatMessage( source, action.getString() )
			);
		}
	}
	
	public void sendChatMessage( CommandSourceStack source, Component message ) {
		
		if( !serverConfig().getChatConfig().suppressServerMessages() ||
			!"Server".equals( source.getTextName() ) ||
			source.getEntity() != null ) {
			if( source.getEntity() instanceof Player player ) {
				sendPlayerMessage(
					player,
					discordMessageBuilder().buildWebhookChatMessage( message.getString() ),
					discordMessageBuilder().buildChatMessage( source, message )
				);
			} else {
				sendServerMessage(
					discordMessageBuilder().buildWebhookChatMessage( message.getString() ),
					discordMessageBuilder().buildChatMessage( source, message )
				);
			}
		}
	}
	
	public void sendChatMessage( ServerPlayer player, String message ) {
		
		sendPlayerMessage(
			player,
			discordMessageBuilder().buildWebhookChatMessage( message ),
			discordMessageBuilder().buildChatMessage( player, message )
		);
	}
	
	public void sendPlayerMessage( @NotNull Player player, @NotNull String message ) {
		
		sendPlayerMessage( player, message, message );
	}
	
	public void sendPlayerMessage(
		@NotNull Player player,
		@NotNull String webhookMessage,
		@NotNull String fallbackMessage ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				if( isInitialized() ) {
					if( webhook != null ) {
						String username = discordMessageBuilder().getEntityName( player );
						String avatarUrl = getPlayerAvatarUrl( player );
						discordMessageSender().sendWebhookMessage( webhook, username, avatarUrl, webhookMessage );
					} else {
						discordMessageSender().sendMessage( channel, fallbackMessage );
					}
				}
			}
		}
	}
	
	public void sendServerMessage( @NotNull String message ) {
		
		sendServerMessage( message, message );
	}
	
	public void sendServerMessage( @NotNull String webhookMessage, @NotNull String fallbackMessage ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				if( isInitialized() ) {
					if( webhook != null ) {
						String username = getServerWebhookName();
						String avatarUrl = getServerAvatarUrl();
						discordMessageSender().sendWebhookMessage( webhook, username, avatarUrl, webhookMessage );
					} else {
						discordMessageSender().sendMessage( channel, fallbackMessage );
					}
				}
			}
		}
	}
	
	@NotNull
	private String getPlayerAvatarUrl( @NotNull Player player ) {
		
		String profileName = player.getGameProfile().getName();
		if( profileName != null && !profileName.isBlank() ) {
			return "https://mc-heads.net/avatar/" + profileName + "/128.png";
		}
		return "https://mc-heads.net/avatar/" + player.getUUID() + "/128.png";
	}
	
	@NotNull
	private String getServerWebhookName() {
		
		String configuredName = serverConfig().getChatConfig().getServerWebhookName();
		if( !configuredName.isBlank() ) {
			return configuredName;
		}
		return discordManager().getSelfUser().getEffectiveName();
	}
	
	@NotNull
	private String getServerAvatarUrl() {
		
		String configuredAvatarUrl = serverConfig().getChatConfig().getServerWebhookAvatarUrl();
		if( !configuredAvatarUrl.isBlank() ) {
			return configuredAvatarUrl;
		}
		return discordManager().getSelfUser().getEffectiveAvatarUrl();
	}
	
	public void sendMessage( String message ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				if( isInitialized() ) {
					discordMessageSender().sendMessage( channel, message );
				}
			}
		}
	}
	
	//package-private
	void sendFeedbackMessage( String message ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				if( isInitialized() ) {
					for( String messagePart : discordMessageBuilder().buildFeedbackMessage( message ) ) {
						discordMessageSender().sendMessage( channel, messagePart );
					}
				}
			}
		}
	}
}
