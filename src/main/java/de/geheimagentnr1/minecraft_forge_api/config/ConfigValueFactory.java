package de.geheimagentnr1.minecraft_forge_api.config;

import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;


@FunctionalInterface
public interface ConfigValueFactory<T> {
	
	
	@NotNull
	ForgeConfigSpec.ConfigValue<T> register( @NotNull ForgeConfigSpec.Builder _builder, @NotNull String _path );
}
