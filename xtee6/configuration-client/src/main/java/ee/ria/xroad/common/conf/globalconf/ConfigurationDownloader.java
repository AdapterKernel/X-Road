package ee.ria.xroad.common.conf.globalconf;

import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.bouncycastle.operator.DigestCalculator;

import ee.ria.xroad.common.CodedException;

import static ee.ria.xroad.common.ErrorCodes.X_IO_ERROR;
import static ee.ria.xroad.common.ErrorCodes.X_MALFORMED_GLOBALCONF;
import static ee.ria.xroad.common.util.CryptoUtils.*;

/**
 * Downloaded configuration directory.
 */
@Slf4j
class ConfigurationDownloader {

    protected final FileNameProvider fileNameProvider;
    protected final String[] instanceIdentifiers;

    @Getter
    protected final Map<String, Set<ConfigurationSource>> additionalSources =
            new HashMap<>();

    ConfigurationDownloader(FileNameProvider fileNameProvider,
            String... instanceIdentifiers) {
        this.fileNameProvider = fileNameProvider;
        this.instanceIdentifiers = instanceIdentifiers;
    }

    ConfigurationParser getParser() {
        return new ConfigurationParser(instanceIdentifiers);
    }

    @SneakyThrows
    Path getFileName(ConfigurationFile file) {
        return fileNameProvider.getFileName(file);
    }

    /**
     * Downloads the configuration from the given configuration source.
     * @param source the configuration source
     * @param contentIdentifiers the content identifier to include
     * @return download result object which contains the downloaded files
     */
    DownloadResult download(ConfigurationSource source,
            String... contentIdentifiers) {
        DownloadResult result = new DownloadResult();

        for (ConfigurationLocation location : source.getLocations()) {
            try {
                return result.success(download(location, contentIdentifiers));
            } catch (Exception e) {
                result.addFailure(location, e);
                continue;
            }
        }

        // did not get a valid configuration from any location
        return result.failure();
    }

    Configuration download(ConfigurationLocation location,
            String[] contentIdentifiers) throws Exception {
        log.info("Downloading configuration from {}",
                location.getDownloadURL());

        additionalSources.clear();

        Configuration configuration =
                getParser().parse(location, contentIdentifiers);

        configuration.eachFile(this::handle);

        return configuration;
    }

    @SneakyThrows
    void handle(ConfigurationLocation location, ConfigurationFile file) {
        log.trace("handle({})", file);

        verifyInstanceIdentifier(location.getSource().getInstanceIdentifier(),
                file);

        Path contentFileName = getFileName(file);
        if (shouldDownload(file, contentFileName)) {
            byte[] content = downloadContent(location, file);

            verifyContent(content, file);
            handleContent(content, file);

            persistContent(content, contentFileName, file);
        } else {
            log.trace("{} is up to date", file.getContentLocation());

            updateExpirationDate(contentFileName, file);
        }
    }

    boolean shouldDownload(ConfigurationFile configurationFile,
            Path file) throws Exception {
        log.trace("shouldDownload({}, {})",
                configurationFile.getContentLocation(),
                configurationFile.getHash());

        if (Files.exists(file)) {
            String contentHash = configurationFile.getHash();
            String existingHash = encodeBase64(hash(file,
                    configurationFile.getHashAlgorithmId()));
            if (!StringUtils.equals(existingHash, contentHash)) {
                log.trace("Downloading {} because file has changed ({} != {})",
                        new Object[] {configurationFile.getContentLocation(),
                            existingHash, contentHash});
                return true;
            } else {
                return false;
            }
        }

        log.trace("Downloading {} because file {} does not exist locally",
                configurationFile.getContentLocation(), file);
        return true;
    }

    byte[] downloadContent(ConfigurationLocation location,
            ConfigurationFile file) throws Exception {
        URL url = getDownloadURL(location, file);

        log.info("Downloading content from {}", url);
        try (InputStream in = url.openStream()) {
            return IOUtils.toByteArray(in);
        }
    }

