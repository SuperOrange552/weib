package com.weib.repository;

import com.weib.entity.UserSanction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface UserSanctionRepository extends JpaRepository<UserSanction, Long> {

    @Query("select s from UserSanction s where s.userId = :userId and s.sanctionType = :type " +
            "and s.status = :status and s.startsAt <= :now and s.endsAt is null")
    List<UserSanction> findActivePermanent(@Param("userId") Long userId,
                                           @Param("type") String type,
                                           @Param("status") String status,
                                           @Param("now") LocalDateTime now);

    @Query("select s from UserSanction s where s.userId = :userId and s.sanctionType = :type " +
            "and s.status = :status and s.startsAt <= :now and s.endsAt > :now")
    List<UserSanction> findActiveTemporary(@Param("userId") Long userId,
                                           @Param("type") String type,
                                           @Param("status") String status,
                                           @Param("now") LocalDateTime now);

    List<UserSanction> findByUserIdAndStatus(Long userId, String status);

    @Modifying
    @Query("update UserSanction s set s.status = 'EXPIRED', s.updatedAt = :now " +
            "where s.status = 'ACTIVE' and s.endsAt is not null and s.endsAt <= :now")
    int expireBefore(@Param("now") LocalDateTime now);
}
