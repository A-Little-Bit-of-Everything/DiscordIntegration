package de.geheimagentnr1.discordintegration.elements.discord;

import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.regex.Pattern;


@Log4j2
public class DiscordMessageSender {
	
	
	private static final Pattern DISCORD_SUBSTRING_PATTERN =
		Pattern.compile( "discord", Pattern.CASE_INSENSITIVE );
	
	private static final Pattern CLYDE_SUBSTRING_PATTERN =
		Pattern.compile( "clyde", Pattern.CASE_INSENSITIVE );
	
	public void sendMessage( @NotNull TextChannel channel, @NotNull String message ) {
		
		try {
			for( int start = 0; start < message.length(); start += 2000 ) {
				channel.sendMessage( message.substring( start, Math.min( message.length(), start + 2000 ) ) ).queue();
			}
		} catch( Exception exception ) {
			log.error( "Message could not be send to channel {}", channel.getIdLong(), exception );
		}
	}
	
	public void sendWebhookMessage(
		@NotNull Webhook webhook,
		@NotNull String username,
		@Nullable String avatarUrl,
		@NotNull String message ) {
		
		try {
			String safeUsername = sanitizeWebhookUsername( username );
			for( int start = 0; start < message.length(); start += 2000 ) {
				var action = webhook.sendMessage(
					message.substring( start, Math.min( message.length(), start + 2000 ) )
				).setUsername( safeUsername );
				if( avatarUrl != null && !avatarUrl.isBlank() ) {
					action = action.setAvatarUrl( avatarUrl );
				}
				action.queue();
			}
		} catch( Exception exception ) {
			log.error( "Webhook message could not be sent via webhook {}", webhook.getIdLong(), exception );
		}
	}
	
	@NotNull
	private String sanitizeWebhookUsername( @NotNull String username ) {
		
		String sanitized = DISCORD_SUBSTRING_PATTERN.matcher( username ).replaceAll( "D\u200Biscord" );
		sanitized = CLYDE_SUBSTRING_PATTERN.matcher( sanitized ).replaceAll( "C\u200Blyde" );
		if( sanitized.length() > 80 ) {
			sanitized = sanitized.substring( 0, 80 );
		}
		if( sanitized.isBlank() ) {
			return "Minecraft";
		}
		return sanitized;
	}
}
