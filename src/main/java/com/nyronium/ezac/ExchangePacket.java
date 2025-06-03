package com.nyronium.ezac;

import com.nyronium.ezac.util.ComponentUtils;
import net.minecraft.ChatFormatting;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.UserBanList;
import net.minecraft.server.players.UserBanListEntry;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.function.Predicate;
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
            Main.LOGGER.info("Initializing packet exchange...");
            Date checkStart = new Date();

            ServerPlayer sender = ctx.get().getSender();
            if(sender == null) return;

            List<String> serverMods = new ArrayList<>();
            ModList.get().getMods().forEach((modInfo -> serverMods.add(modInfo.getModId())));

            Main.LOGGER.info("Parsing resource packs...");
            List<String> resourcePacks = msg.resourcePackList;
            resourcePacks.removeAll(List.of("High Contrast", "Mod Resources", "Programmer Art", "Default"));

            Main.LOGGER.info("Parsing mods...");
            List<String> clientOnlyMods = msg.modList;
            clientOnlyMods.removeAll(serverMods);

            String resourcePackString = String.join(";", resourcePacks);
            String clientOnlyModString = String.join(";", clientOnlyMods);

            Main.LOGGER.info("Received EasyAntiCheat packet from authorized client {} ({}/{}):", sender.getName().getString(), sender.getStringUUID(), sender.getIpAddress());
            Main.LOGGER.info("Resource packs present on client:");
            Main.LOGGER.info("[{}]", resourcePackString);
            Main.LOGGER.info("Mods exclusively present on client:");
            Main.LOGGER.info("[{}]", clientOnlyModString);

            List<String> flaggedMods = new ArrayList<>();
            List<String> flaggedResourcePacks = new ArrayList<>();

            Predicate<String> modsFilter = switch (ConfigHandler.mode.get()) {
                case WHITELIST -> mod -> !ConfigHandler.mods.get().contains(mod);
                case BLACKLIST -> mod -> ConfigHandler.mods.get().contains(mod);
            };

            Predicate<String> resourcePacksFilter = switch (ConfigHandler.mode.get()) {
                case WHITELIST -> resourcePack -> !ConfigHandler.resourcePacks.get().contains(resourcePack);
                case BLACKLIST -> resourcePack -> ConfigHandler.resourcePacks.get().contains(resourcePack);
            };

            for (String mod : clientOnlyMods) {
                if (modsFilter.test(mod)) {
                    flaggedMods.add(mod);
                }
            }
            for (String resourcePack : resourcePacks) {
                if (resourcePacksFilter.test(resourcePack)) {
                    flaggedResourcePacks.add(resourcePack);
                }
            }

            if (!flaggedMods.isEmpty() || !flaggedResourcePacks.isEmpty()) {
                if(sender.getServer() == null) return;
                for(ServerPlayer player : sender.getServer().getPlayerList().getPlayers()) {
                    if(sender.getServer().getPlayerList().isOp(player.getGameProfile())) {
                        MutableComponent message = Component.literal("The player ")
                                .append(ComponentUtils.hoverComponent(ComponentUtils.defaultGradient(sender.getGameProfile().getName()), Component.literal(sender.getIpAddress())))
                                .append("has been determined to use the following flagged modifications:")
                                .append("\n\n");

                        for(String mod : flaggedMods) {
                            message.append(Component.literal("- ").withStyle(ChatFormatting.DARK_GRAY))
                                    .append(ComponentUtils.defaultGradient(mod));
                        }

                        for(String mod : flaggedResourcePacks) {
                            message.append(Component.literal("- ").withStyle(ChatFormatting.DARK_GRAY))
                                    .append(ComponentUtils.defaultGradient(mod));
                        }

                        player.sendSystemMessage(ComponentUtils.defaultGradient("[EasyAntiCheat] ").append("Player Report").withStyle(ChatFormatting.RESET));
                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(message);
                        player.sendSystemMessage(Component.literal(""));
                        player.sendSystemMessage(Component.literal("Please check the console for a more detailed report.").withStyle(ChatFormatting.RESET));
                    }
                }

                switch (ConfigHandler.action.get()) {
                    case RESTRICT -> sender.connection.disconnect(ComponentUtils.disconnectMessage(sender, flaggedMods, "You can't access this server."));
                    case BAN -> {
                        if(sender.getServer() == null) return;

                        MutableComponent message = ComponentUtils.disconnectMessage(sender, flaggedMods, "You have been banned permanently from this server.");

                        UserBanList banList = sender.getServer().getPlayerList().getBans();
                        UserBanListEntry userBanEntry = new UserBanListEntry(
                                sender.getGameProfile(),
                                null,
                                sender.getGameProfile().getName(),
                                null,
                                message.toString()
                        );
                        banList.add(userBanEntry);
                        sender.connection.disconnect(message);
                    }
                }
            }

            double secondsPassed = Math.round(new Date().getTime()-checkStart.getTime()) / 1000.0;
            Main.LOGGER.info("List flagging check on player {} has been performed successfully in {}s with {} mods and {} resource packs flagged.", sender.getGameProfile().getName(), secondsPassed, flaggedMods.size(), flaggedResourcePacks.size());
            Main.LOGGER.info("Packet exchange concluded.");
        });
        ctx.get().setPacketHandled(true);
    }
}
