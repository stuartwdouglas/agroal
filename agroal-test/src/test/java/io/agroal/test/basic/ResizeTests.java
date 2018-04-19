// Copyright (C) 2017 Red Hat, Inc. and individual contributors as indicated by the @author tags.
// You may not use this file except in compliance with the Apache License, Version 2.0.

package io.agroal.test.basic;

import io.agroal.api.AgroalDataSource;
import io.agroal.api.AgroalDataSourceListener;
import io.agroal.api.configuration.supplier.AgroalDataSourceConfigurationSupplier;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CountDownLatch;
import java.util.logging.Logger;

import static io.agroal.test.AgroalTestGroup.FUNCTIONAL;
import static io.agroal.test.MockDriver.deregisterMockDriver;
import static io.agroal.test.MockDriver.registerMockDriver;
import static java.text.MessageFormat.format;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.logging.Logger.getLogger;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author <a href="lbarreiro@redhat.com">Luis Barreiro</a>
 */
@Tag( FUNCTIONAL )
public class ResizeTests {

    private static final Logger logger = getLogger( ResizeTests.class.getName() );

    @BeforeAll
    public static void setupMockDriver() {
        registerMockDriver();
    }

    @AfterAll
    public static void teardown() {
        deregisterMockDriver();
    }

    // --- //

    @Test
    @DisplayName( "resize Max" )
    public void resizeMax() throws SQLException {
        int MAX_POOL_SIZE = 10, NEW_MAX_SIZE = 6, TIMEOUT_MS = 1000;

        AgroalDataSourceConfigurationSupplier configurationSupplier = new AgroalDataSourceConfigurationSupplier()
                .metricsEnabled()
                .connectionPoolConfiguration( cp -> cp
                        .initialSize( MAX_POOL_SIZE )
                        .maxSize( MAX_POOL_SIZE )
                );

        CountDownLatch creationLatch = new CountDownLatch( MAX_POOL_SIZE );
        CountDownLatch destroyLatch = new CountDownLatch( MAX_POOL_SIZE - NEW_MAX_SIZE );
        AgroalDataSourceListener listener = new AgroalDataSourceListener() {
            @Override
            public void onConnectionCreation(Connection connection) {
                creationLatch.countDown();
            }

            @Override
            public void onConnectionDestroy(Connection connection) {
                destroyLatch.countDown();
            }
        };

        try ( AgroalDataSource dataSource = AgroalDataSource.from( configurationSupplier, listener ) ) {
            logger.info( format( "Awaiting fill of all the {0} initial connections on the pool", MAX_POOL_SIZE ) );
            if ( !creationLatch.await( TIMEOUT_MS, MILLISECONDS ) ) {
                fail( format( "{0} connections not created", creationLatch.getCount() ) );
            }

            assertEquals( MAX_POOL_SIZE, dataSource.getMetrics().availableCount(), "Pool not initialized correctly" );
            dataSource.getConfiguration().connectionPoolConfiguration().setMaxSize( NEW_MAX_SIZE );

            for ( int i = MAX_POOL_SIZE; i > 0; i-- ) {
                try ( Connection c = dataSource.getConnection() ) {
                    assertNotNull( c );
                }
            }

            logger.info( format( "Waiting for destruction of {0} connections ", MAX_POOL_SIZE - NEW_MAX_SIZE ) );
            if ( !destroyLatch.await( TIMEOUT_MS, MILLISECONDS ) ) {
                fail( format( "{0} flushed connections not sent for destruction", destroyLatch.getCount() ) );
            }

            assertEquals( NEW_MAX_SIZE, dataSource.getMetrics().availableCount(), "Pool not resized" );
        } catch ( InterruptedException e ) {
            fail( "Test fail due to interrupt" );
        }
    }
}