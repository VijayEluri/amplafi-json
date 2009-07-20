package org.amplafi.json;

import java.io.IOException;

import org.apache.commons.lang.StringUtils;

import com.sworddance.beans.MapByClass;

/**
 * Delegates everything apart from
 * {@link #addRenderer(Class, JsonRenderer)}, {@link #addRenderer(JsonRenderer)}
 * and {@link #value(Object)}.<p/>
 *
 * The main purpose of such a delegator/decorator is to allow using JsonRenderers
 * for a specific part of a json rendering without affecting the original
 * {@link JSONWriter}.
 *
 * @author Andreas Andreou
 */
public class DelegatingJSONWriter implements IJsonWriter {

    private MapByClass<JsonRenderer<?>> renderers = new MapByClass<JsonRenderer<?>>();
    private JSONWriter realWriter;

    public DelegatingJSONWriter(IJsonWriter writer) {
        this.realWriter = (JSONWriter) writer;
    }

    @Override
    public IJsonWriter append(String s) throws JSONException {
        realWriter.append(s);
        return this;
    }

    @Override
    public IJsonWriter array() throws JSONException {
        realWriter.array();
        return this;
    }

    @Override
    public IJsonWriter endArray() throws JSONException {
        realWriter.endArray();
        return this;
    }

    @Override
    public IJsonWriter endObject() throws JSONException {
        realWriter.endObject();
        return this;
    }

    @Override
    public boolean isInKeyMode() {
        return realWriter.isInKeyMode();
    }

    @Override
    public IJsonWriter object() throws JSONException {
        realWriter.object();
        return this;
    }

    @Override
    public boolean isInArrayMode() {
        return realWriter.isInArrayMode();
    }

    @Override
    public boolean isInObjectMode() {
        return realWriter.isInObjectMode();
    }

    @Override
    public IJsonWriter value(boolean b) throws JSONException {
        realWriter.value(b);
        return this;
    }

    @Override
    public IJsonWriter value(double d) throws JSONException {
        realWriter.value(d);
        return this;
    }

    @Override
    public IJsonWriter value(long l) throws JSONException {
        realWriter.value(l);
        return this;
    }
    public <T> IJsonWriter value(T o) throws JSONException {
        if ( o != null ) {
            JsonRenderer<T> renderer = (JsonRenderer<T>) renderers.getRaw(o.getClass());
            if ( renderer == null) {
                if (o instanceof JsonSelfRenderer) {
                    // we check after looking in map so that it has a chance to have been overridden.
                    ((JsonSelfRenderer) o).toJson(realWriter);
                    return this;
                } else {
                    // o.k. go search for a loose match.
                    renderer = (JsonRenderer<T>) renderers.get(o.getClass());
                }
            }
            if ( renderer != null ) {
                renderer.toJson(this, o);
                return this;
            } else {
                realWriter.value(o);
                return this;
            }
        } else {
            realWriter.append(JSONObject.valueToString(o));
            return this;
        }
    }

    @Override
    public String toString() {
        return realWriter.toString();
    }


    /**
     * @see org.amplafi.json.IJsonWriter#addRenderer(java.lang.Class, org.amplafi.json.JsonRenderer)
     */
    public void addRenderer(Class<?> name, JsonRenderer<?> renderer) {
        renderers.put(name, renderer);
    }
    /**
     * @see org.amplafi.json.IJsonWriter#addRenderer(org.amplafi.json.JsonRenderer)
     */
    public void addRenderer(JsonRenderer<?> renderer) {
        this.addRenderer(renderer.getClassToRender(), renderer);
    }

    public <K> IJsonWriter key(K o) throws JSONException {
        if (o == null) {
            throw new JSONException("Null key.");
        } else if ( !isInKeyMode()){
            throw new JSONException("Misplaced key.");
        } else {
            try {
                if (realWriter.comma) {
                    realWriter.writer.write(',');
                }
                JsonRenderer<K> renderer = (JsonRenderer<K>) renderers.getRaw(o.getClass());
                if ( renderer == null ) {
                    if ( o instanceof JsonSelfRenderer) {
                    // we check after looking in map so that it has a chance to have been overridden.
                        ((JsonSelfRenderer) o).toJson(this);
                        return this;
                    } else {
                        // o.k. go search for a loose match.
                        renderer = (JsonRenderer<K>) renderers.get(o.getClass());
                    }
                }
                if ( renderer != null ) {
                    renderer.toJson(this, o);
                    return this;
                }
                return this.append(JSONObject.valueToString(o));
            } catch (IOException e) {
                throw new JSONException(e);
            } finally {
                try {
                    realWriter.writer.write(':');
                } catch (IOException e) {
                    throw new JSONException(e);
                }
                realWriter.comma = false;
                realWriter.mode = JSONWriter.OBJECT_MODE;
            }
        }
    }
    /**
     * @see org.amplafi.json.IJsonWriter#keyValueIfNotBlankValue(java.lang.String, java.lang.String)
     */
    public IJsonWriter keyValueIfNotBlankValue(String key, String value) {
        if ( !StringUtils.isBlank(value)) {
            this.keyValue(key, value);
        }
        return this;
    }

    /**
     * @see org.amplafi.json.IJsonWriter#keyValueIfNotNullValue(java.lang.String, java.lang.Object)
     */
    public IJsonWriter keyValueIfNotNullValue(String key, Object value) {
        if ( value != null) {
            this.keyValue(key, value);
        }
        return this;
    }

    /**
     * @see org.amplafi.json.IJsonWriter#keyValue(K, V)
     */
    public <K,V> IJsonWriter keyValue(K key, V value) {
        this.key(key).value(value);
        return this;
    }
}
