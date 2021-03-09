package com.mochuan.github.v8;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.eclipsesource.v8.Releasable;
import com.eclipsesource.v8.V8Array;
import com.eclipsesource.v8.V8Object;
import com.eclipsesource.v8.V8Value;

import java.nio.ByteBuffer;

/**
 * @version mochuan.zhb on 2020-12-30
 * @Author Zheng Haibo
 * @Blog github.com/nuptboyzhb
 * @Company Alibaba Group
 * @Description v8 工具类
 */
public class XV8Utils {

    static final int MAX_DEPTH = 200;


    /**
     * v8对象转JSONObject
     *
     * @param v8Object
     * @return
     */
    public static JSONObject toJSONObject(V8Object v8Object) {
        try {
            JSONObject jsonObject = (JSONObject) v8toJSON(v8Object, 0);
            return jsonObject;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }


    /**
     * V8数组转JSONArray
     *
     * @param v8Array
     * @return
     */
    public static JSONArray toJSONArray(V8Array v8Array) {
        try {
            JSONArray jsonArray = (JSONArray) v8toJSON(v8Array, 0);
            return jsonArray;
        } catch (Throwable t) {
            t.printStackTrace();
        }
        return null;
    }


    private static JSON v8toJSON(V8Value src, int depth) {
        if (src == null || src.isUndefined()) return null;

        if (depth >= MAX_DEPTH) {
            throw new IllegalArgumentException("Failed to convert V8 to JSON - Exceed max depth (" + MAX_DEPTH + ")");
        }


        if (src instanceof V8Array) {
            JSONArray json = new JSONArray();
            V8Array jsarray = (V8Array) src;
            for (int i = 0; i < jsarray.length(); i++) {
                Object v = jsarray.get(i);

                if (v instanceof ByteBuffer) {
                    json.add(copyArrayBuffer(v));
                } else if (v instanceof V8Value) {
                    json.add(v8toJSON((V8Value) v, depth + 1));
                } else if (v instanceof Double) {
                    json.add(copyNumber(v));
                } else {
                    json.add(v);
                }

                if (v != null && (v instanceof Releasable)) {
                    ((Releasable) v).release();
                }
            }
            jsarray.close();
            return json;
        }
        if (src instanceof V8Object) {
            JSONObject json = new JSONObject();
            V8Object jsobj = (V8Object) src;
            String[] keys = jsobj.getKeys();
            for (int i = 0; keys != null && i < keys.length; i++) {
                String key = keys[i];
                Object v = jsobj.get(key);

                if (v instanceof ByteBuffer) {
                    json.put(key, copyArrayBuffer(v));
                } else if (v instanceof V8Value) {
                    json.put(key, v8toJSON((V8Value) v, depth + 1));
                } else if (v instanceof Double) {
                    json.put(key, copyNumber(v));
                } else {
                    json.put(key, v);
                }

                if (v != null && (v instanceof Releasable)) {
                    ((Releasable) v).release();
                }
            }
            jsobj.close();
            return json;
        }
        return null;
    }

    private static byte[] copyArrayBuffer(Object v) {
        ByteBuffer bb = (ByteBuffer) v;
        int length = bb.capacity();
        byte[] buf = new byte[length];
        bb.get(buf);
        return buf;
    }

    private static Object copyNumber(Object v) {
        Double obj = (Double) v;
        double d = obj.doubleValue();
        if (Long.MIN_VALUE < d && d < Long.MAX_VALUE) {
            long i = obj.longValue();
            if ((double) i == d) {
                return Long.valueOf(i);
            }
        }
        return v;
    }

}
