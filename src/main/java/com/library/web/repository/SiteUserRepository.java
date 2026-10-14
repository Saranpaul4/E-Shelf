package com.library.web.repository;

import java.util.List;
import java.util.Optional;

import com.library.web.model.SiteUser;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SiteUserRepository extends JpaRepository<SiteUser, Long> {

    List<SiteUser> findAllByOrderByUsernameAsc();

    List<SiteUser> findByRoleOrderByUsernameAsc(String role);

    Optional<SiteUser> findByUsername(String username);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);
}
