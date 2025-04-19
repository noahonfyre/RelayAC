package com.nyronium.ezac.util;

import com.nyronium.ezac.ConfigHandler;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.*;
import net.minecraft.server.level.ServerPlayer;

import java.util.List;

public class ComponentUtils {
    public static MutableComponent disconnectMessage(ServerPlayer sender, List<String> flaggedMods, String punishment) {
        MutableComponent message = ComponentUtils.defaultGradient("[EasyAntiCheat]")
                .append(" ")
                .append(Component.literal(punishment).withStyle(ChatFormatting.WHITE));

        message.append(Component.literal("\n\nIf you believe, you have been falsely flagged,\nplease consult the server operators.").withStyle(ChatFormatting.GRAY));
        message.append(Component.literal("\n\n" + sender.getGameProfile().getName() + " - " + sender.getIpAddress()).withStyle(ChatFormatting.DARK_GRAY));

        return message;
    }

    public static MutableComponent defaultGradient(String text) {
        return gradientComponent(text, 0xFF8800, 0xEA00FF);
    }

    public static MutableComponent hoverComponent(MutableComponent base, MutableComponent hover) {
        return base.withStyle(Style.EMPTY.withHoverEvent(new HoverEvent(
                HoverEvent.Action.SHOW_TEXT,
                hover
        )));
    }

    public static MutableComponent gradientComponent(String text, int startHex, int endHex) {
        MutableComponent result = Component.empty();
        int length = text.length();

        int startR = (startHex >> 16) & 0xFF;
        int startG = (startHex >> 8) & 0xFF;
        int startB = startHex & 0xFF;

        int endR = (endHex >> 16) & 0xFF;
        int endG = (endHex >> 8) & 0xFF;
        int endB = endHex & 0xFF;

        for (int i = 0; i < length; i++) {
            float ratio = (length == 1) ? 0 : (float) i / (length - 1);

            int red = (int) (startR + ratio * (endR - startR));
            int green = (int) (startG + ratio * (endG - startG));
            int blue = (int) (startB + ratio * (endB - startB));

            int rgb = (red << 16) | (green << 8) | blue;
            TextColor color = TextColor.fromRgb(rgb);

            Component letter = Component.literal(String.valueOf(text.charAt(i)))
                    .withStyle(Style.EMPTY.withColor(color));
            result = result.append(letter);
        }

        return result;
    }
}
