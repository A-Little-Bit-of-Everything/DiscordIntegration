package de.geheimagentnr1.minecraft_forge_api.config;

import com.electronwill.nightconfig.core.CommentedConfig;
import de.geheimagentnr1.minecraft_forge_api.AbstractMod;
import org.jetbrains.annotations.NotNull;


@FunctionalInterface
public interface SubConfigListFactory<T> {
	
	
	@NotNull
	T register(
		@NotNull AbstractMod _abstractMod,
		@NotNull AbstractSubConfig _parent,
		@NotNull CommentedConfig _commentedConfig );
}
