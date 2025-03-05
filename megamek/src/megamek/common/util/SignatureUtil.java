package megamek.common.util;

import megamek.common.Entity;
import megamek.utilities.FileSignerTool;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;

/**
 * Utility class for handling digital signatures of canonical files.
 */
public class SignatureUtil {
    private static final Logger logger = LogManager.getLogger(SignatureUtil.class);
    private static PublicKey publicKey;

    /**
     * Verifies if a file is a canonical (signed) file.
     *
     * @param file The file to verify
     * @return true if the file has a valid signature, false otherwise
     */
    public static boolean isCanonicalFile(File file, Entity entity) {
        try {
            if (publicKey == null) {
                loadPublicKey();
            }
            String content = Files.readString(file.toPath());
            return FileSignerTool.verifyFile(content, publicKey);
        } catch (Exception e) {
            logger.error("Error verifying file signature: " + file.getPath()
                + " entity: " + entity.getChassis() + " " + entity.getModel(), e);
            return false;
        }
    }

    /**
     * Loads the public key from resources.
     */
    private static void loadPublicKey() throws GeneralSecurityException, IOException {
        // Load the public key from resources
        byte[] keyBytes;

        try (var is = SignatureUtil.class.getResourceAsStream("/public.key")) {
            if (is == null) {
                throw new IOException("Public key file not found in resources");
            }
            keyBytes = is.readAllBytes();
        }
        // Read public key

        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        publicKey = keyFactory.generatePublic(keySpec);
    }
}
