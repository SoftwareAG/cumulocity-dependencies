package com.cumulocity.common.kpi;

import static com.cumulocity.common.kpi.KpiLogger.getKpiLogger;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cumulocity.common.kpi.KpiLogger.KpiLoggerTask;

@RunWith(PowerMockRunner.class)
@PrepareForTest(LoggerFactory.class)
public class KpiLoggerTest {

    private static final Logger LOG = mock(Logger.class);

    private final KpiLoggerTask<?, ?> task = mock(KpiLoggerTask.class);

    private final KpiLogger kpiLogger = getKpiLogger();

    @BeforeClass
    public static void prepareStaticMocks() {
        mockStatic(LoggerFactory.class);
        when(LoggerFactory.getLogger(KpiLogger.class)).thenReturn(LOG);
    }

    @Before
    public void setUp() {
        reset(LOG);
        
        when(task.describe()).thenReturn("test");
    }

    @Test
    public void shouldLog() throws Throwable {
        kpiLogger.log(task);

        verify(LOG, atLeastOnce()).info(anyString());
    }

    @Test
    public void shouldProceed() throws Throwable {
        kpiLogger.log(task);

        verify(task).execute();
    }
}

