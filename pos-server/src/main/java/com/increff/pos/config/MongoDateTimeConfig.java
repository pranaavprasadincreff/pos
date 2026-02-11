package com.increff.pos.config;

import com.increff.pos.util.DateToZonedDateTimeConverter;
import com.increff.pos.util.ZonedDateTimeToDateConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;

import java.util.List;

@Configuration
public class MongoDateTimeConfig {
    @Bean
    public MongoCustomConversions mongoCustomConversions() {
        return new MongoCustomConversions(
                List.of(
                        new DateToZonedDateTimeConverter(),
                        new ZonedDateTimeToDateConverter()
                )
        );
    }
}
