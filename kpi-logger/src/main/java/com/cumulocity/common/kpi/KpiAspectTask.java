package com.cumulocity.common.kpi;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;

import com.cumulocity.common.kpi.KpiLogger.KpiLoggerTask;

public final class KpiAspectTask implements KpiLoggerTask<Object, RuntimeException> {
    
    private final ProceedingJoinPoint joinPoint;

    public KpiAspectTask(ProceedingJoinPoint joinPoint) {
        this.joinPoint = joinPoint;
    }

    @Override
    public Object execute() throws RuntimeException {
        try {
            return joinPoint.proceed();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String describe() {
        StringBuilder sb = new StringBuilder(joinPoint.toShortString() + ":");
        String[] parameterNames = ((MethodSignature) joinPoint.getSignature()).getParameterNames();
        Object[] parameterValues = joinPoint.getArgs();
        for (int i = 0; i < parameterNames.length; i++) {
            sb.append("\n" + parameterNames[i] + ": " + parameterValues[i]);
        }
        return sb.toString();
    }
}