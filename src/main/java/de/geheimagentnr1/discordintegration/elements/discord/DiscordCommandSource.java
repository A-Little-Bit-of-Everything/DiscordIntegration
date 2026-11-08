package de.geheimagentnr1.discordintegration.elements.discord;

import de.geheimagentnr1.discordintegration.net.DiscordNet;
import net.minecraft.commands.CommandSource;
import net.minecraft.network.chat.Component;

import javax.annotation.Nonnull;
import java.util.UUID;
import java.util.function.Consumer;


//package-private
class DiscordCommandSource implements CommandSource {
	
	
	private final Consumer<String> feedbackSender;
	
	private String message = "";
	
	//package-private
	DiscordCommandSource() {
		
		this( DiscordNet::sendFeedbackMessage );
	}
	
	//package-private
	DiscordCommandSource( Consumer<String> feedbackSender ) {
		
		this.feedbackSender = feedbackSender;
	}
	
	//package-private
	void sendMessage() {
		
		if( !message.isEmpty() ) {
			feedbackSender.accept( message );
		}
	}
	
	@Override
	public void sendMessage( Component component, @Nonnull UUID uuid ) {
		
		message += String.format( "%s%n", component.getString() );
	}
	
	@Override
	public boolean acceptsSuccess() {
		
		return true;
	}
	
	@Override
	public boolean acceptsFailure() {
		
		return true;
	}
	
	@Override
	public boolean shouldInformAdmins() {
		
		return false;
	}
}
