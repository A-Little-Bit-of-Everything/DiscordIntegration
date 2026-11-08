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
					SlashCommandData slashCommand = Commands.slash( rawName, description );
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
		guild.updateCommands()
			.addCommands( slashCommands )
			.queue(
				null,
				throwable -> log.error(
					"Could not register Discord slash commands for guild {}",
					guild.getIdLong(),
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
		
		if( !event.isFromGuild() || event.getMember() == null || event.getGuild() == null ) {
			return;
		}
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if( server == null ||
			!serverConfig().isLoaded() ||
			event.getGuild().getIdLong() != serverConfig().getBotConfig().getGuildId() ) {
			return;
		}
		
		Member member = event.getMember();
		OptionMapping argsOption = event.getOption( SLASH_ARGS_OPTION_NAME );
		String fullCommand = serverConfig().getCommandSettingsConfig().getCommandPrefix() +
			event.getName() +
			( argsOption != null ? " " + argsOption.getAsString() : "" );
		
		event.deferReply().queue(
			interactionHook -> handleCommand(
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
			),
			throwable -> log.error( "Failed to defer reply for slash command /{}", event.getName(), throwable )
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
