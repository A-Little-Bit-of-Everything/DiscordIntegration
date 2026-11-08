package de.geheimagentnr1.discordintegration.elements.discord.chat;

import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import de.geheimagentnr1.discordintegration.config.ServerConfig;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordManager;
import de.geheimagentnr1.discordintegration.elements.discord.DiscordMessageBuilder;
import de.geheimagentnr1.discordintegration.elements.discord.commands.DiscordCommandHandler;
import de.geheimagentnr1.minecraft_forge_api.util.MessageUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.MinecraftServer;
import net.minecraftforge.server.ServerLifecycleHooks;
import org.jetbrains.annotations.NotNull;

import java.util.Map;


@Log4j2
@RequiredArgsConstructor
public class ChatMessageEventHandler extends ListenerAdapter {
	
	
	@NotNull
	private final ServerConfig serverConfig;
	
	@NotNull
	private final DiscordManager discordManager;
	
	@NotNull
	private final ChatManager chatManager;
	
	@NotNull
	private final DiscordCommandHandler discordCommandHandler;
	
	@NotNull
	private final DiscordMessageBuilder discordMessageBuilder;
	
	@Override
	public void onSlashCommandInteraction( @NotNull SlashCommandInteractionEvent event ) {
		
		discordCommandHandler.handleSlashCommandInteraction( event );
	}
	
	@Override
	public void onMessageReceived( @NotNull MessageReceivedEvent event ) {
		
		MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
		if( server == null || !chatManager.isCorrectChannel( event.getChannel().getIdLong() ) ) {
			return;
		}
		User author = event.getAuthor();
		if( chatManager.isOwnWebhook( author.getIdLong() ) ) {
			return;
		}
		String message = event.getMessage().getContentDisplay();
		Member member = event.getMember();
		
		if( member != null && discordCommandHandler.isCommand( message ) ) {
			handleCommand( member, message, server );
			return;
		}
		
		if( shouldMessageBeCanceled( author, event.getMessage() ) ) {
			return;
		}
		
		if( isMessageTooLong( message ) ) {
			handleTooLongMessage( member, message );
			return;
		}
		
		try {
			Component messageComponent = formatMessageForMinecraft( author, member, message );
			for( Component line : messageComponent.toFlatList() ) {
				server.getPlayerList().broadcastSystemMessage( line, false );
			}
		} catch( JsonParseException exception ) {
			log.error( "Invalid raw format for Discord to Minecraft messages", exception );
			handleMalformedJsonFormatForRawMessage( member, exception );
		}
	}
	
	private void handleCommand( Member member, String command, MinecraftServer server ) {
		
		discordCommandHandler.handleCommand(
			member,
			command,
			server,
			chatManager::sendFeedbackMessage
		);
	}
	
	private boolean shouldMessageBeCanceled( User author, Message message ) {
		
		String rawMessage = message.getContentRaw();
		boolean isOwnBotFeedback = author.getIdLong() == discordManager.getSelfUser().getIdLong() &&
			discordMessageBuilder.isMessageBotFeedback( rawMessage );
		boolean isIgnoredBotMessage = ( author.isBot() || message.isWebhookMessage() ) &&
			!serverConfig.getChatConfig().transmitBotMessages();
		return isOwnBotFeedback ||
			isIgnoredBotMessage ||
			isCommandOfOtherBots( rawMessage );
	}
	
	private boolean isCommandOfOtherBots( String rawMessage ) {
		
		for( String prefix : serverConfig.getCommandSettingsConfig().getOtherBotsCommandPrefixes() ) {
			if( rawMessage.startsWith( prefix ) ) {
				return true;
			}
		}
		return false;
	}
	
	private boolean isMessageTooLong( String message ) {
		
		int maxCharCount = serverConfig.getChatConfig().getMaxCharCount();
		return maxCharCount != -1 && message.length() > maxCharCount;
	}
	
	private void handleTooLongMessage( Member member, String message ) {
		
		if( member != null ) {
			chatManager.sendFeedbackMessage(
				MessageUtil.replaceParameters(
					serverConfig.getChatConfig().getMaxCharCountErrorMessage(),
					Map.of(
						"username", discordManager.getMemberAsTag( member ),
						"nickname", member.getEffectiveName(),
						"max_char_count", String.valueOf( serverConfig.getChatConfig().getMaxCharCount() ),
						"actual_message_char_count", String.valueOf( message.length() ),
						"new_line", System.lineSeparator()
					)
				)
			);
		}
	}
	
	@NotNull
	private Component formatMessageForMinecraft( User author, Member member, String message )
		throws JsonParseException {
		
		String displayName = member != null ? discordManager.getMemberAsTag( member ) : author.getEffectiveName();
		String hoverHandle = "@" + ( member != null ? member.getUser().getName() : author.getName() );
		boolean isRaw = serverConfig.getChatConfig().useRawMessageFormatDiscordToMinecraft();
		if( isRaw ) {
			String formattedMessage = MessageUtil.replaceParameters(
				serverConfig.getChatConfig().getMessageFormatDiscordToMinecraft(),
				Map.of(
					"username", escapeForJsonString( displayName ),
					"nickname", escapeForJsonString( displayName ),
					"message", escapeForJsonString( message )
				)
			);
			return Component.Serializer.fromJson(
				formattedMessage,
				ServerLifecycleHooks.getCurrentServer().registryAccess()
			);
		}
		return buildMinecraftComponentWithHover(
			serverConfig.getChatConfig().getMessageFormatDiscordToMinecraft(),
			displayName,
			hoverHandle,
			message
		);
	}
	
	@NotNull
	private Component buildMinecraftComponentWithHover(
		@NotNull String format,
		@NotNull String displayName,
		@NotNull String hoverHandle,
		@NotNull String message ) {
		
		String normalizedFormat = format.replace( "%username%", "%nickname%" );
		String token = "%nickname%";
		Component nameComponent = Component.literal( displayName ).withStyle(
			style -> style.withHoverEvent(
				new HoverEvent( HoverEvent.Action.SHOW_TEXT, Component.literal( hoverHandle ) )
			)
		);
		
		MutableComponent result = Component.empty();
		int index = 0;
		int matchIndex;
		while( ( matchIndex = normalizedFormat.indexOf( token, index ) ) != -1 ) {
			String before = normalizedFormat.substring( index, matchIndex ).replace( "%message%", message );
			if( !before.isEmpty() ) {
				result.append( Component.literal( before ) );
			}
			result.append( nameComponent.copy() );
			index = matchIndex + token.length();
		}
		String tail = normalizedFormat.substring( index ).replace( "%message%", message );
		if( !tail.isEmpty() ) {
			result.append( Component.literal( tail ) );
		}
		return result;
	}
	
	@NotNull
	private String escapeForJsonString( @NotNull String value ) {
		
		String jsonString = new JsonPrimitive( value ).toString();
		return jsonString.substring( 1, jsonString.length() - 1 );
	}
	
	private void handleMalformedJsonFormatForRawMessage( Member member, JsonParseException exception ) {
		
		if( member != null ) {
			chatManager.sendFeedbackMessage(
				MessageUtil.replaceParameters(
					serverConfig.getChatConfig().getInvalidRawMessageFormatForDiscordToMinecraftErrorMessage(),
					Map.of(
						"username", discordManager.getMemberAsTag( member ),
						"nickname", member.getEffectiveName(),
						"error_message", exception.getMessage(),
						"new_line", System.lineSeparator()
					)
				)
			);
		}
	}
}
