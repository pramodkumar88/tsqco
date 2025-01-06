package com.tsqco.repo;

import com.tsqco.models.TsqcoKiteInstruments;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.stereotype.Repository;

import java.math.BigInteger;

@Repository
public interface TsqcoKiteInstrumentsRepo extends JpaRepository<TsqcoKiteInstruments, BigInteger> {
}
