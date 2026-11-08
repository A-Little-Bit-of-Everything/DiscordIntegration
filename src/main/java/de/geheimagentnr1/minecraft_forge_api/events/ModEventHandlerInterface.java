package de.geheimagentnr1.minecraft_forge_api.events;

import net.minecraftforge.fml.event.config.ModConfigEvent;
import org.jetbrains.annotations.NotNull;


public interface ModEventHandlerInterface {
	
	
	default void handleModConfigLoadingEvent( @NotNull ModConfigEvent.Loading event ) {
		
		//NOOP
	}
	
	default void handleModConfigReloadingEvent( @NotNull ModConfigEvent.Reloading event ) {
		
		//NOOP
	}
}
