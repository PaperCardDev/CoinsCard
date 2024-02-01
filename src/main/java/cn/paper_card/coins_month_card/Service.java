package cn.paper_card.coins_month_card;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.UUID;

interface Service {

    // 查询玩家有效的所有月卡
    @NotNull List<CardInfo> queryValid(@NotNull UUID uuid, long cur) throws Exception;

    // 添加月卡记录
    int addCard(@NotNull CardInfo info) throws Exception;

    // 查询赠送记录
    @Nullable GivenInfo queryTimeAfter(@NotNull UUID uuid, int cardId, long time) throws Exception;

    // 添加赠送记录
    void addGiven(@NotNull GivenInfo info) throws Exception;

    int queryGivenCount(int id) throws Exception;
}