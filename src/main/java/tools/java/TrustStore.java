package tools.java;

import java.io.ByteArrayInputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Set;

public record TrustStore(Path path, String password) {

    public static TrustStore fromCaCertificate(byte[] caCertBytes)
            throws Exception {

        // Convertir le certificat PEM en X509Certificate
        CertificateFactory certificateFactory =
                CertificateFactory.getInstance("X.509");

        X509Certificate certificate =
                (X509Certificate) certificateFactory.generateCertificate(
                        new ByteArrayInputStream(caCertBytes)
                );

        // Générer un mot de passe temporaire
        byte[] randomPassword = new byte[24];
        new SecureRandom().nextBytes(randomPassword);

        String password = java.util.Base64.getEncoder()
                .encodeToString(randomPassword);

        // Créer un truststore PKCS12
        KeyStore keyStore = KeyStore.getInstance("PKCS12");
        keyStore.load(null, password.toCharArray());

        keyStore.setCertificateEntry("elasticsearch-ca", certificate);

        // Écrire le truststore dans un fichier temporaire
        Path trustStorePath = Files.createTempFile("elasticsearch-truststore-", ".p12");

        try (OutputStream outputStream = Files.newOutputStream(trustStorePath)) {
            keyStore.store(outputStream, password.toCharArray());
        }

        Files.setPosixFilePermissions(
                trustStorePath,
                Set.of(
                        PosixFilePermission.OWNER_READ,
                        PosixFilePermission.OWNER_WRITE,
                        PosixFilePermission.GROUP_READ,
                        PosixFilePermission.OTHERS_READ
                )
        );

        return new TrustStore(trustStorePath, password);
    }
}
