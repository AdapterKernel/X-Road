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

import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.servlet.http.HttpServletRequest;

import com.codahale.metrics.MetricRegistry;
import com.google.gson.Gson;

import lombok.extern.slf4j.Slf4j;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;

import ee.ria.xroad.common.util.JsonUtils;

import static ee.ria.xroad.common.util.TimeUtils.getEpochSecond;

/**
 * The processor class for store operational monitoring data (JSON) requests.
 */
@Slf4j
class StoreRequestProcessor {

    private static final Gson GSON = JsonUtils.getSerializer();

    /** The servlet request. */
    private HttpServletRequest servletRequest;

    /** The registry of health data. */
    private MetricRegistry healthMetricRegistry;

    StoreRequestProcessor(HttpServletRequest servletRequest,
            MetricRegistry healthMetricRegistry) {
        this.servletRequest = servletRequest;
        this.healthMetricRegistry = healthMetricRegistry;
    }

    /**
     * Processes the incoming message: stores the data and updates the related
     * statistics.
     * @throws Exception in case of any errors
     */
    void process() throws Exception {
        String rawJson = IOUtils.toString(servletRequest.getInputStream(),
                StandardCharsets.UTF_8);

        log.trace("Incoming JSON: {}", rawJson);

        List<OperationalDataRecord> records = prepareRawStoreData(rawJson);

        log.debug("Process {} record{}", records.size(),
                records.size() == 1 ? "" : "s");

        OperationalDataRecordManager.storeRecords(records, getEpochSecond());

        HealthDataMetrics.processRecords(healthMetricRegistry, records);
    }

    // Get usable operational data to be stored. If no such data is found,
    // send an error message right away.
    private static List<OperationalDataRecord> prepareRawStoreData(
            String rawJsonData) throws Exception {
        if (StringUtils.isBlank(rawJsonData)) {
            throw new Exception(
                    "No data was found in the request to store data");
        }

        OperationalDataRecords records;

        try {
            records = GSON.fromJson(rawJsonData, OperationalDataRecords.class);
        } catch (Exception e) {
            throw new Exception("Received invalid request", e);
        }

        return records.getRecords();
    }

}
