package de.geheimagentnr1.discordintegration.config;

import de.geheimagentnr1.discordintegration.api.AbstractMod;
import de.geheimagentnr1.discordintegration.api.config.AbstractSubConfig;
import org.jetbrains.annotations.NotNull;


public class MessageConfig extends AbstractSubConfig {
	
	
	@NotNull
	private static final String ENABLED_KEY = "enabled";
	
	@NotNull
	private static final String MESSAGE_KEY = "message";
	
	@NotNull
	private static final String USE_PLAYER_WEBHOOK_KEY = "use_player_webhook";
	
	@NotNull
	private final String enabledComment;
	
	@NotNull
	private final String messageComment;
	
	@NotNull
	private final String messageDefaultValue;
	
	private final boolean hasPlayerWebhookOption;
	
	private final boolean usePlayerWebhookDefaultValue;
	
	MessageConfig(
		@NotNull AbstractMod _abstractMod,
		@NotNull AbstractSubConfig _parent,
		@NotNull String enabledComment,
		@NotNull String messageComment,
		@NotNull String messageDefaultValue ) {
		
		this( _abstractMod, _parent, enabledComment, messageComment, messageDefaultValue, false, false );
	}
	
	MessageConfig(
		@NotNull AbstractMod _abstractMod,
		@NotNull AbstractSubConfig _parent,
		@NotNull String enabledComment,
		@NotNull String messageComment,
		@NotNull String messageDefaultValue,
		boolean hasPlayerWebhookOption,
		boolean usePlayerWebhookDefaultValue ) {
		
		super( _abstractMod, _parent );
		this.enabledComment = enabledComment;
		this.messageComment = messageComment;
		this.messageDefaultValue = messageDefaultValue;
		this.hasPlayerWebhookOption = hasPlayerWebhookOption;
		this.usePlayerWebhookDefaultValue = usePlayerWebhookDefaultValue;
	}
	
	@Override
	protected void registerConfigValues() {
		
		registerConfigValue( enabledComment, ENABLED_KEY, true );
		registerConfigValue( messageComment, MESSAGE_KEY, messageDefaultValue );
		if( hasPlayerWebhookOption ) {
			registerConfigValue(
				"Should this message use the player's name and avatar for the webhook (true) " +
					"or the server webhook name and avatar (false)?",
				USE_PLAYER_WEBHOOK_KEY,
				usePlayerWebhookDefaultValue
			);
		}
	}
	
	public boolean isEnabled() {
		
		return getValue( Boolean.class, ENABLED_KEY );
	}
	
	@NotNull
	public String getMessage() {
		
		return getValue( String.class, MESSAGE_KEY );
	}
	
	public boolean usePlayerWebhook() {
		
		return hasPlayerWebhookOption && getValue( Boolean.class, USE_PLAYER_WEBHOOK_KEY );
	}
}
