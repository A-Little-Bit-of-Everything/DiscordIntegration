package de.geheimagentnr1.discordintegration.elements.discord;

import lombok.extern.log4j.Log4j2;
import net.dv8tion.jda.api.entities.Webhook;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


@Log4j2
public class DiscordMessageSender {
	
	
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
		@NotNull TextChannel fallbackChannel,
		@NotNull String username,
		@Nullable String avatarUrl,
		@NotNull String webhookMessage,
		@NotNull String fallbackMessage ) {
		
		String safeUsername = sanitizeWebhookUsername( username );
		try {
			for( int start = 0; start < webhookMessage.length(); start += 2000 ) {
				String part = webhookMessage.substring( start, Math.min( webhookMessage.length(), start + 2000 ) );
				webhook.sendMessage( part )
					.setUsername( safeUsername )
					.setAvatarUrl( avatarUrl )
					.queue(
						null,
						throwable -> {
							log.warn(
								"Webhook message could not be sent to channel {}, falling back to bot message",
								fallbackChannel.getIdLong(),
								throwable
							);
							sendMessage( fallbackChannel, fallbackMessage );
						}
					);
			}
		} catch( Exception exception ) {
			log.warn(
				"Webhook message could not be sent to channel {}, falling back to bot message",
				fallbackChannel.getIdLong(),
				exception
			);
			sendMessage( fallbackChannel, fallbackMessage );
		}
	}
	
	@NotNull
	private String sanitizeWebhookUsername( @NotNull String username ) {
		
		// Discord forbids "discord" and "clyde" (case-insensitive) in webhook usernames
		String sanitized = username
			.replaceAll( "(?i)discord", "D\u200Biscord" )
			.replaceAll( "(?i)clyde", "C\u200Blyde" )
			.trim();
		if( sanitized.isEmpty() ) {
			return "Player";
		}
		if( sanitized.length() > 80 ) {
			return sanitized.substring( 0, 80 );
		}
		return sanitized;
	}
}
