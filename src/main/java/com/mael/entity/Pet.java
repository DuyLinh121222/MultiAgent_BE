package com.mael.entity;

import com.mael.entity.enums.PetRarity;
import jakarta.persistence.*;
import lombok.*;

/**
 * Thú đồng hành. FE vẽ hình theo code (components/game/Pet.jsx).
 */
@Entity
@Table(name = "pet")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pet {

    @Id
    @Column(name = "pet_code", length = 50)
    private String code;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private PetRarity rarity;

    @Column(columnDefinition = "TEXT")
    private String description;

    /** Giá đổi bằng xu, null = chỉ nhận từ rương báu ải trùm */
    @Column(name = "price_coins")
    private Integer priceCoins;

    /** Được chọn miễn phí ở màn chào mừng */
    @Column(name = "is_starter", nullable = false)
    @Builder.Default
    private Boolean starter = false;

    @Column(name = "sort_order", nullable = false)
    @Builder.Default
    private Integer sortOrder = 0;
}
