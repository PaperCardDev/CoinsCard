package cn.paper_card.coins_month_card;

import cn.paper_card.database.api.Parser;
import cn.paper_card.database.api.Util;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;

class GivenTable extends Parser<GivenInfo> {
    private final static @NotNull String NAME = "coins_month_card_given";

    private final Connection connection;

    private PreparedStatement psInsert = null;
    private PreparedStatement psQuery = null;

    private PreparedStatement psQueryCount = null;

    public GivenTable(@NotNull Connection connection) throws SQLException {
        this.connection = connection;
        this.create();
    }

    private void create() throws SQLException {
        Util.executeSQL(this.connection, """
                CREATE TABLE IF NOT EXISTS %s
                (
                    id   INT,
                    uid1 BIGINT NOT NULL,
                    uid2 BIGINT NOT NULL,
                    time BIGINT NOT NULL,
                    FOREIGN KEY (id) REFERENCES %s (id)
                );""".formatted(NAME, CardTable.NAME));
    }

    void close() throws SQLException {
        Util.closeAllStatements(this.getClass(), this);
    }

    private @NotNull PreparedStatement getPsInsert() throws SQLException {
        if (this.psInsert == null) {
            this.psInsert = this.connection.prepareStatement("""
                    INSERT INTO %s (id, uid1, uid2, time)
                    VALUES (?, ?, ?, ?);""".formatted(NAME));
        }
        return this.psInsert;
    }

    private @NotNull PreparedStatement getPsQuery() throws SQLException {
        if (this.psQuery == null) {
            this.psQuery = this.connection.prepareStatement("""
                    SELECT id, uid1, uid2, time
                    FROM %s
                    WHERE (id, uid1, uid2) = (?, ?, ?)
                      AND time > ?
                    LIMIT 1;""".formatted(NAME));
        }
        return this.psQuery;
    }

    private @NotNull PreparedStatement getPsQueryCount() throws SQLException {
        if (this.psQueryCount == null) {
            this.psQueryCount = this.connection.prepareStatement("""
                    SELECT count(*)
                    FROM %s
                    WHERE id = ?;""".formatted(NAME));
        }
        return this.psQueryCount;
    }

    int insert(@NotNull GivenInfo info) throws SQLException {
        final PreparedStatement ps = this.getPsInsert();
        ps.setInt(1, info.cardId());
        ps.setLong(2, info.playerId().getMostSignificantBits());
        ps.setLong(3, info.playerId().getLeastSignificantBits());
        ps.setLong(4, info.time());
        return ps.executeUpdate();
    }

    @Nullable GivenInfo queryTimeAfter(int cardId, @NotNull UUID uuid, long time) throws SQLException {
        final PreparedStatement ps = this.getPsQuery();
        ps.setInt(1, cardId);
        ps.setLong(2, uuid.getMostSignificantBits());
        ps.setLong(3, uuid.getLeastSignificantBits());
        ps.setLong(4, time);
        final ResultSet resultSet = ps.executeQuery();
        return this.parseOne(resultSet);
    }

    int queryCount(int id) throws SQLException {
        final PreparedStatement ps = this.getPsQueryCount();
        ps.setInt(1, id);
        final ResultSet resultSet = ps.executeQuery();
        return Parser.parseOneInt(resultSet);
    }

    @Override
    public @NotNull GivenInfo parseRow(@NotNull ResultSet resultSet) throws SQLException {
        final int id = resultSet.getInt(1);
        final long uid1 = resultSet.getLong(2);
        final long uid2 = resultSet.getLong(3);
        final long time = resultSet.getLong(4);
        return new GivenInfo(id, new UUID(uid1, uid2), time);
    }
}
