package com.ecommerce.auth.repository;

import com.ecommerce.auth.model.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/** Rol veritabanı erişim katmanı */
@Repository
public interface RoleRepository extends JpaRepository<Role, Long> {

    /**
     * Rol adıyla arama — kullanıcıya rol atarken kullanılır.
     * Örnek: roleRepository.findByName(Role.RoleName.ROLE_USER)
     */
    Optional<Role> findByName(Role.RoleName name);
}
