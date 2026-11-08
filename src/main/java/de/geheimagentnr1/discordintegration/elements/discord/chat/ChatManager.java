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
						initWebhook( jda );
					}
				}
			}
		}
	}
	
	private void initWebhook( @NotNull JDA jda ) {
		
		if( channel == null ) {
			return;
		}
		try {
			long selfUserId = jda.getSelfUser().getIdLong();
			List<Webhook> webhooks = channel.retrieveWebhooks().complete();
			webhook = webhooks.stream()
				.filter( existing -> existing.getToken() != null &&
					( ( existing.getOwnerAsUser() != null && existing.getOwnerAsUser().getIdLong() == selfUserId ) ||
						WEBHOOK_NAME.equals( existing.getName() ) ) )
				.findFirst()
				.orElseGet( () -> channel.createWebhook( WEBHOOK_NAME ).complete() );
		} catch( Exception exception ) {
			webhook = null;
			log.warn(
				"Could not initialize webhook for Discord Chat Text Channel {} (missing MANAGE_WEBHOOKS permission?), falling back to bot messages",
				channel.getIdLong(),
				exception
			);
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
	boolean isOwnWebhook( long authorId ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				return webhook != null && webhook.getIdLong() == authorId;
			}
		}
	}
	
	public void sendEmoteChatMessage( CommandSourceStack source, Component action ) {
		
		String fallbackMessage = discordMessageBuilder().buildMeChatMessage( source, action.getString() );
		if( source.getEntity() instanceof Player player ) {
			sendPlayerMessage(
				player,
				discordMessageBuilder().buildWebhookMeChatMessage( action.getString() ),
				fallbackMessage
			);
		} else {
			sendServerWebhookMessage(
				discordMessageBuilder().buildWebhookMeChatMessage( action.getString() ),
				fallbackMessage
			);
		}
	}
	
	public void sendChatMessage( CommandSourceStack source, Component message ) {
		
		if( !serverConfig().getChatConfig().suppressServerMessages() ||
			!"Server".equals( source.getTextName() ) ||
			source.getEntity() != null ) {
			String fallbackMessage = discordMessageBuilder().buildChatMessage( source, message );
			if( source.getEntity() instanceof Player player ) {
				sendPlayerMessage( player, message.getString(), fallbackMessage );
			} else {
				sendServerWebhookMessage( message.getString(), fallbackMessage );
			}
		}
	}
	
	public void sendChatMessage( ServerPlayer player, String message ) {
		
		sendPlayerMessage( player, message, discordMessageBuilder().buildChatMessage( player, message ) );
	}
	
	public void sendPlayerMessage(
		@NotNull Player player,
		@NotNull String webhookMessage,
		@NotNull String fallbackMessage ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				if( isInitialized() ) {
					if( webhook != null ) {
						discordMessageSender().sendWebhookMessage(
							webhook,
							channel,
							discordMessageBuilder().getEntityName( player ),
							discordMessageBuilder().getPlayerAvatarUrl( player ),
							webhookMessage,
							fallbackMessage
						);
					} else {
						discordMessageSender().sendMessage( channel, fallbackMessage );
					}
				}
			}
		}
	}
	
	public void sendServerWebhookMessage( @NotNull String message ) {
		
		sendServerWebhookMessage( message, message );
	}
	
	public void sendServerWebhookMessage( @NotNull String webhookMessage, @NotNull String fallbackMessage ) {
		
		synchronized( DiscordManager.class ) {
			synchronized( ChatManager.class ) {
				if( isInitialized() ) {
					if( webhook != null ) {
						String configuredAvatar = serverConfig().getChatConfig().getServerWebhookAvatarUrl().trim();
						String avatarUrl = !configuredAvatar.isEmpty()
							? configuredAvatar
							: discordManager().getSelfUser().getEffectiveAvatarUrl();
						discordMessageSender().sendWebhookMessage(
							webhook,
							channel,
							serverConfig().getChatConfig().getServerWebhookName(),
							avatarUrl,
							webhookMessage,
							fallbackMessage
						);
					} else {
						discordMessageSender().sendMessage( channel, fallbackMessage );
					}
				}
			}
		}
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
