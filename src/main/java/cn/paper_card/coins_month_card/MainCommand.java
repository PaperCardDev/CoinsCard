package cn.paper_card.coins_month_card;

import cn.paper_card.mc_command.TheMcCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

class MainCommand extends TheMcCommand.HasSub {

    private final @NotNull Permission permission;
    private final @NotNull ThePlugin plugin;

    public MainCommand(@NotNull ThePlugin plugin) {
        super("coins-month-card");
        this.plugin = plugin;
        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission("coins-month-card.command"));

        final PluginCommand c = plugin.getCommand(this.getLabel());
        assert c != null;
        c.setExecutor(this);
        c.setTabCompleter(this);

        this.addSubCommand(new Add());
    }

    @Override
    protected boolean canNotExecute(@NotNull CommandSender commandSender) {
        return !commandSender.hasPermission(this.permission);
    }

    class Add extends TheMcCommand {

        protected Add() {
            super("add");
        }

        @Override
        protected boolean canNotExecute(@NotNull CommandSender commandSender) {
            return !(commandSender instanceof ConsoleCommandSender);
        }

        @Override
        public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
            final String argPlayer = strings.length > 0 ? strings[0] : null;
            final String argCoins = strings.length > 1 ? strings[1] : null;
            final String argDays = strings.length > 2 ? strings[2] : null;

            if (argPlayer == null) {
                plugin.sendError(commandSender, "必须指定参数：游戏名或UUID");
                return true;
            }

            if (argCoins == null) {
                plugin.sendError(commandSender, "必须指定参数：硬币数量");
                return true;
            }

            if (argDays == null) {
                plugin.sendError(commandSender, "必须指定参数：有效天数");
                return true;
            }

            if (strings.length != 3) {
                plugin.sendError(commandSender, "只需要3个参数，你提供了%d个参数！".formatted(strings.length));
                return true;
            }

            final UUID uuid = plugin.parseArgPlayer(argPlayer);
            if (uuid == null) {
                plugin.sendError(commandSender, "找不到该玩家：" + argPlayer);
                return true;
            }


            final long coins;

            try {
                coins = Long.parseLong(argCoins);
            } catch (NumberFormatException e) {
                plugin.sendError(commandSender, "%s 不是正确的硬币数量".formatted(argCoins));
                return true;
            }


            final long days;

            try {
                days = Long.parseLong(argDays);
            } catch (NumberFormatException e) {
                plugin.sendError(commandSender, "%s 不是正确的天数".formatted(argDays));
                return true;
            }

            plugin.getTaskScheduler().runTaskAsynchronously(() -> {

                final ServiceImpl service = plugin.getService();

                String name = plugin.getServer().getOfflinePlayer(uuid).getName();
                if (name == null) name = "null";

                final SimpleDateFormat format = new SimpleDateFormat("yyyy年MM月dd日_HH:mm:ss");

                final long begin = System.currentTimeMillis();
                final long ONE_DAY = 24 * 60 * 60 * 1000L;

                final long vTime = days * ONE_DAY;

                final CardInfo info = new CardInfo(0,
                        uuid,
                        begin,
                        vTime,
                        coins,
                        "游戏名：%s，开始：%s，到期：%s".formatted(
                                name, format.format(begin), format.format(begin + vTime)
                        ));

                final int id;

                try {
                    id = service.addCard(info);
                } catch (SQLException e) {
                    plugin.getSLF4JLogger().error("", e);
                    plugin.sendException(commandSender, e);
                    return;
                }

                final TextComponent.Builder text = Component.text();
                plugin.appendPrefix(text);
                text.appendSpace();
                text.append(Component.text("添加月卡成功，ID："));
                text.append(Component.text(id).color(NamedTextColor.RED));
                text.append(Component.text("，玩家："));
                text.append(Component.text(name).color(NamedTextColor.AQUA));
                text.append(Component.text("，天数："));
                text.append(Component.text(days).color(NamedTextColor.GOLD));
                text.append(Component.text("，硬币："));
                text.append(Component.text(coins).color(NamedTextColor.GOLD));

                commandSender.sendMessage(text.build().color(NamedTextColor.GREEN));
            });

            return true;
        }

        @Override
        public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

            if (strings.length == 1) {
                final String argPlayer = strings[0];

                final LinkedList<String> list = new LinkedList<>();
                list.add("<游戏名或UUID>");
                for (final OfflinePlayer offlinePlayer : plugin.getServer().getOfflinePlayers()) {
                    final String name = offlinePlayer.getName();
                    if (name == null) continue;

                    if (name.startsWith(argPlayer)) list.add(name);
                }

                return list;
            }

            if (strings.length == 2) {
                if (strings[1].isEmpty())
                    return Collections.singletonList("<硬币数量>");
                return null;
            }

            if (strings.length == 3) {
                if (strings[2].isEmpty())
                    return Collections.singletonList("<天数>");
                return null;
            }

            return null;
        }
    }
}
