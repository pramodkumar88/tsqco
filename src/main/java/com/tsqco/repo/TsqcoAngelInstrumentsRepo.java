package com.tsqco.repo;

import com.tsqco.models.TsqcoAngelInstruments;
import com.tsqco.models.dto.TsqcoInstrumentByEquityDTO;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;
import org.springframework.data.repository.query.Param;

import java.math.BigInteger;
import java.util.List;

public interface TsqcoAngelInstrumentsRepo extends JpaRepository<TsqcoAngelInstruments, BigInteger> {

    List<TsqcoAngelInstruments> findDistinctBySymbolStartsWith(String symbol);

    List<TsqcoAngelInstruments> findDistinctBySymbolStartsWithAndExchseg(String symbol, String exchseg);

    TsqcoAngelInstruments findByToken(String token);

    @Procedure(procedureName = "tsqco.backup_and_clean_tsqco_angel_instruments")
    void callBackupAndCleanInstruments();

    @Query("SELECT new com.tsqco.models.dto.TsqcoInstrumentByEquityDTO(t.instrument_id, "
            + "t.name, t.token, t.symbol, t.exchseg, t.ltp) "
            + "FROM TsqcoAngelInstruments t "
            + "WHERE t.symbol LIKE CONCAT('%', :suffix)")
    List<TsqcoInstrumentByEquityDTO> findInstrumentsBySymbolSuffix(@Param("suffix") String suffix);

    @Transactional
    @Modifying
    @Query(value = "UPDATE tsqco.tsqco_angel_instruments SET eps = :eps WHERE name = :name AND symbol LIKE '%-EQ'", nativeQuery = true)
    void updateEpsByName(@Param("name") String name, @Param("eps") Float eps);


    @Modifying
    @Transactional
    @Query("UPDATE TsqcoAngelInstruments SET ltp = :ltp WHERE instrument_id = :instrumentId")
    void updateLTPById(@Param("ltp") Float ltp, @Param("instrumentId") Long instrumentId);


    @Transactional
    @Modifying
    @Query(value = "truncate table tsqco.tsqco_angel_instruments", nativeQuery = true)
    void truncateInstruments();

    @Transactional
    @Modifying
    @Query(value = "ALTER SEQUENCE tsqco.tsqco_angel_instruments_instrument_id_seq RESTART WITH 1", nativeQuery = true)
    void resetInstrumentSequence();


    @Transactional
    @Query(value = "SELECT tsqco.manage_instruments_table()", nativeQuery = true)
    void manageInstrumentsTable();

    /*@Query("SELECT t.token, t.ltp FROM TsqcoAngelInstruments t WHERE t.symbol LIKE %:pattern")
    List<Object[]> findTokenAndLtpBySymbolPattern(@Param("pattern") String pattern);*/

}
