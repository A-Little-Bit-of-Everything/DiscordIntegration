package de.geheimagentnr1.minecraft_forge_api.elements.commands;

import de.geheimagentnr1.minecraft_forge_api.events.ForgeEventHandlerInterface;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;


public abstract class CommandsRegisterFactory implements ForgeEventHandlerInterface {
	
	
	@SubscribeEvent
	@Override
	public void handleRegisterCommandsEvent( @NotNull RegisterCommandsEvent event ) {
		
		commands().forEach( commandInterface -> commandInterface.register( event.getDispatcher() ) );
	}
	
	@NotNull
	protected abstract List<CommandInterface> commands();
}
