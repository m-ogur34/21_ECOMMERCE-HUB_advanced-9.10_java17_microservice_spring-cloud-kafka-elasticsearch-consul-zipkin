package com.ecommerce.order.model;

/**
 * Sipariş durum geçiş makinesi (State Machine).
 *
 * Geçerli durum geçişleri:
 * PENDING → CONFIRMED → SHIPPED → DELIVERED
 * PENDING → CANCELLED   (stok rezerve edilemezse)
 * CONFIRMED → CANCELLED (ödeme başarısızsa)
 *
 * Her durum hangi durumlara geçebileceğini bilir — encapsulation örneği.
 */
public enum OrderStatus {

    /** Sipariş oluşturuldu, stok rezervasyonu bekleniyor */
    PENDING {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return next == CONFIRMED || next == CANCELLED;
        }
    },

    /** Stok rezerve edildi, ödeme alındı */
    CONFIRMED {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return next == SHIPPED || next == CANCELLED;
        }
    },

    /** Kargoya verildi */
    SHIPPED {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return next == DELIVERED;
        }
    },

    /** Teslim edildi — terminal durum */
    DELIVERED {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return false; // Teslimattan sonra durum değişemez
        }
    },

    /** İptal edildi — terminal durum */
    CANCELLED {
        @Override
        public boolean canTransitionTo(OrderStatus next) {
            return false; // İptalden sonra durum değişemez
        }
    };

    /** Bu durumdan next durumuna geçiş geçerli mi? */
    public abstract boolean canTransitionTo(OrderStatus next);
}
