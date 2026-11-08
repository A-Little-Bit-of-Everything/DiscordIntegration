package de.geheimagentnr1.discordintegration.handlers;

import de.geheimagentnr1.discordintegration.config.MessageConfig;
import de.geheimagentnr1.discordintegration.config.ServerConfig;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordManager;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordMessageBuilder;
import de.geheimagentnr1.discordintegration.elements.discord.chat.ChatManager;
import de.geheimagentnr1.discordintegration.elements.discord.management.ManagementManager;
import de.geheimagentnr1.discordintegration.api.events.ForgeEventHandlerInterface;
import de.geheimagentnr1.discordintegration.api.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.event.ServerChatEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.player.AdvancementEvent;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
import net.neoforged.neoforge.event.server.ServerStartedEvent;
import net.neoforged.neoforge.event.server.ServerStoppedEvent;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


@RequiredArgsConstructor
public class DiscordMessageHandler implements ForgeEventHandlerInterface {
	
	
	@NotNull
	private final ServerConfig serverConfig;
	
	@NotNull
	private final DiscordManager discordManager;
	
	@NotNull
	private final ChatManager chatManager;
	
	@NotNull
	private final ManagementManager managementManager;
	
	@NotNull
	private final DiscordMessageBuilder discordMessageBuilder;
	
	private void sendConfiguredPlayerEventMessage(
		@NotNull Player player,
		@NotNull MessageConfig messageConfig,
		@NotNull String message ) {
		
		if( messageConfig.usePlayerWebhook() ) {
			chatManager.sendPlayerMessage( player, message, message );
		} else {
			chatManager.sendServerWebhookMessage( message, message );
		}
	}
	
	@Override
	public void handleServerStartedEvent( @NotNull ServerStartedEvent event ) {
		
		// Initialize Discord connection when server starts (config values are now accessible)
		discordManager.init();
		if( serverConfig.getChatConfig().getChatMessagesConfig().getServerStarted().isEnabled() ) {
			chatManager.sendServerWebhookMessage(
				serverConfig.getChatConfig().getChatMessagesConfig().getServerStarted().getMessage()
			);
		}
		if( serverConfig.getManagementConfig().getManagementMessagesConfig().getServerStarted().isEnabled() ) {
			managementManager.sendMessage(
				serverConfig.getManagementConfig().getManagementMessagesConfig().getServerStarted().getMessage()
			);
		}
		discordManager.setServerStarted();
	}
	
	@Override
	public void handleServerStoppedEvent( @NotNull ServerStoppedEvent event ) {
		
		if( event.getServer().isRunning() ) {
			if( serverConfig.getChatConfig().getChatMessagesConfig().getServerCrashed().isEnabled() ) {
				chatManager.sendServerWebhookMessage(
					serverConfig.getChatConfig().getChatMessagesConfig().getServerCrashed().getMessage()
				);
			}
			if( serverConfig.getManagementConfig().getManagementMessagesConfig().getServerCrashed().isEnabled() ) {
				managementManager.sendMessage(
					serverConfig.getManagementConfig().getManagementMessagesConfig().getServerCrashed().getMessage()
				);
			}
		} else {
			if( serverConfig.getChatConfig().getChatMessagesConfig().getServerStopped().isEnabled() ) {
				chatManager.sendServerWebhookMessage(
					serverConfig.getChatConfig().getChatMessagesConfig().getServerStopped().getMessage()
				);
			}
			if( serverConfig.getManagementConfig().getManagementMessagesConfig().getServerStopped().isEnabled() ) {
				managementManager.sendMessage(
					serverConfig.getManagementConfig().getManagementMessagesConfig().getServerStopped().getMessage()
				);
			}
		}
		discordManager.stop();
	}
	
