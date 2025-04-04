package ru.itis.test;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import ru.itis.orm.management.api.EntityManagerFactory;
import ru.itis.orm.management.impl.SimpleEntityManagerFactory;

import javax.sql.DataSource;

@Configuration
public class Config {

    @Bean
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost:5432/kamalov-oris-2025");
        config.setUsername("postgres");
        config.setPassword("qwerty007");
        config.setDriverClassName("org.postgresql.Driver");
        config.setMaximumPoolSize(10);
        return new HikariDataSource(config);
    }


    @Bean
    public EntityManagerFactory entityManagerFactory() {
        return new SimpleEntityManagerFactory(dataSource(), "ru.itis.test.model");
    }
}
