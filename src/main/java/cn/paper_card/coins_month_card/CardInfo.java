package cn.paper_card.coins_month_card;

import java.util.UUID;

record CardInfo(
        int id,
        UUID playerId,
        long createTime,
        long validTime,
        long coins,
        String remark
) {
}

