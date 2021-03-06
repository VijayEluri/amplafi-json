package org.amplafi.flow.json;

/*
Copyright (c) 2002 JSON.org

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

The Software shall be used for Good, not Evil.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
 */

import java.io.IOException;
import java.io.Writer;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.apache.commons.lang.ObjectUtils;
import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;

/**
 * A JSONObject is an unordered collection of name/value pairs. Its
 * external form is a string wrapped in curly braces with colons between the
 * names and values, and commas between the values and names. The internal form
 * is an object having <code>get</code> and <code>opt</code> methods for
 * accessing the values by name, and <code>put</code> methods for adding or
 * replacing values by name. The values can be any of these types:
 * <code>Boolean</code>, <code>JSONArray</code>, <code>JSONObject</code>,
 * <code>Number</code>, <code>String</code>, or the <code>JSONObject.NULL</code>
 * object. A JSONObject constructor can be used to convert an external form
 * JSON text into an internal form whose values can be retrieved with the
 * <code>get</code> and <code>opt</code> methods, or to convert values into a
 * JSON text using the <code>put</code> and <code>toString</code> methods.
 * A <code>get</code> method returns a value if one can be found, and throws an
 * exception if one cannot be found. An <code>opt</code> method returns a
 * default value instead of throwing an exception, and so is useful for
 * obtaining optional values.
 * <p>
 * The generic <code>get()</code> and <code>opt()</code> methods return an
 * object, which you can cast or query for type. There are also typed
 * <code>get</code> and <code>opt</code> methods that do type checking and type
 * coersion for you.
 * <p>
 * The <code>put</code> methods adds values to an object. For example, <pre>
 *     myString = new JSONObject().put("JSON", "Hello, World!").toString();</pre>
 * produces the string <code>{"JSON": "Hello, World"}</code>.
 * <p>
 * The texts produced by the <code>toString</code> methods strictly conform to
 * the JSON syntax rules.
 * The constructors are more forgiving in the texts they will accept:
 * <ul>
 * <li>An extra <code>,</code>&nbsp;<small>(comma)</small> may appear just
 *     before the closing brace.</li>
 * <li>Strings may be quoted with <code>'</code>&nbsp;<small>(single
 *     quote)</small>.</li>
 * <li>Strings do not need to be quoted at all if they do not begin with a quote
 *     or single quote, and if they do not contain leading or trailing spaces,
 *     and if they do not contain any of these characters:
 *     <code>{ } [ ] / \ : , = ; #</code> and if they do not look like numbers
 *     and if they are not the reserved words <code>true</code>,
 *     <code>false</code>, or <code>null</code>.</li>
 * <li>Keys can be followed by <code>=</code> or <code>=></code> as well as
 *     by <code>:</code>.</li>
 * <li>Values can be followed by <code>;</code> <small>(semicolon)</small> as
 *     well as by <code>,</code> <small>(comma)</small>.</li>
 * <li>Numbers may have the <code>0-</code> <small>(octal)</small> or
 *     <code>0x-</code> <small>(hex)</small> prefix.</li>
 * <li>Comments written in the slashshlash, slashstar, and hash conventions
 *     will be ignored.</li>
 * </ul>
 * @author JSON.org
 * @version 2
 */
public class JSONObject implements JsonConstruct {

    private static final String STRINGED_NULL = "null";

	/**
     * The hash map where the JSONObject's properties are kept.
     */
    private Map<String, Object> myLinkedHashMap;


    /**
     * It is sometimes more convenient and less ambiguous to have a
     * <code>NULL</code> object than to use Java's <code>null</code> value.
     * <code>JSONObject.NULL.equals(null)</code> returns <code>true</code>.
     * <code>JSONObject.NULL.toString()</code> returns <code>"null"</code>.
     */
    public static final Object NULL = new Null();


    /**
     * Construct an empty JSONObject.
     */
    public JSONObject() {
        // LinkedHashMap makes testing easier (particularly when converting to strings)
        this.myLinkedHashMap = new LinkedHashMap<String, Object>();
    }
    /**
     * The passed map is NOT copied (useful when it has some other properties - i.e. TreeMap)
     * @param myLinkedHashMap
     */
    public JSONObject(Map<String, ? extends Object> myLinkedHashMap) {
        this.myLinkedHashMap = (Map<String, Object>) myLinkedHashMap;
    }


