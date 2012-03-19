package com.cumulocity.common.kpi;

import static com.cumulocity.common.kpi.KpiLogger.getKpiLogger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class KpiCouchAspect {

    @Pointcut("execution(* com.cumulocity.connector.couchdb.CouchDBConnection.*(..))")
    public void executeAnyCouchdbConnectionMethod() { }
    
    @Around("executeAnyCouchdbConnectionMethod()")
    public Object log(final ProceedingJoinPoint joinPoint) {
        return getKpiLogger().log(new KpiAspectTask(joinPoint));
    }
}
