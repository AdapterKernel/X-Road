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

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x500.style.BCStyle;

import ee.ria.xroad.common.certificateprofile.AuthCertificateProfileInfo;
import ee.ria.xroad.common.certificateprofile.CertificateProfileInfoProvider;
import ee.ria.xroad.common.certificateprofile.DnFieldDescription;
import ee.ria.xroad.common.certificateprofile.SignCertificateProfileInfo;
import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.common.util.CertUtils;

/**
 * Certificate profile for SK Klass 3 certificates.
 */
public class SkKlass3CertificateProfileInfoProvider
        implements CertificateProfileInfoProvider {

    @Override
    public AuthCertificateProfileInfo getAuthCertProfile(
            AuthCertificateProfileInfo.Parameters params) {
        return new SkAuthCertificateProfileInfo(params);
    }

    @Override
    public SignCertificateProfileInfo getSignCertProfile(
            SignCertificateProfileInfo.Parameters params) {
        return new SkSignCertificateProfileInfo(params);
    }

    // ------------------------------------------------------------------------

    private static class SkAuthCertificateProfileInfo
            extends AbstractCertificateProfileInfo
            implements AuthCertificateProfileInfo {

        SkAuthCertificateProfileInfo(
                AuthCertificateProfileInfo.Parameters params) {
            super(new DnFieldDescription[] {
                    new DnFieldDescriptionImpl("SN", "Serial Number (SN)",
                        params.getServerId().getMemberCode()
                    ).setReadOnly(true),
                    new DnFieldDescriptionImpl("CN", "Common Name (CN)",
                        params.getMemberName()
                    ).setReadOnly(true)
                }
            );
        }
    }

    private static class SkSignCertificateProfileInfo
            extends AbstractCertificateProfileInfo
            implements SignCertificateProfileInfo {

        private static final int SN_LENGTH = 8;

        private final String instanceIdentifier;

        SkSignCertificateProfileInfo(
                SignCertificateProfileInfo.Parameters params) {
            super(new DnFieldDescription[] {
                    new DnFieldDescriptionImpl("SN", "Serial Number (SN)",
                        params.getClientId().getMemberCode()
                    ).setReadOnly(true),
                    new DnFieldDescriptionImpl("CN", "Common Name (CN)",
                        params.getMemberName()
                    ).setReadOnly(true)
                }
            );

            instanceIdentifier = params.getClientId().getXRoadInstance();
        }

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

            if (!StringUtils.isNumeric(sn)) {
                throw new Exception("Serial number is not an integer");
            }

            if (sn.length() != SN_LENGTH) {
                throw new Exception(
                        "Serial number must be " + SN_LENGTH + " digits long");
            }

            return ClientId.create(instanceIdentifier, getMemberClass(sn), sn);
        }

        // Returns the hardcoded member class based on the first number in
        // the serial number.
        private static String getMemberClass(String sn) throws Exception {
            switch (sn.charAt(0)) {
                case '1': // Fall through
                case '2': // Fall through
                case '3': // Fall through
                case '4': // Fall through
                case '5': // Fall through
                case '6':
                    return "COM";
                case '7':
                    return "GOV";
                case '8': // Fall through
                case '9':
                    return "NGO";
                default:
                    throw new Exception("Malformed serial number: " + sn);
            }
        }
    }
}
