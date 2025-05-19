package com.binbash.mobigo;

import com.binbash.mobigo.config.AsyncSyncConfiguration;
import com.binbash.mobigo.config.EmbeddedElasticsearch;
import com.binbash.mobigo.config.EmbeddedSQL;
import com.binbash.mobigo.config.JacksonConfiguration;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * Base composite annotation for integration tests.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SpringBootTest(classes = { MobigoApp.class, JacksonConfiguration.class, AsyncSyncConfiguration.class })
@EmbeddedElasticsearch
@EmbeddedSQL
public @interface IntegrationTest {
}
