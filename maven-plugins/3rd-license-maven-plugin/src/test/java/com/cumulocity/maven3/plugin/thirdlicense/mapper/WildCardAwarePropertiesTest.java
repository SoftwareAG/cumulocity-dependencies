package com.cumulocity.maven3.plugin.thirdlicense.mapper;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class WildCardAwarePropertiesTest {

    @Test
    public void shouldGetProperty() throws Exception {
        WildCardAwareProperties properties = new WildCardAwareProperties();
        properties.setProperty("key", "value");

        assertThat(properties.getProperty("key")).isEqualTo("value");
        assertThat(properties.getProperty("no_key")).isNull();
    }

    @Test
    public void shouldGetPropertyWithWildCardedKey() throws Exception {
        WildCardAwareProperties properties = new WildCardAwareProperties();
        properties.setProperty("liquibase-core-1.9.*jar.license", "value");

        assertThat(properties.getProperty("liquibase-core-1.9.1.jar.license")).isEqualTo("value");

        properties = new WildCardAwareProperties();
        properties.setProperty("liquibase-core-1.*jar.license", "value1");
        properties.setProperty("liquibase-core-2.*jar.license", "value2");

        assertThat(properties.getProperty("liquibase-core-1.jar.license")).isEqualTo("value1");
        assertThat(properties.getProperty("liquibase-core-1.5.1.jar.license")).isEqualTo("value1");
        assertThat(properties.getProperty("liquibase-core-2.5.1.jar.license")).isEqualTo("value2");
        assertThat(properties.getProperty("liquibase-core-2.5.1-SNAPSHPOT.jar.license")).isEqualTo("value2");
        assertThat(properties.getProperty("liquibase-core-2.5.1-7.0.0-SNAPSHPOT.jar.license")).isEqualTo("value2");
    }
}
