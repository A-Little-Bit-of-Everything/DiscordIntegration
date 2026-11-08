package de.geheimagentnr1.discordintegration.config.command_config;

import de.geheimagentnr1.minecraft_forge_api.AbstractMod;
import de.geheimagentnr1.minecraft_forge_api.config.AbstractSubConfig;
import org.jetbrains.annotations.NotNull;


public class WeatherCommandConfig extends CommandConfig {
	
	
	public WeatherCommandConfig( @NotNull AbstractMod _abstractMod, @NotNull AbstractSubConfig _parent ) {
		
		super( _abstractMod, _parent );
	}
	
	@NotNull
	@Override
	protected String discordCommandDefaultValue() {
		
		return "weather";
	}
	
	@NotNull
	@Override
	protected String minecraftCommandDefaultValue() {
		
		return "discord weather";
	}
	
	@NotNull
	@Override
	protected String descriptionDefaultValue() {
		
		return "%command%%command_description_separator%shows the current weather on the server.";
	}
}
