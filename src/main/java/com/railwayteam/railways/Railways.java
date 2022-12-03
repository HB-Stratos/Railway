package com.railwayteam.railways;

import com.railwayteam.railways.base.data.lang.LangMerger;
import com.railwayteam.railways.base.data.recipe.RailwaysSequencedAssemblyRecipeGen;
import com.railwayteam.railways.content.conductor.ConductorCapModel;
import com.railwayteam.railways.content.conductor.ConductorEntityModel;
import com.railwayteam.railways.registry.CRCommands;
import com.simibubi.create.foundation.ModFilePackResources;
import com.simibubi.create.foundation.data.CreateRegistrate;
import com.simibubi.create.foundation.ponder.PonderLocalization;
import com.tterrag.registrate.util.nullness.NonNullSupplier;
import net.minecraft.data.DataGenerator;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackSource;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AddPackFindersEvent;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent;
import net.minecraftforge.forgespi.language.IModFileInfo;
import net.minecraftforge.forgespi.locating.IModFile;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@Mod(Railways.MODID)
public class Railways {
	public static final String MODID = "railways";
	public static Railways instance;
  public static final Logger LOGGER = LogManager.getLogger(MODID);
  public static final ModSetup setup = new ModSetup();

  private static final NonNullSupplier<CreateRegistrate> REGISTRATE = CreateRegistrate.lazy(MODID);

  public static IEventBus MOD_EVENT_BUS;

  public Railways() {
  	instance = this;

  	ModLoadingContext.get().registerConfig(ModConfig.Type.CLIENT, Config.CLIENT_CONFIG);
    ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, Config.COMMON_CONFIG);

    MOD_EVENT_BUS = FMLJavaModLoadingContext.get().getModEventBus();

    ModSetup.register();



    MOD_EVENT_BUS.addListener(this::setup);
    MOD_EVENT_BUS.addListener(EventPriority.LOWEST, Railways::gatherData);
    MOD_EVENT_BUS.addListener(this::registerModelLayers);
    MOD_EVENT_BUS.addListener(this::addPackFinders);
    MinecraftForge.EVENT_BUS.register(this);

    Config.loadConfig(Config.CLIENT_CONFIG, FMLPaths.CONFIGDIR.get().resolve(MODID + "-client.toml"));
    Config.loadConfig(Config.COMMON_CONFIG, FMLPaths.CONFIGDIR.get().resolve(MODID + "-common.toml"));

    MOD_EVENT_BUS.addListener(RailwaysClient::clientSetup);

    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> RailwaysClient::clientRegister);
  }

  private void setup(final FMLCommonSetupEvent event) {
    setup.init();
  }

  public static ResourceLocation asResource(String name) {
		return new ResourceLocation(MODID, name);
	}

  public static void gatherData(GatherDataEvent event) {
    DataGenerator gen = event.getGenerator();
    if (event.includeServer()) {
      gen.addProvider(new RailwaysSequencedAssemblyRecipeGen(gen));
    }
    if (event.includeClient()) {
      PonderLocalization.provideRegistrateLang(REGISTRATE.get());
      gen.addProvider(new LangMerger(gen));
    }

  }

  //Thanks to Create for this event
  public void addPackFinders(AddPackFindersEvent event) {
    if (event.getPackType() == PackType.CLIENT_RESOURCES) {
      IModFileInfo modFileInfo = ModList.get().getModFileById(Railways.MODID);
      if (modFileInfo == null) {
        Railways.LOGGER.error("Could not find Steam & Rails mod file info; built-in resource packs will be missing!");
        return;
      }
      IModFile modFile = modFileInfo.getFile();
      event.addRepositorySource((consumer, constructor) -> consumer.accept(Pack.create(Railways.asResource("legacy_semaphore").toString(),
          false, () -> new ModFilePackResources("Steam 'n Rails Legacy Semaphores", modFile, "resourcepacks/legacy_semaphore"),
          constructor, Pack.Position.TOP, PackSource.DEFAULT)));
      event.addRepositorySource((consumer, constructor) -> consumer.accept(Pack.create(Railways.asResource("green_signals").toString(),
          false, () -> new ModFilePackResources("Steam 'n Rails Green Signals", modFile, "resourcepacks/green_signals"),
          constructor, Pack.Position.TOP, PackSource.DEFAULT)));
    }
  }

  @SubscribeEvent
  public void registerModelLayers (EntityRenderersEvent.RegisterLayerDefinitions event) {
    event.registerLayerDefinition(ConductorEntityModel.LAYER_LOCATION, ConductorEntityModel::createBodyLayer);
    event.registerLayerDefinition(ConductorCapModel.LAYER_LOCATION, ConductorCapModel::createBodyLayer);
  }

  @SubscribeEvent
  public void registerCommands(RegisterCommandsEvent event) {
    CRCommands.register(event.getDispatcher());
  }

  public static CreateRegistrate registrate() {
    return REGISTRATE.get();
  }
}
