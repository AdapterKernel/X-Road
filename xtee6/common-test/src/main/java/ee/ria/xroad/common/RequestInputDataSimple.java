/**
 * The MIT License
 * Copyright (c) 2015 Estonian Information System Authority (RIA), Population Register Centre (VRK)
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
package ee.ria.xroad.common;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import org.apache.commons.lang3.tuple.Pair;

/**
 * Encapsulates necessary information about a simple request.
 */
public class RequestInputDataSimple extends RequestInputData {

    private String contentType;

    /**
     * Creates a simple request.
     * @param clientUrl the client URL
     * @param testRequest the test request
     * @param contentType content type of the request
     */
    public RequestInputDataSimple(String clientUrl, TestRequest testRequest,
            String contentType) {
        super(clientUrl, testRequest);
        this.contentType = contentType;
    }

    @Override
    public Pair<String, InputStream> getRequestInput() {
        try {
            return Pair.of(contentType, getRequestContentInputStream());
        } catch (Exception e) {
            throw new RuntimeException("Cannot create request input", e);
        }
    }

    private InputStream getRequestContentInputStream() throws Exception {
        return new ByteArrayInputStream(testRequest.getContent().getBytes());
    }
}
