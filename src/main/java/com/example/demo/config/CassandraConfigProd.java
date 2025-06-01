package com.example.demo.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.cassandra.config.AbstractCassandraConfiguration;
import org.springframework.data.cassandra.config.SchemaAction;
import jakarta.annotation.PostConstruct;
import software.aws.mcs.auth.SigV4AuthProvider;
import javax.net.ssl.SSLContext;
import org.springframework.data.cassandra.config.SessionBuilderConfigurer;

@Configuration
@Profile("prod")
public class CassandraConfigProd extends AbstractCassandraConfiguration {

    @Value("${spring.data.cassandra.keyspace-name}")
    private String keyspaceName;

    @Value("${spring.data.cassandra.contact-points}")
    private String contactPoints;

    @Value("${spring.data.cassandra.local-datacenter}")
    private String localDatacenter;

    @Value("${spring.data.cassandra.port}")
    private int port;

    @Override
    protected String getKeyspaceName() {
        return keyspaceName;
    }

    @Override
    public SchemaAction getSchemaAction() {
        return SchemaAction.CREATE_IF_NOT_EXISTS;
    }

    @Override
    public String[] getEntityBasePackages() {
        return new String[]{"com.example.demo"};
    }

    @PostConstruct
    public void printCassandraHost() {
        System.out.println("SPRING_PROFILES_ACTIVE: " + System.getenv("SPRING_PROFILES_ACTIVE"));
        System.out.println("CASSANDRA_HOST: " + System.getenv("CASSANDRA_HOST"));
        System.out.println("spring.data.cassandra.contact-points: " + contactPoints);
        System.out.println("Keyspace Name: " + keyspaceName);
        System.out.println("Local Datacenter: " + localDatacenter);
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    @Override
    protected SessionBuilderConfigurer getSessionBuilderConfigurer() {
        return sessionBuilder -> {
            try {
                SSLContext sslContext = SSLContext.getInstance("TLS");
                java.security.KeyStore ks = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());
                ks.load(null, null);
                java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
                try (java.io.InputStream caInput = new java.io.FileInputStream("/var/app/current/src/main/resources/AmazonRootCA1.pem")) {
                    java.security.cert.Certificate ca = cf.generateCertificate(caInput);
                    ks.setCertificateEntry("ca", ca);
                }
                javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
                return sessionBuilder
                    .withAuthProvider(new SigV4AuthProvider("eu-north-1"))
                    .withSslContext(sslContext);
            } catch (Exception e) {
                throw new RuntimeException("Failed to configure Cassandra session with SigV4", e);
            }
        };
    }
}
