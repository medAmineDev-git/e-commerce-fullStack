package com.ecommerce.backend.product;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Embeddable
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProductColor {

    @Column(name = "color_name", nullable = false, length = 80)
    private String name;

    @Column(name = "color_hex", nullable = false, length = 7)
    private String hex;
}
