package com.example.demo.config;

import com.datastax.oss.driver.api.core.CqlSession;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.aws.mcs.auth.SigV4AuthProvider;

import javax.net.ssl.TrustManagerFactory;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Configuration
@Profile("prod")
public class CassandraConfigProd {

    @Value("${spring.data.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.data.cassandra.port}")
    private int port;

    @Value("${spring.data.cassandra.local-datacenter}")
    private String datacenter;

    @Value("${aws.region}")
    private String awsRegion;

    @Bean
    public CqlSession cqlSession() throws Exception {
        // 1. Load AmazonRootCA1.pem from filesystem
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        X509Certificate amazonRootCA;
        try (InputStream pemStream = new FileInputStream("/var/app/current/src/main/resources/AmazonRootCA1.pem")) {
            if (pemStream == null) {
                throw new IllegalStateException("Could not find AmazonRootCA1.pem at /var/app/current/src/main/resources/");
            }
            amazonRootCA = (X509Certificate) certFactory.generateCertificate(pemStream);
        }

        // 2. Put it into an in-memory truststore
        KeyStore ts = KeyStore.getInstance(KeyStore.getDefaultType());
        ts.load(null, null);
        ts.setCertificateEntry("amazonrootca1", amazonRootCA);

        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(ts);

        // 3. Build Netty SslContext that trusts only AmazonRootCA1
        SslContext nettySslContext = SslContextBuilder.forClient()
                .trustManager(tmf)
                .build();

        // 4. Create SigV4AuthProvider with AWS credentials
        SigV4AuthProvider sigV4AuthProvider = new SigV4AuthProvider(awsRegion);

        // 5. Build the CqlSession with TLS and SigV4 authentication
        return CqlSession.builder()
                .addContactPoint(new InetSocketAddress(contactPoints, port))
                .withLocalDatacenter(datacenter)
                .withAuthProvider(sigV4AuthProvider)
                .withSslEngineFactory(new NettySslEngineFactory(nettySslContext))
                .build();
    }
}
