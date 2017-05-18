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
package ee.ria.xroad.proxy.serverproxy;

import ee.ria.xroad.common.CodedException;
import ee.ria.xroad.common.ErrorCodes;
import ee.ria.xroad.common.SystemProperties;
import ee.ria.xroad.common.conf.globalconf.GlobalConf;
import ee.ria.xroad.common.conf.serverconf.ServerConf;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.SecurityServerId;
import ee.ria.xroad.common.identifier.ServiceId;
import ee.ria.xroad.common.message.SoapHeader;
import ee.ria.xroad.common.message.SoapMessageImpl;
import ee.ria.xroad.common.metadata.ObjectFactory;
import ee.ria.xroad.common.opmonitoring.OpMonitoringData;
import ee.ria.xroad.common.util.MimeTypes;
import ee.ria.xroad.proxy.conf.KeyConf;
import ee.ria.xroad.proxy.protocol.ProxyMessage;
import ee.ria.xroad.proxy.testsuite.TestGlobalConf;
import ee.ria.xroad.proxy.testsuite.TestKeyConf;
import ee.ria.xroad.proxy.testsuite.TestServerConf;
import ee.ria.xroad.proxymonitor.RestoreMonitorClientAfterTest;
import ee.ria.xroad.proxymonitor.message.GetSecurityServerMetricsResponse;
import ee.ria.xroad.proxymonitor.message.MetricSetType;
import ee.ria.xroad.proxymonitor.message.MetricType;
import ee.ria.xroad.proxymonitor.message.StringMetricType;
import ee.ria.xroad.proxymonitor.util.MonitorClient;
import org.apache.http.client.HttpClient;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.ProvideSystemProperty;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.servlet.http.HttpServletRequest;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.soap.MessageFactory;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;
import java.io.IOException;
import java.util.List;

import static ee.ria.xroad.common.message.SoapMessageTestUtil.build;
import static ee.ria.xroad.common.util.MimeTypes.TEXT_XML_UTF8;
import static ee.ria.xroad.proxy.util.MetaserviceTestUtil.CodedExceptionMatcher.faultCodeEquals;
import static ee.ria.xroad.proxy.util.MetaserviceTestUtil.verifyAndGetSingleBodyElementOfType;
import static java.util.UUID.randomUUID;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
 * Unit tests for {@link ProxyMonitorServiceHandlerImpl}
 */
@RunWith(PowerMockRunner.class)
@PrepareForTest(MonitorClient.class)
public class ProxyMonitorServiceHandlerTest {

    private static final String EXPECTED_XR_INSTANCE = "EE";
    private static final ClientId DEFAULT_OWNER_CLIENT = ClientId.create(EXPECTED_XR_INSTANCE, "GOV",
            "1234TEST_CLIENT");

    private static final SecurityServerId DEFAULT_OWNER_SERVER =
            SecurityServerId.create(DEFAULT_OWNER_CLIENT, "ownerServer");

    private static final ServiceId MONITOR_SERVICE_ID = ServiceId.create(DEFAULT_OWNER_CLIENT,
            ProxyMonitorServiceHandlerImpl.SERVICE_CODE);

    private static Unmarshaller unmarshaller;
    private static MessageFactory messageFactory;

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    @Rule
    public final ProvideSystemProperty hibernatePropertiesProperty
            = new ProvideSystemProperty(SystemProperties.DATABASE_PROPERTIES,
            "src/test/resources/hibernate.properties");

    @Rule
    public final ProvideSystemProperty configurationPathProperty
            = new ProvideSystemProperty(SystemProperties.CONFIGURATION_PATH,
            "src/test/resources/");

    @Rule
    public final RestoreMonitorClientAfterTest monitorClientRestoreRule = new RestoreMonitorClientAfterTest();

    private HttpServletRequest mockRequest;
    private ProxyMessage mockProxyMessage;

    /**
     * Init class-wide test instances
     */
    @BeforeClass
    public static void initCommon() throws JAXBException, SOAPException {
        unmarshaller = JAXBContext.newInstance(ObjectFactory.class, SoapHeader.class,
                GetSecurityServerMetricsResponse.class)
                .createUnmarshaller();
        messageFactory = MessageFactory.newInstance();
    }

    /**
     * Init data for tests
     */
    @Before
    public void init() throws IOException {

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

        mockRequest = mock(HttpServletRequest.class);
        mockProxyMessage = mock(ProxyMessage.class);

        when(mockProxyMessage.getSoapContentType()).thenReturn(MimeTypes.TEXT_XML_UTF8);
    }

    @Test
    public void shouldNotBeAbleToHandleWithIncorrectServiceCode() throws Exception {

        // setup
        ProxyMonitorServiceHandlerImpl handlerToTest = new ProxyMonitorServiceHandlerImpl();

        final ServiceId requestedService = ServiceId.create(DEFAULT_OWNER_CLIENT, "theWrongService");

        // execution & verification
        assertThat("Should not be able to handle wrong service",
                handlerToTest.canHandle(requestedService, mockProxyMessage), is(false));
    }

    @Test
    public void shouldBeAbleToHandleWithCorrectServiceCode() throws Exception {

        // setup
        ProxyMonitorServiceHandlerImpl handlerToTest = new ProxyMonitorServiceHandlerImpl();

        // execution & verification
        assertThat("Should be able to handle the right service",
                handlerToTest.canHandle(MONITOR_SERVICE_ID, mockProxyMessage), is(true));
    }

