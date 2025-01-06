package com.tsqco.repo;

import com.tsqco.models.AngelApplicationConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.keyvalue.repository.KeyValueRepository;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface TsqcoAngelAppConfigRepo extends JpaRepository<AngelApplicationConfig, Integer>{
    Optional<AngelApplicationConfig> findByConfigKey(String configKey);
}
