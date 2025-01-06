package com.tsqco.repo;

import com.tsqco.models.TsqcoAngelStockTransaction;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TsqcoAngelStockTransactionRepo extends JpaRepository<TsqcoAngelStockTransaction, Long> {
}
