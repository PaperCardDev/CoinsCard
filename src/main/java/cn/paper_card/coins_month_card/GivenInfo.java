package cn.paper_card.coins_month_card;

import java.util.UUID;

record GivenInfo(
        int cardId, // 月卡ID
        UUID playerId, // 玩家ID
        long time // 赠送时间
) {
}


