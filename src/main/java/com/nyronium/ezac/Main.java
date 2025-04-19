package com.nyronium.ezac;

import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.repository.Pack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.ClientPlayerNetworkEvent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Mod(Main.ID)
public class Main {
    public static final String ID = "ezac";
    public static final Logger LOGGER = LogUtils.getLogger();
    public static FMLJavaModLoadingContext CONTEXT;

    private static final String PROTOCOL_VERSION = "1";
    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            ResourceLocation.fromNamespaceAndPath(ID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    private static int packetId = 0;

    public Main(FMLJavaModLoadingContext context) {
        CONTEXT = context;

        MinecraftForge.EVENT_BUS.register(this);
        ConfigHandler.register();

        CHANNEL.registerMessage(packetId++,
                ExchangePacket.class,
                ExchangePacket::encode,
                ExchangePacket::decode,
                ExchangePacket::handle);
    }

    @Mod.EventBusSubscriber(modid = ID, bus = Mod.EventBusSubscriber.Bus.FORGE, value = Dist.CLIENT)
    public static class ClientForgeEvents {

        @SubscribeEvent
        public static void onClientJoin(ClientPlayerNetworkEvent.LoggingIn event) {
            Collection<Pack> resourcePacks = Minecraft.getInstance().getResourcePackRepository().getAvailablePacks();
            List<String> resourcePackNames = new ArrayList<>();
            resourcePacks.forEach(pack -> resourcePackNames.add(pack.getTitle().getString()));

            List<IModInfo> mods = ModList.get().getMods();
            List<String> modIds = new ArrayList<>();
            mods.forEach((modInfo) -> modIds.add(modInfo.getModId()));
            CHANNEL.sendToServer(new ExchangePacket(resourcePackNames, modIds));
        }
    }
}
