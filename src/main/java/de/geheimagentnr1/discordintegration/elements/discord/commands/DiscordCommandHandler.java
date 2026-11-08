package de.geheimagentnr1.discordintegration.elements.discord.commands;

import com.electronwill.nightconfig.core.AbstractCommentedConfig;
import de.geheimagentnr1.discordintegration.config.ServerConfig;
import de.geheimagentnr1.discordintegration.config.command_config.CommandConfig;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordManager;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordMessageBuilder;
import de.geheimagentnr1.discordintegration.elements.discord.commands.models.DiscordCommandSource;
import de.geheimagentnr1.discordintegration.elements.discord.commands.models.DiscordCommandSourceStack;
import de.geheimagentnr1.discordintegration.elements.discord.management.ManagementManager;
import de.geheimagentnr1.discordintegration.util.MessageUtil;
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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;


@Log4j2
public class DiscordCommandHandler {
	
	
	private static final String SLASH_ARGUMENTS_OPTION = "arguments";
	
	public static boolean isCommand( String message ) {
		
		return message.startsWith( ServerConfig.COMMAND_SETTINGS_CONFIG.getCommandPrefix() );
	}
	
	public static void registerSlashCommands( Guild guild ) {
		
		try {
			List<SlashCommandData> slashCommands = new ArrayList<>();
			Set<String> registeredNames = new HashSet<>();
			for( AbstractCommentedConfig commandConfig : ServerConfig.COMMAND_SETTINGS_CONFIG.getCommands() ) {
				if( !CommandConfig.isEnabled( commandConfig ) ) {
					continue;
				}
				String name = CommandConfig.getDiscordCommand( commandConfig ).trim().toLowerCase( Locale.ROOT );
				if( name.isEmpty() || name.contains( " " ) || !registeredNames.add( name ) ) {
					continue;
				}
				String description = MessageUtil.replaceParameters(
					CommandConfig.getDescription( commandConfig ),
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
				if( CommandConfig.useParameters( commandConfig ) ) {
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
	
	public static void handleSlashCommandInteraction( SlashCommandInteractionEvent event ) {
		
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		Member member = event.getMember();
		if( server == null || member == null ) {
			return;
		}
		long channelId = event.getChannel().getIdLong();
		boolean isChatChannel = ServerConfig.CHAT_CONFIG.isEnabled() &&
			ServerConfig.CHAT_CONFIG.getChannelId() == channelId;
		boolean isManagementChannel = ServerConfig.MANAGEMENT_CONFIG.isEnabled() &&
			ServerConfig.MANAGEMENT_CONFIG.getChannelId() == channelId;
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
		String fullCommand = ServerConfig.COMMAND_SETTINGS_CONFIG.getCommandPrefix() +
			event.getName() + args;
		
		event.deferReply().queue(
			interactionHook -> server.execute( () -> handleCommand(
				member,
				fullCommand,
				server,
				feedback -> {
					for( String part : DiscordMessageBuilder.buildFeedbackMessage( feedback ) ) {
						interactionHook.sendMessage( part ).queue();
					}
				}
			) )
		);
	}
	
	public static void handleCommand(
		Member member,
		String command,
		MinecraftServer server,
		Consumer<String> feedbackSender ) {
		
		if( !executeCommand( member, command, server, feedbackSender ) ) {
			feedbackSender.accept( MessageUtil.replaceParameters(
				ServerConfig.COMMAND_SETTINGS_CONFIG.getCommandMessagesConfig()
					.getUnknownCommandErrorMessage(),
				Map.of(
					"username", DiscordManager.getMemberAsTag( member ),
					"nickname", member.getEffectiveName(),
					"new_line", System.lineSeparator()
				)
			) );
		}
	}
	
	private static boolean executeCommand(
		Member member,
		String command,
		MinecraftServer server,
		Consumer<String> feedbackSender ) {
		
		boolean hasManagementRole = ManagementManager.hasManagementRole( member );
		DiscordCommandSource discordCommandSource = new DiscordCommandSource( feedbackSender );
		CommandSourceStack source = new DiscordCommandSourceStack(
			discordCommandSource,
			hasManagementRole
				? ServerConfig.COMMAND_SETTINGS_CONFIG.getCommandManagementUserPermissionLevel()
				: ServerConfig.COMMAND_SETTINGS_CONFIG.getCommandNormalUserPermissionLevel(),
			server,
			member
		);
		
		for( AbstractCommentedConfig commandConfig : ServerConfig.COMMAND_SETTINGS_CONFIG.getCommands() ) {
			if( CommandConfig.isEnabled( commandConfig ) ) {
				
				String discordCommand = buildDiscordCommand( commandConfig );
				
				if( doesCommandMatch( commandConfig, command, discordCommand ) ) {
					if( !CommandConfig.isManagementCommand( commandConfig ) || hasManagementRole ) {
						server.getCommands().performCommand(
							source,
							buildMinecraftCommand( commandConfig, discordCommand, command )
						);
						discordCommandSource.sendMessage();
					} else {
						feedbackSender.accept( MessageUtil.replaceParameters(
							ServerConfig.COMMAND_SETTINGS_CONFIG.getCommandMessagesConfig()
								.getInvalidPermissionsErrorMessage(),
							Map.of(
								"username", DiscordManager.getMemberAsTag( member ),
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
	
	private static boolean doesCommandMatch(
		AbstractCommentedConfig commandConfig,
		String command,
		String discordCommand ) {
		
		if( CommandConfig.useParameters( commandConfig ) ) {
			return command.startsWith( discordCommand + " " );
		} else {
			return command.equals( discordCommand );
		}
	}
	
	private static String buildDiscordCommand( AbstractCommentedConfig commandConfig ) {
		
		return ServerConfig.COMMAND_SETTINGS_CONFIG.getCommandPrefix() +
			CommandConfig.getDiscordCommand( commandConfig );
	}
	
	private static String buildMinecraftCommand(
		AbstractCommentedConfig commandConfig,
		String discordCommand,
		String command ) {
		
		if( CommandConfig.useParameters( commandConfig ) ) {
			return "/" + CommandConfig.getMinecraftCommand( commandConfig ) + command.replaceFirst(
				discordCommand,
				""
			);
		}
		return "/" + CommandConfig.getMinecraftCommand( commandConfig );
	}
}