    /**
     * Construct a JSONObject from a subset of another JSONObject.
     * An array of strings is used to identify the keys that should be copied.
     * Missing keys are ignored.
     * @param jo A JSONObject.
     * @param sa An array of strings.
     * @exception JSONException If a value is a non-finite number.
     */
    public JSONObject(JSONObject jo, String[] sa) throws JSONException {
        this();
        for (String element : sa) {
            putOpt(element, jo.opt(element));
        }
    }


    /**
     * Construct a JSONObject from a JSONTokener.
     * @param x A JSONTokener object containing the source string.
     * @throws JSONException If there is a syntax error in the source string.
     */
    public JSONObject(JSONTokener x) throws JSONException {
        this();
        char c;
        String key;

        if ((c = x.nextClean()) != '{') {
            throw x.syntaxError("A JSONObject text must begin with '{'", c);
        }
        for (;;) {
            c = x.nextClean();
            switch (c) {
            case 0:
                throw x.syntaxError("A JSONObject text must end with '}' but there was nothing.");
            case '}':
                return;
            default:
                x.back();
                key = x.nextValue().toString();
            }

            /*
             * The key is followed by ':'. We will also tolerate '=' or '=>'.
             */

            c = x.nextClean();
            if (c == '=') {
                if (x.next() != '>') {
                    x.back();
                }
            } else if (c != ':') {
                throw x.syntaxError("Expected a ':' after a key", c);
            }
            this.myLinkedHashMap.put(key, x.nextValue());

            /*
             * Pairs are separated by ','. We will also tolerate ';'.
             */

            switch (c=x.nextClean()) {
            case ';':
            case ',':
                if (x.nextClean() == '}') {
                    return;
                }
                x.back();
                break;
            case '}':
                return;
            default:
                throw x.syntaxError("Expected a ',' or '}'", c);
            }
        }
    }

    /**
     * Construct a JSONObject from a string.
     * This is the most commonly used JSONObject constructor.
     * @param string    A string beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @exception JSONException If there is a syntax error in the source string.
     */
    public JSONObject(String string) throws JSONException {
        this(new JSONTokener(StringUtils.isBlank(string) || STRINGED_NULL.equals(string)?"{}":string));
    }


    /**
     * Accumulate values under a key. It is similar to the put method except
     * that if there is already an object stored under the key then a
     * JSONArray is stored under the key to hold all of the accumulated values.
     * If there is already a JSONArray, then the new value is appended to it.
     * In contrast, the put method replaces the previous value.
     * @param key   A key string.
     * @param value An object to be accumulated under the key.
     * @return this.
     * @throws JSONException If the value is an invalid number
     *  or if the key is null.
     */
    public JSONObject accumulate(String key, Object value)
    throws JSONException {
        testValidity(value);
        Object o = opt(key);
        if (o == null) {
            put(key, value);
        } else if (o instanceof JSONArray) {
            ((JSONArray)o).put(value);
        } else {
            put(key, new JSONArray().put(o).put(value));
        }
        return this;
    }


    /**
     * Get the value object associated with a key.
     *
     * @param key   A key string.
     * @return      The object associated with the key.
     * @throws   JSONException if the key is not found.
     */
    public Object get(String key) throws JSONException {
        Object o = opt(key);
        if (o == null) {
            throw new JSONException("JSONObject[" + quote(key) +
            "] not found.");
        }
        return o;
    }


    /**
     * Get the boolean value associated with a key.
     *
     * @param key   A key string.
     * @return      The truth.
     * @throws   JSONException
     *  if the value is not a Boolean or the String "true" or "false".
     */
    public boolean getBoolean(String key) throws JSONException {
        Object o = get(key);
        if (o.equals(Boolean.FALSE) ||
                (o instanceof String &&
                        ((String)o).equalsIgnoreCase("false"))) {
            return false;
        } else if (o.equals(Boolean.TRUE) ||
                (o instanceof String &&
                        ((String)o).equalsIgnoreCase("true"))) {
            return true;
        }
        throw new JSONException("JSONObject[" + quote(key) +
        "] is not a Boolean.");
    }


