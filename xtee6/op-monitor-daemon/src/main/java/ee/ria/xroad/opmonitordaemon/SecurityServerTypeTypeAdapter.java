/**
 * The MIT License
 * Copyright (c) 2016 Estonian Information System Authority (RIA), Population Register Centre (VRK)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package ee.ria.xroad.opmonitordaemon;

import java.io.IOException;

import com.google.gson.TypeAdapter;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import ee.ria.xroad.common.opmonitoring.OpMonitoringData;

/**
 * Type adapter for the securityServerType field used with the JsonAdapter annotation.
 *
 * We use this type adapter to ensure an exception is thrown if the given value cannot
 * be converted to any values defined in SecurityServerType, instead of silently setting
 * the securityServerField to null.
 */
class SecurityServerTypeTypeAdapter extends TypeAdapter<String> {

    @Override
    public String read(final JsonReader in) throws IOException {
        String value = in.nextString();

        if (OpMonitoringData.SecurityServerType.fromString(value) == null) {
            throw new RuntimeException("Invalid value of securityServerType");
        }

        return value;
    }

    @Override
    public void write(JsonWriter out, String value) throws IOException {
        out.value(value);
    }
}
