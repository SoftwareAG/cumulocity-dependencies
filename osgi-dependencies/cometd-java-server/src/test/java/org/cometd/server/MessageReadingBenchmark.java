package org.cometd.server;

import org.cometd.bayeux.server.ServerMessage;
import org.cometd.common.JSONContext;
import org.junit.jupiter.api.Test;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.openjdk.jmh.annotations.Mode.AverageTime;

@BenchmarkMode(AverageTime)
@Fork(1)
@Warmup(iterations = 0)
@org.openjdk.jmh.annotations.Measurement(iterations = 5)
@State(Scope.Benchmark)
public class MessageReadingBenchmark {

    private JSONContext.Server jsonContext = null;

    private Map data;
    private List<ServerMessage.Mutable> oldQueue;

    @Setup
    public void setup() throws ParseException {
        data = generateData(500);
        oldQueue = new ArrayList<>();
        for(int i=0; i<100_000; i++) {
            ServerMessageImpl serverMessage = new ServerMessageImpl();
            serverMessage.setData(data);
            serverMessage.setChannel("/some/setChannel/*");
            serverMessage.setId("123");
            serverMessage.setClientId("321");
            oldQueue.add(serverMessage);
            serverMessage.getData();
        }
    }

    @Test
    public void messageReadingBenchmark() throws RunnerException {
        Options opt = new OptionsBuilder()
                .result("target/" + MessageReadingBenchmark.class.getSimpleName() + "-jmh.json")
                .resultFormat(ResultFormatType.JSON)
                .include(MessageReadingBenchmark.class.getSimpleName())
                .build();
        new Runner(opt).run();
    }

    @Benchmark
    public void readingBenchmark() {
        for(int i=0; i<oldQueue.size(); i++) {
            WeakMessage weakMessage = new WeakMessage(oldQueue.get(i),0, jsonContext);
            weakMessage.getData();
        }
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