    /**
     * Get the double value associated with a key.
     * @param key   A key string.
     * @return      The numeric value.
     * @throws JSONException if the key is not found or
     *  if the value is not a Number object and cannot be converted to a number.
     */
    public double getDouble(String key) throws JSONException {
        Object o = get(key);
        try {
            return o instanceof Number ?
                    ((Number)o).doubleValue() : Double.parseDouble((String)o);
        } catch (Exception e) {
            throw new JSONException("JSONObject[" + quote(key) +
            "] is not a number. value is ="+o, e);
        }
    }


    /**
     * Get the int value associated with a key. If the number value is too
     * large for an int, it will be clipped.
     *
     * @param key   A key string.
     * @return      The integer value.
     * @throws   JSONException if the key is not found or if the value cannot
     *  be converted to an integer.
     */
    public int getInt(String key) throws JSONException {
        Object o = get(key);
        return o instanceof Number ?
                ((Number)o).intValue() : (int)getDouble(key);
    }


    /**
     * Get the JSONArray value associated with a key.
     *
     * @param key   A key string.
     * @return      A JSONArray which is the value.
     * @throws   JSONException if the key is not found or
     *  if the value is not a JSONArray.
     */
    @SuppressWarnings("unchecked")
    public <T> JSONArray<T> getJSONArray(String key) throws JSONException {
        Object o = get(key);
        if (o instanceof JSONArray) {
            return (JSONArray<T>)o;
        } else if ( o == null) {
            throw new JSONException("JSONObject[" + quote(key) + "] is null");
        } else {
            throw new JSONException("JSONObject[" + quote(key) + "] is a "+o.getClass()+" not a JSONArray.");
        }
    }


    /**
     * Get the JSONObject value associated with a key.
     *
     * @param key   A key string.
     * @return      A JSONObject which is the value.
     * @throws   JSONException if the key is not found or
     *  if the value is not a JSONObject.
     */
    public JSONObject getJSONObject(String key) throws JSONException {
        Object o = get(key);
        if (o instanceof JSONObject) {
            return (JSONObject)o;
        }
        throw new JSONException("JSONObject[" + quote(key) + "] is not a JSONObject.");
    }


    /**
     * Get the long value associated with a key. If the number value is too
     * long for a long, it will be clipped.
     *
     * @param key   A key string.
     * @return      The long value.
     * @throws   JSONException if the key is not found or if the value cannot
     *  be converted to a long.
     */
    public long getLong(String key) throws JSONException {
        Object o = get(key);
        return o instanceof Number ?
                ((Number)o).longValue() : (long)getDouble(key);
    }

    /**
     * @param key
     * @param defaultValue
     * @return optional integer
     */
    public Integer optInteger(String key, Integer defaultValue) {
        Object o = opt(key);
        if ( o == null ) {
            return defaultValue;
        } else if ( o instanceof Integer) {
            return (Integer)o;
        } else if ( o instanceof Number) {
            return Integer.valueOf(((Number)o).intValue());
        } else {
            try {
                return Integer.valueOf(o.toString());
            } catch( NumberFormatException e) {
                throw new JSONException("JSONObject[" + quote(key) +
                    "] is not a number. value is ="+o, e);
            }
        }
    }

    /**
     * Get the string associated with a key.
     *
     * @param key   A key string.
     * @return      A string which is the value.
     * @throws   JSONException if the key is not found.
     */
    public String getString(String key) throws JSONException {
        return get(key).toString();
    }


    /**
     * Determine if the JSONObject contains a specific key.
     * @param key   A key string.
     * @return      true if the key exists in the JSONObject.
     */
    public boolean has(String key) {
        return this.myLinkedHashMap.containsKey(key);
    }


    /**
     * Determine if the value associated with the key is null or if there is
     *  no value.
     * @param key   A key string.
     * @return      true if there is no value associated with the key or if
     *  the value is the JSONObject.NULL object.
     */
    public boolean isNull(String key) {
        return JSONObject.NULL.equals(opt(key));
    }


    /**
     * Get an enumeration of the keys of the JSONObject.
     *
     * @return An iterator of the keys.
     */
    public Set<String> keys() {
        return this.myLinkedHashMap.keySet();
    }


    /**
     * Get the number of keys stored in the JSONObject.
     *
     * @return The number of keys in the JSONObject.
     */
    public int length() {
        return this.myLinkedHashMap.size();
    }


