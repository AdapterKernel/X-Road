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
package ee.ria.xroad.signer.model;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import org.bouncycastle.cert.ocsp.OCSPResp;

import ee.ria.xroad.common.identifier.ClientId;
import ee.ria.xroad.signer.protocol.dto.CertificateInfo;

import static ee.ria.xroad.common.ErrorCodes.translateException;
import static ee.ria.xroad.common.util.CryptoUtils.calculateCertHexHash;
import static ee.ria.xroad.common.util.CryptoUtils.readCertificate;

/**
 * Model object that holds the information associated with a certificate.
 */
@Data
public class Cert {

    /** ID for cert that is used by the OCSP request keys. (optional) */
    private final String id;

    /** If this certificate belongs to signing key, then this attribute contains
     * identifier of the member that uses this certificate. */
    private ClientId memberId;

    /** Whether this certificate can be used by the proxy. */
    private boolean active;

    /** Whether or not this certificate is in the configuration. */
    private boolean savedToConfiguration;

    /** Holds the status of the certificate. */
    private String status;

    /** Holds the precalculated hash of the certificate. */
    @Setter(AccessLevel.PRIVATE)
    private String hash;

    /** Holds the certificate instance. */
    private X509Certificate certificate;

    /** Holds the OCSP response of the certificate. */
    private OCSPResp ocspResponse;

    /**
     * Sets the certificate and hash
     * @param cert the certificate
     */
    public void setCertificate(X509Certificate cert) {
        try {
            hash = calculateCertHexHash(cert);
            certificate = cert;
        } catch (Exception e) {
            throw translateException(e);
        }
    }

    /**
     * Sets the certificate and hash
     * @param certBytes the bytes of the certificate
     */
    public void setCertificate(byte[] certBytes) {
        try {
            setCertificate(readCertificate(certBytes));
        } catch (Exception e) {
            throw translateException(e);
        }
    }

    /**
     * Sets the ocsp response
     * @param ocspBytes the bytes of the ocsp response
     */
    public void setOcspResponse(byte[] ocspBytes) {
        if (ocspBytes == null) {
            return;
        }

        try {
            setOcspResponse(new OCSPResp(ocspBytes));
        } catch (Exception e) {
            throw translateException(e);
        }
    }

    /**
     * Sets the ocsp response
     * @param ocsp the ocsp response
     */
    public void setOcspResponse(OCSPResp ocsp) {
        ocspResponse = ocsp;
    }

    /**
     * @return the bytes of the certificate
     */
    public byte[] getBytes() {
        try {
            return certificate.getEncoded();
        } catch (CertificateEncodingException e) {
            throw translateException(e);
        }
    }

    /**
     * Converts this object to value object.
     * @return the value object
     */
    public CertificateInfo toDTO() {
        try {
            return new CertificateInfo(memberId, active, savedToConfiguration,
                    status, id, certificate.getEncoded(),
                    ocspResponse != null ? ocspResponse.getEncoded() : null);
        } catch (Exception e) {
            throw translateException(e);
        }
    }
}