    @Test
    public void shouldNotNeedToVerifyWhenOwnerCalling() throws Exception {

        // setup

        ProxyMonitorServiceHandlerImpl handlerToTest = new ProxyMonitorServiceHandlerImpl();

        final SoapMessageImpl mockSoap = mock(SoapMessageImpl.class);
        when(mockSoap.getClient()).thenReturn(DEFAULT_OWNER_CLIENT);

        when(mockProxyMessage.getSoap()).thenReturn(mockSoap);

        // execution & verification
        handlerToTest.canHandle(MONITOR_SERVICE_ID, mockProxyMessage);

        assertThat("Should not need a verification", handlerToTest.shouldVerifyAccess(), is(false));
    }

    @Test
    public void shouldNotNeedToVerifyWhenAllowedMonitoringClientCalling() throws Exception {

        // setup

        ProxyMonitorServiceHandlerImpl handlerToTest = new ProxyMonitorServiceHandlerImpl();

        // the allowed monitoring client from test resources monitoring params
        final ClientId allowedClient = ClientId.create(EXPECTED_XR_INSTANCE, "BUSINESS",
                "producer");

        final SoapMessageImpl mockSoap = mock(SoapMessageImpl.class);
        when(mockSoap.getClient()).thenReturn(allowedClient);

        when(mockProxyMessage.getSoap()).thenReturn(mockSoap);

        handlerToTest.canHandle(MONITOR_SERVICE_ID, mockProxyMessage);

        // execution & verification
        assertThat("Should not need a verification", handlerToTest.shouldVerifyAccess(), is(false));
    }

    @Test
    public void shouldThrowWhenAccessNotAllowed() throws Exception {

        // setup

        ProxyMonitorServiceHandlerImpl handlerToTest = new ProxyMonitorServiceHandlerImpl();

        // the allowed monitoring client from test resources monitoring params
        final ClientId nonAllowedClient = ClientId.create(EXPECTED_XR_INSTANCE, "COM",
                "justSomeClient");

        final SoapMessageImpl mockSoap = mock(SoapMessageImpl.class);
        when(mockSoap.getClient()).thenReturn(nonAllowedClient);

        when(mockProxyMessage.getSoap()).thenReturn(mockSoap);

        thrown.expect(CodedException.class);
        thrown.expect(faultCodeEquals(ErrorCodes.X_ACCESS_DENIED));
        thrown.expectMessage(containsString("Request is not allowed"));

        handlerToTest.canHandle(MONITOR_SERVICE_ID, mockProxyMessage);

        // execution
        handlerToTest.shouldVerifyAccess();

        // expecting an exception..
    }

    @Test
    public void startHandingShouldProduceMetrics() throws Exception {

        // setup
        ProxyMonitorServiceHandlerImpl handlerToTest = new ProxyMonitorServiceHandlerImpl();

        final SoapMessageImpl soapMessage = build(DEFAULT_OWNER_CLIENT, MONITOR_SERVICE_ID,
                "testUser", randomUUID().toString());

        when(mockProxyMessage.getSoap()).thenReturn(soapMessage);

        handlerToTest.canHandle(MONITOR_SERVICE_ID, mockProxyMessage);

        final String expectedMetricsSetName = "someName";

        MetricSetType metricSetType = new MetricSetType();
        metricSetType.setName(expectedMetricsSetName);

        final List<MetricType> metrics = metricSetType.getMetrics();

        StringMetricType type = new StringMetricType();
        final String expectedMetricName = "metricName123-23";
        type.setName(expectedMetricName);

        final String expectedMetricValue = "123SomeValue";
        type.setValue(expectedMetricValue);
        metrics.add(type);

        MonitorClient mockMonitorClient = PowerMockito.mock(MonitorClient.class);
        PowerMockito.when(mockMonitorClient.getMetrics()).thenReturn(metricSetType);

        RestoreMonitorClientAfterTest.setMonitorClient(mockMonitorClient);

        // execution
        handlerToTest.startHandling(mockRequest, mockProxyMessage, mock(HttpClient.class),
                mock(OpMonitoringData.class));

        //verification
        assertThat("Wrong content type", handlerToTest.getResponseContentType(), is(TEXT_XML_UTF8));

        final SOAPMessage message = messageFactory.createMessage(null, handlerToTest.getResponseContent());

        final SoapHeader xrHeader = unmarshaller.unmarshal(message.getSOAPHeader(), SoapHeader.class).getValue();

        assertThat("Response client does not match", xrHeader.getClient(), is(DEFAULT_OWNER_CLIENT));
        assertThat("Response client does not match", xrHeader.getService(), is(MONITOR_SERVICE_ID));

        final MetricSetType root = verifyAndGetSingleBodyElementOfType(message.getSOAPBody(),
                GetSecurityServerMetricsResponse.class, () -> unmarshaller).getMetricSet();
        // root metrics should have the security server id as name
        assertThat("Metrics set name does not match", root.getName(), is(DEFAULT_OWNER_SERVER.toString()));

        final List<MetricType> responseMetrics = root.getMetrics();
        assertThat("Missing proxy version from metrics", responseMetrics,
                hasItem(hasProperty("name", is("proxyVersion"))));

        assertThat("Missing the expected metrics set",
                responseMetrics, hasItem(instanceOf(MetricSetType.class)));

        final MetricSetType responseDataMetrics = (MetricSetType) responseMetrics.stream() // we just asserted this..
                .filter(m -> m instanceof MetricSetType).findFirst().orElseThrow(IllegalStateException::new);

        assertThat(responseDataMetrics.getName(), is(expectedMetricsSetName));
        assertThat(responseDataMetrics.getMetrics().size(), is(1));

        final StringMetricType responseMetric = (StringMetricType) responseDataMetrics.getMetrics().get(0);
        assertThat("Wrong metric name", responseMetric.getName(), is(expectedMetricName));
        assertThat("Wrong metric value", responseMetric.getValue(), is(expectedMetricValue));
    }

}
