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
import java.io.File;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@Profile("prod")
public class CassandraConfigProd extends AbstractCassandraConfiguration {

    private static final Logger logger = LoggerFactory.getLogger(CassandraConfigProd.class);
    private static final String CERT_PATH = "/var/app/staging/src/main/resources/AmazonRootCA1.pem";
    private static final String CERT_READY_FLAG = "/var/app/staging/src/main/resources/.certificate_ready";
    private static final int MAX_RETRIES = 5;
    private static final int RETRY_DELAY_MS = 5000;

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
        logger.info("SPRING_PROFILES_ACTIVE: {}", System.getenv("SPRING_PROFILES_ACTIVE"));
        logger.info("CASSANDRA_HOST: {}", System.getenv("CASSANDRA_HOST"));
        logger.info("spring.data.cassandra.contact-points: {}", contactPoints);
        logger.info("Keyspace Name: {}", keyspaceName);
        logger.info("Local Datacenter: {}", localDatacenter);
    }

    @Override
    protected String getContactPoints() {
        return contactPoints;
    }

    @Override
    protected int getPort() {
        return port;
    }

    private void waitForCertificate() {
        logger.info("Starting to wait for certificate at path: {}", CERT_PATH);
        int retries = 0;
        while (retries < MAX_RETRIES) {
            File certFile = new File(CERT_PATH);
            File readyFlag = new File(CERT_READY_FLAG);
            
            if (certFile.exists() && certFile.length() > 0 && readyFlag.exists()) {
                logger.info("Certificate found after {} retries", retries);
                logger.info("Certificate file size: {} bytes", certFile.length());
                return;
            }
            
            if (!certFile.exists()) {
                logger.warn("Certificate file does not exist at path: {}", CERT_PATH);
            } else if (certFile.length() == 0) {
                logger.warn("Certificate file exists but is empty");
            }
            
            if (!readyFlag.exists()) {
                logger.warn("Certificate ready flag does not exist at path: {}", CERT_READY_FLAG);
            }
            
            logger.info("Certificate not found or not ready, retrying in {}ms... (attempt {}/{})", 
                RETRY_DELAY_MS, retries + 1, MAX_RETRIES);
            
            try {
                TimeUnit.MILLISECONDS.sleep(RETRY_DELAY_MS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for certificate", e);
            }
            retries++;
        }
        throw new RuntimeException("Failed to find certificate after " + MAX_RETRIES + " retries. Please check the certificate download script logs.");
    }

    @Override
    protected SessionBuilderConfigurer getSessionBuilderConfigurer() {
        return sessionBuilder -> {
            try {
                waitForCertificate();
                
                logger.info("Configuring SSL context with certificate from: {}", CERT_PATH);
                SSLContext sslContext = SSLContext.getInstance("TLS");
                java.security.KeyStore ks = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());
                ks.load(null, null);
                java.security.cert.CertificateFactory cf = java.security.cert.CertificateFactory.getInstance("X.509");
                
                try (java.io.InputStream caInput = new java.io.FileInputStream(CERT_PATH)) {
                    java.security.cert.Certificate ca = cf.generateCertificate(caInput);
                    ks.setCertificateEntry("ca", ca);
                    logger.info("Successfully loaded certificate into keystore");
                }
                
                javax.net.ssl.TrustManagerFactory tmf = javax.net.ssl.TrustManagerFactory.getInstance(
                    javax.net.ssl.TrustManagerFactory.getDefaultAlgorithm());
                tmf.init(ks);
                sslContext.init(null, tmf.getTrustManagers(), new java.security.SecureRandom());
                logger.info("Successfully initialized SSL context");
                
                return sessionBuilder
                    .withAuthProvider(new SigV4AuthProvider("eu-north-1"))
                    .withSslContext(sslContext);
            } catch (Exception e) {
                logger.error("Failed to configure Cassandra session with SigV4", e);
                throw new RuntimeException("Failed to configure Cassandra session with SigV4", e);
            }
        };
    }
}
