package org.cometd.server;

import org.apache.commons.io.IOUtils;
import org.cometd.bayeux.server.ServerMessage;
import org.cometd.common.JSONContext;
import org.eclipse.jetty.util.ajax.JSON;
import org.json.JSONException;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.skyscreamer.jsonassert.JSONAssert;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.text.ParseException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import static org.assertj.core.api.Assertions.assertThat;

public class WeakMessageTest {

    private JSON generator = new JSON();
    private JSONContext.Server jsonContext = null;

    @Test
    public void shouldGenerateSameJSON() throws ParseException, JSONException {
        //Given
        String jsonData = "{\"realtimeAction\":\"UPDATE\",\"data\":{\"additionParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/additionParents\",\"references\":[]},\"owner\":\"admin\",\"childDevices\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childDevices\",\"references\":[]},\"childAssets\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAssets\",\"references\":[{\"managedObject\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3200\",\"id\":\"3200\"},\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAssets/3200\"}]},\"creationTime\":\"2020-08-28T09:20:30.186Z\",\"lastUpdated\":\"2020-08-28T09:20:30.186Z\",\"childAdditions\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAdditions\",\"references\":[]},\"name\":\"testGroup1\",\"assetParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/assetParents\",\"references\":[]},\"deviceParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/deviceParents\",\"references\":[]},\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201\",\"id\":\"3201\",\"c8y_IsDeviceGroup\":{}}}";
        Map data = WeakMessage.parseJsonToMap(jsonData, jsonContext);
        ServerMessageImpl serverMessage = new ServerMessageImpl();
        serverMessage.setData(data);
        serverMessage.setChannel("/some/setChannel/*");
        serverMessage.setId("123");
        serverMessage.setClientId("321");

        //When
        WeakMessage weakMessage = new WeakMessage(serverMessage,50000, jsonContext);
        weakMessage.freeze(serverMessage.getJSON());

        //Then
        JSONAssert.assertEquals(serverMessage.getJSON(), weakMessage.getJSON(), false);
    }

    @Test
    public void shouldGenerateSameJSONDataWhenGCWeakReference() throws ParseException, JSONException {
        //Given
        String jsonData = new String("{\"realtimeAction\":\"UPDATE\",\"data\":{\"additionParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/additionParents\",\"references\":[]},\"owner\":\"admin\",\"childDevices\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childDevices\",\"references\":[]},\"childAssets\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAssets\",\"references\":[{\"managedObject\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3200\",\"id\":\"3200\"},\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAssets/3200\"}]},\"creationTime\":\"2020-08-28T09:20:30.186Z\",\"lastUpdated\":\"2020-08-28T09:20:30.186Z\",\"childAdditions\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAdditions\",\"references\":[]},\"name\":\"testGroup1\",\"assetParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/assetParents\",\"references\":[]},\"deviceParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/deviceParents\",\"references\":[]},\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201\",\"id\":\"3201\",\"c8y_IsDeviceGroup\":{}}}");
        Map weakData = WeakMessage.parseJsonToMap(jsonData, jsonContext);

        ServerMessageImpl serverMessage = new ServerMessageImpl();
        serverMessage.setData(weakData);
        serverMessage.setChannel("/some/setChannel/*");
        serverMessage.setId("123");
        serverMessage.setClientId("321");

        WeakMessage weakMessage = new WeakMessage(serverMessage, 50000, jsonContext);
        weakMessage.freeze(serverMessage.getJSON());
        String copyOfserverMessageJosnData = new String(generator.toJSON(serverMessage.getData()));

        //When
        jsonData = null;
        weakData = null;
        serverMessage = null;
        System.gc();

        //Then
        JSONAssert.assertEquals(copyOfserverMessageJosnData, generator.toJSON(weakMessage.getData()), false);
    }

    @Test
    public void shouldGetDataFromJson() {
        //Given
        WeakMessage weakMessage = new WeakMessage(50000, jsonContext);
        String weakData = new String("WeakReferenceData");
        weakMessage.setData(weakData);
        weakMessage.freeze("{\"data\":\"JsonData\"}");

        //When
        weakData = null;
        System.gc(); // clear data in WeakReference

        //Then
        assertThat(weakMessage.get("data")).isEqualTo("JsonData");
    }

    @Test
    public void shouldGetDataFromWeakReference_NotFromJsonWhenNoGC() {
        //Given
        WeakMessage weakMessage = new WeakMessage( 50000, jsonContext);
        String weakData = new String("WeakReferenceData");
        weakMessage.setData(weakData);

        //When
        weakData = null;
        weakMessage.freeze("{\"data\":\"JsonData\"}");

        //Then
        assertThat(weakMessage.get("data")).isEqualTo("WeakReferenceData");
    }

    @Test
    public void shouldGCWeakReferenceFromWeakMessageAndGetDataFromJson() throws ParseException {
        //Given
        WeakMessage weakMessage = new WeakMessage(50000, jsonContext);
        String weakData = new String("WeakReferenceData");
        weakMessage.setData(weakData);

        weakMessage.freeze("{\"data\":\"JsonData\"}");

        //When
        weakData = null;
        System.gc(); // clear data in WeakReference
        String data = (String) weakMessage.get("data");

        //Then
        assertThat(data).isEqualTo("JsonData");
    }

