package com.tsqco.repo;

import com.tsqco.models.AngelStockSubscription;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Repository
public interface TsqcoAngelStockSubscriptionRepo extends JpaRepository<AngelStockSubscription, Long> {
    List<AngelStockSubscription> findBySubscribeTrue();

    void deleteByToken(String token);

    boolean existsByToken(String token);

    @Modifying
    @Transactional
    @Query(value = "TRUNCATE TABLE tsqco.tsqco_angel_stock_subscription RESTART IDENTITY", nativeQuery = true)
    void deleteAll();
}
