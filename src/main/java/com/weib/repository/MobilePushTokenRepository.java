package com.weib.repository;
import com.weib.entity.MobilePushToken;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List; import java.util.Optional;
public interface MobilePushTokenRepository extends JpaRepository<MobilePushToken,Long>{
    Optional<MobilePushToken> findByInstallationId(String installationId);
    List<MobilePushToken> findByUserIdAndActiveRoleAndStatus(Long userId,String activeRole,String status);
}
