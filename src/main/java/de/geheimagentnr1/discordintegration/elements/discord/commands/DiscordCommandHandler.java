package de.geheimagentnr1.discordintegration.elements.discord.commands;

import de.geheimagentnr1.discordintegration.DiscordIntegration;
import de.geheimagentnr1.discordintegration.config.command_config.CommandConfig;
import de.geheimagentnr1.discordintegration.elements.discord.AbstractDiscordIntegrationServiceProvider;
import de.geheimagentnr1.discordintegration.elements.discord.commands.models.DiscordCommandSource;
import de.geheimagentnr1.discordintegration.elements.discord.commands.models.DiscordCommandSourceStack;
import de.geheimagentnr1.minecraft_forge_api.util.MessageUtil;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
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
	private static final String SLASH_ARGUMENTS_OPTION = "arguments";
	
	@NotNull
	private final DiscordIntegration discordIntegration;
	
	public boolean isCommand( String message ) {
		
		return message.startsWith( serverConfig().getCommandSettingsConfig().getCommandPrefix() );
	}
	
	public void registerSlashCommands( @NotNull Guild guild ) {
		
		try {
			List<SlashCommandData> slashCommands = new ArrayList<>();
			Set<String> registeredNames = new HashSet<>();
			for( CommandConfig commandConfig : serverConfig().getCommandSettingsConfig().getCommands() ) {
				if( !commandConfig.isEnabled() ) {
					continue;
				}
				String name = commandConfig.getDiscordCommand().trim().toLowerCase( Locale.ROOT );
				if( name.isEmpty() || name.contains( " " ) || !registeredNames.add( name ) ) {
					continue;
				}
				String description = MessageUtil.replaceParameters(
					commandConfig.getDescription(),
					Map.of(
						"command", "",
						"command_description_separator", ""
					)
				).trim();
				if( description.isEmpty() ) {
					description = "Execute " + name + " command";
				} else if( description.length() > 100 ) {
					description = description.substring( 0, 100 );
				}
				SlashCommandData slashCommand = Commands.slash( name, description );
				if( commandConfig.useParameters() ) {
					slashCommand.addOption(
						OptionType.STRING,
						SLASH_ARGUMENTS_OPTION,
						"Command arguments",
						true
					);
				}
				slashCommands.add( slashCommand );
			}
			guild.updateCommands().addCommands( slashCommands ).queue(
				unused -> log.info( "Registered {} Discord slash commands", slashCommands.size() ),
				throwable -> log.warn( "Could not register Discord slash commands", throwable )
			);
		} catch( Exception exception ) {
			log.warn( "Failed to build Discord slash commands", exception );
		}
	}
	
	public void handleSlashCommandInteraction( @NotNull SlashCommandInteractionEvent event ) {
		
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		Member member = event.getMember();
		if( server == null || member == null ) {
			return;
		}
		long channelId = event.getChannel().getIdLong();
		boolean isChatChannel = serverConfig().getChatConfig().isEnabled() &&
			serverConfig().getChatConfig().getChannelId() == channelId;
		boolean isManagementChannel = serverConfig().getManagementConfig().isEnabled() &&
			serverConfig().getManagementConfig().getChannelId() == channelId;
		if( !isChatChannel && !isManagementChannel ) {
			event.reply( "Commands can only be used in the configured chat or management channel." )
				.setEphemeral( true )
				.queue();
			return;
		}
		
		String args = "";
		OptionMapping argsOption = event.getOption( SLASH_ARGUMENTS_OPTION );
		if( argsOption != null ) {
			args = " " + argsOption.getAsString().trim();
		}
		String fullCommand = serverConfig().getCommandSettingsConfig().getCommandPrefix() +
			event.getName() + args;
		
		event.deferReply().queue(
			interactionHook -> server.execute( () -> handleCommand(
				member,
				fullCommand,
				server,
				feedback -> {
					for( String part : discordMessageBuilder().buildFeedbackMessage( feedback ) ) {
						interactionHook.sendMessage( part ).queue();
					}
				}
			) )
		);
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
			return command.startsWith( discordCommand + " " );
		} else {
			return command.equals( discordCommand );
		}
	}
	
	private String buildDiscordCommand( CommandConfig commandConfig ) {
		
		return serverConfig().getCommandSettingsConfig().getCommandPrefix() + commandConfig.getDiscordCommand();
	}
	
	private String buildMinecraftCommand(
		CommandConfig commandConfig,
		String discordCommand,
		String command ) {
		
		if( commandConfig.useParameters() ) {
			return "/" + commandConfig.getMinecraftCommand() + command.replaceFirst( discordCommand, "" );
		}
		return "/" + commandConfig.getMinecraftCommand();
	}
}
