package com.example.mtls;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;

/**
 * Builds an SSLContext from a PKCS12 identity keystore (private key + certificate)
 * and a PKCS12 truststore (CA certificates), the two pieces every mTLS peer needs.
 */
final class MtlsSslContextFactory {

    private MtlsSslContextFactory() {
    }

    static SSLContext create(Path keystorePath, char[] keystorePassword,
                              Path truststorePath, char[] truststorePassword)
            throws GeneralSecurityException, IOException {

        KeyStore keyStore = loadPkcs12(keystorePath, keystorePassword);
        KeyManagerFactory keyManagerFactory =
                KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
        keyManagerFactory.init(keyStore, keystorePassword);

        KeyStore trustStore = loadPkcs12(truststorePath, truststorePassword);
        TrustManagerFactory trustManagerFactory =
                TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        trustManagerFactory.init(trustStore);

        SSLContext sslContext = SSLContext.getInstance("TLSv1.3");
        sslContext.init(keyManagerFactory.getKeyManagers(),
                trustManagerFactory.getTrustManagers(), null);
        return sslContext;
    }

    private static KeyStore loadPkcs12(Path path, char[] password)
            throws GeneralSecurityException, IOException {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        try (FileInputStream in = new FileInputStream(path.toFile())) {
            keyStore.load(in, password);
        }
        return keyStore;
    }
}