	@Override
	public void handlePlayerLoggedInEvent( @NotNull PlayerEvent.PlayerLoggedInEvent event ) {
		
		Player player = event.getEntity();
		MessageConfig playerJoinedConfig = serverConfig.getChatConfig().getChatMessagesConfig().getPlayerJoined();
		if( playerJoinedConfig.isEnabled() ) {
			String message = MessageUtil.replaceParameters(
				playerJoinedConfig.getMessage(),
				Map.of(
					"player", discordMessageBuilder.getEntityName( player )
				)
			);
			sendConfiguredPlayerEventMessage( player, playerJoinedConfig, message );
		}
		if( serverConfig.getManagementConfig().getManagementMessagesConfig().getPlayerJoined().isEnabled() ) {
			managementManager.sendMessage(
				MessageUtil.replaceParameters(
					serverConfig.getManagementConfig().getManagementMessagesConfig().getPlayerJoined().getMessage(),
					Map.of(
						"player", discordMessageBuilder.getEntityName( player )
					)
				)
			);
		}
		discordManager.updatePresence( ServerLifecycleHooks.getCurrentServer().getPlayerCount() );
	}
	
	@Override
	public void handlePlayerLoggedOutEvent( @NotNull PlayerEvent.PlayerLoggedOutEvent event ) {
		
		Player player = event.getEntity();
		MessageConfig playerLeftConfig = serverConfig.getChatConfig().getChatMessagesConfig().getPlayerLeft();
		if( playerLeftConfig.isEnabled() ) {
			String message = MessageUtil.replaceParameters(
				playerLeftConfig.getMessage(),
				Map.of(
					"player", discordMessageBuilder.getEntityName( player )
				)
			);
			sendConfiguredPlayerEventMessage( player, playerLeftConfig, message );
		}
		if( serverConfig.getManagementConfig().getManagementMessagesConfig().getPlayerLeft().isEnabled() ) {
			managementManager.sendMessage(
				MessageUtil.replaceParameters(
					serverConfig.getManagementConfig().getManagementMessagesConfig().getPlayerLeft().getMessage(),
					Map.of(
						"player", discordMessageBuilder.getEntityName( player )
					)
				)
			);
		}
		discordManager.updatePresence( ServerLifecycleHooks.getCurrentServer().getPlayerCount() - 1 );
	}
	
	@Override
	public void handleServerChatEvent( @NotNull ServerChatEvent event ) {
		
		if( !event.isCanceled() ) {
			chatManager.sendChatMessage( event.getPlayer(), event.getRawText() );
		}
	}
	
	@Override
	public void handleLivingDeathEvent( @NotNull LivingDeathEvent event ) {
		
		LivingEntity entity = event.getEntity();
		String name = discordMessageBuilder.getEntityName( entity );
		String default_message = discordMessageBuilder.buildDeathMessage( event, entity, name );
		
		if( entity instanceof Player player ) {
			MessageConfig playerDiedConfig = serverConfig.getChatConfig().getChatMessagesConfig().getPlayerDied();
			if( playerDiedConfig.isEnabled() ) {
				String message = MessageUtil.replaceParameters(
					playerDiedConfig.getMessage(),
					Map.of(
						"player", name,
						"default_message", default_message
					)
				);
				sendConfiguredPlayerEventMessage( player, playerDiedConfig, message );
			}
		} else {
			if( entity instanceof TamableAnimal && ( (TamableAnimal)entity ).isTame() ) {
				if( serverConfig.getChatConfig().getChatMessagesConfig().getTamedMobDied().isEnabled() ) {
					chatManager.sendServerWebhookMessage(
						MessageUtil.replaceParameters(
							serverConfig.getChatConfig().getChatMessagesConfig().getTamedMobDied().getMessage(),
							Map.of(
								"tamed_mob", name,
								"default_message", default_message
							)
						)
					);
				}
			}
		}
	}
	
	@Override
	public void handleAdvancementEarnEvent( @NotNull AdvancementEvent.AdvancementEarnEvent event ) {
		
		Player player = event.getEntity();
		MessageConfig advancementConfig = serverConfig.getChatConfig().getChatMessagesConfig().getPlayerGotAdvancement();
		event.getAdvancement().value().display().ifPresent( displayInfo -> {
			if( displayInfo.shouldAnnounceChat() && advancementConfig.isEnabled() ) {
				String message = MessageUtil.replaceParameters(
					advancementConfig.getMessage(),
					Map.of(
						"player", discordMessageBuilder.getEntityName( player ),
						"advancement_title", displayInfo.getTitle().getString(),
						"advancement_description", displayInfo.getDescription().getString(),
						"new_line", System.lineSeparator()
					)
				);
				sendConfiguredPlayerEventMessage( player, advancementConfig, message );
			}
		} );
	}
}
