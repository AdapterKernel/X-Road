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

import java.security.cert.X509Certificate;

import javax.security.auth.x500.X500Principal;

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;

import ee.ria.xroad.common.certificateprofile.AuthCertificateProfileInfo;
import ee.ria.xroad.common.certificateprofile.CertificateProfileInfo;
import ee.ria.xroad.common.certificateprofile.CertificateProfileInfoProvider;
import ee.ria.xroad.common.certificateprofile.DnFieldDescription;
import ee.ria.xroad.common.certificateprofile.DnFieldValue;
import ee.ria.xroad.common.certificateprofile.SignCertificateProfileInfo;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.util.CertUtils;
import lombok.RequiredArgsConstructor;

/**
 * Certificate profile for SK ESTEID.
 */
public class SkEsteIdCertificateProfileInfoProvider
        implements CertificateProfileInfoProvider {

    @Override
    public AuthCertificateProfileInfo getAuthCertProfile(
            AuthCertificateProfileInfo.Parameters params) {
        return new SkEsteidAuthCertificateProfile();
    }

    @Override
    public SignCertificateProfileInfo getSignCertProfile(
            SignCertificateProfileInfo.Parameters params) {
        return new SkEsteidSignCertificateProfile(params);
    }

    private static class SkEsteidCertificateProfileInfo
            implements CertificateProfileInfo {

        @Override
        public DnFieldDescription[] getSubjectFields() {
            return new DnFieldDescription[] {};
        }

        @Override
        public X500Principal createSubjectDn(DnFieldValue[] values) {
            return null;
        }

        @Override
        public void validateSubjectField(DnFieldValue field) throws Exception {
        }

    }

    private static class SkEsteidAuthCertificateProfile
            extends SkEsteidCertificateProfileInfo
            implements AuthCertificateProfileInfo {
    }

    @RequiredArgsConstructor
    private static class SkEsteidSignCertificateProfile
            extends SkEsteidCertificateProfileInfo
            implements SignCertificateProfileInfo {

        private static final String PERSON = "PERSON";
        private final SignCertificateProfileInfo.Parameters params;

        @Override
        public ClientId getSubjectIdentifier(X509Certificate certificate)
                throws Exception {
            return getSubjectIdentifier(
                new X500Name(certificate.getSubjectX500Principal().getName())
            );
        }

        ClientId getSubjectIdentifier(X500Name x500name) throws Exception {
            String sn = CertUtils.getRDNValue(x500name, BCStyle.SERIALNUMBER);
            if (StringUtils.isEmpty(sn)) {
                throw new Exception(
                        "Subject name does not contain serial number");
            }

            return ClientId.create(
                params.getClientId().getXRoadInstance(),
                PERSON,
                sn
            );
        }
    }
}
