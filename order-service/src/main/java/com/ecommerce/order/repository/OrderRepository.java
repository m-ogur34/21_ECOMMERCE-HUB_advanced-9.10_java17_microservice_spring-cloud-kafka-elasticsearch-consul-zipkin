package com.ecommerce.order.repository;

import com.ecommerce.order.model.Order;
import com.ecommerce.order.model.OrderStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Sipariş veritabanı erişim katmanı */
@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {

    Optional<Order> findByOrderNumber(String orderNumber);

    /** Kullanıcının tüm siparişleri — sayfalı */
    Page<Order> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /** Belirli durumdaki siparişler */
    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    /** Kullanıcının belirli durumdaki siparişleri */
    Page<Order> findByUserIdAndStatus(Long userId, OrderStatus status, Pageable pageable);

    /** Sipariş sayısı — istatistik için */
    long countByStatus(OrderStatus status);

    /**
     * Sipariş kalemleriyle birlikte getir — N+1 problemini önler.
     * ORDER için items listesi LAZY yüklendiğinden JOIN FETCH zorunlu.
     */
    @Query("SELECT o FROM Order o JOIN FETCH o.items WHERE o.id = :id")
    Optional<Order> findByIdWithItems(@Param("id") Long id);
}
