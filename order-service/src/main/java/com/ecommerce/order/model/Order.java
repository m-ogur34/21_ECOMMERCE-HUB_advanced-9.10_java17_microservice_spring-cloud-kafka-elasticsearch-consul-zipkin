package com.ecommerce.order.model;

import com.ecommerce.common.exception.BusinessException;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Sipariş entity sınıfı.
 *
 * Aggregate Root: DDD (Domain Driven Design) kavramı.
 * Order, OrderItem'ların sahibidir — tüm işlemler Order üzerinden yapılır.
 * OrderItem doğrudan güncellenmez, Order.addItem() kullanılır.
 */
@Entity
@Table(
    name = "orders",
    indexes = {
        @Index(name = "idx_orders_user_id", columnList = "user_id"),
        @Index(name = "idx_orders_order_number", columnList = "order_number"),
        @Index(name = "idx_orders_status", columnList = "status")
    }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    /** Kullanıcı ID — auth-service'den alınır, foreign key yok (farklı DB) */
    @Column(name = "user_id", nullable = false)
    private Long userId;

    /** Kullanıcı e-postası — bildirim için */
    @Column(name = "user_email", nullable = false, length = 100)
    private String userEmail;

    /** Kullanıcıya gösterilen sipariş numarası */
    @Column(name = "order_number", nullable = false, unique = true, length = 30)
    private String orderNumber;

    /**
     * Sipariş durumu — Enum olarak saklanır.
     * EnumType.STRING: "PENDING", "CONFIRMED" gibi yazıyla saklanır.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Column(name = "total_amount", nullable = false, precision = 14, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "shipping_address", nullable = false, length = 500)
    private String shippingAddress;

    @Column(name = "notes", length = 1000)
    private String notes;

    /**
     * Sipariş kalemleri.
     * CascadeType.ALL: Order kaydedilince/silinince OrderItem'lar da cascade olur.
     * orphanRemoval: kalem listeden çıkarılınca DB'den de silinir.
     */
    @OneToMany(mappedBy = "order",
               cascade = CascadeType.ALL,
               orphanRemoval = true,
               fetch = FetchType.LAZY)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    // ===== DOMAIN METODLARI =====

    /**
     * Durum geçişi yapar — geçersiz geçiş kabul edilmez.
     * State machine mantığı OrderStatus enum'ında tanımlı.
     */
    public void transitionTo(OrderStatus newStatus) {
        if (!this.status.canTransitionTo(newStatus)) {
            throw new BusinessException(
                String.format("Geçersiz durum geçişi: %s → %s", this.status, newStatus),
                "INVALID_STATUS_TRANSITION"
            );
        }
        this.status = newStatus;
    }

    /** Kalem ekle — iki taraflı ilişkiyi set et */
    public void addItem(OrderItem item) {
        items.add(item);
        item.setOrder(this);
    }

    /** Toplam tutarı hesapla — tüm kalemlerin toplamı */
    public void calculateTotal() {
        this.totalAmount = items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Sipariş iptal edilebilir mi? */
    public boolean isCancellable() {
        return status == OrderStatus.PENDING || status == OrderStatus.CONFIRMED;
    }
}
