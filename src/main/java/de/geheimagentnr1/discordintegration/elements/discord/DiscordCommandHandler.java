package de.geheimagentnr1.discordintegration.elements.discord;

import com.electronwill.nightconfig.core.AbstractCommentedConfig;
import de.geheimagentnr1.discordintegration.config.CommandConfig;
import de.geheimagentnr1.discordintegration.config.ServerConfig;
import de.geheimagentnr1.discordintegration.net.DiscordNet;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.interactions.commands.build.SlashCommandData;
import net.minecraft.command.CommandSource;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.vector.Vector2f;
import net.minecraft.util.math.vector.Vector3d;
import net.minecraft.util.text.StringTextComponent;
import net.minecraft.world.World;
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
				slashCommands.add( Commands.slash( rawName, description ) );
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
		
		String fullCommand = ServerConfig.getCommandPrefix() + event.getName();
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
		CommandSource source = buildSource( server, discordCommandSource );
		for( AbstractCommentedConfig abstractCommentedConfig : ServerConfig.getCommands() ) {
			if( CommandConfig.getEnabled( abstractCommentedConfig ) &&
				buildDiscordCommand( abstractCommentedConfig ).equals( command ) ) {
				server.getCommandManager().handleCommand( source, buildMinecraftCommand( abstractCommentedConfig ) );
				discordCommandSource.sendMessage();
				return true;
			}
		}
		return false;
	}
	
	private static CommandSource buildSource( MinecraftServer server, DiscordCommandSource discordCommandSource ) {
		
		return new CommandSource(
			discordCommandSource,
			Vector3d.ZERO,
			Vector2f.ZERO,
			Objects.requireNonNull( server.getWorld( World.OVERWORLD ) ),
			4,
			MOD_NAME,
			new StringTextComponent( MOD_NAME ),
			server,
			null
		);
	}
	
	private static String buildDiscordCommand( AbstractCommentedConfig abstractCommentedConfig ) {
		
		return ServerConfig.getCommandPrefix() + CommandConfig.getDiscordCommand( abstractCommentedConfig );
	}
	
	private static String buildMinecraftCommand( AbstractCommentedConfig abstractCommentedConfig ) {
		
		return "/" + CommandConfig.getMinecraftCommand( abstractCommentedConfig );
	}
}
