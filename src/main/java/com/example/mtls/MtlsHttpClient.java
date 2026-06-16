package com.example.mtls;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Path;
import javax.net.ssl.SSLContext;

/**
 * Bare-bones example of calling an HTTPS endpoint with mutual TLS: the client
 * presents its own certificate (from a PKCS12 keystore) and validates the
 * server's certificate against a CA truststore.
 *
 * Run `certs/generate-certs.sh` once, start {@link MtlsServer} in one terminal,
 * then run this class to make the mTLS call.
 */
public final class MtlsHttpClient {

    public static void main(String[] args) throws Exception {
        String url = System.getProperty("mtls.url", "https://localhost:8443/hello");
        Path keystorePath = Path.of(System.getProperty("mtls.keystore", "certs/client-keystore.p12"));
        char[] keystorePassword = System.getProperty("mtls.keystorePassword", "changeit").toCharArray();
        Path truststorePath = Path.of(System.getProperty("mtls.truststore", "certs/truststore.p12"));
        char[] truststorePassword = System.getProperty("mtls.truststorePassword", "changeit").toCharArray();

        SSLContext sslContext = MtlsSslContextFactory.create(
                keystorePath, keystorePassword, truststorePath, truststorePassword);

        HttpClient httpClient = HttpClient.newBuilder()
                .sslContext(sslContext)
                .version(HttpClient.Version.HTTP_1_1)
                .build();

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .GET()
                .build();

        System.out.println("Calling " + url + " with client certificate from " + keystorePath);
        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        System.out.println("Status: " + response.statusCode());
        System.out.println("Body:   " + response.body());
    }

    private MtlsHttpClient() {
    }
}
