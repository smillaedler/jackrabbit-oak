/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jackrabbit.mk.json;

/**
 * A fast Jsop writer / reader.
 */
public class JsopStream implements JsopReader, JsopWriter {

    private boolean needComma;
    private int len, pos, lastPos, valuesLen;
    private int[] tokens = new int[4];
    private Object[] values = new Object[4];

    // write

    public JsopStream append(JsopWriter w) {
        JsopStream s = (JsopStream) w;
        for (int i = s.pos; i < s.len; i++) {
            int token = s.tokens[i];
            switch (token & 255) {
            case JsopReader.STRING:
            case JsopReader.NUMBER:
            case JsopReader.IDENTIFIER:
            case JsopReader.COMMENT:
                Object o = s.values[token >> 8];
                addToken((token & 255) + addValue(o));
                break;
            default:
                addToken(token);
            }
        }
        return this;
    }

    private void addToken(int x) {
        if (tokens.length < len + 1) {
            growTokens();
        }
        tokens[len++] = x;
    }

    private int addValue(Object x) {
        if (values.length < valuesLen + 1) {
            growValues();
        }
        values[valuesLen] = x;
        return valuesLen++ << 8;
    }

    private void growTokens() {
        int[] t2 = new int[tokens.length * 2];
        System.arraycopy(tokens, 0, t2, 0, len);
        tokens = t2;
    }

    private void growValues() {
        Object[] v2 = new Object[values.length * 2];
        System.arraycopy(values, 0, v2, 0, valuesLen);
        values = v2;
    }

    public JsopStream tag(char tag) {
        addToken(tag);
        needComma = false;
        return this;
    }

    public JsopStream array() {
        optionalComma();
        addToken('[');
        needComma = false;
        return this;
    }

    public JsopStream encodedValue(String raw) {
        optionalComma();
        addToken(JsopReader.COMMENT + addValue(raw));
        needComma = true;
        return this;
    }

    public JsopStream endArray() {
        addToken(']');
        needComma = true;
        return this;
    }

    public JsopStream endObject() {
        addToken('}');
        needComma = true;
        return this;
    }

    public JsopStream key(String key) {
        optionalComma();
        addToken(JsopReader.STRING + addValue(key));
        addToken(':');
        needComma = false;
        return this;
    }

    public JsopStream newline() {
        addToken('\n');
        return this;
    }

    public JsopStream object() {
        optionalComma();
        addToken('{');
        needComma = false;
        return this;
    }

    public JsopStream value(String value) {
        optionalComma();
        if (value == null) {
            addToken(JsopReader.NULL);
        } else {
            addToken(JsopReader.STRING + addValue(value));
        }
        needComma = true;
        return this;
    }

    public JsopStream value(long x) {
        optionalComma();
        addToken(JsopReader.NUMBER + addValue(Long.valueOf(x)));
        needComma = true;
        return this;
    }

    public JsopStream value(boolean b) {
        optionalComma();
        addToken(b ? JsopReader.TRUE : JsopReader.FALSE);
        needComma = true;
        return this;
    }

    public void resetReader() {
        pos = 0;
    }

    public void resetWriter() {
        needComma = false;
        len = 0;
    }

    public void setLineLength(int i) {
        // ignore
    }

    private void optionalComma() {
        if (needComma) {
            addToken(',');
        }
    }

    // read

    public String getToken() {
        int x = tokens[lastPos];
        switch (x & 255) {
        case JsopReader.STRING:
        case JsopReader.NUMBER:
        case JsopReader.IDENTIFIER:
        case JsopReader.COMMENT:
            return values[x >> 8].toString();
        case JsopReader.TRUE:
            return "true";
        case JsopReader.FALSE:
            return "false";
        case JsopReader.NULL:
            return "null";
        }
        return Character.toString((char) (x & 255));
    }

    public int getTokenType() {
        return tokens[lastPos] & 255;
    }

    public boolean matches(int type) {
        if (getType() == type) {
            lastPos = pos;
            pos++;
            return true;
        }
        return false;
    }

    private int getType() {
        skipNewline();
        return tokens[pos] & 255;
    }

    public String read(int type) {
        if (matches(type)) {
            return getToken();
        }
        throw new IllegalArgumentException("expected: " + type + " got: " + tokens[pos]);
    }

    public int read() {
        int t = getType();
        lastPos = pos++;
        return t;
    }

    private void skipNewline() {
        while (true) {
            int x = tokens[pos];
            if (x != '\n') {
                return;
            }
            pos++;
        }
    }

    public String readRawValue() {
        skipNewline();
        int x = tokens[pos];
        lastPos = pos++;
        switch (x & 255) {
        case JsopReader.COMMENT:
        case JsopReader.NUMBER:
        case JsopReader.IDENTIFIER:
            return values[x >> 8].toString();
        case JsopReader.STRING:
            return JsopBuilder.encode(values[x >> 8].toString());
        case JsopReader.TRUE:
            return "true";
        case JsopReader.FALSE:
            return "false";
        case JsopReader.NULL:
            return "null";
        case '[':
            StringBuilder buff = new StringBuilder();
            buff.append('[');
            while (true) {
                String s = readRawValue();
                buff.append(s);
                if ("]".equals(s)) {
                    break;
                }
            }
            return buff.toString();
        }
        return Character.toString((char) (x & 255));
    }

    public String readString() {
        return read(JsopReader.STRING);
    }

    public String toString() {
        JsopBuilder buff = new JsopBuilder();
        for (int i = 0; i < len; i++) {
            int x = tokens[i];
            switch (x & 255) {
            case '{':
                buff.object();
                break;
            case '}':
                buff.endObject();
                break;
            case '[':
                buff.array();
                break;
            case ']':
                buff.endArray();
                break;
            case JsopReader.STRING:
                buff.value(values[x >> 8].toString());
                break;
            case JsopReader.TRUE:
                buff.value(true);
                break;
            case JsopReader.FALSE:
                buff.value(false);
                break;
            case JsopReader.NULL:
                buff.value(null);
                break;
            case JsopReader.IDENTIFIER:
            case JsopReader.NUMBER:
            case JsopReader.COMMENT:
                buff.encodedValue(values[x >> 8].toString());
                break;
            default:
                buff.tag((char) (x & 255));
            }
        }
        return buff.toString();
    }

}
