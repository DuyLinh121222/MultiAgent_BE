package com.mael.dto.game;

import com.mael.entity.enums.PetRarity;

/**
 * @param howToGet mô tả cách nhận (đổi xu / phá đảo chương nào / thú khởi đầu)
 */
public record PetView(
        String code,
        String name,
        PetRarity rarity,
        String description,
        Integer priceCoins,
        boolean starter,
        boolean owned,
        boolean active,
        String howToGet
) {
}
