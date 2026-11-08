package de.geheimagentnr1.discordintegration.config.command_config;


import com.electronwill.nightconfig.core.UnmodifiableCommentedConfig;

import java.util.Map;
import java.util.function.Supplier;


public class WeatherCommandConfig extends CommandConfig {
	
	
	public WeatherCommandConfig() {
		
		super(
			"weather",
			"discord weather",
			false,
			true,
			false,
			"%command%%command_description_separator%shows the current weather on the server."
		);
	}
	
	private WeatherCommandConfig(
		String discordCommand,
		String minecraftCommand,
		boolean useParameters,
		boolean enabled,
		boolean managementCommand,
		String description ) {
		
		super( discordCommand, minecraftCommand, useParameters, enabled, managementCommand, description );
	}
	
	private WeatherCommandConfig(
		UnmodifiableCommentedConfig toCopy,
		@SuppressWarnings( "ParameterHidesMemberVariable" ) Supplier<Map<String, Object>> mapCreator ) {
		
		super( toCopy, mapCreator );
	}
	
	@SuppressWarnings( { "FinalMethod", "UseOfClone" } )
	@Override
	public final WeatherCommandConfig clone() {
		
		return new WeatherCommandConfig( this, mapCreator );
	}
	
	@Override
	public WeatherCommandConfig createSubConfig() {
		
		return new WeatherCommandConfig(
			getDiscordCommand( this ),
			getMinecraftCommand( this ),
			useParameters( this ),
			isEnabled( this ),
			isManagementCommand( this ),
			getDescription( this )
		);
	}
}
