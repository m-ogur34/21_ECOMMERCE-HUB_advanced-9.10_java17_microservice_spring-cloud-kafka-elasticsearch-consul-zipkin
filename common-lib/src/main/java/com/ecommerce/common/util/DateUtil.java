package com.ecommerce.common.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;

/**
 * Tarih/saat dönüşüm yardımcı sınıfı.
 *
 * Neden LocalDateTime?
 * - Java 8+ ile gelen modern tarih/saat API'si
 * - Thread-safe (immutable nesneler)
 * - java.util.Date ve java.util.Calendar aksine kullanımı kolay
 *
 * Neden Date de var?
 * - JWT kütüphanesi (jjwt) expiration için java.util.Date kullanır
 * - Bu dönüşüm metotları köprü görevi görür
 */
public final class DateUtil {

    /** Standart görüntüleme formatı — API yanıtlarında kullanılır */
    public static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");

    /** ISO 8601 formatı — JSON serializasyon için */
    public static final DateTimeFormatter ISO_FORMAT =
            DateTimeFormatter.ISO_LOCAL_DATE_TIME;

    private DateUtil() {
        throw new UnsupportedOperationException("Bu sınıf örneklenemez");
    }

    /**
     * java.util.Date → LocalDateTime dönüşümü.
     * JWT expiration tarihini LocalDateTime'a çevirmek için kullanılır.
     */
    public static LocalDateTime toLocalDateTime(Date date) {
        return date.toInstant()
                .atZone(ZoneId.systemDefault()) // Sistem zaman dilimine göre
                .toLocalDateTime();
    }

    /**
     * LocalDateTime → java.util.Date dönüşümü.
     * JWT token oluşturulurken expiration Date olarak verilmesi için.
     */
    public static Date toDate(LocalDateTime localDateTime) {
        return Date.from(localDateTime
                .atZone(ZoneId.systemDefault())
                .toInstant());
    }

    /**
     * Verilen süre (milisaniye) kadar sonraki zamanı döner.
     * JWT expiration hesaplamak için: now() + ACCESS_TOKEN_EXPIRATION
     */
    public static Date futureDate(long milliseconds) {
        return new Date(System.currentTimeMillis() + milliseconds);
    }

    /** Şu anki zamanı görüntüleme formatında string olarak döner */
    public static String nowFormatted() {
        return LocalDateTime.now().format(DISPLAY_FORMAT);
    }
}
