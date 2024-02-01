package cn.paper_card.coins_month_card;

import cn.paper_card.database.api.Parser;
import cn.paper_card.database.api.Util;
import org.jetbrains.annotations.NotNull;

import java.sql.*;
import java.util.List;
import java.util.UUID;

class CardTable extends Parser<CardInfo> {
    final static @NotNull String NAME = "coins_month_card";

    private final @NotNull Connection connection;

    private PreparedStatement psInsert = null;
    private PreparedStatement psQuery = null;

    CardTable(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.create();
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private void create() throws SQLException {
        Util.executeSQL(this.connection, """
                CREATE TABLE IF NOT EXISTS %s
                (
                    id     INT PRIMARY KEY AUTO_INCREMENT,
                    uid1   BIGINT       NOT NULL,
                    uid2   BIGINT       NOT NULL,
                    c_time BIGINT       NOT NULL,
                    v_time BIGINT       NOT NULL,
                    coins  BIGINT       NOT NULL,
                    remark VARCHAR(128) NOT NULL
                );""".formatted(NAME));
    }

    private @NotNull PreparedStatement getPsInsert() throws SQLException {
        if (this.psInsert == null) {
            this.psInsert = this.connection.prepareStatement("""
                    INSERT INTO %s (uid1, uid2, c_time, v_time, coins, remark)
                    VALUES (?, ?, ?, ?, ?, ?);""".formatted(NAME), Statement.RETURN_GENERATED_KEYS);
        }
        return this.psInsert;
    }

    private @NotNull PreparedStatement getPsQuery() throws SQLException {
        if (this.psQuery == null) {
            this.psQuery = this.connection.prepareStatement("""
                    SELECT
                           id,
                           uid1,
                           uid2,
                           c_time,
                           v_time,
                           coins,
                           remark
                    FROM %s
                    WHERE (uid1, uid2) = (?, ?)
                      AND c_time - ? + v_time > 0;""".formatted(NAME));
        }
        return this.psQuery;
    }

    int insert(@NotNull CardInfo info) throws SQLException {
        final PreparedStatement ps = this.getPsInsert();

        ps.setLong(1, info.playerId().getMostSignificantBits());
        ps.setLong(2, info.playerId().getLeastSignificantBits());
        ps.setLong(3, info.createTime());
        ps.setLong(4, info.validTime());
        ps.setLong(5, info.coins());
        ps.setString(6, info.remark());

        ps.executeUpdate();

        final ResultSet generatedKeys = ps.getGeneratedKeys();

        return Parser.parseOneInt(generatedKeys);
    }

    @NotNull List<CardInfo> query(@NotNull UUID uuid, long cur) throws SQLException {
        final PreparedStatement ps = this.getPsQuery();
        ps.setLong(1, uuid.getMostSignificantBits());
        ps.setLong(2, uuid.getLeastSignificantBits());
        ps.setLong(3, cur);
        final ResultSet resultSet = ps.executeQuery();

        return this.parseAll(resultSet);
    }

    @Override
    public @NotNull CardInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {

        final int id = resultSet.getInt(1);
        final long uid1 = resultSet.getLong(2);
        final long uid2 = resultSet.getLong(3);
        final long cTime = resultSet.getLong(4);
        final long vTime = resultSet.getLong(5);
        final long coins = resultSet.getLong(6);
        final String remark = resultSet.getString(7);

        return new CardInfo(id, new UUID(uid1, uid2), cTime, vTime, coins, remark);
    }
}
