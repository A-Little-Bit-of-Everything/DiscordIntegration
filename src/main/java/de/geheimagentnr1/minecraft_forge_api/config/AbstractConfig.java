package de.geheimagentnr1.minecraft_forge_api.config;

import de.geheimagentnr1.minecraft_forge_api.AbstractMod;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.jetbrains.annotations.NotNull;


@Log4j2
public abstract class AbstractConfig extends AbstractSubConfig {
	
	
	@NotNull
	@Getter
	private final ForgeConfigSpec spec;
	
	protected AbstractConfig( @NotNull AbstractMod _abstractMod ) {
		
		super( _abstractMod, null, new ForgeConfigSpec.Builder() );
		spec = getBuilder().build();
	}
	
	@Override
	public boolean isLoaded() {
		
		return spec.isLoaded();
	}
	
	public void load() {
		
		// Loaded by FML
	}
	
	@NotNull
	public abstract ModConfig.Type type();
	
	public abstract boolean isEarlyLoad();
	
	public void handleServerStartingEvent( @NotNull ServerStartingEvent event ) {
		
		if( isEarlyLoad() && type() == ModConfig.Type.SERVER ) {
			load();
		}
	}
	
	public void handleModConfigLoadingEvent( @NotNull ModConfigEvent.Loading event ) {
		
		log.info( "\"{}\" {} Config loaded", abstractMod.getModName(), type() );
		handleConfigChanging();
		handleConfigLoading();
	}
	
	public void handleModConfigReloadingEvent( @NotNull ModConfigEvent.Reloading event ) {
		
		log.info( "\"{}\" {} Config reloaded", abstractMod.getModName(), type() );
		handleConfigChanging();
		handleConfigReloading();
	}
	
	protected void handleConfigChanging() {
		
		//NOOP
	}
	
	protected void handleConfigLoading() {
		
		//NOOP
	}
	
	protected void handleConfigReloading() {
		
		//NOOP
	}
}