    /**
     * Produce a JSONArray containing the names of the elements of this
     * JSONObject.
     * @return A JSONArray containing the key strings, or null if the JSONObject
     * is empty.
     */
    public JSONArray names() {
        JSONArray ja = new JSONArray();
        Iterator<String>  keys = keys().iterator();
        while (keys.hasNext()) {
            ja.put(keys.next());
        }
        return ja.length() == 0 ? null : ja;
    }

    /**
     * This method lookup for the nested value according to the field path. The field path
     * should have the form field1.field2.field3. The current implementation does not support
     * the array.
     * @param fieldPath
     * @return null or a string value
     */
    public String optStringByPath(String fieldPath) {
        if (fieldPath == null) {
            return null ;
        }
        String[] path = fieldPath.split("\\.");
        String value = null;
        int idx = 0;
        JSONObject jsonObject = this ;
        for (String name : path) {
            if (idx == path.length - 1) {
                //last field and found the value
                if(!jsonObject.has(name)) {
                    return null ;
                }
                value = jsonObject.getString(name);
            } else {
                if (jsonObject.has(name)) {
                    jsonObject = jsonObject.getJSONObject(name);
                } else {
                    //do not find the matched field, the value will be null
                    break;
                }
            }
            idx++;
        }
        return value ;
    }

	/**
	 * @return flattened version of the object.. all the internal objects mapped to the top level trough pathlike keys.
	 */
	public JSONObject flatten() {
		JSONObject jsonObject = new JSONObject();
		for (Entry<String, Object> entry : asMap().entrySet()) {
			Object value = entry.getValue();
            if (value instanceof JsonConstruct) {
				Object flatten = ((JsonConstruct)value).flatten();
				if ( flatten instanceof JSONObject ) {
    				Map<String, Object> map = ((JSONObject)flatten).asMap();
    				for (Entry<String, Object> e : map.entrySet()) {
    					jsonObject.put(entry.getKey() + "." + e.getKey(), e.getValue());
    				}
				} else if ( flatten != null){
				    jsonObject.put(entry.getKey(), flatten);
				}
			} else {
				jsonObject.put(entry.getKey(), value);
			}
		}
		if ( jsonObject.isEmpty()) {
		    return null;
		} else {
		    return jsonObject;
		}
	}

    /**
     * Produce a string from a number.
     * @param  n A Number
     * @return A String.
     * @throws JSONException If n is a non-finite number.
     */
    static public String numberToString(Number n)
    throws JSONException {
        if (n == null) {
            throw new JSONException("Null pointer");
        }
        testValidity(n);

        // Shave off trailing zeros and decimal point, if possible.

        String s = n.toString();
        if (s.indexOf('.') > 0 && s.indexOf('e') < 0 && s.indexOf('E') < 0) {
            while (s.endsWith("0")) {
                s = s.substring(0, s.length() - 1);
            }
            if (s.endsWith(".")) {
                s = s.substring(0, s.length() - 1);
            }
        }
        return s;
    }


    /**
     * Get an optional value associated with a key.
     * @param <T>
     * @param key   A key string.
     * @return      An object which is the value, or null if there is no value.
     */
    @SuppressWarnings("unchecked")
    public <T> T opt(String key) {
        return (T) opt(key, null);
    }
    @SuppressWarnings("unchecked")
    public <T> T opt(String key, T defaultValue) {
        T value = key == null ? null : (T)this.myLinkedHashMap.get(key);
        if ( value == null) {
            value = defaultValue;
        }
        return value;
    }

    /**
     * Get an optional boolean associated with a key.
     * It returns false if there is no such key, or if the value is not
     * Boolean.TRUE or the String "true".
     *
     * @param key   A key string.
     * @return      The truth.
     */
    public boolean optBoolean(String key) {
        return optBoolean(key, false);
    }


