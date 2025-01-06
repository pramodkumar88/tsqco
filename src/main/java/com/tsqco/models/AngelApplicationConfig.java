package com.tsqco.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.data.redis.core.RedisHash;

@Entity
@Getter
@Setter
@Table(name = "tsqco_angel_application_config", schema = "tsqco")
@NoArgsConstructor
@AllArgsConstructor
public class AngelApplicationConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer configid;

    @Column(name = "config_key")
    @NotBlank
    private String configKey;

    @Column(name = "config_value")
    private String configValue;

    public AngelApplicationConfig(String configKey, String configValue) {
        this.configKey = configKey;
        this.configValue = configValue;
    }

}
