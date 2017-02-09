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
 package ee.ria.xroad.common.certificateprofile.impl;

import static org.bouncycastle.util.Arrays.areEqual;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.junit.Test;
import org.mockito.Mockito;

import ee.ria.xroad.common.certificateprofile.AuthCertificateProfileInfo;
import ee.ria.xroad.common.certificateprofile.CertificateProfileInfoProvider;
import ee.ria.xroad.common.certificateprofile.DnFieldDescription;
import ee.ria.xroad.common.certificateprofile.SignCertificateProfileInfo;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.identifier.SecurityServerId;

/**
 * Tests the implementation of SkKlass3CertificateProfileInfoProvider.
 */
public class SkKlass3CertificateProfileInfoProviderTest {

    /**
     * Tests whether getting expected subject fields of auth and sing profile
     * succeeds as expected.
     */
    @Test
    public void returnsCorrectSubjectFields() {
        DnFieldDescription[] expected = {
            new DnFieldDescriptionImpl("SN", "Serial Number (SN)", "bar")
                    .setReadOnly(true),
            new DnFieldDescriptionImpl("CN", "Common Name (CN)", "foobar")
                    .setReadOnly(true)
        };

        assertTrue(areEqual(expected, getSignProfile().getSubjectFields()));
        assertTrue(areEqual(expected, getAuthProfile().getSubjectFields()));
    }

    /**
     * Tests whether subject name parts are read correctly.
     * @throws Exception in case of any unexpected errors
     */
    @Test
    public void getSubjectIdentifierSuccessfully() throws Exception {
        ClientId parts;

        parts = id("SERIALNUMBER=16543212");
        assertEquals("XX", parts.getXRoadInstance());
        assertEquals("COM", parts.getMemberClass());
        assertEquals("16543212", parts.getMemberCode());

        parts = id("SERIALNUMBER=26543212");
        assertEquals("XX", parts.getXRoadInstance());
        assertEquals("COM", parts.getMemberClass());
        assertEquals("26543212", parts.getMemberCode());

        parts = id("SERIALNUMBER=36543212");
        assertEquals("XX", parts.getXRoadInstance());
        assertEquals("COM", parts.getMemberClass());
        assertEquals("36543212", parts.getMemberCode());

        parts = id("SERIALNUMBER=46543212");
        assertEquals("XX", parts.getXRoadInstance());
        assertEquals("COM", parts.getMemberClass());
        assertEquals("46543212", parts.getMemberCode());

        parts = id("SERIALNUMBER=56543212");
        assertEquals("XX", parts.getXRoadInstance());
        assertEquals("COM", parts.getMemberClass());
        assertEquals("56543212", parts.getMemberCode());

        parts = id("SERIALNUMBER=66543212");
        assertEquals("XX", parts.getXRoadInstance());
        assertEquals("COM", parts.getMemberClass());
        assertEquals("66543212", parts.getMemberCode());

        parts = id("SERIALNUMBER=76543212");
        assertEquals("XX", parts.getXRoadInstance());
        assertEquals("GOV", parts.getMemberClass());
        assertEquals("76543212", parts.getMemberCode());

        parts = id("SERIALNUMBER=86543212");
        assertEquals("XX", parts.getXRoadInstance());
        assertEquals("NGO", parts.getMemberClass());
        assertEquals("86543212", parts.getMemberCode());

        parts = id("SERIALNUMBER=96543212");
        assertEquals("XX", parts.getXRoadInstance());
        assertEquals("NGO", parts.getMemberClass());
        assertEquals("96543212", parts.getMemberCode());
    }

    /**
     * Tests whether a invalid serial number length is detected.
     * @throws Exception in case of an expected error
     */
    @Test(expected = Exception.class)
    public void invalidLength() throws Exception {
        id("SERIALNUMBER=123");
        fail();
    }

    /**
     * Tests whether a missing serial number is detected.
     * @throws Exception in case of an expected error
     */
    @Test(expected = Exception.class)
    public void missingSerialNumber() throws Exception {
        id("S=7654321");
        fail();
    }

    /**
     * Tests whether an unknown serial number is detected.
     * @throws Exception in case of an expected error
     */
    @Test(expected = Exception.class)
    public void unknownSerialNumber() throws Exception {
        id("SERIALNUMBER=s1234567");
        fail();
    }

    // ------------------------------------------------------------------------

    private ClientId id(String name) throws Exception {
        X509Certificate mockCert = Mockito.mock(X509Certificate.class);

        Mockito.when(mockCert.getSubjectX500Principal()).thenReturn(
            new X500Principal(name)
        );

        return getSignProfile().getSubjectIdentifier(mockCert);
    }

    private CertificateProfileInfoProvider provider() {
        return new SkKlass3CertificateProfileInfoProvider();
    }

    private SignCertificateProfileInfo getSignProfile() {
        return provider().getSignCertProfile(
                new SignCertificateProfileInfo.Parameters() {
            @Override
            public ClientId getClientId() {
                return ClientId.create("XX", "foo", "bar");
            }
            @Override
            public String getMemberName() {
                return "foobar";
            }
            @Override
            public SecurityServerId getServerId() {
                return null;
            }
        });
    }

    private AuthCertificateProfileInfo getAuthProfile() {
        return provider().getAuthCertProfile(
                new AuthCertificateProfileInfo.Parameters() {
            @Override
            public SecurityServerId getServerId() {
                return SecurityServerId.create("XX", "foo", "bar", "server");
            }
            @Override
            public String getMemberName() {
                return "foobar";
            }
        });
    }
}
