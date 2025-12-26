package com.salonplatform.repository;

import com.salonplatform.entity.*;
import com.salonplatform.enums.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.*;

// ============== UserRepository ==============
@Repository
public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByPhone(String phone);
    Optional<User> findByEmail(String email);
    boolean existsByPhone(String phone);
    boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
}