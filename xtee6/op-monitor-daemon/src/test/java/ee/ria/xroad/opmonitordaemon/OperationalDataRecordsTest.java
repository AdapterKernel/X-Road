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

import java.util.ArrayList;
import java.util.List;

import com.google.gson.Gson;

import org.junit.Test;

import ee.ria.xroad.common.util.JsonUtils;

import static org.junit.Assert.assertEquals;

/**
 * Tests operational data records payload.
 */
public class OperationalDataRecordsTest {

    private static final Gson GSON = JsonUtils.getSerializer();

    /**
     * Test empty records payload.
     * @throws Exception if an error occurs.
     */
    @Test
    public void emptyRecordsPayload() throws Exception {
        List<OperationalDataRecord> recordList = new ArrayList<>();
        OperationalDataRecords records = new OperationalDataRecords(recordList);

        recordList.add(new OperationalDataRecord());
        recordList.add(new OperationalDataRecord());

        assertEquals("{\"records\":[{},{}]}", records.getPayload(GSON));
    }
}
