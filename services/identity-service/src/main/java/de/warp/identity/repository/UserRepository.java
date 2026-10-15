package de.warp.identity.repository;

import de.warp.identity.entity.UserEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<UserEntity, String> {

    boolean existsByEmailIgnoreCase(String email);
}
