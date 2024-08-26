package com.tsqco.repo;

import com.tsqco.models.AngelGainersLosers;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.sql.Date;
import java.util.List;

public interface TsqcoAngelGainersAndLoserRepo extends JpaRepository<AngelGainersLosers , String> {
    @Query(value = "SELECT * FROM tsqco.get_top_gainers_and_losers(?1, ?2);", nativeQuery = true)
    List<AngelGainersLosers> getTopGainersAndLosers(@Param("target_date") Date targetDate, @Param("top_n") int topN);

    @Query(value = "SELECT * FROM tsqco.get_top_gainers_and_losers_last_5_days(?1, ?2);", nativeQuery = true)
    List<AngelGainersLosers> getTopGainersAndLosersForLastFiveDays(@Param("target_date") Date targetDate, @Param("top_n") int topN);
}
