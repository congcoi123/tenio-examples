/*
The MIT License

Copyright (c) 2016-2026 kong <congcoi123@gmail.com>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/

package com.tenio.examples.example14;

import com.tenio.common.data.DataCollection;
import com.tenio.common.data.DataType;
import com.tenio.common.data.msgpack.element.MsgPackMap;
import com.tenio.common.data.zero.ZeroArray;
import com.tenio.common.data.zero.ZeroElement;
import com.tenio.common.data.zero.ZeroMap;
import com.tenio.common.data.zero.ZeroType;
import com.tenio.common.data.zero.utility.ZeroUtility;
import com.tenio.core.entity.Player;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Map;

/**
 * Application protocol bridge; server code has no dependency on the Java client SDK.
 */
public final class ShowcaseWire {

    private ShowcaseWire() {
    }

    /**
     * Adapts either core map format to the demonstration Zero command view.
     *
     * @param value incoming core payload
     * @return the map with a validated integral short operation code when present
     * @throws IllegalArgumentException if the payload or operation code is invalid
     */
    public static ZeroMap request(DataCollection value) {
        if (value instanceof ZeroMap map) {
            return map;
        }
        if (!(value instanceof MsgPackMap map)) {
            throw new IllegalArgumentException("Map request expected");
        }
        ZeroMap result = zero(map);
        if (map.containsKey("op")) {
            Object code = map.get("op");
            if (!(code instanceof Byte
                    || code instanceof Short
                    || code instanceof Integer
                    || code instanceof Long)
                    || ((Number) code).longValue() < Short.MIN_VALUE
                    || ((Number) code).longValue() > Short.MAX_VALUE) {
                throw new IllegalArgumentException("Invalid operation");
            }
            result.putShort("op", ((Number) code).shortValue());
        }
        return result;
    }

    private static ZeroMap zero(Map<?, ?> map) {
        var result = ZeroUtility.newZeroMap();
        for (var entry : map.entrySet()) {
            if (!(entry.getKey() instanceof String key)) {
                throw new IllegalArgumentException("String key expected");
            }
            result.putZeroElement(key, element(entry.getValue()));
        }
        return result;
    }

    private static ZeroElement element(Object v) {
        if (v == null) {
            return ZeroUtility.newZeroElement(ZeroType.NULL, null);
        }
        ZeroType type = v instanceof Boolean
                        ? ZeroType.BOOLEAN
                        : v instanceof Byte
                        ? ZeroType.BYTE
                        : v instanceof Short
                        ? ZeroType.SHORT
                        : v instanceof Integer
                        ? ZeroType.INTEGER
                        : v instanceof Long
                        ? ZeroType.LONG
                        : v instanceof Float
                        ? ZeroType.FLOAT
                        : v instanceof Double
                        ? ZeroType.DOUBLE
                        : v instanceof String
                        ? ZeroType.STRING
                        : v instanceof byte[] ? ZeroType.BYTE_ARRAY : null;
        if (type != null) {
            return ZeroUtility.newZeroElement(type, v);
        }
        if (v instanceof Map<?, ?> map) {
            return ZeroUtility.newZeroElement(ZeroType.ZERO_MAP, zero(map));
        }
        if (v.getClass().isArray()) {
            var result = ZeroUtility.newZeroArray();
            for (int i = 0; i < Array.getLength(v); i++) {
                result.addZeroElement(element(Array.get(v, i)));
            }
            return ZeroUtility.newZeroElement(ZeroType.ZERO_ARRAY, result);
        }
        throw new IllegalArgumentException("Unsupported request field");
    }

    /**
     * Adapts the response to the serialization format selected by the recipient.
     *
     * @param value  outgoing demonstration map
     * @param player recipient with its negotiated example format
     * @return the response collection in the recipient format
     */
    public static DataCollection response(ZeroMap value, Player player) {
        return player.getProperty("sdk-format") == DataType.MSG_PACK ? msg(value) : value;
    }

    private static MsgPackMap msg(ZeroMap value) {
        var result = MsgPackMap.newInstance();
        for (String key : value.getKeys()) {
            result.put(key, raw(value.getZeroElement(key).getData()));
        }
        return result;
    }

    private static Object raw(Object value) {
        if (value instanceof ZeroMap map) {
            return msg(map);
        }
        if (value instanceof ZeroArray array) {
            var result = new ArrayList<>();
            for (int i = 0; i < array.size(); i++) {
                result.add(raw(array.getZeroElement(i).getData()));
            }
            return result;
        }
        return value;
    }

    /**
     * Reads an integral protocol field without floating-point truncation.
     *
     * @param data source map
     * @param key  integral field name
     * @return the exact integral value as a long
     * @throws IllegalArgumentException if the value is not an integral Java type
     */
    public static long number(ZeroMap data, String key) {
        Object value = data.getZeroElement(key).getData();
        if (!(value instanceof Byte
                || value instanceof Short
                || value instanceof Integer
                || value instanceof Long)) {
            throw new IllegalArgumentException("Integer required");
        }
        return ((Number) value).longValue();
    }
}
