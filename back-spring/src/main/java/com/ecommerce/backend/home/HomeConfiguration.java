package com.ecommerce.backend.home;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "home_configurations")
@Getter
@Setter
@NoArgsConstructor
public class HomeConfiguration {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 40)
    private String configKey = "home";

    @Column(nullable = false, length = 160)
    private String title;

    @Column(nullable = false, length = 1200)
    private String text;

    @Column(nullable = false)
    private Long featuredProductId;
}
