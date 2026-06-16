package com.example.mtls;

import com.sun.net.httpserver.HttpsConfigurator;
import com.sun.net.httpserver.HttpsExchange;
import com.sun.net.httpserver.HttpsParameters;
import com.sun.net.httpserver.HttpsServer;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.security.cert.Certificate;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

/**
 * Minimal HTTPS server that demands a client certificate, purely so this
 * repo's mTLS client example has something to call. Not production code.
 */
public final class MtlsServer {

    public static void main(String[] args) throws Exception {
        int port = Integer.getInteger("mtls.port", 8443);
        Path keystorePath = Path.of(System.getProperty("mtls.keystore", "certs/server-keystore.p12"));
        char[] keystorePassword = System.getProperty("mtls.keystorePassword", "changeit").toCharArray();
        Path truststorePath = Path.of(System.getProperty("mtls.truststore", "certs/truststore.p12"));
        char[] truststorePassword = System.getProperty("mtls.truststorePassword", "changeit").toCharArray();

        SSLContext sslContext = MtlsSslContextFactory.create(
                keystorePath, keystorePassword, truststorePath, truststorePassword);

        HttpsServer server = HttpsServer.create(new InetSocketAddress(port), 0);
        server.setHttpsConfigurator(new HttpsConfigurator(sslContext) {
            @Override
            public void configure(HttpsParameters params) {
                SSLParameters sslParameters = getSSLContext().getDefaultSSLParameters();
                sslParameters.setNeedClientAuth(true);
                // com.sun.net.httpserver.HttpsServer mishandles the TLS 1.3
                // post-handshake NewSessionTicket message, so pin to 1.2 here.
                sslParameters.setProtocols(new String[] {"TLSv1.2"});
                params.setSSLParameters(sslParameters);
            }
        });

        server.createContext("/hello", exchange -> {
            String clientDn = "unknown";
            SSLSession session = ((HttpsExchange) exchange).getSSLSession();
            Certificate[] peerCertificates = session.getPeerCertificates();
            if (peerCertificates.length > 0 && peerCertificates[0] instanceof java.security.cert.X509Certificate cert) {
                clientDn = cert.getSubjectX500Principal().getName();
            }

            String body = "Hello, " + clientDn + "! Your mTLS handshake succeeded.\n";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(200, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });

        server.start();
        System.out.println("mTLS server listening on https://localhost:" + port + "/hello (client cert required)");
    }

    private MtlsServer() {
    }
}
