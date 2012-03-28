package com.cumulocity.common.kpi;

import static com.cumulocity.common.kpi.KpiLogger.getKpiLogger;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.springframework.web.filter.GenericFilterBean;

import com.cumulocity.common.kpi.KpiLogger.KpiLoggerTask;

public class KpiFilter extends GenericFilterBean {
    
    private String taskName;
    
    public void setTaskName(String taskName) {
        this.taskName = taskName;
    }
    
    @Override
    public void doFilter(final ServletRequest request, final ServletResponse response, final FilterChain chain) 
            throws IOException, ServletException {
        
        try {
            getKpiLogger().log(new KpiFilterTask(request, response, chain));
        } catch (IOException e) {
            throw e;
        } catch (ServletException e) {
            throw e;
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
    
    private final class KpiFilterTask implements KpiLoggerTask<Void, Exception> {
        
        private final HttpServletRequest request;
        private final ServletResponse response;
        private final FilterChain chain;
        
        private KpiFilterTask(ServletRequest request, ServletResponse response, FilterChain chain) {
            this.chain = chain;
            this.response = response;
            this.request = (HttpServletRequest) request;
        }
        
        @Override
        public Void execute() throws Exception {
            chain.doFilter(request, response);
            return null;
        }
        
        @Override
        public String describe() {
            StringBuilder sb = new StringBuilder(taskName + ": ");
            sb.append(request.getMethod() + " ");
            sb.append(request.getRequestURL());
            sb.append(request.getQueryString() == null ? "" : "?" + request.getQueryString());
            return sb.toString();
        }
    }
}
