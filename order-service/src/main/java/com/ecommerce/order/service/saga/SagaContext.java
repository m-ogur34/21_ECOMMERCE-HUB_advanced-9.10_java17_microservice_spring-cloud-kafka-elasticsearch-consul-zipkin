package com.ecommerce.order.service.saga;

import com.ecommerce.common.dto.order.OrderRequest;
import com.ecommerce.order.model.Order;
import lombok.Builder;
import lombok.Data;

/**
 * Saga adımları arasında paylaşılan bağlam nesnesi.
 * Her adım bu nesneyi okur ve/veya günceller.
 */
@Data
@Builder
public class SagaContext {

    /** Saga'yı başlatan sipariş isteği */
    private OrderRequest orderRequest;

    /** İşleme alınan kullanıcı ID */
    private Long userId;

    /** İşleme alınan kullanıcı e-postası */
    private String userEmail;

    /** CreateOrderStep tarafından oluşturulan Order — diğer adımlar bu nesneyi kullanır */
    private Order createdOrder;

    /** Saga başarılı mı tamamlandı? */
    private boolean completed;

    /** Hata oluştuysa sebebi */
    private String failureReason;
}