    @Test
    public void shouldZipJsonData_WhenZipMessageThresholdReached() throws IOException {
        //Given
        WeakMessage weakMessage = new WeakMessage(0, jsonContext);

        //When
        weakMessage.freeze("{\"data\":\"JsonData\"}");

        //Then
        String unzipData = IOUtils.toString(new GZIPInputStream(new ByteArrayInputStream(weakMessage.getRawData())));
        assertThat(unzipData).isEqualTo("{\"data\":\"JsonData\"}");
    }

    @Test
    public void shouldNotZipJsonData_WhenZipMessageNotReachedThreshold() {
        //Given
        WeakMessage weakMessage =  new WeakMessage(50000, jsonContext);

        //When
        weakMessage.freeze("{\"data\":\"JsonData\"}");

        //Then
        assertThat(new String(weakMessage.getRawData())).isEqualTo("{\"data\":\"JsonData\"}");
    }

    @Test
    public void shouldCreateShallowMessageCopy() throws ParseException {
        String jsonData = "{\"realtimeAction\":\"UPDATE\",\"data\":{\"additionParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/additionParents\",\"references\":[]},\"owner\":\"admin\",\"childDevices\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childDevices\",\"references\":[]},\"childAssets\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAssets\",\"references\":[{\"managedObject\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3200\",\"id\":\"3200\"},\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAssets/3200\"}]},\"creationTime\":\"2020-08-28T09:20:30.186Z\",\"lastUpdated\":\"2020-08-28T09:20:30.186Z\",\"childAdditions\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAdditions\",\"references\":[]},\"name\":\"testGroup1\",\"assetParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/assetParents\",\"references\":[]},\"deviceParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/deviceParents\",\"references\":[]},\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201\",\"id\":\"3201\",\"c8y_IsDeviceGroup\":{}}}";
        Map data = WeakMessage.parseJsonToMap(jsonData, jsonContext);
        WeakMessage weakMessage = new WeakMessage(50000, jsonContext);
        weakMessage.setData(data);
        weakMessage.setChannel("/some/setChannel/*");
        weakMessage.setId("123");
        weakMessage.setClientId("321");

        //When
        WeakMessage copyOfWeakMessage = weakMessage.copy();

        //then
        assertThat(copyOfWeakMessage.get("data")).isEqualTo(weakMessage.get("data"));
        assertThat(copyOfWeakMessage.get("channel")).isEqualTo(weakMessage.get("channel"));
        assertThat(copyOfWeakMessage.get("id")).isEqualTo(weakMessage.get("id"));
    }

    @Test
    @Tag("PerformanceTest")
    public void zippingPerformanceTest() throws ParseException {
        Map data = generateData(500);
        List<ServerMessage.Mutable> oldQueue = new ArrayList<>();
        for(int i=0; i<1; i++) {
            ServerMessageImpl serverMessage = new ServerMessageImpl();
            serverMessage.setData(data);
            serverMessage.setChannel("/some/setChannel/*");
            serverMessage.setId("123");
            serverMessage.setClientId("321");
            oldQueue.add(serverMessage);
            serverMessage.getData();
        }

        Instant start = Instant.now();
        for(int i=0; i<oldQueue.size(); i++) {
            WeakMessage weakMessage = new WeakMessage(oldQueue.get(i),0, jsonContext);
            weakMessage.getData();
        }

        Instant end = Instant.now();
        System.out.println("Message zip and unzip time = " + Duration.between(start, end).toMillis() );
    }

    private Map generateData(int numberOfChildDeviceReferences) throws ParseException {
        StringBuilder childDeviceReferencesBuilder = new StringBuilder();
        for(int i=0; i <numberOfChildDeviceReferences; i++) {
            childDeviceReferencesBuilder.append("{ \"managedObject\": { \"self\": \"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/"+i+"\", \"id\": \""+i+"\" }, \"self\": \"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/10107/childAssets/"+i+"\" },");
        }
        String jsonData = "{\"realtimeAction\":\"UPDATE\",\"data\":{\"additionParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/additionParents\",\"references\":[]},\"owner\":\"admin\",\"childDevices\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childDevices\",\"references\":[]},\"childAssets\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAssets\",\"references\":[ "+childDeviceReferencesBuilder+" {\"managedObject\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3200\",\"id\":\"3200\"},\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAssets/3200\"}]},\"creationTime\":\"2020-08-28T09:20:30.186Z\",\"lastUpdated\":\"2020-08-28T09:20:30.186Z\",\"childAdditions\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/childAdditions\",\"references\":[]},\"name\":\"testGroup1\",\"assetParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/assetParents\",\"references\":[]},\"deviceParents\":{\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201/deviceParents\",\"references\":[]},\"self\":\"http://cumulocity.default.svc.cluster.local/inventory/managedObjects/3201\",\"id\":\"3201\",\"c8y_IsDeviceGroup\":{}}}";
        System.out.println("size = " + jsonData.getBytes().length);
        return WeakMessage.parseJsonToMap(jsonData, jsonContext);
    }
}
