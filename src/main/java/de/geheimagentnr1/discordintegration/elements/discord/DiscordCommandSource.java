package de.geheimagentnr1.discordintegration.elements.discord;

import de.geheimagentnr1.discordintegration.net.DiscordNet;
import net.minecraft.command.ICommandSource;
import net.minecraft.util.text.ITextComponent;

import java.util.function.Consumer;


//package-private
class DiscordCommandSource implements ICommandSource {
	
	
	private String message = "";
	
	private final Consumer<String> feedbackSender;
	
	//package-private
	DiscordCommandSource() {
		
		this( DiscordNet::sendFeedbackMessage );
	}
	
	//package-private
	DiscordCommandSource( Consumer<String> _feedbackSender ) {
		
		feedbackSender = _feedbackSender;
	}
	
	//package-private
	void sendMessage() {
		
		feedbackSender.accept( message );
	}
	
	/**
	 * Send a chat message to the CommandSender
	 */
	@Override
	public void sendMessage( ITextComponent component ) {
		
		message += String.format( "%s%n", component.getString() );
	}
	
	@Override
	public boolean shouldReceiveFeedback() {
		
		return true;
	}
	
	@Override
	public boolean shouldReceiveErrors() {
		
		return true;
	}
	
	@Override
	public boolean allowLogging() {
		
		return false;
	}
}
