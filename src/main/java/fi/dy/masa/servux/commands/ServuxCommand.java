package fi.dy.masa.servux.commands;

import java.util.*;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.ArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.dataproviders.DataProviderManager;
import fi.dy.masa.servux.dataproviders.IDataProvider;
import fi.dy.masa.servux.dataproviders.ServuxConfigProvider;
import fi.dy.masa.servux.interfaces.IServerCommand;
import fi.dy.masa.servux.settings.IServuxSetting;
import fi.dy.masa.servux.util.PermissionsUtil;
import fi.dy.masa.servux.util.StringUtils;

public class ServuxCommand implements IServerCommand
{
    public static final ServuxCommand INSTANCE = new ServuxCommand();

    @Override
    public void register(CommandDispatcher<CommandSourceStack> dispatcher,
                         CommandBuildContext registryAccess,
                         Commands.CommandSelection environment)
    {
        dispatcher.register(Commands
                                    .literal(Reference.MOD_ID).requires(PermissionsUtil.require(Reference.MOD_ID + ".commands", 4))
                                    .executes(this::sendAbout)
                                    .then(Commands.literal("reload").requires(PermissionsUtil.require(Reference.MOD_ID + ".commands.reload", 4))
                                                        .executes((ctx) ->
                                                                  {
                                                                      ServuxConfigProvider.INSTANCE.doReloadConfig(ctx.getSource());
                                                                      return 1;
                                                                  }))
                                    .then(Commands.literal("save").requires(PermissionsUtil.require(Reference.MOD_ID + ".commands.save", 4))
                                                        .executes((ctx) ->
                                                                  {
                                                                      ServuxConfigProvider.INSTANCE.doSaveConfig(ctx.getSource());
                                                                      return 1;
                                                                  }))
                                    .then(Commands.literal("set")
                                                        .requires(PermissionsUtil.require(Reference.MOD_ID + ".commands.set", 4))
                                                        .then(settingsNode().then(Commands.argument("value", StringArgumentType.greedyString())
                                                                                                .suggests((ctx, builder) ->
                                                                                                          {
                                                                                                              Identifier settingId = ctx.getArgument("setting", Identifier.class);
                                                                                                              String settingName = StringUtils.removeDefaultMinecraftNamespace(settingId);
                                                                                                              var setting = DataProviderManager.INSTANCE.getSettingByName(settingName);
                                                                                                              if (setting != null)
                                                                                                              {
                                                                                                                  return SharedSuggestionProvider.suggest(setting.examples(), builder);
                                                                                                              }
                                                                                                              return builder.buildFuture();
                                                                                                          })
                                                                                                .executes(ServuxCommand::configModify))))
                                    .then(Commands.literal("info")
                                                        .requires(PermissionsUtil.require(Reference.MOD_ID + ".commands.info", 4))
                                                        .then(settingsNode().executes(ServuxCommand::configInfo)))
                                    .then(Commands.literal("list")
                                                        .requires(PermissionsUtil.require(Reference.MOD_ID + ".commands.list", 4))
                                                        .executes(ctx -> configList(ctx, DataProviderManager.INSTANCE.getAllProviders().stream()
                                                                                                                     .flatMap(iDataProvider -> iDataProvider.getSettings().stream()).toList()))
                                                        .then(Commands.argument("provider", StringArgumentType.string())
                                                                            .suggests((ctx, builder) -> SharedSuggestionProvider.suggest(DataProviderManager.INSTANCE.getAllProviders(), builder, IDataProvider::getName, iDataProvider -> Component.literal(iDataProvider.getDescription()).append(StringUtils.translate("servux.suffix.data_provider"))))
                                                                            .executes(ctx ->
                                                                                      {
                                                                                          String provider = StringArgumentType.getString(ctx, "provider");
                                                                                          Optional<IDataProvider> dataProvider = DataProviderManager.INSTANCE.getProviderByName(provider);
                                                                                          if (dataProvider.isEmpty())
                                                                                          {
                                                                                              throw StringUtils.translateError("servux.command.error.unknown_data_provider");
                                                                                          }
                                                                                          ctx.getSource().sendSuccess(() -> StringUtils.translate("servux.command.config.list.data_provider", provider), false);
                                                                                          return configList(ctx, dataProvider.get().getSettings());
                                                                                      })))
                                    .then(Commands.literal("search")
                                                        .requires(PermissionsUtil.require(Reference.MOD_ID + ".commands.list", 4))
                                                        .then(Commands.argument("query", StringArgumentType.greedyString())
                                                                            .executes(ctx ->
                                                                                      {
                                                                                          String query = StringArgumentType.getString(ctx, "query");
                                                                                          var settings = configSearch(ctx, query);
                                                                                          if (settings.isEmpty())
                                                                                          {
                                                                                              ctx.getSource().sendSuccess(() -> StringUtils.translate("servux.command.search.none", query), false);
                                                                                              return 0;
                                                                                          }
                                                                                          else
                                                                                          {
                                                                                              ctx.getSource().sendSuccess(() -> StringUtils.translate("servux.command.search.results", settings.size(), query), false);
                                                                                              return configList(ctx, settings);
                                                                                          }
                                                                                      })))
        );
    }

