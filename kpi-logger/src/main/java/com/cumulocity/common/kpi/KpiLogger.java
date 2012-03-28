package com.cumulocity.common.kpi;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

public class KpiLogger {

    private static final Logger LOG = LoggerFactory.getLogger(KpiLogger.class);

    private static final KpiLogger INSTANCE = new KpiLogger();

    private KpiLogger() {
    }

    public static KpiLogger getKpiLogger() {
        return INSTANCE;
    }

    public <T, E extends Throwable> Object log(KpiLoggerTask<T, E> task) throws E {
        StopWatch sw = new StopWatch(KpiLogger.class.getSimpleName());
        try {
            sw.start(task.describe());
            return task.execute();
        } finally {
            sw.stop();
            LOG.info(sw.toString());
        }
    }
    
    public static interface KpiLoggerTask<T, E extends Throwable> {
        
        T execute() throws E;
        
        String describe();
    }
}
