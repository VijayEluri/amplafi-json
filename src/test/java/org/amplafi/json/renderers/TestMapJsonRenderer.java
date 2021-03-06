/**
 *
 */
package org.amplafi.json.renderers;

import static org.testng.Assert.assertEquals;

import java.util.LinkedHashMap;
import java.util.Map;

import org.amplafi.flow.json.JSONObject;
import org.amplafi.flow.json.JSONStringer;
import org.amplafi.flow.json.renderers.MapJsonRenderer;
import org.testng.annotations.Test;

public class TestMapJsonRenderer {

    // TODO : this shows the weakness we have currently. We really need to have a map of the expected datatypes to be deserialized.
    // lots of issues when handling complex types. hopefully json-lib will help with this.
//    @Test
//    public void testValueAsJsonSelfRenderer(){
//        Map<String,SampleJsonData> inputMap = new HashMap<String, SampleJsonData>();
//        inputMap.put("1", new SampleJsonData("A",20));
//        inputMap.put("2", new SampleJsonData("B",30));
//
//        MapJsonRenderer<String, SampleJsonData> renderer = new MapJsonRenderer<String, SampleJsonData>(true);
//        Map<String, SampleJsonData> outMap = TestJsonRendererUtil.toFromJson(renderer, inputMap);
//        assertEquals(outMap, inputMap);
//    }
//
//    @Test
//    public void testKeyAsJsonSelfRenderer(){
//        Map<SampleJsonData,SampleJsonData> inputMap = new HashMap<SampleJsonData, SampleJsonData>();
//        inputMap.put(new SampleJsonData("Key",1), new SampleJsonData("A",20));
//        inputMap.put(new SampleJsonData("Key",2), new SampleJsonData("B",30));
//
//        MapJsonRenderer<SampleJsonData, SampleJsonData> renderer = new MapJsonRenderer<SampleJsonData, SampleJsonData>(true);
//        Map<SampleJsonData, SampleJsonData> outMap = TestJsonRendererUtil.toFromJson(renderer, inputMap);
//        assertEquals(outMap, inputMap);
//    }

    @Test
    public void testTofromJson() {
        Map<String, Object> inMap = new LinkedHashMap<String, Object>();
        MapJsonRenderer<String, Object> renderer = new MapJsonRenderer<String, Object>(false);
        Map<String, Object> outMap = TestJsonRendererUtil.toFromJson(renderer, inMap);
        assertEquals(outMap, inMap);

        inMap.put("foo", true);
        // check recursion
        inMap.put("bar", new LinkedHashMap<Object,Object>());
        inMap.put("str", "\'\":);");
        outMap = TestJsonRendererUtil.toFromJson(renderer, inMap);
        assertEquals(outMap.size(), inMap.size());
        assertEquals(outMap.get("foo"), inMap.get("foo"));
        assertEquals(outMap.get("str"), inMap.get("str"));

        // we can't know that the nested map should be interpreted as a map.
        JSONObject actual = (JSONObject) outMap.get("bar");
        assertEquals(actual.asMap(), inMap.get("bar"));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testBasic() {
        MapJsonRenderer renderer = new MapJsonRenderer(false);
        JSONStringer jsonWriter = new JSONStringer();
        jsonWriter.addRenderer(renderer);
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        jsonWriter.value(map);
        assertEquals(jsonWriter.toString(), "{}");

        jsonWriter = new JSONStringer();
        jsonWriter.addRenderer(renderer);
        map.put("foo", true);
        // check recursion
        map.put("bar", new LinkedHashMap());
        map.put("str", "\'\":);");
        jsonWriter.value(map);
        assertEquals(jsonWriter.toString(), "{\"foo\":true,\"bar\":{},\"str\":\"'\\\":);\"}");
    }

    @Test
    public void testNullFiltering() {
        MapJsonRenderer renderer = new MapJsonRenderer(false);
        JSONStringer jsonWriter = new JSONStringer();
        jsonWriter.addRenderer(renderer);
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put(null, "gg");
        map.put("ff", null);
        jsonWriter.value(map);
        assertEquals(jsonWriter.toString(), "{}");
    }

    /**
     * allow null values but not null keys
     */
    @Test
    public void testAllowNulls() {
        MapJsonRenderer renderer = new MapJsonRenderer(true);
        JSONStringer jsonWriter = new JSONStringer();
        jsonWriter.addRenderer(renderer);
        Map<String, Object> map = new LinkedHashMap<String, Object>();
        map.put("ff", null);
        jsonWriter.value(map);

        assertEquals(jsonWriter.toString(), "{\"ff\":null}");
    }

    @Test
    public void testFromJsonBasic() {
        MapJsonRenderer<String, String> renderer = new MapJsonRenderer<String, String>(false);
        String input = "{\"foo\":true,\"bar\":{},\"str\":\"'\\\":);\"}";
        Map<String, String> map = renderer.fromSerialization(Map.class, JSONObject.toJsonObject(input));
        assertEquals(map.entrySet().size(), 3);
        assertEquals(map.get("foo"), Boolean.TRUE);
        assertEquals(map.get("str"), "\'\":);");
        Object obj = map.get("bar");
        Map<String, String> mapElement = renderer.fromSerialization(Map.class, obj);
        assertEquals(mapElement.entrySet().size(), 0);
    }
}
