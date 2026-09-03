package com.ecommerce.backend.order;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import com.ecommerce.backend.store.Store;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
public class CustomerOrder {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "store_id", nullable = false)
    private Store store;

    @Column(nullable = false, unique = true, length = 24)
    private String orderNumber;

    @Column(nullable = false, length = 120)
    private String customerName;

    @Column(nullable = false, length = 40)
    private String phone;

    @Column(nullable = false, length = 120)
    private String city;

    @Column(nullable = false, length = 500)
    private String address;

    @Column(length = 1000)
    private String note;

    @Column(nullable = false, length = 40)
    private String paymentMethod;

    @Column(length = 20)
    private String publisherRef;

    @Column(nullable = false, length = 30)
    private String status;

    @Column(nullable = false)
    private LocalDate estimatedDelivery;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal total;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderItem> items = new ArrayList<>();

    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }
}
