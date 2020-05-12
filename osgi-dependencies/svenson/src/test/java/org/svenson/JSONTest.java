package org.svenson;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class JSONTest {

    @Test
    public void test() {
        Map<String, Object> test = new HashMap<String, Object>();
        test.put("self", "http://integration.cumulocity.com/inventory/managedObjects/10200/assetParents");

        String parsedTest = JSON.defaultJSON().forValue(test);

        assertEquals("{\"self\":\"http://integration.cumulocity.com/inventory/managedObjects/10200/assetParents\"}", parsedTest);
    }

    @Test
    public void shouldDeserializeObjectWithOverloadedSetterAndOverriddenProperties() {
        Map<String, Object> test = new HashMap<String, Object>();
        test.put("value", "ANY");
        test.put("text","test");

        String serialized = JSON.defaultJSON().forValue(test);

        final TestJsonObject result = JSONParser.defaultJSONParser().parse(TestJsonObject.class, serialized);

        assertEquals(TestJsonObject.Value.ANY, result.getValue());
        assertEquals("test",result.getText());
    }



}


