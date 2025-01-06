package com.tsqco.service.impl;

import com.tsqco.models.AngelApplicationConfig;
import com.tsqco.repo.TsqcoAngelAppConfigRepo;
import com.tsqco.service.TsqcoAngelAppConfigService;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@AllArgsConstructor
@RefreshScope
public class TsqcoAngelAppConfigServiceImpl implements TsqcoAngelAppConfigService, ApplicationListener<EnvironmentChangeEvent> {

    private final TsqcoAngelAppConfigRepo tsqcoAngelAppConfigRepo;

    private Map<String, String> cachedConfig = new HashMap<>();

    @PostConstruct
    public void loadConfig() {
        List<AngelApplicationConfig> configs = tsqcoAngelAppConfigRepo.findAll();
        cachedConfig = configs.stream()
                .collect(Collectors.toMap(AngelApplicationConfig::getConfigKey, AngelApplicationConfig::getConfigValue));
    }

    @Override
    public String getConfigValue(String key) {
        return cachedConfig.get(key);
    }


    @Override
    public void onApplicationEvent(EnvironmentChangeEvent event) {
        loadConfig();
    }
}
