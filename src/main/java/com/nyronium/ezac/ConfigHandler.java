package com.nyronium.ezac;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.config.ModConfig;

import java.util.List;

public class ConfigHandler {
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static ForgeConfigSpec.ConfigValue<List<? extends String>> list;
    public static ForgeConfigSpec.EnumValue<AntiCheatMode> mode;
    public static ForgeConfigSpec.EnumValue<AntiCheatAction> action;

    static {
        BUILDER.push("Server Configuration");

        list = BUILDER
                .comment(
                        " A list of all mods that should be added to either the blacklist or whitelist."
                )
                .defineList(
                        "list",
                        List.of(),
                        obj -> obj instanceof String
                );

        mode = BUILDER
                .comment(
                        " How should the mods in the list be treated?",
                        " BLACKLIST will flag all mods of the client that are in the list.",
                        " WHITELIST will flag all mods of the client that aren't in the list.",
                        " IGNORE won't do anything with the mods in the list."
                )
                .defineEnum(
                        "mode",
                        AntiCheatMode.IGNORE
                );

        action = BUILDER
                .comment(
                        " What should be done, when a player uses a flagged mod?",
                        " NOTIFY will log the flagged mods in the console (and send a message to the operators).",
                        " RESTRICT will do the same as NOTIFY, but will additionally deny the player everytime they join with a flagged mod.",
                        " BAN will also do the same as NOTIFY, but is a bit harsher than RESTRICT and will ban the player, so once they joined with a flagged mod, they will never be able to join again, until they are pardoned by an operator.",
                        " This will only work, when the mode selected is either BLACKLIST or WHITELIST."
                )
                .defineEnum(
                        "action",
                        AntiCheatAction.NOTIFY
                );

        BUILDER.pop();

        SPEC = BUILDER.build();
    }

    public static void register() {
        Main.CONTEXT.registerConfig(ModConfig.Type.COMMON, SPEC);
    }

    public enum AntiCheatMode {
        BLACKLIST,
        WHITELIST,
        IGNORE
    }

    public enum AntiCheatAction {
        NOTIFY,
        RESTRICT,
        BAN
    }
}
