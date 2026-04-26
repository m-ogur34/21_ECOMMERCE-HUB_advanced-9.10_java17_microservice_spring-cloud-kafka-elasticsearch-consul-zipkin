package com.ecommerce.common.constants;

/**
 * Kafka topic isimlerini merkezi olarak tutan sabit sınıf.
 *
 * Neden bu yaklaşım?
 * - Topic isimlerini String olarak dağınık biçimde yazmak "magic string" antipattern'dir.
 * - Bir isim değiştiğinde sadece burayı güncellemek yeterli olur.
 * - Producer ve consumer aynı sabiti kullandığı için yazım hatası riski sıfıra iner.
 *
 * 'final' sınıf: kalıtım yapılamaz, sadece sabit deposu olarak kullanılır.
 * private constructor: new KafkaTopics() diyerek nesne oluşturulamaz.
 */
public final class KafkaTopics {

    // Dışarıdan nesne oluşturmayı engelle — bu sınıf sadece sabit tutar
    private KafkaTopics() {
        throw new UnsupportedOperationException("Bu sınıf örneklenemez");
    }

    // ===== SİPARİŞ OLAYLARI =====

    /** Yeni sipariş oluşturulduğunda order-service bu topic'e yazar */
    public static final String ORDER_CREATED = "order.created";

    /** Sipariş onaylandığında (stok rezerve edildi) bu topic'e yazılır */
    public static final String ORDER_CONFIRMED = "order.confirmed";

    /** Sipariş iptal edildiğinde bu topic'e yazılır (saga rollback) */
    public static final String ORDER_CANCELLED = "order.cancelled";

    /** Kargo çıktığında bu topic'e yazılır */
    public static final String ORDER_SHIPPED = "order.shipped";

    /** Teslimat tamamlandığında bu topic'e yazılır */
    public static final String ORDER_DELIVERED = "order.delivered";

    // ===== STOK OLAYLARI =====

    /** Order-service stok rezerve etmek istediğinde bu topic'e yazar */
    public static final String STOCK_RESERVE_REQUEST = "stock.reserve.request";

    /** Product-service stok rezervasyonunu onayladığında bu topic'e yazar */
    public static final String STOCK_RESERVED = "stock.reserved";

    /** Product-service stok yetersizliğinde bu topic'e yazar */
    public static final String STOCK_INSUFFICIENT = "stock.insufficient";

    /** Saga rollback sırasında rezerv edilen stok serbest bırakılırken */
    public static final String STOCK_RELEASED = "stock.released";

    // ===== BİLDİRİM OLAYLARI =====

    /** Notification service'in dinlediği genel bildirim topic'i */
    public static final String NOTIFICATION_EVENTS = "notification.events";

    // ===== CONSUMER GROUP İSİMLERİ =====

    /** Her servis kendi consumer group'uyla okur — aynı mesajı birden fazla servis alabilir */
    public static final String ORDER_SERVICE_GROUP = "order-service-group";
    public static final String NOTIFICATION_SERVICE_GROUP = "notification-service-group";
    public static final String PRODUCT_SERVICE_GROUP = "product-service-group";
}
