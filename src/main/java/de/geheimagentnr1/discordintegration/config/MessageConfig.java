package de.geheimagentnr1.discordintegration.config;

import de.geheimagentnr1.minecraft_forge_api.AbstractMod;
import de.geheimagentnr1.minecraft_forge_api.config.AbstractSubConfig;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


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
	
	@Nullable
	private final String usePlayerWebhookComment;
	
	@Nullable
	private final Boolean usePlayerWebhookDefaultValue;
	
	MessageConfig(
		@NotNull AbstractMod _abstractMod,
		@NotNull AbstractSubConfig _parent,
		@NotNull String enabledComment,
		@NotNull String messageComment,
		@NotNull String messageDefaultValue ) {
		
		this( _abstractMod, _parent, enabledComment, messageComment, messageDefaultValue, null, null );
	}
	
	MessageConfig(
		@NotNull AbstractMod _abstractMod,
		@NotNull AbstractSubConfig _parent,
		@NotNull String enabledComment,
		@NotNull String messageComment,
		@NotNull String messageDefaultValue,
		@Nullable String usePlayerWebhookComment,
		@Nullable Boolean usePlayerWebhookDefaultValue ) {
		
		super( _abstractMod, _parent );
		this.enabledComment = enabledComment;
		this.messageComment = messageComment;
		this.messageDefaultValue = messageDefaultValue;
		this.usePlayerWebhookComment = usePlayerWebhookComment;
		this.usePlayerWebhookDefaultValue = usePlayerWebhookDefaultValue;
	}
	
	@Override
	protected void registerConfigValues() {
		
		registerConfigValue( enabledComment, ENABLED_KEY, true );
		registerConfigValue( messageComment, MESSAGE_KEY, messageDefaultValue );
		if( usePlayerWebhookComment != null && usePlayerWebhookDefaultValue != null ) {
			registerConfigValue( usePlayerWebhookComment, USE_PLAYER_WEBHOOK_KEY, usePlayerWebhookDefaultValue );
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
		
		if( usePlayerWebhookComment == null || usePlayerWebhookDefaultValue == null ) {
			return false;
		}
		return getValue( Boolean.class, USE_PLAYER_WEBHOOK_KEY );
	}
}
