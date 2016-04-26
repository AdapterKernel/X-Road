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
package ee.ria.xroad.signer.certmanager;

import akka.actor.ActorRef;
import ee.ria.xroad.common.cert.CertChain;
import ee.ria.xroad.common.conf.globalconf.GlobalConf;
import ee.ria.xroad.common.conf.globalconfextension.GlobalConfExtensions;
import ee.ria.xroad.common.ocsp.OcspVerifier;
import ee.ria.xroad.common.ocsp.OcspVerifierOptions;
import ee.ria.xroad.common.util.CertUtils;
import ee.ria.xroad.signer.OcspClientJob;
import ee.ria.xroad.signer.certmanager.OcspResponseManager.IsCachedOcspResponse;
import ee.ria.xroad.signer.protocol.dto.CertificateInfo;
import ee.ria.xroad.signer.protocol.message.SetOcspResponses;
import ee.ria.xroad.signer.tokenmanager.TokenManager;
import ee.ria.xroad.signer.util.AbstractSignerActor;
import ee.ria.xroad.signer.util.SignerUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.cert.ocsp.OCSPResp;
import org.joda.time.DateTime;

import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.Map.Entry;

import static ee.ria.xroad.common.util.CryptoUtils.*;
import static ee.ria.xroad.signer.protocol.ComponentNames.OCSP_CLIENT_JOB;
import static ee.ria.xroad.signer.tokenmanager.ServiceLocator.getOcspResponseManager;
import static java.util.Collections.emptyList;


/**
 * This class is responsible for retrieving the OCSP responses from
 * the OCSP server and providing the responses to the message signer.
 *
 * The certificate status is queried from the server at a fixed interval.
 */
@Slf4j
@RequiredArgsConstructor
public class OcspClientWorker extends AbstractSignerActor {

    private static final int MIN_FRESHNESS_SECONDS = 5;
    private static final int DEFAULT_FRESHNESS_SECONDS = 600;
    private static final int FRESHNESS_DIVISOR = 10;

    public static final String EXECUTE = "Execute";
    public static final String RELOAD = "Reload";

    private static final String OCSP_FRESHNESS_SECONDS = "ocspFreshnessSeconds";
    private static final String VERIFY_OCSP_NEXTUPDATE = "verifyOcspNextUpdate";

    private GlobalConfChangeChecker changeChecker;

    @Override
    public void preStart() throws Exception {
        super.preStart();
        changeChecker = new GlobalConfChangeChecker();
    }

    @Override
    public void onReceive(Object message) throws Exception {
        if (EXECUTE.equals(message)) {
            handleExecute();
        } else if (RELOAD.equals(message)) {
            handleReload();
        } else {
            unhandled(message);
        }
    }

    void handleReload() {
        log.trace("handleReload()");

        GlobalConf.reload();

        if (!GlobalConf.isValid()) {
            log.error("global configuration is not valid");
            return;
        }

        boolean changeDetected = false;
        changeChecker.addChange(OCSP_FRESHNESS_SECONDS, GlobalConf.getOcspFreshnessSeconds(true));
        changeChecker.addChange(VERIFY_OCSP_NEXTUPDATE,
                GlobalConfExtensions.getInstance().shouldVerifyOcspNextUpdate());
        if (changeChecker.hasChanged(OCSP_FRESHNESS_SECONDS)) {
            log.info("detected change in global configuration ocspFreshnessSeconds parameter");
            changeDetected = true;
        }
        if (changeChecker.hasChanged(VERIFY_OCSP_NEXTUPDATE)) {
            log.info("detected change in global configuration extension shouldVerifyOcspNextUpdate parameter");
            changeDetected = true;
        }
        if (changeDetected) {
            log.trace("sending cancel");
            getContext().actorSelection("/user/" + OCSP_CLIENT_JOB).tell(OcspClientJob.CANCEL, ActorRef.noSender());
            log.trace("sending execute");
            getContext().actorSelection("/user/" + OCSP_CLIENT_JOB).tell(OcspClientWorker.EXECUTE, ActorRef.noSender());
            log.trace("done");
        }
    }

    void handleExecute() {
        log.trace("handleExecute()");

        if (!GlobalConf.isValid()) {
            return;
        }

        List<X509Certificate> certs = getCertsForOcsp();
        if (certs == null || certs.isEmpty()) {
            log.trace("Found no certificates that need OCSP responses");
            return;
        }

        log.debug("Fetching OCSP responses for {} certificates", certs.size());

        Map<String, OCSPResp> statuses = new HashMap<>();
        for (X509Certificate subject : certs) {
            try {
                OCSPResp status = queryCertStatus(subject,
                        new OcspVerifierOptions(GlobalConfExtensions.getInstance().shouldVerifyOcspNextUpdate()));
                if (status != null) {
                    String subjectHash = calculateCertHexHash(subject);
                    statuses.put(subjectHash, status);
                }
            } catch (Exception e) {
                log.error("Error when querying certificate '"
                        + subject.getSerialNumber() + "'", e);
            }
        }

        try {
            updateCertStatuses(statuses);
        } catch (Exception e) {
            log.error("Error updating certificate statuses", e);
        }
    }

