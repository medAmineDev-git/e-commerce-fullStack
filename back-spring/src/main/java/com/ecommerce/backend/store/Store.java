package com.ecommerce.backend.store;

import com.ecommerce.backend.auth.AdminUser;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "stores")
@Getter
@Setter
@NoArgsConstructor
public class Store {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String name;

    @Column(nullable = false, unique = true, length = 80)
    private String slug;

    @Column(length = 1500)
    private String description;

    @Column(name = "logo_url", length = 2000)
    private String logoUrl;

    @Column(name = "banner_url", length = 2000)
    private String bannerUrl;

    /** Visuel compose pour l'ecran etroit. Absent, la banniere de bureau sert. */
    @Column(name = "banner_mobile_url", length = 2000)
    private String bannerMobileUrl;

    @Column(length = 40)
    private String phone;

    @Column(length = 160)
    private String email;

    @Column(length = 500)
    private String address;

    @Column(unique = true, length = 255)
    private String domain;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt = Instant.now();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private AdminUser owner;

    @PrePersist
    public void prePersist() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
        if (updatedAt == null) {
            updatedAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        updatedAt = Instant.now();
    }
}
