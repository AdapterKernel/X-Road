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
package ee.ria.xroad.proxy.testsuite.testcases;

import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.actor.UntypedActor;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import ee.ria.xroad.common.conf.globalconf.GlobalConf;
import ee.ria.xroad.common.conf.serverconf.ServerConf;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.SecurityServerId;
import ee.ria.xroad.common.message.SoapMessageImpl;
import ee.ria.xroad.monitor.common.SystemMetricsRequest;
import ee.ria.xroad.monitor.common.SystemMetricsResponse;
import ee.ria.xroad.monitor.common.dto.HistogramDto;
import ee.ria.xroad.monitor.common.dto.MetricSetDto;
import ee.ria.xroad.proxy.conf.KeyConf;
import ee.ria.xroad.proxy.testsuite.*;
import ee.ria.xroad.proxy.util.MetaserviceTestUtil;
import ee.ria.xroad.proxymonitor.message.GetSecurityServerMetricsResponse;
import ee.ria.xroad.proxymonitor.message.HistogramMetricType;
import ee.ria.xroad.proxymonitor.message.MetricSetType;
import ee.ria.xroad.proxymonitor.message.MetricType;
import lombok.extern.slf4j.Slf4j;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.SOAPBody;
import java.math.BigDecimal;
import java.nio.file.Paths;
import java.util.List;

import static ee.ria.xroad.proxy.util.MetaserviceTestUtil.verifyAndGetSingleBodyElementOfType;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Test member list retrieval
 * Result: client receives a list of members.
 */
@Slf4j
public class SecurityServerMetricsMessage extends MessageTestCase {

    private static final String EXPECTED_XR_INSTANCE = "EE";
    private static final ClientId DEFAULT_OWNER_CLIENT = ClientId.create(EXPECTED_XR_INSTANCE, "BUSINESS",
            "producer");

    private static final SecurityServerId DEFAULT_OWNER_SERVER =
            SecurityServerId.create(DEFAULT_OWNER_CLIENT, "ownerServer");

    private static final ActorSystem ACTOR_SYSTEM
            = ActorSystem.create("xroad-monitor", loadAkkaConfiguration());

    private static final String EXPECTED_METRIC_SET_NAME = "someMetricSet";
    private static final double MIN_VALUE = 0.125;
    private static final BigDecimal EXPECTED_RESPONSE_MIN_VALUE = BigDecimal.valueOf(MIN_VALUE);
    private static final double MAX_VALUE = 500.143;
    private static final BigDecimal EXPECTED_RESPONSE_MAX_VALUE = BigDecimal.valueOf(MAX_VALUE);

    private static Unmarshaller unmarshaller;


    /**
     * Constructs the test case.
     */
    public SecurityServerMetricsMessage() {
        this.requestFileName = "getMetrics.query";
    }



    @Override
    protected void validateNormalResponse(Message receivedResponse)
            throws Exception {

        SoapMessageImpl soap = (SoapMessageImpl) receivedResponse.parse().getSoap();
        SOAPBody body = soap.getSoap().getSOAPBody();

        // the content type might arrive without the space,
        // even though the MetadataServiceHandler uses the same MimeUtils value
        List<String> expectedContentTypes = MetaserviceTestUtil.xmlUtf8ContentTypes();

        assertThat("Wrong content type", receivedResponse.getContentType(), isIn(expectedContentTypes));

        final MetricSetType rootSet =
                verifyAndGetSingleBodyElementOfType(body,
                        GetSecurityServerMetricsResponse.class,
                        () -> unmarshaller).getMetricSet();

        assertThat("Wrong root name", rootSet.getName(), is(DEFAULT_OWNER_SERVER.toString()));
        assertThat("Wrong amount of received metrics", rootSet.getMetrics().size(), is(2));

        final MetricType proxyVersionMetric = rootSet.getMetrics().get(0);

        assertThat("Missing proxy version from response", proxyVersionMetric.getName(),
                equalTo("proxyVersion"));

        final MetricType containingMetric = rootSet.getMetrics().get(1);

        assertThat("Wrong name on metric set", containingMetric.getName(), equalTo(EXPECTED_METRIC_SET_NAME));

        assertThat("Was expecting a set of data", containingMetric, instanceOf(MetricSetType.class));
        MetricSetType metricSet = (MetricSetType) containingMetric;


        HistogramMetricType histogram = (HistogramMetricType) metricSet.getMetrics().get(0);
        assertThat("Wrong min value", histogram.getMin(), is(EXPECTED_RESPONSE_MIN_VALUE));
        assertThat("Wrong max value", histogram.getMax(), is(EXPECTED_RESPONSE_MAX_VALUE));
    }

    @Override
    protected void startUp() throws Exception {
        super.startUp();

        GlobalConf.reload(new TestGlobalConf() {
            @Override
            public String getInstanceIdentifier() {
                return EXPECTED_XR_INSTANCE;
            }
        });
        KeyConf.reload(new TestKeyConf());
        ServerConf.reload(new TestServerConf() {
            @Override
            public SecurityServerId getIdentifier() {
                return DEFAULT_OWNER_SERVER;
            }
        });

        unmarshaller = JAXBContext.newInstance(GetSecurityServerMetricsResponse.class).createUnmarshaller();

        ACTOR_SYSTEM.actorOf(Props.create(MockMetricsProvider.class), "MetricsProviderActor");
    }

    @Override
    protected void closeDown() throws Exception {
        ACTOR_SYSTEM.terminate();
    }

    private static SystemMetricsResponse createMetricsResponse() {
        HistogramDto histogramDto = new HistogramDto("exampleHistogram",
                75, 95, 98, 99,
                99.9, MAX_VALUE, 50, 51, MIN_VALUE, 2);
        MetricSetDto.Builder builder = new MetricSetDto.Builder(EXPECTED_METRIC_SET_NAME);
        builder.withMetric(histogramDto);
        return new SystemMetricsResponse(builder.build());
    }


    private static Config loadAkkaConfiguration() {
        Config config = ConfigFactory.parseFile(Paths.get("src/test/resources/application.conf").toFile());
        return ConfigFactory.load(config);
    }

    /**
     * Mock provider for metrics data
     */
    public static class MockMetricsProvider extends UntypedActor {

        @Override
        public void onReceive(Object message) throws Throwable {
            if (message instanceof SystemMetricsRequest) {
                getSender().tell(createMetricsResponse(), getSelf());

            } else {
                unhandled(message);
            }

        }
    }
}
