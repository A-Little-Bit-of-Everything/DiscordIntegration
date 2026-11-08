package de.geheimagentnr1.discordintegration.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.apache.logging.log4j.Logger;


public class MessageConfig {
	
	
	private final ForgeConfigSpec.BooleanValue enabled;
	
	private final ForgeConfigSpec.ConfigValue<String> message;
	
	private final ForgeConfigSpec.BooleanValue use_player_webhook;
	
	//package-private
	MessageConfig(
		ForgeConfigSpec.Builder builder,
		String path,
		String pathComment,
		String enabledComment,
		String messageComment,
		String messageDefaultValue ) {
		
		this( builder, path, pathComment, enabledComment, messageComment, messageDefaultValue, null, null );
	}
	
	//package-private
	MessageConfig(
		ForgeConfigSpec.Builder builder,
		String path,
		String pathComment,
		String enabledComment,
		String messageComment,
		String messageDefaultValue,
		String usePlayerWebhookComment,
		Boolean usePlayerWebhookDefaultValue ) {
		
		builder.comment( pathComment )
			.push( path );
		enabled = builder.comment( enabledComment )
			.define( "enabled", true );
		message = builder.comment( messageComment )
			.define( "message", messageDefaultValue );
		if( usePlayerWebhookComment != null && usePlayerWebhookDefaultValue != null ) {
			use_player_webhook = builder.comment( usePlayerWebhookComment )
				.define( "use_player_webhook", usePlayerWebhookDefaultValue.booleanValue() );
		} else {
			use_player_webhook = null;
		}
		builder.pop();
	}
	
	public boolean isEnabled() {
		
		return enabled.get();
	}
	
	public String getMessage() {
		
		return message.get();
	}
	
	public boolean usePlayerWebhook() {
		
		return use_player_webhook != null && use_player_webhook.get();
	}
	
	//package-private
	void printConfig( Logger logger ) {
		
		logger.info( "{} = {}", enabled.getPath(), enabled.get() );
		logger.info( "{} = {}", message.getPath(), message.get() );
		if( use_player_webhook != null ) {
			logger.info( "{} = {}", use_player_webhook.getPath(), use_player_webhook.get() );
		}
	}
}
