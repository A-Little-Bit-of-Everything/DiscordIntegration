package de.geheimagentnr1.discordintegration.elements.discord;

import com.electronwill.nightconfig.core.AbstractCommentedConfig;
import de.geheimagentnr1.discordintegration.config.CommandConfig;
import de.geheimagentnr1.discordintegration.config.ServerConfig;
import de.geheimagentnr1.discordintegration.net.DiscordNet;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Consumer;


public class DiscordCommandHandler {
	
	
	private static final Logger LOGGER = LogManager.getLogger( DiscordCommandHandler.class );
	
	private static final String MOD_NAME = ModLoadingContext.get().getActiveContainer().getModInfo().getDisplayName();
	
	private static final String SLASH_ARGUMENTS_OPTION = "arguments";
	
	public static void registerSlashCommands( Guild guild ) {
		
		if( guild == null ) {
			return;
		}
		try {
			List<SlashCommandData> slashCommands = new ArrayList<>();
			Set<String> registeredNames = new HashSet<>();
			for( AbstractCommentedConfig commandConfig : ServerConfig.getCommands() ) {
				if( !CommandConfig.getEnabled( commandConfig ) ) {
					continue;
				}
				String rawName = CommandConfig.getDiscordCommand( commandConfig )
					.trim()
					.toLowerCase( Locale.ROOT );
				if( rawName.isEmpty() || !rawName.matches( "^[\\w-]{1,32}$" ) || !registeredNames.add( rawName ) ) {
					continue;
				}
				String description = CommandConfig.getDescription( commandConfig ).trim();
				if( description.isEmpty() ) {
					description = "Executes the " + rawName + " command.";
				}
				if( description.length() > 100 ) {
					description = description.substring( 0, 97 ) + "...";
				}
				SlashCommandData data = Commands.slash( rawName, description );
				if( CommandConfig.getUseParameter( commandConfig ) ) {
					data.addOption(
						OptionType.STRING,
						SLASH_ARGUMENTS_OPTION,
						"Command arguments",
						true
					);
				}
				slashCommands.add( data );
			}
			guild.updateCommands().addCommands( slashCommands ).queue(
				unused -> LOGGER.info( "Registered {} Discord slash commands", slashCommands.size() ),
				throwable -> LOGGER.warn( "Could not register Discord slash commands", throwable )
			);
		} catch( Exception exception ) {
			LOGGER.warn( "Could not register Discord slash commands", exception );
		}
	}
	
	public static void handleSlashCommandInteraction( SlashCommandInteractionEvent event, MinecraftServer server ) {
		
		OptionMapping argsOption = event.getOption( SLASH_ARGUMENTS_OPTION );
		String fullCommand = ServerConfig.getCommandPrefix() + event.getName() +
			( argsOption != null ? " " + argsOption.getAsString() : "" );
		event.deferReply().queue(
			interactionHook -> {
				Consumer<String> feedbackSender = feedbackMessage -> {
					for( int start = 0; start <= feedbackMessage.length(); start += 1990 ) {
						String part = DiscordNet.FEEDBACK_START +
							feedbackMessage.substring( start, Math.min( feedbackMessage.length(), start + 1990 ) ) +
							DiscordNet.FEEDBACK_END;
						interactionHook.sendMessage( part ).queue();
					}
				};
				if( !handleCommand( fullCommand, server, feedbackSender ) ) {
					feedbackSender.accept( String.format( "%n%s%nError: Unknown Command", event.getUser().getName() ) );
				}
			}
		);
	}
	
	//package-private
	static boolean handleCommand( String command, MinecraftServer server ) {
		
		return handleCommand( command, server, DiscordNet::sendFeedbackMessage );
	}
	
	//package-private
	static boolean handleCommand( String command, MinecraftServer server, Consumer<String> feedbackSender ) {
		
		DiscordCommandSource discordCommandSource = new DiscordCommandSource( feedbackSender );
		CommandSourceStack source = buildSource( server, discordCommandSource );
		for( AbstractCommentedConfig abstractCommentedConfig : ServerConfig.getCommands() ) {
			String discordCommand = buildDiscordCommand( abstractCommentedConfig );
			if( CommandConfig.getEnabled( abstractCommentedConfig ) && (
				discordCommand.equals( command ) ||
					CommandConfig.getUseParameter( abstractCommentedConfig ) &&
						command.startsWith( discordCommand + " " ) ) ) {
				server.getCommands().performCommand(
					source,
					buildMinecraftCommand( abstractCommentedConfig, discordCommand, command )
				);
				discordCommandSource.sendMessage();
				return true;
			}
		}
		return false;
	}
	
	private static CommandSourceStack buildSource(
		MinecraftServer server,
		DiscordCommandSource discordCommandSource ) {
		
		return new CommandSourceStack(
			discordCommandSource,
			Vec3.ZERO,
			Vec2.ZERO,
			Objects.requireNonNull( server.overworld() ),
			4,
			MOD_NAME,
			new TextComponent( MOD_NAME ),
			server,
			null
		);
	}
	
	private static String buildDiscordCommand( AbstractCommentedConfig abstractCommentedConfig ) {
		
		return ServerConfig.getCommandPrefix() + CommandConfig.getDiscordCommand( abstractCommentedConfig );
	}
	
	private static String buildMinecraftCommand(
		AbstractCommentedConfig abstractCommentedConfig,
		String discordCommand,
		String command ) {
		
		if( CommandConfig.getUseParameter( abstractCommentedConfig ) ) {
			return "/" + CommandConfig.getMinecraftCommand( abstractCommentedConfig ) + command.replaceFirst(
				discordCommand,
				""
			);
		}
		return "/" + CommandConfig.getMinecraftCommand( abstractCommentedConfig );
	}
}
