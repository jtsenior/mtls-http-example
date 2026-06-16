# mTLS HTTP Example (Java)

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

Bare-bones example of making an HTTP call with mutual TLS using only the JDK's
built-in `java.net.http.HttpClient` — no external libraries.

- `MtlsHttpClient` — builds an `SSLContext` from a client PKCS12 keystore (identity)
  and a PKCS12 truststore (CA cert), then makes a GET request over mTLS. This is
  the part worth reading.
- `MtlsServer` — a minimal HTTPS server that requires a client certificate, just
  so the client has something to call. Not production code.
- `MtlsSslContextFactory` — shared helper both use to load keystore/truststore
  files into an `SSLContext`.

## Run it

1. Generate a throwaway CA, server cert, and client cert:

   ```
   certs/generate-certs.sh
   ```

2. Build:

   ```
   mvn clean package
   ```

3. In one terminal, start the demo server:

   ```
   java -cp target/classes com.example.mtls.MtlsServer
   ```

4. In another terminal, run the client:

   ```
   java -jar target/mtls-http-example.jar
   ```

   Expected output:

   ```
   Calling https://localhost:8443/hello with client certificate from certs/client-keystore.p12
   Status: 200
   Body:   Hello, CN=mtls-example-client,O=Mtls Example! Your mTLS handshake succeeded.
   ```

## Pointing at a real mTLS endpoint

`MtlsHttpClient` reads everything from system properties, so you can swap in
real certs and a real URL without touching code:

```
java -Dmtls.url=https://api.example.com/resource \
     -Dmtls.keystore=/path/to/client.p12 \
     -Dmtls.keystorePassword=secret \
     -Dmtls.truststore=/path/to/truststore.p12 \
     -Dmtls.truststorePassword=secret \
     -jar target/mtls-http-example.jar
```

## Note on `MtlsServer`

It pins TLS to 1.2 (`MtlsServer.java`, in the `HttpsConfigurator`). The JDK's
built-in `com.sun.net.httpserver.HttpsServer` doesn't correctly handle the TLS
1.3 post-handshake `NewSessionTicket` message and drops the connection right
after the handshake — pinning to 1.2 sidesteps that. This only affects the
demo server; `MtlsHttpClient` and `MtlsSslContextFactory` use the JDK's normal
TLS defaults and need no such workaround against a real server.
