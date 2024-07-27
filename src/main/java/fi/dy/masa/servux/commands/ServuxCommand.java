package fi.dy.masa.servux.commands;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import fi.dy.masa.servux.settings.IServuxSetting;
import me.lucko.fabric.api.permissions.v0.Permissions;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import fi.dy.masa.servux.util.StringUtils;

import java.util.List;
import java.util.Optional;

public class ServuxCommand
{
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher)
    {
        dispatcher.register(CommandManager
            .literal(Reference.MOD_ID).requires(Permissions.require(Reference.MOD_ID + ".commands", 4))
            .then(CommandManager.literal("reload").requires(Permissions.require(Reference.MOD_ID + ".commands.reload", 4))
                .executes((ctx) ->
                {
                    ServuxConfigProvider.INSTANCE.doReloadConfig(ctx.getSource());
                    return 1;
                }))
            .then(CommandManager.literal("save").requires(Permissions.require(Reference.MOD_ID + ".commands.save", 4))
                .executes((ctx) ->
                {
                    ServuxConfigProvider.INSTANCE.doSaveConfig(ctx.getSource());
                    return 1;
                }))
            .then(CommandManager.literal("set")
                .requires(Permissions.require(Reference.MOD_ID + ".commands.set", 4))
                .then(settingsNode().then(CommandManager.argument("value", StringArgumentType.greedyString())
                        .suggests((ctx, builder) -> {
                            Identifier settingId = ctx.getArgument("setting", Identifier.class);
                            String settingName = StringUtils.removeDefaultMinecraftNamespace(settingId);
                            var setting = DataProviderManager.INSTANCE.getSettingByName(settingName);
                            if (setting != null)
                            {
                                return CommandSource.suggestMatching(setting.examples(), builder);
                            }
                            return builder.buildFuture();
                        })
                        .executes(ServuxCommand::configModify))))
            .then(CommandManager.literal("info")
                .requires(Permissions.require(Reference.MOD_ID + ".commands.info", 4))
                .then(settingsNode().executes(ServuxCommand::configInfo)))
            .then(CommandManager.literal("list")
                .requires(Permissions.require(Reference.MOD_ID + ".commands.list", 4))
                .executes(ctx -> configList(ctx, DataProviderManager.INSTANCE.getAllProviders().stream()
                    .flatMap(iDataProvider -> iDataProvider.getSettings().stream()).toList()))
                .then(CommandManager.argument("provider", StringArgumentType.string())
                    .suggests((ctx, builder) -> CommandSource.suggestMatching(DataProviderManager.INSTANCE.getAllProviders(), builder, IDataProvider::getName, iDataProvider -> Text.literal(iDataProvider.getDescription()).append(StringUtils.translate("servux.suffix.data_provider"))))
                    .executes(ctx -> {
                        String provider = StringArgumentType.getString(ctx, "provider");
                        Optional<IDataProvider> dataProvider = DataProviderManager.INSTANCE.getProviderByName(provider);
                        if (dataProvider.isEmpty())
                        {
                            throw new SimpleCommandExceptionType(StringUtils.translate("servux.command.error.unknown_data_provider")).create();
                        }
                        ctx.getSource().sendFeedback(() -> StringUtils.translate("servux.command.config.list.data_provider", provider), false);
                        return configList(ctx, dataProvider.get().getSettings());
                    })))
            .then(CommandManager.literal("search")
                .requires(Permissions.require(Reference.MOD_ID + ".commands.list", 4))
                .then(CommandManager.argument("query", StringArgumentType.string())
                    .executes(ctx -> configList(ctx, configSearch(ctx, StringArgumentType.getString(ctx, "query"))))))
        );
    }

    private static List<IServuxSetting<?>> configSearch(CommandContext<ServerCommandSource> ctx, String query)
    {
        String[] searchParts = query.split(" ");
        return DataProviderManager.INSTANCE.getAllProviders().stream()
            .flatMap(iDataProvider -> iDataProvider.getSettings().stream())
            .filter(iServuxSetting ->
            {
                for (String part : searchParts)
                {
                    if (iServuxSetting.name().contains(part))
                    {
                        continue;
                    }
                    if (iServuxSetting.comment().getString().contains(part))
                    {
                        continue;
                    }
                    if (iServuxSetting.dataProvider().getName().contains(part))
                    {
                        continue;
                    }
                    return false;
                }
                return true;
            }).toList();
    }

    private static int configList(CommandContext<ServerCommandSource> ctx, List<IServuxSetting<?>> list)
    {
        if (list.isEmpty())
        {
            ctx.getSource().sendFeedback(() -> StringUtils.translate("servux.command.error.no_settings"), false);
            return 0;
        }

        for (IServuxSetting<?> setting : list)
        {
            ctx.getSource().sendFeedback(() ->
            {
                MutableText text = Text.empty();
                text.append(setting.shortDisplayName().copy().styled(style -> style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/servux info " + setting.qualifiedName()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, StringUtils.translate("servux.command.info.click_to_see_more")))));
                text.append(Text.literal(" (" + setting.dataProvider().getName() + ")").formatted(Formatting.GRAY));
                String value = setting.valueToString(setting.getValue());
                if (value.length() < 10)
                {
                    text.append(": ").append(value);
                }
                return text;
            }, false);
        }
        return list.size();
    }

    private static ArgumentBuilder<ServerCommandSource, ?> settingsNode() {
        var node = CommandManager.argument("setting", IdentifierArgumentType.identifier());
        node.suggests((ctx, builder) -> {
            if (builder.getRemainingLowerCase().contains(":"))
            {
                String providerName = builder.getRemaining().split(":")[0];
                DataProviderManager.INSTANCE.getProviderByName(providerName).ifPresent(iDataProvider ->
                    iDataProvider.getSettings().forEach(iServuxSetting ->
                    {
                        builder.suggest(providerName + ":" + iServuxSetting.name(), iServuxSetting.prettyName());
                    }));
            }
            else
            {
                CommandSource.suggestMatching(DataProviderManager.INSTANCE.getAllProviders().stream()
                    .flatMap(iDataProvider -> iDataProvider.getSettings().stream()).toList(), builder, IServuxSetting::name, IServuxSetting::prettyName);

                CommandSource.suggestMatching(DataProviderManager.INSTANCE.getAllProviders(), builder, IDataProvider::getName, iDataProvider -> Text.literal(iDataProvider.getDescription()).append(StringUtils.translate("servux.suffix.data_provider")));
            }
            return builder.buildFuture();
        });
        return node;
    }

    private static int configInfo(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException
    {
        Identifier settingId = ctx.getArgument("setting", Identifier.class);
        String settingName = StringUtils.removeDefaultMinecraftNamespace(settingId);
        var setting = DataProviderManager.INSTANCE.getSettingByName(settingName);
        if (setting == null)
        {
            throw new SimpleCommandExceptionType(StringUtils.translate("servux.command.error.unknown_setting")).create();
        }

        ctx.getSource().sendFeedback(Text::empty, false);
        ctx.getSource().sendFeedback(() ->
        {
            MutableText text = setting.prettyName().copy().formatted(Formatting.YELLOW);
            text.append(" (");
            text.append(Text.literal(setting.qualifiedName()).styled(style ->
                style.withColor(Formatting.GRAY)
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, StringUtils.translate("servux.command.info.click_to_copy")))
                    .withClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, setting.qualifiedName()))
            ));
            text.append(")");
            return text;
        }, false);
        ctx.getSource().sendFeedback(() -> setting.comment().copy().formatted(Formatting.GRAY), false);
        ctx.getSource().sendFeedback(() -> StringUtils.translate("servux.command.info.current_value", setting.valueToString(setting.getValue())).styled(style -> style
            .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, StringUtils.translate("servux.command.info.click_to_set", setting.prettyName())))
            .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/servux set " + setting.qualifiedName() + " "))
        ).append(StringUtils.translate("servux.command.info.default_value", setting.valueToString(setting.getDefaultValue())).formatted(Formatting.GRAY)), false);
        if (!setting.examples().isEmpty())
        {
            MutableText text = StringUtils.translate("servux.command.info.examples");
            setting.examples().forEach(example ->
            {
                MutableText optionText = Text.literal(example).styled(style -> {
                    if (example.equals(setting.valueToString(setting.getValue())))
                    {
                        style = style.withColor(Formatting.GREEN);
                    }
                    else
                    {
                        style = style.withColor(Formatting.GRAY);
                    }
                    return style
                        .withClickEvent(new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/servux set " + setting.qualifiedName() + " " + example))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, StringUtils.translate("servux.command.info.click_to_set", example)));
                });
                text.append(optionText).append(" ");
            });
            ctx.getSource().sendFeedback(() -> text, false);
        }

        return 1;
    }

    private static int configModify(CommandContext<ServerCommandSource> ctx) throws CommandSyntaxException
    {
        Identifier settingId = ctx.getArgument("setting", Identifier.class);
        String settingName = StringUtils.removeDefaultMinecraftNamespace(settingId);
        var setting = DataProviderManager.INSTANCE.getSettingByName(settingName);
        if (setting == null)
        {
            throw new SimpleCommandExceptionType(StringUtils.translate("servux.command.error.unknown_setting")).create();
        }
        String value = ctx.getArgument("value", String.class);
        if (!setting.validateString(value))
        {
            throw new SimpleCommandExceptionType(StringUtils.translate("servux.command.error.invalid_value")).create();
        }
        setting.setValueFromString(value);
        ctx.getSource().sendFeedback(() ->
            StringUtils.translate("servux.command.config.set_value",
                setting.shortDisplayName().copy().styled(style -> style
                    .withClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/servux info " + setting.qualifiedName()))
                    .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, StringUtils.translate("servux.command.info.click_to_see_more")))),
                value),
            true
        );
        return 1;
    }
}
