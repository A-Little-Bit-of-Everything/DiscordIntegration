package de.geheimagentnr1.discordintegration.elements.discord.commands;

import de.geheimagentnr1.discordintegration.DiscordIntegration;
import de.geheimagentnr1.discordintegration.config.command_config.CommandConfig;
import de.geheimagentnr1.discordintegration.elements.discord.AbstractDiscordIntegrationServiceProvider;
import de.geheimagentnr1.discordintegration.elements.discord.commands.models.DiscordCommandSource;
import de.geheimagentnr1.discordintegration.elements.discord.commands.models.DiscordCommandSourceStack;
import de.geheimagentnr1.discordintegration.api.util.MessageUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.InteractionContextType;
import net.dv8tion.jda.api.interactions.IntegrationType;
import net.dv8tion.jda.api.interactions.InteractionHook;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.neoforged.neoforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


@Log4j2
@Getter( AccessLevel.PROTECTED )
@RequiredArgsConstructor
public class DiscordCommandHandler extends AbstractDiscordIntegrationServiceProvider {
	
	
	@NotNull
	private static final String SLASH_ARGS_OPTION_NAME = "arguments";
	
	@NotNull
	private final DiscordIntegration discordIntegration;
	
	@NotNull
	public ListenerAdapter createSlashCommandListener() {
		
		return new ListenerAdapter() {
			
			@Override
			public void onSlashCommandInteraction( @NotNull SlashCommandInteractionEvent event ) {
				
				handleSlashCommandInteraction( event );
			}
		};
	}
	
	public void registerSlashCommands( @NotNull Guild guild ) {
		
		List<SlashCommandData> slashCommands = new ArrayList<>();
		Set<String> registeredNames = new HashSet<>();
		for( CommandConfig commandConfig : serverConfig().getCommandSettingsConfig().getCommands() ) {
			if( commandConfig.isEnabled() ) {
				String rawName = commandConfig.getDiscordCommand().trim().toLowerCase( Locale.ROOT );
				if( rawName.matches( "^[-_\\p{L}\\p{N}]{1,32}$" ) && registeredNames.add( rawName ) ) {
					String description = buildSlashCommandDescription( commandConfig );
					SlashCommandData slashCommand = Commands.slash( rawName, description )
						.setContexts( InteractionContextType.GUILD, InteractionContextType.BOT_DM )
						.setIntegrationTypes( IntegrationType.GUILD_INSTALL );
					if( commandConfig.useParameters() ) {
						slashCommand.addOption(
							OptionType.STRING,
							SLASH_ARGS_OPTION_NAME,
							"Arguments for the command",
							true
						);
					}
					slashCommands.add( slashCommand );
				} else {
					log.warn(
						"Skipping Discord slash command registration for '{}': invalid or duplicate command name",
						commandConfig.getDiscordCommand()
					);
				}
			}
		}
		guild.updateCommands().queue(
			null,
			throwable -> log.warn( "Could not clear guild commands for guild {}", guild.getIdLong(), throwable )
		);
		guild.getJDA().updateCommands()
			.addCommands( slashCommands )
			.queue(
				null,
				throwable -> log.error(
					"Could not register Discord global slash commands",
					throwable
				)
			);
	}
	
	@NotNull
	private String buildSlashCommandDescription( @NotNull CommandConfig commandConfig ) {
		
		String rawDescription = commandConfig.getDescription();
		String separatorToken = "%command_description_separator%";
		int separatorIndex = rawDescription.indexOf( separatorToken );
		String description;
		if( separatorIndex >= 0 ) {
			description = rawDescription.substring( separatorIndex + separatorToken.length() ).trim();
		} else {
			description = MessageUtil.replaceParameters(
				rawDescription,
				Map.of(
					"command", "/" + commandConfig.getDiscordCommand(),
					"command_description_separator", " - "
				)
			).trim();
		}
		if( description.isEmpty() ) {
			description = "Executes the " + commandConfig.getDiscordCommand() + " command.";
		}
		if( description.length() > 100 ) {
			description = description.substring( 0, 97 ) + "...";
		}
		return description;
	}
	
	private void handleSlashCommandInteraction( @NotNull SlashCommandInteractionEvent event ) {
		
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if( server == null || !serverConfig().isLoaded() ) {
			return;
		}
		net.dv8tion.jda.api.JDA jda = discordManager().getJda();
		if( jda == null ) {
			return;
		}
		Guild configuredGuild = jda.getGuildById( serverConfig().getBotConfig().getGuildId() );
		if( configuredGuild == null ) {
			return;
		}
		
		if( event.isFromGuild() ) {
			if( event.getGuild() == null ||
				event.getGuild().getIdLong() != configuredGuild.getIdLong() ||
				event.getMember() == null ) {
				return;
			}
			event.deferReply().queue(
				interactionHook -> executeSlashCommand( event, event.getMember(), server, interactionHook ),
				throwable -> log.error( "Failed to defer reply for slash command /{}", event.getName(), throwable )
			);
		} else {
			event.deferReply().queue(
				interactionHook -> configuredGuild.retrieveMember( event.getUser() ).queue(
					member -> executeSlashCommand( event, member, server, interactionHook ),
					throwable -> interactionHook.sendMessage( "You must be a member of the linked Discord server to use commands." ).queue()
				),
				throwable -> log.error( "Failed to defer reply for slash command /{} in DM", event.getName(), throwable )
			);
		}
	}
	
