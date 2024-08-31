package com.tsqco.repo;

import com.tsqco.models.TsqcoAngelInstruments;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.query.Procedure;

import java.math.BigInteger;
import java.util.List;

public interface TsqcoAngelInstrumentsRepo extends JpaRepository<TsqcoAngelInstruments, BigInteger> {

    List<TsqcoAngelInstruments> findDistinctBySymbolStartsWith(String symbol);

    List<TsqcoAngelInstruments> findDistinctBySymbolStartsWithAndExchseg(String symbol, String exchseg);

    @Procedure(procedureName = "tsqco.backup_and_clean_tsqco_angel_instruments")
    void callBackupAndCleanInstruments();

/*

    @Transactional
    @Modifying
    @Query(value = "truncate table tsqco.tsqco_angel_instruments", nativeQuery = true)
    void truncateInstruments();

    @Transactional
    @Modifying
    @Query(value = "ALTER SEQUENCE tsqco.tsqco_angel_instruments_seq RESTART WITH 1", nativeQuery = true)
    void resetInstrumentSequence();
*/

    @Transactional
    @Query(value = "SELECT tsqco.manage_instruments_table()", nativeQuery = true)
    void manageInstrumentsTable();
}
