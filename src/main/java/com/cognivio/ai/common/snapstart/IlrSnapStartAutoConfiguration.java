package com.cognivio.ai.common.snapstart;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.crac.Resource;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;

/**
 * Registers the {@link SnapStartDataSourceResource} so every JPA service is SnapStart-safe out of the
 * box (KAN-59). Active only when the CRaC API and HikariCP are on the classpath and the app defines a
 * {@link DataSource}; the resource is harmless off-Lambda, where the checkpoint never fires.
 */
@AutoConfiguration
@ConditionalOnClass({Resource.class, HikariDataSource.class})
public class IlrSnapStartAutoConfiguration {

    @Bean
    @ConditionalOnBean(DataSource.class)
    @ConditionalOnMissingBean
    public SnapStartDataSourceResource snapStartDataSourceResource(DataSource dataSource) {
        return new SnapStartDataSourceResource(dataSource);
    }
}
