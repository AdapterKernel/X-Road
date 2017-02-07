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

import java.util.Collections;

import com.google.common.collect.Sets;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import ee.ria.xroad.common.CodedException;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.util.TimeUtils;
import ee.ria.xroad.opmonitordaemon.message.GetSecurityServerOperationalDataResponseType;

import static org.junit.Assert.assertNotNull;


/**
 * Tests for verifying query request handler behavior.
 */
public class OperationalDataRequestHandlerTest extends BaseTestUsingDB {

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Test
    public void checkOkSearchCriteriaOutputFields() throws Exception {
        OperationalDataRequestHandler.checkOutputFields(Collections.emptySet());
        OperationalDataRequestHandler.checkOutputFields(Sets.newHashSet(
                "monitoringDataTs", "securityServerInternalIp"));
    }

    @Test
    public void checkInvalidSearchCriteriaOutputFields() throws Exception {
        thrown.expect(CodedException.class);
        thrown.expectMessage(
                "Unknown output field in search criteria: UNKNOWN-FIELD");

        OperationalDataRequestHandler.checkOutputFields(Sets.newHashSet(
                "monitoringDataTs", "UNKNOWN-FIELD"));
    }

    @Test
    public void buildOperationalDataResponseWithNotAvailableRecordsTo()
            throws Exception {
        ClientId client = ClientId.create(
                "XTEE-CI-XM", "00000001", "GOV", "System1");
        OperationalDataRequestHandler handler =
                new OperationalDataRequestHandler();
        long recordsAvailableBefore = TimeUtils.getEpochSecond();

        GetSecurityServerOperationalDataResponseType response = handler
                .buildOperationalDataResponse(client, 1474968960L,
                        recordsAvailableBefore + 10, null,
                        Collections.emptySet(), recordsAvailableBefore);

        assertNotNull(response.getNextRecordsFrom());
    }

    @Test
    public void checkNegativeRecordsFromTimestamps() throws Exception {
        thrown.expect(CodedException.class);
        thrown.expectMessage("Records from timestamp is a negative number");

        OperationalDataRequestHandler.checkTimestamps(-10, 10, 10);
    }

    @Test
    public void checkNegativeRecordsToTimestamps() throws Exception {
        thrown.expect(CodedException.class);
        thrown.expectMessage("Records to timestamp is a negative number");

        OperationalDataRequestHandler.checkTimestamps(10, -10, 10);
    }

    @Test
    public void checkEarlierRecordsToTimestamps() throws Exception {
        thrown.expect(CodedException.class);
        thrown.expectMessage("Records to timestamp is earlier than records"
                + " from timestamp");

        OperationalDataRequestHandler.checkTimestamps(10, 5, 10);
    }

    @Test
    public void checkRecordsNotAvailable() throws Exception {
        thrown.expect(CodedException.class);
        thrown.expectMessage("Records not available from " + 10 + " yet");

        OperationalDataRequestHandler.checkTimestamps(10, 10, 5);
    }
}
