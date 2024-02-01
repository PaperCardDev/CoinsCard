package cn.paper_card.coins_month_card;

import cn.paper_card.database.api.DatabaseApi;
import cn.paper_card.player_coins.api.PlayerCoinsApi;
import com.github.Anon8281.universalScheduler.UniversalScheduler;
import com.github.Anon8281.universalScheduler.scheduling.schedulers.TaskScheduler;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.OfflinePlayer;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;

public final class ThePlugin extends JavaPlugin implements Listener {

    private final @NotNull TaskScheduler taskScheduler;

    private ServiceImpl service = null;

    private PlayerCoinsApi playerCoinsApi = null;

    public ThePlugin() {
        this.taskScheduler = UniversalScheduler.getScheduler(this);
    }

    @Override
    public void onEnable() {
        final DatabaseApi api = this.getServer().getServicesManager().load(DatabaseApi.class);
        if (api == null) throw new RuntimeException("无法连接到DatabaseApi");

        this.playerCoinsApi = this.getServer().getServicesManager().load(PlayerCoinsApi.class);
        if (this.playerCoinsApi == null) throw new RuntimeException("无法连接到PlayerCoinsApi");

        this.service = new ServiceImpl(api.getRemoteMySQL().getConnectionImportant());

        this.getServer().getPluginManager().registerEvents(this, this);

        new MainCommand(this);
        new YueKaCommand(this);
    }

    @Override
    public void onDisable() {
        this.taskScheduler.cancelTasks(this);

        if (this.service != null) {
            try {
                this.service.close1();
            } catch (SQLException e) {
                this.getSLF4JLogger().error("", e);
            }

            try {
                this.service.close2();
            } catch (SQLException e) {
                this.getSLF4JLogger().error("", e);
            }
            this.service = null;
        }
    }

    long getTodayBeginTime(long cur) {
        final long delta = (cur + TimeZone.getDefault().getRawOffset()) % (24 * 60 * 60 * 1000L);
        return cur - delta;
    }

    @EventHandler
    public void on(@NotNull PlayerJoinEvent event) {
        final ServiceImpl serv = this.service;
        assert serv != null;

        final PlayerCoinsApi api = this.playerCoinsApi;
        assert api != null;

        this.taskScheduler.runTaskAsynchronously(() -> {
            final Player player = event.getPlayer();

            final long cur = System.currentTimeMillis();

            // 查询有效月卡
            final List<CardInfo> cards;

            try {
                cards = serv.queryValid(player.getUniqueId(), cur);
            } catch (SQLException e) {
                this.getSLF4JLogger().error("", e);
                this.sendException(player, e);
                return;
            }

            final int size = cards.size();

            if (size == 0) {
                this.getSLF4JLogger().info("玩家%s没有任何一张有效月卡".formatted(player.getName()));
                return;
            }
            this.getSLF4JLogger().info("玩家%s有%d张有效月卡".formatted(player.getName(), size));

            long totalCoins = 0;
            long shouldCoins = 0;
            long cardCount = 0;

            for (final CardInfo card : cards) {

                // 查询今天是否赠送了
                final GivenInfo info;

                try {
                    info = serv.queryTimeAfter(player.getUniqueId(), card.id(), this.getTodayBeginTime(cur));
                } catch (SQLException e) {
                    this.getSLF4JLogger().error("", e);
                    this.sendException(player, e);
                    continue;
                }

                // 已经赠送
                if (info != null) continue;

                ++cardCount;

                shouldCoins += card.coins();

                // 记录，标识今天已经赠送
                try {
                    serv.addGiven(new GivenInfo(card.id(), player.getUniqueId(), cur));
                } catch (SQLException e) {
                    this.getSLF4JLogger().error("", e);
                    this.sendException(player, e);
                    continue;
                }

                // 赠送硬币
                try {
                    api.addCoins(player.getUniqueId(), card.coins(), "%d硬币月卡，ID：%d".formatted(
                            card.coins(), card.id()
                    ));
                    totalCoins += card.coins();
                } catch (Exception e) {
                    this.getSLF4JLogger().error("", e);
                    this.sendException(player, e);
                }
            }

            // 查询剩余硬币
            final long leftCoins;

            try {
                leftCoins = api.queryCoins(player.getUniqueId());
            } catch (Exception e) {
                this.getSLF4JLogger().error("", e);
                this.sendException(player, new Exception("已经给你赠送%d枚硬币，但是无法查询剩余硬币".formatted(totalCoins)));
                return;
            }

            if (cardCount == 0) return;

            // 发送消息
            final TextComponent.Builder text = Component.text();
            this.appendPrefix(text);
            text.appendSpace();
            text.append(Component.text("你一共有"));
            text.append(this.coinsNumber(size));
            text.append(Component.text("张有效月卡。已赠送给你"));
            text.append(this.coinsNumber(totalCoins));
            text.append(Component.text("枚硬币，你还有"));
            text.append(this.coinsNumber(leftCoins));
            text.append(Component.text("枚硬币~"));

            if (shouldCoins != totalCoins) {
                text.appendNewline();
                text.append(Component.text("应该赠送给你").color(NamedTextColor.YELLOW));
                text.append(this.coinsNumber(shouldCoins));
                text.append(Component.text("枚硬币！").color(NamedTextColor.YELLOW));
            }

            player.sendMessage(text.build().color(NamedTextColor.GREEN));
            // 音效
            player.playSound(player.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, SoundCategory.PLAYERS, 1.0F, 1.0F);
        });
    }

    @NotNull TaskScheduler getTaskScheduler() {
        return this.taskScheduler;
    }

    @NotNull ServiceImpl getService() {
        return this.service;
    }

    void appendPrefix(@NotNull TextComponent.Builder text) {
        text.append(Component.text("[").color(NamedTextColor.GRAY));
        text.append(Component.text("硬币月卡").color(NamedTextColor.DARK_AQUA));
        text.append(Component.text("]").color(NamedTextColor.GRAY));
    }

    @NotNull TextComponent coinsNumber(long c) {
        return Component.text(c).color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD);
    }


    void sendError(@NotNull CommandSender sender, @NotNull String error) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);
        text.appendSpace();
        text.append(Component.text(error).color(NamedTextColor.RED));
        sender.sendMessage(text.build());
    }

    void sendException(@NotNull CommandSender sender, @NotNull Throwable e) {
        final TextComponent.Builder text = Component.text();
        this.appendPrefix(text);

        text.append(Component.text(" ==== 异常信息 ====").color(NamedTextColor.DARK_RED));

        for (Throwable t = e; t != null; t = t.getCause()) {
            text.appendNewline();
            text.append(Component.text(t.toString()).color(NamedTextColor.RED));
        }

        sender.sendMessage(text.build());
    }

    @Nullable UUID parseArgPlayer(@NotNull String argPlayer) {

        try {
            return UUID.fromString(argPlayer);
        } catch (IllegalArgumentException ignored) {
        }

        for (final OfflinePlayer player : this.getServer().getOfflinePlayers()) {
            if (argPlayer.equals(player.getName())) return player.getUniqueId();
        }
        return null;
    }
}
