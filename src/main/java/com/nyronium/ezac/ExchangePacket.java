package com.nyronium.ezac;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ExchangePacket {
    private final List<String> modList;
    private final List<String> resourcePackList;

    public ExchangePacket(List<String> resourcePacks, List<String> modIdList) {
        this.modList = modIdList;
        this.resourcePackList = resourcePacks;
    }

    public static void encode(ExchangePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.resourcePackList.size());
        for (String resourcePack : msg.resourcePackList) {
            buf.writeUtf(resourcePack);
        }
        buf.writeVarInt(msg.modList.size());
        for (String mod : msg.modList) {
            buf.writeUtf(mod);
        }
    }

    public static ExchangePacket decode(FriendlyByteBuf buf) {
        int sizeResourcePacks = buf.readVarInt();
        List<String> resourcePacks = new ArrayList<>();
        for (int i = 0; i < sizeResourcePacks; i++) {
            resourcePacks.add(buf.readUtf());
        }

        int sizeMods = buf.readVarInt();
        List<String> mods = new ArrayList<>();
        for (int i = 0; i < sizeMods; i++) {
            mods.add(buf.readUtf());
        }

        return new ExchangePacket(resourcePacks, mods);
    }

    public static void handle(ExchangePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if(sender == null) return;

            List<String> serverMods = new ArrayList<>();
            ModList.get().getMods().forEach((modInfo -> serverMods.add(modInfo.getModId())));

            List<String> resourcePacks = msg.resourcePackList;
            resourcePacks.removeAll(List.of("High Contrast", "Mod Resources", "Programmer Art", "Default"));

            List<String> clientOnlyMods = msg.modList;
            clientOnlyMods.removeAll(serverMods);

            String resourcePackString = String.join(";", resourcePacks);
            String clientOnlyModString = String.join(";", clientOnlyMods);

            Main.LOGGER.info("Received EasyAntiCheat packet data from verified client {} ({}):", sender.getName().getString(), sender.getStringUUID());
            Main.LOGGER.info("Resource packs on client:");
            Main.LOGGER.info("[{}]", resourcePackString);
            Main.LOGGER.info("Mods exclusively on client:");
            Main.LOGGER.info("[{}]", clientOnlyModString);
        });
        ctx.get().setPacketHandled(true);
    }
}
