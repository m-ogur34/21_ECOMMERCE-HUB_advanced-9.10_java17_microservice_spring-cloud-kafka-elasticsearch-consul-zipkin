package com.ecommerce.common.util;

import java.util.regex.Pattern;

/**
 * Yaygın validation işlemleri için yardımcı sınıf.
 * @Valid annotasyonunun kapsamadığı özel iş kuralları için kullanılır.
 */
public final class ValidationUtil {

    /** Türk telefon numarası formatı: 05XX XXX XXXX veya +905XX XXX XXXX */
    private static final Pattern PHONE_PATTERN =
            Pattern.compile("^(\\+90|0)?[5][0-9]{9}$");

    /** Güçlü şifre: en az 1 büyük harf, 1 küçük harf, 1 rakam, 1 özel karakter */
    private static final Pattern STRONG_PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$");

    private ValidationUtil() {
        throw new UnsupportedOperationException("Bu sınıf örneklenemez");
    }

    /** Telefon numarası formatını doğrular */
    public static boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /** Şifrenin güçlü olup olmadığını kontrol eder */
    public static boolean isStrongPassword(String password) {
        return password != null && STRONG_PASSWORD_PATTERN.matcher(password).matches();
    }

    /** String'in null veya boş olmadığını kontrol eder */
    public static boolean isNotEmpty(String value) {
        return value != null && !value.trim().isEmpty();
    }

    /**
     * Sipariş numarası üretir: ORD-YYYYMMDD-XXXXXX
     * @param orderId Sipariş veritabanı ID'si
     */
    public static String generateOrderNumber(Long orderId) {
        return String.format("ORD-%tY%<tm%<td-%06d",
                new java.util.Date(), orderId);
    }
}