    List<X509Certificate> getCertsForOcsp() {
        Set<X509Certificate> certs = new HashSet<>();

        for (CertificateInfo certInfo : TokenManager.getAllCerts()) {
            if (!certInfo.isActive()) {
                // do not download OCSP responses for inactive certificates
                continue;
            }

            X509Certificate cert;
            try {
                cert = readCertificate(certInfo.getCertificateBytes());
            } catch (Exception e) {
                log.error("Failed to parse certificate " + certInfo.getId(), e);
                continue;
            }

            if (CertUtils.isSelfSigned(cert)) {
                continue; // ignore self-signed certificates
            }

            List<X509Certificate> chain = getCertChain(cert);
            for (X509Certificate certChainCert : chain) {
                try {
                    if (shouldFetchResponse(certChainCert)) {
                        certs.add(certChainCert);
                    }
                } catch (Exception e) {
                    log.error("Unable to check if should fetch status for "
                            + certChainCert.getSerialNumber(), e);
                }
            }
        }

        return new ArrayList<>(certs);
    }

    OCSPResp queryCertStatus(X509Certificate subject, OcspVerifierOptions verifierOptions) throws Exception {
        X509Certificate issuer = GlobalConf.getCaCert(
                GlobalConf.getInstanceIdentifier(), subject);

        PrivateKey signerKey = OcspClient.getOcspRequestKey(subject);
        X509Certificate signer = OcspClient.getOcspSignerCert();

        OCSPResp response =
                OcspClient.fetchResponse(subject, issuer, signerKey, signer);
        try {
            OcspVerifier verifier =
                    new OcspVerifier(GlobalConf.getOcspFreshnessSeconds(true), verifierOptions);
            verifier.verifyValidity(response, subject, issuer);

            log.trace("Received OCSP response for certificate '{}'",
                    subject.getSubjectX500Principal());
            return response;
        } catch (Exception e) {
            log.warn("Received OCSP response that failed verification", e);
            return null;
        }
    }

    void updateCertStatuses(Map<String, OCSPResp> statuses) throws Exception {
        List<String> hashes = new ArrayList<>(statuses.size());
        List<String> responses = new ArrayList<>(statuses.size());

        for (Entry<String, OCSPResp> e : statuses.entrySet()) {
            hashes.add(e.getKey());
            responses.add(encodeBase64(e.getValue().getEncoded()));
        }

        getOcspResponseManager(getContext()).tell(
                new SetOcspResponses(
                        hashes.toArray(new String[statuses.size()]),
                        responses.toArray(new String[statuses.size()])),
                        getSelf());
    }

    /**
     * @return true if the response for given certificate does not exist,
     * is expired (in which case it is also removed from cache) or is not
     * valid
     */
    boolean shouldFetchResponse(X509Certificate subject) throws Exception {
        if (!CertUtils.isValid(subject)) {
            log.warn("Certificate '{}' is not valid",
                    subject.getSubjectX500Principal());
            return false;
        }
        String subjectHash = calculateCertHexHash(subject);
        try {
            log.trace("shouldFetchResponse for cert: {}", subjectHash);
            return !isCachedOcspResponse(subjectHash);
        } catch (Exception e) {
            // Ignore this error, since any kind of failure to get the response
            // or validate it means we should fetch the response from the responder.
            return true;
        }
    }

    boolean isCachedOcspResponse(String certHash) throws Exception {
        // Check if the OCSP response is in the cache. We need to check if the
        // OCSP response expires in the future in order to not leave a gap,
        // where the OCSP is expired, but the new one is currently being
        // retrieved.
        Date atDate = new DateTime().plusSeconds(
                getNextOcspFreshnessSeconds()).toDate();

        Boolean isCachedOcspResponse = (Boolean) SignerUtil.ask(getOcspResponseManager(getContext()),
                new IsCachedOcspResponse(certHash, atDate));
        log.trace("isCachedOcspResponse(certHash: {}, atDate: {}) = {}", certHash,
                atDate, isCachedOcspResponse);
        return isCachedOcspResponse;
    }

    private List<X509Certificate> getCertChain(X509Certificate cert) {
        try {
            CertChain chain = GlobalConf.getCertChain(
                    GlobalConf.getInstanceIdentifier(), cert);
            if (chain == null) {
                return Arrays.asList(cert);
            }

            return chain.getAllCertsWithoutTrustedRoot();
        } catch (Exception e) {
            log.error("Error getting certificate chain for certificate "
                    + cert.getSubjectX500Principal(), e);
        }

        return emptyList();
    }

    /**
     * @return the next ocsp freshness time in seconds
     */
    public static int getNextOcspFreshnessSeconds() {
        int freshness;
        try {
            freshness = GlobalConf.getOcspFreshnessSeconds(true);
        } catch (Exception ignored) {
            freshness = DEFAULT_FRESHNESS_SECONDS;
        }

        return Math.max(freshness / FRESHNESS_DIVISOR, MIN_FRESHNESS_SECONDS);
    }
}