    /**
     * Get an optional boolean associated with a key.
     * It returns the defaultValue if there is no such key, or if it is not
     * a Boolean or the String "true" or "false" (case insensitive).
     *
     * @param key              A key string.
     * @param defaultValue     The default.
     * @return      The truth.
     */
    public Boolean optBoolean(String key, Boolean defaultValue) {
        try {
            return getBoolean(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Get an optional double associated with a key,
     * or NaN if there is no such key or if its value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A string which is the key.
     * @return      An object which is the value.
     */
    public Double optDouble(String key) {
        return optDouble(key, Double.NaN);
    }


    /**
     * Get an optional double associated with a key, or the
     * defaultValue if there is no such key or if its value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public Double optDouble(String key, Double defaultValue) {
        try {
            Object o = opt(key);
            if ( o instanceof Double) {
            	return (Double)o;
            } else if (o instanceof Number) {
            	return Double.valueOf(((Number)o).doubleValue());
            } else {
                return Double.valueOf((String)o);
            }
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Get an optional int value associated with a key,
     * or zero if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @return      An object which is the value.
     */
    public int optInt(String key) {
        return optInt(key, 0);
    }


    /**
     * Get an optional int value associated with a key,
     * or the default if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public int optInt(String key, int defaultValue) {
        try {
            return getInt(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Get an optional JSONArray associated with a key.
     * It returns null if there is no such key, or if its value is not a
     * JSONArray.
     *
     * @param key   A key string.
     * @return      A JSONArray which is the value.
     */
    public <T> JSONArray<T> optJSONArray(String key) {
        Object o = opt(key);
        return o instanceof JSONArray ? (JSONArray)o : null;
    }


    /**
     * Get an optional JSONObject associated with a key.
     * It returns null if there is no such key, or if its value is not a
     * JSONObject.
     *
     * @param key   A key string.
     * @return      A JSONObject which is the value.
     */
    public JSONObject optJSONObject(String key) {
        Object o = opt(key);
        return o instanceof JSONObject ? (JSONObject)o : null;
    }


    /**
     * Get an optional long value associated with a key,
     * or zero if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @return      An object which is the value.
     */
    public long optLong(String key) {
        return optLong(key, Long.valueOf(0L));
    }


    /**
     * Get an optional long value associated with a key,
     * or the default if there is no such key or if the value is not a number.
     * If the value is a string, an attempt will be made to evaluate it as
     * a number.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      An object which is the value.
     */
    public Long optLong(String key, Long defaultValue) {
        try {
            return getLong(key);
        } catch (Exception e) {
            return defaultValue;
        }
    }


    /**
     * Get an optional string associated with a key.
     * It returns an empty string if there is no such key. If the value is not
     * a string and is not null, then it is coverted to a string.
     *
     * @param key   A key string.
     * @return      A string which is the value.
     */
    public String optString(String key) {
        return optString(key, "");
    }


    /**
     * Get an optional string associated with a key.
     * It returns the defaultValue if there is no such key.
     *
     * @param key   A key string.
     * @param defaultValue     The default.
     * @return      A string which is the value.
     */
    public String optString(String key, String defaultValue) {
        Object o = opt(key);
        return o != null ? o.toString() : defaultValue;
    }


    /**
     * Put a key/boolean pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value A boolean which is the value.
     * @return this.
     * @throws JSONException If the key is null.
     */
    public JSONObject put(String key, boolean value) throws JSONException {
        put(key, Boolean.valueOf(value));
        return this;
    }


    /**
     * Put a key/double pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value A double which is the value.
     * @return this.
     * @throws JSONException If the key is null or if the number is invalid.
     */
    public JSONObject put(String key, double value) throws JSONException {
        put(key, Double.valueOf(value));
        return this;
    }


    /**
     * Put a key/int pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value An int which is the value.
     * @return this.
     * @throws JSONException If the key is null.
     */
    public JSONObject put(String key, int value) throws JSONException {
        put(key, Integer.valueOf(value));
        return this;
    }


    /**
     * Put a key/long pair in the JSONObject.
     *
     * @param key   A key string.
     * @param value A long which is the value.
     * @return this.
     * @throws JSONException If the key is null.
     */
    public JSONObject put(String key, long value) throws JSONException {
        put(key, Long.valueOf(value));
        return this;
    }


    /**
     * Put a key/value pair in the JSONObject. If the value is null,
     * then the key will be removed from the JSONObject if it is present.
     * @param key   A key string.
     * @param value An object which is the value. It should be of one of these
     *  types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,
     *  or the JSONObject.NULL object.
     * @return this.
     * @throws JSONException If the value is non-finite number
     *  or if the key is null.
     */
    public JSONObject put(String key, Object value) throws JSONException {
        if (key == null) {
            throw new JSONException("Null key.");
        }
        if (value != null) {
            testValidity(value);
            this.myLinkedHashMap.put(key, value);
        } else {
            remove(key);
        }
        return this;
    }


    /**
     * Put a key/value pair in the JSONObject, but only if the
     * key and the value are both non-null.
     * @param key   A key string.
     * @param value An object which is the value. It should be of one of these
     *  types: Boolean, Double, Integer, JSONArray, JSONObject, Long, String,
     *  or the JSONObject.NULL object.
     * @return this.
     * @throws JSONException If the value is a non-finite number.
     */
    public JSONObject putOpt(String key, Object value) throws JSONException {
        if (key != null && value != null) {
            put(key, value);
        }
        return this;
    }


    /**
     * Produce a string in double quotes with backslash sequences in all the
     * right places. A backslash will be inserted within </, allowing JSON
     * text to be delivered in HTML. In JSON text, a string cannot contain a
     * control character or an unescaped quote or backslash.
     * @param string A String
     * @return  A String correctly formatted for insertion in a JSON text.
     */
    public static String quote(String string) {
        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char         b;
        char         c = 0;
        int          len = string.length();
        StringBuffer sb = new StringBuffer(len + 4);
        String       t;

        sb.append('"');
        for (int i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
            case '\\':
            case '"':
                sb.append('\\');
                sb.append(c);
                break;
            case '/':
                if (b == '<') {
                    sb.append('\\');
                }
                sb.append(c);
                break;
            case '\b':
                sb.append("\\b");
                break;
            case '\t':
                sb.append("\\t");
                break;
            case '\n':
                sb.append("\\n");
                break;
            case '\f':
                sb.append("\\f");
                break;
            case '\r':
                sb.append("\\r");
                break;
            default:
                if (c < ' ') {
                    t = "000" + Integer.toHexString(c);
                    sb.append("\\u").append(t.substring(t.length() - 4));
                } else {
                    sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    /**
     * Unquotes a string produced by {@link #quote(String)}.
     * @param string
     * @return unquoted string
     */
    public static String unquote(String string) {
        if (string==null || string.length()==0) {
            return string;
        }
        if (string.startsWith("\"") && string.endsWith("\"")) {
            string = string.substring(1, string.length()-1);
        }

        return StringEscapeUtils.unescapeJavaScript(string);
    }

    /**
     * Remove a name and its value, if present.
     * @param key The name to be removed.
     * @return The value that was associated with the name,
     * or null if there was no value.
     */
    public Object remove(String key) {
        return this.myLinkedHashMap.remove(key);
    }

    /**
     * Throw an exception if the object is an NaN or infinite number.
     * @param o The object to test.
     * @throws JSONException If o is a non-finite number.
     */
    static void testValidity(Object o) throws JSONException {
        if (o != null) {
            if (o instanceof Double) {
                if (((Double)o).isInfinite() || ((Double)o).isNaN()) {
                    throw new JSONException(
                    "JSON does not allow non-finite numbers");
                }
            } else if (o instanceof Float) {
                if (((Float)o).isInfinite() || ((Float)o).isNaN()) {
                    throw new JSONException(
                    "JSON does not allow non-finite numbers.");
                }
            }
        }
    }


    /**
     * Produce a JSONArray containing the values of the members of this
     * JSONObject.
     * @param names A JSONArray containing a list of key strings. This
     * determines the sequence of the values in the result.
     * @return A JSONArray of values.
     * @throws JSONException If any of the values are non-finite numbers.
     */
    public JSONArray toJSONArray(JSONArray names) throws JSONException {
        if (names == null || names.length() == 0) {
            return null;
        }
        JSONArray ja = new JSONArray();
        for (int i = 0; i < names.length(); i += 1) {
            ja.put(this.opt(names.getString(i)));
        }
        return ja;
    }

    /**
     * Make an JSON text of this JSONObject. For compactness, no whitespace
     * is added. If this would not result in a syntactically correct JSON text,
     * then null will be returned instead.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     *
     * @return a printable, displayable, portable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     */
    @Override
    public String toString() {
        try {
            Iterator<String>     keys = keys().iterator();
            StringBuilder sb = new StringBuilder("{");

            while (keys.hasNext()) {
                if (sb.length() > 1) {
                    sb.append(',');
                }
                Object o = keys.next();
                sb.append(quote(o.toString()));
                sb.append(':');
                sb.append(valueToString(this.myLinkedHashMap.get(o)));
            }
            sb.append('}');
            return sb.toString();
        } catch (JSONException e) {
            return null;
        }
    }


    /**
     * Make a prettyprinted JSON text of this JSONObject.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @return a printable, displayable, portable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the object contains an invalid number.
     */
    public String toString(int indentFactor) throws JSONException {
        return toString(indentFactor, 0);
    }


    /**
     * Make a prettyprinted JSON text of this JSONObject.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @param indent The indentation of the top level.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the object contains an invalid number.
     */
    String toString(int indentFactor, int indent) throws JSONException {
        int          i;
        int          n = length();
        if (n == 0) {
            return "{}";
        }
        Iterator<String>     keys = keys().iterator();
        StringBuffer sb = new StringBuffer("{");
        int          newindent = indent + indentFactor;
        Object       o;
        if (n == 1) {
            o = keys.next();
            sb.append(quote(o.toString()));
            sb.append(": ");
            sb.append(valueToString(this.myLinkedHashMap.get(o), indentFactor,
                                    indent));
        } else {
            while (keys.hasNext()) {
                o = keys.next();
                if (sb.length() > 1) {
                    sb.append(",\n");
                } else {
                    sb.append('\n');
                }
                for (i = 0; i < newindent; i += 1) {
                    sb.append(' ');
                }
                sb.append(quote(String.valueOf(o)));
                sb.append(": ");
                sb.append(valueToString(this.myLinkedHashMap.get(o), indentFactor,
                                        newindent));
            }
            if (sb.length() > 1) {
                sb.append('\n');
                for (i = 0; i < indent; i += 1) {
                    sb.append(' ');
                }
            }
        }
        sb.append('}');
        return sb.toString();
    }


    /**
     * Make a JSON text of an object value.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param value The value to be serialized.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the value is or contains an invalid number.
     */
    static String valueToString(Object value) throws JSONException {
        if (value == null || value.equals(null)) { //see JSONObject.NULL
            return STRINGED_NULL;
        }
        if (value instanceof Number) {
            return numberToString((Number) value);
        }
        if (value instanceof String) {
            JsonConstruct jsonConstruct = JsonConstruct.Parser.toJsonConstruct((String) value);
            if (jsonConstruct != null) {
                return valueToString(jsonConstruct);
            }
        }
        if (value instanceof Boolean || value instanceof JSONObject ||
                value instanceof JSONArray) {
            return value.toString();
        }
        return quote(value.toString());
    }


    /**
     * Make a prettyprinted JSON text of an object value.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param value The value to be serialized.
     * @param indentFactor The number of spaces to add to each level of
     *  indentation.
     * @param indent The indentation of the top level.
     * @return a printable, displayable, transmittable
     *  representation of the object, beginning
     *  with <code>{</code>&nbsp;<small>(left brace)</small> and ending
     *  with <code>}</code>&nbsp;<small>(right brace)</small>.
     * @throws JSONException If the object contains an invalid number.
     */
    static String valueToString(Object value, int indentFactor, int indent)
    throws JSONException {
        if (value == null || value.equals(null)) { //see JSONObject.NULL
            return STRINGED_NULL;
        }
        if (value instanceof String) {
            JsonConstruct jsonConstruct = JsonConstruct.Parser.toJsonConstruct((String) value);
            if (jsonConstruct != null) {
                return valueToString(jsonConstruct, indentFactor, indent);
            }
        }
        if (value instanceof Number) {
            return numberToString((Number) value);
        }
        if (value instanceof Boolean) {
            return value.toString();
        }
        if (value instanceof JSONObject) {
            return ((JSONObject)value).toString(indentFactor, indent);
        }
        if (value instanceof JSONArray) {
            return ((JSONArray)value).toString(indentFactor, indent);
        }
        return quote(value.toString());
    }


    /**
     * Write the contents of the JSONObject as JSON text to a writer.
     * For compactness, no whitespace is added.
     *
     * This defines the json object entirely. So any other keys will lose their values.
     * <p>
     * Warning: This method assumes that the data structure is acyclical.
     * @param writer
     *
     * @return The writer.
     * @throws JSONException
     */
    public Writer write(Writer writer) throws JSONException {
        try {
            boolean  b = false;
            Iterator<String> keys = keys().iterator();
            writer.write('{');

            while (keys.hasNext()) {
                if (b) {
                    writer.write(',');
                }
                Object k = keys.next();
                writer.write(quote(k.toString()));
                writer.write(':');
                Object v = this.myLinkedHashMap.get(k);
                if (v instanceof JSONObject) {
                    ((JSONObject)v).write(writer);
                } else if (v instanceof JSONArray) {
                    ((JSONArray)v).write(writer);
                } else {
                    writer.write(valueToString(v));
                }
                b = true;
            }
            writer.write('}');
            return writer;
        } catch (IOException e) {
            throw new JSONException(e);
        }
    }


    /**
     * Write the contents of the JSONObject as js that initializes the given
     * root object.<p/>
     * So, this method generates <code>root.a=true;root.b='text';</code>.<p/>
     * For compactness, no whitespace is added.
     *
     * Note : defines each key as a separate statement so any other keys are not replaced ( different than {@link #write(Writer)} )
     * <p/>
     * Warning: This method assumes that the data structure is acyclical.
     * @param writer
     * @param onlyIfUndefined TODO
     * @param root
     * @return The writer.
     * @throws JSONException
     */
    public Writer writeInitialization(Writer writer, boolean onlyIfUndefined, String root) throws JSONException {
        try {
        	writePathInitialization(writer, root);
            for (String key : keys()) {
            	Object value = this.myLinkedHashMap.get(key);
            	writeConditional(writer, onlyIfUndefined, root+"."+key, value);
            }
            return writer;
        } catch (IOException e) {
            throw new JSONException(e);
        }
    }


	private void writePathInitialization(Writer writer, String root) throws IOException {
		JSONObject empty = new JSONObject();
		if ( root != null ) {
			int dotPos = -1;
			do {
				dotPos++;
				if ( root.charAt(dotPos) == '.') {
					throw new JSONException("double '.', or leading '.' in position "+dotPos+" in string "+root);
				}
				dotPos = root.indexOf('.', dotPos);
				if ( dotPos < 0 ) {
					writeConditional(writer, true, root, empty);
				} else if (dotPos == root.length()-1){
					throw new JSONException("trailing '.' in position "+dotPos+" in string "+root);
				} else {
					writeConditional(writer, true, root.subSequence(0, dotPos).toString(), empty);
				}
			} while(dotPos >0);
		}
	}

    private void writeConditional(Writer writer, boolean onlyIfUndefined, String variableName, Object value) throws IOException {
    	if ( onlyIfUndefined) {
    		writer.write("if(typeof(");
    		writer.write(variableName);
    		writer.write(")==\"undefined\"){");
    	}
    	writer.write(variableName);
    	writer.write('=');
    	if (value instanceof JSONObject) {
    		((JSONObject) value).write(writer);
    	} else if (value instanceof JSONArray<?>) {
    		((JSONArray<?>) value).write(writer);
    	} else {
    		writer.write(valueToString(value));
    	}
    	writer.write(';');
       	if ( onlyIfUndefined) {
       		writer.write("}");
       	}
       	writer.write("\n");
	}


	public Map<String,Object> asMap() {
        return this.myLinkedHashMap;
    }

    public Map<String, String> asStringMap() {
        Map<String, String> stringsMap = new HashMap<String, String>();
        for (Entry<String, Object> entry : myLinkedHashMap.entrySet()) {
            stringsMap.put(entry.getKey(), String.valueOf(entry.getValue()));
        }
        return stringsMap;
    }

    @Override
    public boolean equals(Object o) {
        if ( o == this) {
            return true;
        } else if ( !(o instanceof JSONObject)) {
            return equals(toJsonObject(o));
        } else {
            return this.myLinkedHashMap.equals(((JSONObject)o).myLinkedHashMap);
        }
    }

    public static JSONObject toJsonObject(Object object) {
        if ( object instanceof JSONObject) {
            return (JSONObject)object;
        } else {
            return new JSONObject(ObjectUtils.toString(object, "{}"));
        }
    }


    /**
     * For every key in the passed json object, call {@link #accumulate(String, Object)} to merge the json parameter with this.
     * @param json
     */
    public void merge(JSONObject json) {
        if ( json != null ) {
            for(Map.Entry<String, Object> entry: json.asMap().entrySet()) {
                this.accumulate(entry.getKey(), entry.getValue());
            }
        }
    }


    /**
     * @return true if no data.
     */
    public boolean isEmpty() {
        return this.myLinkedHashMap.isEmpty();
    }

}