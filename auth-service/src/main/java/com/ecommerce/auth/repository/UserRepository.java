package com.ecommerce.auth.repository;

import com.ecommerce.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Kullanıcı veritabanı erişim katmanı.
 *
 * Repository Pattern: veritabanı operasyonlarını soyutlar.
 * JpaRepository<User, Long>:
 *   - User: entity tipi
 *   - Long: primary key tipi
 * Bu interface sayesinde save(), findById(), findAll(), delete() vb.
 * metodlar otomatik gelir — tek satır SQL yazmadan!
 *
 * Spring Data JPA, metod isimlerinden SQL üretir:
 * findByEmail → SELECT * FROM users WHERE email = ?
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * E-posta ile kullanıcı arama — login işleminde kullanılır.
     * Optional<T>: kullanıcı bulunmazsa null yerine Optional.empty() döner.
     * Bu, NullPointerException riskini ortadan kaldırır.
     */
    Optional<User> findByEmail(String email);

    /**
     * E-posta adresinin kayıtlı olup olmadığını kontrol eder.
     * COUNT sorgusu yapar, tüm alanları çekmez — verimli.
     * Kayıt formunda "bu e-posta zaten kullanılıyor" kontrolü için.
     */
    boolean existsByEmail(String email);

    /**
     * E-posta ile kullanıcıyı rolleriyle birlikte getirir.
     * JOIN FETCH: N+1 sorgu problemini önler.
     * Normalde User fetch edilince roller için ayrı N sorgu gider,
     * JOIN FETCH ile tek sorguda hepsi gelir.
     *
     * @param email Aranacak e-posta
     * @return Rolleri yüklü kullanıcı
     */
    @Query("SELECT u FROM User u JOIN FETCH u.roles WHERE u.email = :email AND u.deletedAt IS NULL")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    /**
     * Silinmemiş (aktif) kullanıcıyı ID ile getirir.
     * deletedAt IS NULL: soft delete yapılmış kullanıcıları hariç tutar.
     */
    @Query("SELECT u FROM User u WHERE u.id = :id AND u.deletedAt IS NULL")
    Optional<User> findActiveById(@Param("id") Long id);
}
