package de.geheimagentnr1.minecraft_forge_api.config;

import lombok.RequiredArgsConstructor;
import net.minecraftforge.common.ForgeConfigSpec;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;


@RequiredArgsConstructor
public class ConfigValue<T> {
	
	
	private final ForgeConfigSpec.ConfigValue<T> configValue;
	
	public void setValue( @Nullable T value ) {
		
		configValue.set( value );
	}
	
	@NotNull
	public T getValue() {
		
		return configValue.get();
	}
}
