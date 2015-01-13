package org.svenson;

import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class JSONTest {

    @Test
    public void test() {
        Map<String, Object> test = new HashMap<String, Object>();
        test.put("self", "http://integration.cumulocity.com/inventory/managedObjects/10200/assetParents");
        
        String parsedTest = JSON.defaultJSON().forValue(test);
        
        assertEquals("{\"self\":\"http://integration.cumulocity.com/inventory/managedObjects/10200/assetParents\"}", parsedTest);
    }

}
