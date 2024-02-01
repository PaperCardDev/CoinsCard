package cn.paper_card.coins_month_card;

import cn.paper_card.database.api.DatabaseApi;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;

class ServiceImpl implements Service {

    private GivenTable givenTable = null;
    private CardTable cardTable = null;

    private Connection con1 = null;
    private Connection con2 = null;

    private final @NotNull DatabaseApi.MySqlConnection mySqlConnection;

    ServiceImpl(DatabaseApi.@NotNull MySqlConnection mySqlConnection) {
        this.mySqlConnection = mySqlConnection;
    }

    private @NotNull CardTable getCardTable() throws SQLException {

        final Connection newCon = mySqlConnection.getRawConnection();

        if (this.con1 != null && this.con1 == newCon && this.cardTable != null) return this.cardTable;

        if (this.cardTable != null) this.cardTable.close();
        this.cardTable = new CardTable(newCon);
        this.con1 = newCon;

        return this.cardTable;
    }

    private @NotNull GivenTable getGivenTale() throws SQLException {

        final Connection newCon = this.mySqlConnection.getRawConnection();

        if (this.con2 != null && this.con2 == newCon && this.givenTable != null) return this.givenTable;

        if (this.givenTable != null) this.givenTable.close();

        this.givenTable = new GivenTable(newCon);
        this.con2 = newCon;

        return this.givenTable;
    }

    void close1() throws SQLException {
        synchronized (this.mySqlConnection) {
            final CardTable t = this.cardTable;

            this.con1 = null;
            this.cardTable = null;

            if (t != null) t.close();
        }
    }

    void close2() throws SQLException {
        synchronized (this.mySqlConnection) {
            final GivenTable t = this.givenTable;

            this.con2 = null;
            this.cardTable = null;

            if (t != null) t.close();
        }
    }

    @Override
    public @NotNull List<CardInfo> queryValid(@NotNull UUID uuid, long cur) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final CardTable t = this.getCardTable();
                final List<CardInfo> list = t.query(uuid, cur);
                this.mySqlConnection.setLastUseTime();
                return list;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public int addCard(@NotNull CardInfo info) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final CardTable t = this.getCardTable();
                final int id = t.insert(info);
                this.mySqlConnection.setLastUseTime();
                return id;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public @Nullable GivenInfo queryTimeAfter(@NotNull UUID uuid, int cardId, long time) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final GivenTable t = this.getGivenTale();
                final GivenInfo info = t.queryTimeAfter(cardId, uuid, time);
                this.mySqlConnection.setLastUseTime();
                return info;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public void addGiven(@NotNull GivenInfo info) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final GivenTable t = this.getGivenTale();
                final int inserted = t.insert(info);
                this.mySqlConnection.setLastUseTime();
                if (inserted != 1) throw new RuntimeException("插入了%d条数据！".formatted(inserted));
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }

    @Override
    public int queryGivenCount(int id) throws SQLException {
        synchronized (this.mySqlConnection) {
            try {
                final GivenTable t = this.getGivenTale();
                final int c = t.queryCount(id);
                this.mySqlConnection.setLastUseTime();
                return c;
            } catch (SQLException e) {
                try {
                    this.mySqlConnection.handleException(e);
                } catch (SQLException ignored) {
                }
                throw e;
            }
        }
    }
}
