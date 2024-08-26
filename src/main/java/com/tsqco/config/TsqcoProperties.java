package com.tsqco.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "tsqco")
@Getter
@Setter
public class TsqcoProperties {
    private String kiteApiKey;
    private String kiteSecretKey;
    private String kiteUserId;
    private String kiteRequestToken;
    private String kiteAccessToken;
    private String filterSymbol = "NSE,BSE";
}
