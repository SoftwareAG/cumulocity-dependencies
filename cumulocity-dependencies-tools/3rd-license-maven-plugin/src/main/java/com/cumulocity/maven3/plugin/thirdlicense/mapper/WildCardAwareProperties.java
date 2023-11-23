package com.cumulocity.maven3.plugin.thirdlicense.mapper;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

public class WildCardAwareProperties {

    private final Map<KeyExpression, String> content = new HashMap<KeyExpression, String>();

    public static WildCardAwareProperties create(Properties... lProperties) {
        WildCardAwareProperties result = new WildCardAwareProperties();
        for (Properties properties : lProperties) {
            for (String propName : properties.stringPropertyNames()) {
                String value = properties.getProperty(propName);
                result.setProperty(propName, value);
            }            
        }
        return result;
    }

    WildCardAwareProperties setProperty(String key, String value) {
        content.put(new KeyExpression(key), value);
        return this;
    }
    
    public String getProperty(String key, String defaultValue) {
        String val = getProperty(key);
        return (val == null) ? defaultValue : val;
    }

    public String getProperty(String key) {
        Set<KeyExpression> keys = content.keySet();
        for (KeyExpression keyExpression : keys) {
            if (keyExpression.match(key)) {
                return content.get(keyExpression);
            }
        }
        return null;
    }

    
    
    @Override
    public String toString() {
        return content.toString();
    }

    private static class KeyExpression {

        private final String regex;

        public KeyExpression(String key) {
            regex = key.replaceAll("\\*", ".*");
        }

        public boolean match(String input) {
            return Pattern.matches(regex, input);
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((regex == null) ? 0 : regex.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            KeyExpression other = (KeyExpression) obj;
            if (regex == null) {
                if (other.regex != null)
                    return false;
            } else if (!regex.equals(other.regex))
                return false;
            return true;
        }

        @Override
        public String toString() {
            return regex;
        }
    }
}