    private List<IServuxSetting<?>> configSearch(CommandContext<CommandSourceStack> ctx, String query)
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

    private int sendAbout(CommandContext<CommandSourceStack> ctx)
    {
        ctx.getSource().sendSuccess(() -> StringUtils.translate("servux.command.about", Reference.MOD_STRING), false);
        return 1;
    }

    private int configList(CommandContext<CommandSourceStack> ctx, List<IServuxSetting<?>> list)
    {
        if (list.isEmpty())
        {
            ctx.getSource().sendSuccess(() -> StringUtils.translate("servux.command.error.no_settings"), false);
            return 0;
        }

        Set<String> appearedNames = new HashSet<>();
        Set<String> appearedMultiTimes = new HashSet<>();
        for (IServuxSetting<?> setting : list)
        {
            if (!appearedNames.add(setting.name()))
            {
                appearedMultiTimes.add(setting.name());
            }
        }

        for (IServuxSetting<?> setting : list)
        {
            ctx.getSource().sendSuccess(() ->
                                         {
                                             MutableComponent text = Component.empty();
                                             text.append(setting.shortDisplayName().copy().withStyle(style -> style
                                                     .withBold(true)
                                                     .withClickEvent(new ClickEvent.RunCommand("/servux info " + setting.qualifiedName()))));
                                             if (appearedMultiTimes.contains(setting.name()))
                                             {
                                                 text.append(Component.literal(" (").append(Component.nullToEmpty(setting.dataProvider().getName())).append(")").withStyle(ChatFormatting.GRAY));
                                             }
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

    private ArgumentBuilder<CommandSourceStack, ?> settingsNode()
    {
        var node = Commands.argument("setting", IdentifierArgument.id());
        node.suggests((ctx, builder) ->
                      {
                          if (builder.getRemainingLowerCase().contains(":"))
                          {
                              String providerName = builder.getRemaining().split(":")[0];
                              DataProviderManager.INSTANCE.getProviderByName(providerName).ifPresent(iDataProvider ->
                                                                                                             iDataProvider.getSettings().forEach(iServuxSetting ->
		                                                                                                                                                 builder.suggest(providerName + ":" + iServuxSetting.name(), iServuxSetting.prettyName()))
                              );
                          }
                          else
                          {
                              SharedSuggestionProvider.suggest(DataProviderManager.INSTANCE.getAllProviders().stream()
                                                                                        .flatMap(iDataProvider -> iDataProvider.getSettings().stream()).toList(), builder, IServuxSetting::name, IServuxSetting::prettyName);

                              SharedSuggestionProvider.suggest(DataProviderManager.INSTANCE.getAllProviders(), builder, IDataProvider::getName, iDataProvider -> Component.literal(iDataProvider.getDescription()).append(StringUtils.translate("servux.suffix.data_provider")));
                          }
                          return builder.buildFuture();
                      });
        return node;
    }

    private static int configInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Identifier settingId = ctx.getArgument("setting", Identifier.class);
        String settingName = StringUtils.removeDefaultMinecraftNamespace(settingId);
        var setting = DataProviderManager.INSTANCE.getSettingByName(settingName);
        if (setting == null)
        {
            throw StringUtils.translateError("servux.command.error.unknown_setting");
        }

        ctx.getSource().sendSuccess(Component::empty, false);
        ctx.getSource().sendSuccess(() ->
                                     {
                                         MutableComponent text = Component.empty();
                                         text.append(setting.prettyName().copy().withStyle(style ->
                                                                                                style.withColor(ChatFormatting.YELLOW).withBold(true)));
                                         text.append(" (");
                                         text.append(Component.literal(setting.qualifiedName()).withStyle(style ->
                                                                                                          style.withColor(ChatFormatting.GRAY)
                                                                                                               .withHoverEvent(new HoverEvent.ShowText(StringUtils.translate("servux.command.info.click_to_copy")))
                                                                                                               .withClickEvent(new ClickEvent.CopyToClipboard(setting.qualifiedName()))
                                         ));
                                         text.append(")");
                                         return text;
                                     }, false);
        ctx.getSource().sendSuccess(() -> setting.comment().copy().withStyle(ChatFormatting.GRAY), false);
        ctx.getSource().sendSuccess(() ->
                                     {
                                         MutableComponent text = StringUtils.translate("servux.command.info.value", setting.valueToString(setting.getValue())).withStyle(style -> style
                                                 .withHoverEvent(new HoverEvent.ShowText(StringUtils.translate("servux.command.info.click_to_set", setting.name())))
                                                 .withClickEvent(new ClickEvent.SuggestCommand("/servux set " + setting.qualifiedName() + " "))
                                         ).append(" ");
                                         if (Objects.equals(setting.getDefaultValue(), setting.getValue()))
                                         {
                                             text.append(StringUtils.translate("servux.command.suffix.default_value").withStyle(ChatFormatting.GRAY));
                                         }
                                         else
                                         {
                                             text.append(StringUtils.translate("servux.command.suffix.modified").withStyle(ChatFormatting.GREEN));
                                             text.append(" ");
                                             text.append(StringUtils.translate("servux.command.info.reset").withStyle(ChatFormatting.GRAY)
                                                                    .withStyle(style -> style
                                                                            .withClickEvent(new ClickEvent.SuggestCommand("/servux set " + setting.qualifiedName() + " " + setting.valueToString(setting.getDefaultValue())))
                                                                            .withHoverEvent(new HoverEvent.ShowText(StringUtils.translate("servux.command.info.click_to_reset_to", setting.valueToString(setting.getDefaultValue()))))
                                                                    ));
                                         }
                                         return text;
                                     }, false);
        if (!setting.examples().isEmpty())
        {
            MutableComponent text = StringUtils.translate("servux.command.info.examples");
            setting.examples().forEach(example ->
                                       {
                                           MutableComponent optionText = Component.literal(example).withStyle(style ->
                                                                                                 {
                                                                                                     if (example.equals(setting.valueToString(setting.getValue())))
                                                                                                     {
                                                                                                         style = style.withColor(ChatFormatting.GREEN);
                                                                                                     }
                                                                                                     else
                                                                                                     {
                                                                                                         style = style.withColor(ChatFormatting.GRAY);
                                                                                                     }
                                                                                                     return style
                                                                                                             .withClickEvent(new ClickEvent.SuggestCommand("/servux set " + setting.qualifiedName() + " " + example))
                                                                                                             .withHoverEvent(new HoverEvent.ShowText(StringUtils.translate("servux.command.info.click_to_set", example)));
                                                                                                 });
                                           text.append(optionText).append(" ");
                                       });
            ctx.getSource().sendSuccess(() -> text, false);
        }

        return 1;
    }

    private static int configModify(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException
    {
        Identifier settingId = ctx.getArgument("setting", Identifier.class);
        String settingName = StringUtils.removeDefaultMinecraftNamespace(settingId);
        var setting = DataProviderManager.INSTANCE.getSettingByName(settingName);
        if (setting == null)
        {
            throw StringUtils.translateError("servux.command.error.unknown_setting");
        }
        String value;
        try
        {
            value = ctx.getArgument("value", String.class);
        }
        catch (Exception e)
        {
            // No argument given
            value = setting.getDefaultValue().toString();
        }
        if (value == null || value.isEmpty())
        {
            // No argument given
            value = setting.getDefaultValue().toString();
        }
        if (!setting.validateString(value))
        {
            throw StringUtils.translateError("servux.command.error.invalid_value");
        }
        String finalValue = value;
        setting.setValueFromString(finalValue);
        ctx.getSource().sendSuccess(() ->
                                             StringUtils.translate("servux.command.config.set_value",
//                                                                   setting.shortDisplayName().copy().withStyle(style -> style
//                                                                           .withClickEvent(new ClickEvent.RunCommand("/servux info " + setting.qualifiedName()))),
                                                                   setting.name(),
                                                                   finalValue)
                                                        .copy().withStyle(style -> style
                                                                .withClickEvent(new ClickEvent.RunCommand("/servux info " + setting.qualifiedName()))),
                                     true
        );
        return 1;
    }
}
