package com.cumulocity.common.kpi;

import static com.cumulocity.common.kpi.KpiLogger.getKpiLogger;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;

@Aspect
public class KpiJdbcAspect {    

    @Pointcut("execution(* java.sql.Statement.execute*(..)) || execution(* java.sql.PreparedStatement.execute*(..))")
    public void executeJdbcStatement() { }
    
    @Pointcut("execution(* java.sql.Connection.commit(..))")
    public void commitJdbcConnection() { }
    
    @Around("executeJdbcStatement() || commitJdbcConnection()")
    public Object log(final ProceedingJoinPoint joinPoint) {
        return getKpiLogger().log(new KpiAspectTask(joinPoint));
    }
}
