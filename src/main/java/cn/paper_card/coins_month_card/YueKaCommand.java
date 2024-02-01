package cn.paper_card.coins_month_card;

import cn.paper_card.mc_command.TheMcCommand;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permission;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Objects;

class YueKaCommand extends TheMcCommand {

    private final @NotNull Permission permission;

    private final @NotNull ThePlugin plugin;

    protected YueKaCommand(@NotNull ThePlugin plugin) {
        super("yue-ka");
        this.permission = Objects.requireNonNull(plugin.getServer().getPluginManager().getPermission("coins-month-card.command.yue-ka"));
        this.plugin = plugin;
        final PluginCommand c = plugin.getCommand(this.getLabel());
        assert c != null;
        c.setTabCompleter(this);
        c.setExecutor(this);
    }

    @Override
    protected boolean canNotExecute(@NotNull CommandSender commandSender) {
        return !commandSender.hasPermission(this.permission);
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {

        if (!(commandSender instanceof final Player player)) {
            plugin.sendError(commandSender, "该命令只能由玩家来执行");
            return true;
        }

        plugin.getTaskScheduler().runTaskAsynchronously(() -> {
            // 查询有效月卡
            final ServiceImpl service = plugin.getService();

            final long cur = System.currentTimeMillis();
            final List<CardInfo> cards;

            try {
                cards = service.queryValid(player.getUniqueId(), cur);
            } catch (SQLException e) {
                plugin.getSLF4JLogger().error("", e);
                plugin.sendException(commandSender, e);
                return;
            }

            final int size = cards.size();

            final TextComponent.Builder text = Component.text();
            plugin.appendPrefix(text);
            text.appendSpace();

            if (size == 0) {
                text.append(Component.text("你没有任何一张有效月卡噢，可以联系腐竹赞助得到月卡~"));
                player.sendMessage(text.build().color(NamedTextColor.GREEN));
                return;
            }

            text.append(Component.text("你一共有%d张有效月卡：".formatted(size)));

            final SimpleDateFormat format = new SimpleDateFormat("MM月dd日_HH:mm");

            text.appendNewline();
            text.append(Component.text("ID | 每日硬币 | 已领取次数 | 开始时间 | 到期时间").color(NamedTextColor.GRAY));
            for (final CardInfo card : cards) {
                String givenCount;

                try {
                    final long c = service.queryGivenCount(card.id());
                    givenCount = "%d".formatted(c);
                } catch (SQLException e) {
                    plugin.getSLF4JLogger().error("", e);
                    givenCount = "ERROR";
                }

                text.appendNewline();

                // id
                text.append(Component.text(card.id()).color(NamedTextColor.GRAY));

                // 每日硬币
                text.append(Component.text(" | ").color(NamedTextColor.GRAY));
                text.append(plugin.coinsNumber(card.coins()));

                // 领取次数
                text.append(Component.text(" | ").color(NamedTextColor.GRAY));
                text.append(Component.text(givenCount).color(NamedTextColor.LIGHT_PURPLE).decorate(TextDecoration.BOLD));

                // 开始时间
                text.append(Component.text(" | ").color(NamedTextColor.GRAY));
                text.append(Component.text(format.format(card.createTime())).color(NamedTextColor.DARK_AQUA));

                // 到期时间
                text.append(Component.text(" | ").color(NamedTextColor.GRAY));
                text.append(Component.text(format.format(card.createTime() + card.validTime())).color(NamedTextColor.DARK_AQUA));
            }
            text.appendNewline();
            text.append(Component.text("记得每日登录服务器领取硬币噢~"));

            player.sendMessage(text.build().color(NamedTextColor.GREEN));
        });
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String s, @NotNull String[] strings) {
        return null;
    }
}