    void verifyContent(byte[] content, ConfigurationFile file)
            throws Exception {
        String algoId = getAlgorithmId(file.getHashAlgorithmId());
        log.trace("verifyContent({}, {})", file.getHash(), algoId);

        DigestCalculator dc = createDigestCalculator(algoId);
        dc.getOutputStream().write(content);

        byte[] hash = dc.getDigest();
        if (!Arrays.equals(hash, decodeBase64(file.getHash()))) {
            log.trace("Content {} hash {} does not match expected hash {}",
                    new Object[] {
                        file, encodeBase64(hash), file.getHash()});
            throw new CodedException(X_IO_ERROR,
                    "Failed to verify content integrity (%s)", file);
        }
    }

    void handleContent(byte[] content, ConfigurationFile file)
            throws Exception {
        switch (file.getContentIdentifier()) {
            case PrivateParameters.CONTENT_ID_PRIVATE_PARAMETERS:
                PrivateParameters privateParameters = new PrivateParameters();
                privateParameters.load(content);
                handlePrivateParameters(privateParameters, file);
                break;
            case SharedParameters.CONTENT_ID_SHARED_PARAMETERS:
                SharedParameters sharedParameters = new SharedParameters();
                sharedParameters.load(content);
                handleSharedParameters(sharedParameters, file);
                break;
            default: // do nothing
                break;
        }
    }

    void handlePrivateParameters(PrivateParameters privateParameters,
            ConfigurationFile file) throws Exception {
        verifyInstanceIdentifier(privateParameters.getInstanceIdentifier(),
                file);
        addAdditionalConfigurationSources(privateParameters);
    }

    void addAdditionalConfigurationSources(
            PrivateParameters privateParameters) {
        // If there are any additional configuration sources,
        // we need to download the shared parameters from these
        // configuration sources.
        Set<ConfigurationSource> sources = new HashSet<ConfigurationSource>();

        if (!privateParameters.getConfigurationSource().isEmpty()) {
            log.trace("Received private parameters with additional "
                    + privateParameters.getConfigurationSource().size()
                    + " configuration sources");
            sources.addAll(privateParameters.getConfigurationSource());
        }

        additionalSources.put(privateParameters.getInstanceIdentifier(),
                sources);
    }

    void handleSharedParameters(SharedParameters sharedParameters,
            ConfigurationFile file) throws Exception {
        verifyInstanceIdentifier(sharedParameters.getInstanceIdentifier(),
                file);
    }

    void persistContent(byte[] content, Path destination,
            ConfigurationFile file) throws Exception {
        log.info("Saving {} to {}", file, destination);

        ConfigurationDirectory.save(destination, content, file.getMetadata());
    }

    void updateExpirationDate(Path destination, ConfigurationFile file)
            throws Exception {
        log.trace("{} expires {}", file, file.getExpirationDate());

        ConfigurationDirectory.saveMetadata(destination, file.getMetadata());
    }

    void verifyInstanceIdentifier(String instanceIdentifier,
            ConfigurationFile file) {
        if (StringUtils.isBlank(file.getInstanceIdentifier())) {
            return;
        }

        if (!instanceIdentifier.equals(file.getInstanceIdentifier())) {
            throw new CodedException(X_MALFORMED_GLOBALCONF,
                    "Content part %s has invalid instance identifier "
                            + "(expected %s, but was %s)", file,
                            file.getInstanceIdentifier(), instanceIdentifier);
        }
    }

    URL getDownloadURL(ConfigurationLocation location,
            ConfigurationFile file) throws Exception {
        return new URI(location.getDownloadURL()).resolve(
                file.getContentLocation()).toURL();
    }

    // ------------------------------------------------------------------------

    static byte[] hash(Path file, String algoId) throws Exception {
        DigestCalculator dc = createDigestCalculator(getAlgorithmId(algoId));

        try (InputStream in = Files.newInputStream(file)) {
            IOUtils.copy(in, dc.getOutputStream());
            return dc.getDigest();
        }
    }
}