package com.ecommerce.backend.product;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Column;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import com.ecommerce.backend.store.Store;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "products")
@Getter
@Setter
@NoArgsConstructor
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, length = 80)
    private String category;

    @Column(length = 80)
    private String subcategory;

    @Column(nullable = false, length = 1500)
    private String description;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private Integer stockQuantity;

    @Column(length = 80)
    private String sku;

    @Column(precision = 12, scale = 2)
    private BigDecimal compareAtPrice;

    @Column(nullable = false, length = 20)
    private String status = "ACTIVE";

    @Column(length = 160)
    private String seoTitle;

    @Column(length = 320)
    private String seoDescription;

    @ElementCollection
    @CollectionTable(name = "product_images", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "position")
    @Column(name = "image_url", nullable = false, length = 2000)
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "position")
    @Column(name = "size_value", nullable = false, length = 20)
    private List<String> sizes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_seasons", joinColumns = @JoinColumn(name = "product_id"))
    @OrderColumn(name = "position")
    @Column(name = "season_value", nullable = false, length = 20)
    private List<String> seasons = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_colors", joinColumns = @JoinColumn(name = "product_id"))
    private List<ProductColor> colors = new ArrayList<>();
}
