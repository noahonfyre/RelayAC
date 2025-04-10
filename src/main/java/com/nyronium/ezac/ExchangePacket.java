package com.nyronium.ezac;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.forgespi.language.IModInfo;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class ExchangePacket {
    private final List<String> data;

    public ExchangePacket(List<String> data) {
        this.data = data;
    }

    public static void encode(ExchangePacket msg, FriendlyByteBuf buf) {
        buf.writeVarInt(msg.data.size());
        for (String mod : msg.data) {
            buf.writeUtf(mod);
        }
    }

    public static ExchangePacket decode(FriendlyByteBuf buf) {
        int size = buf.readVarInt();
        List<String> mods = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            mods.add(buf.readUtf());
        }
        return new ExchangePacket(mods);
    }

    public static void handle(ExchangePacket msg, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer sender = ctx.get().getSender();
            if(sender == null) return;

            List<String> serverMods = new ArrayList<>();
            ModList.get().getMods().forEach((modInfo -> serverMods.add(modInfo.getModId())));

            List<String> clientOnlyMods = msg.data;
            clientOnlyMods.removeAll(serverMods);

            String clientOnlyModString = String.join(";", clientOnlyMods);

            Main.LOGGER.info("Received mod list from client {}:", sender.getName().getString());
            Main.LOGGER.info(clientOnlyModString);
            Main.LOGGER.info("Logged in at: {} {} {} in {}", sender.getBlockX(), sender.getBlockY(), sender.getBlockZ(), sender.level().dimension().location());
        });
        ctx.get().setPacketHandled(true);
    }
}
