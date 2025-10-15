package com.rdpk.config;

import com.rdpk.features.voting.domain.VoteChoice;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.data.r2dbc.convert.R2dbcCustomConversions;
import org.springframework.data.r2dbc.dialect.DialectResolver;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

import java.util.ArrayList;
import java.util.List;

@Configuration
public class R2dbcConfiguration {

    @Bean
    public R2dbcCustomConversions r2dbcCustomConversions(ConnectionFactory connectionFactory) {
        R2dbcDialect dialect = DialectResolver.getDialect(connectionFactory);
        List<Object> converters = new ArrayList<>(dialect.getConverters());
        converters.add(new VoteChoiceWritingConverter());
        converters.add(new VoteChoiceReadingConverter());
        return new R2dbcCustomConversions(
                org.springframework.data.convert.CustomConversions.StoreConversions.of(dialect.getSimpleTypeHolder()),
                converters
        );
    }

    @WritingConverter
    static class VoteChoiceWritingConverter implements Converter<VoteChoice, String> {
        @Override
        public String convert(VoteChoice source) {
            return source.name();
        }
    }

    @ReadingConverter
    static class VoteChoiceReadingConverter implements Converter<String, VoteChoice> {
        @Override
        public VoteChoice convert(String source) {
            return VoteChoice.valueOf(source);
        }
    }
}

