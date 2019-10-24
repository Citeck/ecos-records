package ru.citeck.ecos.records2.spring;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Auto configuration to initialize records beans.
 *
 * @author Roman Makarskiy
 */
@Configuration
@ComponentScan(basePackages = {"ru.citeck.ecos.records2.spring"})
public class RecordsAutoConfiguration {
}