	private void executeSlashCommand(
		@NotNull SlashCommandInteractionEvent event,
		@NotNull Member member,
		@NotNull MinecraftServer server,
		@NotNull InteractionHook interactionHook ) {
		
		OptionMapping argsOption = event.getOption( SLASH_ARGS_OPTION_NAME );
		String fullCommand = serverConfig().getCommandSettingsConfig().getCommandPrefix() +
			event.getName() +
			( argsOption != null ? " " + argsOption.getAsString() : "" );
		
		handleCommand(
			member,
			fullCommand,
			server,
			feedbackMessage -> {
				List<String> parts = discordMessageBuilder().buildFeedbackMessage( feedbackMessage );
				if( parts.isEmpty() ) {
					interactionHook.sendMessage( "Command executed." ).queue();
				} else {
					for( String messagePart : parts ) {
						interactionHook.sendMessage( messagePart ).queue();
					}
				}
			}
		);
	}
	
	public boolean isCommand( String message ) {
		
		return message.startsWith( serverConfig().getCommandSettingsConfig().getCommandPrefix() );
	}
	
	public void handleCommand(
		Member member,
		String command,
		MinecraftServer server,
		Consumer<String> feedbackSender ) {
		
		if( !executeCommand( member, command, server, feedbackSender ) ) {
			feedbackSender.accept( MessageUtil.replaceParameters(
				serverConfig().getCommandSettingsConfig().getCommandMessagesConfig().getUnknownCommandErrorMessage(),
				Map.of(
					"username", discordManager().getMemberAsTag( member ),
					"nickname", member.getEffectiveName(),
					"new_line", System.lineSeparator()
				)
			) );
		}
	}
	
	private boolean executeCommand(
		Member member,
		String command,
		MinecraftServer server,
		Consumer<String> feedbackSender ) {
		
		boolean hasManagementRole = managementManager().hasManagementRole( member );
		DiscordCommandSource discordCommandSource = new DiscordCommandSource( feedbackSender );
		CommandSourceStack source = new DiscordCommandSourceStack(
			discordIntegration,
			discordCommandSource,
			hasManagementRole
				? serverConfig().getCommandSettingsConfig().getCommandManagementUserPermissionLevel()
				: serverConfig().getCommandSettingsConfig().getCommandNormalUserPermissionLevel(),
			server,
			member
		);
		
		for( CommandConfig commandConfig : serverConfig().getCommandSettingsConfig().getCommands() ) {
			if( commandConfig.isEnabled() ) {
				
				String discordCommand = buildDiscordCommand( commandConfig );
				
				if( doesCommandMatch( commandConfig, command, discordCommand ) ) {
					if( !commandConfig.isManagementCommand() || hasManagementRole ) {
						server.getCommands().performPrefixedCommand(
							source,
							buildMinecraftCommand( commandConfig, discordCommand, command )
						);
						discordCommandSource.sendMessage();
					} else {
						feedbackSender.accept( MessageUtil.replaceParameters(
							serverConfig().getCommandSettingsConfig()
								.getCommandMessagesConfig()
								.getInvalidPermissionsErrorMessage(),
							Map.of(
								"username", discordManager().getMemberAsTag( member ),
								"nickname", member.getEffectiveName(),
								"new_line", System.lineSeparator()
							)
						) );
					}
					return true;
				}
			}
		}
		return false;
	}
	
	private boolean doesCommandMatch(
		CommandConfig commandConfig,
		String command,
		String discordCommand ) {
		
		if( commandConfig.useParameters() ) {
			return command.regionMatches( true, 0, discordCommand + " ", 0, discordCommand.length() + 1 );
		} else {
			return command.equalsIgnoreCase( discordCommand );
		}
	}
	
	private String buildDiscordCommand( CommandConfig commandConfig ) {
		
		return serverConfig().getCommandSettingsConfig().getCommandPrefix() + commandConfig.getDiscordCommand();
	}
	
	private String buildMinecraftCommand(
		CommandConfig commandConfig,
		String discordCommand,
		String command ) {
		
		if( commandConfig.useParameters() && command.length() > discordCommand.length() ) {
			return "/" + commandConfig.getMinecraftCommand() + command.substring( discordCommand.length() );
		}
		return "/" + commandConfig.getMinecraftCommand();
	}
}
