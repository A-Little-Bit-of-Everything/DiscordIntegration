package de.geheimagentnr1.minecraft_forge_api.events;

import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.AdvancementEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import org.jetbrains.annotations.NotNull;


public interface ForgeEventHandlerInterface {
	
	
	default void handleServerStartingEvent( @NotNull ServerStartingEvent event ) {
		
		//NOOP
	}
	
	default void handleServerStartedEvent( @NotNull ServerStartedEvent event ) {
		
		//NOOP
	}
	
	default void handleServerStoppedEvent( @NotNull ServerStoppedEvent event ) {
		
		//NOOP
	}
	
	default void handleRegisterCommandsEvent( @NotNull RegisterCommandsEvent event ) {
		
		//NOOP
	}
	
	default void handlePlayerLoggedInEvent( @NotNull PlayerEvent.PlayerLoggedInEvent event ) {
		
		//NOOP
	}
	
	default void handlePlayerLoggedOutEvent( @NotNull PlayerEvent.PlayerLoggedOutEvent event ) {
		
		//NOOP
	}
	
	default void handleServerChatEvent( @NotNull ServerChatEvent event ) {
		
		//NOOP
	}
	
	default void handleLivingDeathEvent( @NotNull LivingDeathEvent event ) {
		
		//NOOP
	}
	
	default void handleAdvancementEarnEvent( @NotNull AdvancementEvent.AdvancementEarnEvent event ) {
		
		//NOOP
	}
}
