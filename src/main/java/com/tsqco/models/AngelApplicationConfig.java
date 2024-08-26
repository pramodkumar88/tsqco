package com.tsqco.models;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.DateDeserializers;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.cglib.core.Local;

import java.time.LocalDateTime;
import java.time.ZonedDateTime;

@Entity
@Getter
@Setter
@Table(name = "tsqco_angel_application_config", schema = "tsqco")
@AllArgsConstructor
@NoArgsConstructor
public class AngelApplicationConfig {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer configid;
    @JsonFormat(pattern = "yyyy-mm-dd HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @JsonDeserialize(using= DateDeserializers.TimestampDeserializer.class)
    private String instrumentlastloaded;
}
