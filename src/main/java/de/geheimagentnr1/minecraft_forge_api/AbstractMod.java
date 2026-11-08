package de.geheimagentnr1.minecraft_forge_api;

import de.geheimagentnr1.minecraft_forge_api.config.AbstractConfig;
import de.geheimagentnr1.minecraft_forge_api.events.ForgeEventHandlerInterface;
import de.geheimagentnr1.minecraft_forge_api.events.ModEventHandlerInterface;
import lombok.Getter;
import lombok.extern.log4j.Log4j2;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.ModContainer;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.jetbrains.annotations.NotNull;

import java.util.EnumMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;


@Log4j2
public abstract class AbstractMod {
	
	
	@Getter
	@NotNull
	private final String modName;
	
	@NotNull
	private final IEventBus modEventBus;
	
	@NotNull
	private final ModContainer modContainer;
	
	@NotNull
	private final Map<ModConfig.Type, AbstractConfig> configs = new EnumMap<>( ModConfig.Type.class );
	
	@SuppressWarnings( "removal" )
	protected AbstractMod() {
		
		this.modEventBus = FMLJavaModLoadingContext.get().getModEventBus();
		this.modContainer = ModLoadingContext.get().getActiveContainer();
		this.modName = modContainer.getModInfo().getDisplayName();
		init();
	}
	
	private void init() {
		
		initMod();
	}
	
	@NotNull
	public abstract String getModId();
	
	protected abstract void initMod();
	
	@NotNull
	protected <T extends ForgeEventHandlerInterface> T registerEventHandler( @NotNull T handler ) {
		
		forgeEventBus().register( handler );
		return handler;
	}
	
	@NotNull
	protected <T extends ModEventHandlerInterface> T registerEventHandler( @NotNull T handler ) {
		
		modEventBus().register( handler );
		return handler;
	}
	
	@NotNull
	protected IEventBus forgeEventBus() {
		
		return MinecraftForge.EVENT_BUS;
	}
	
	@NotNull
	protected IEventBus modEventBus() {
		
		return modEventBus;
	}
	
	@SuppressWarnings( "removal" )
	@NotNull
	protected <T extends AbstractConfig> T registerConfig( @NotNull Function<AbstractMod, T> builder ) {
		
		T config = builder.apply( this );
		ModConfig.Type configType = config.type();
		configs.put( configType, config );
		ModLoadingContext.get().registerConfig( configType, config.getSpec() );
		modEventBus.addListener( config::handleModConfigLoadingEvent );
		modEventBus.addListener( config::handleModConfigReloadingEvent );
		MinecraftForge.EVENT_BUS.addListener( config::handleServerStartingEvent );
		if( config.isEarlyLoad() && configType != ModConfig.Type.SERVER ) {
			config.load();
		}
		return config;
	}
	
	@NotNull
	public <T> Optional<T> getConfig( @NotNull ModConfig.Type type, @NotNull Class<T> configClass ) {
		
		AbstractConfig config = configs.get( type );
		if( configClass.isInstance( config ) ) {
			return Optional.of( configClass.cast( config ) );
		}
		return Optional.empty();
	}
}
