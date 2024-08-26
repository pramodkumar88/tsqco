package com.tsqco.repo;

import com.tsqco.models.AngelApplicationConfig;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TsqcoAngelAppConfigRepo extends JpaRepository<AngelApplicationConfig, Integer> {
}
