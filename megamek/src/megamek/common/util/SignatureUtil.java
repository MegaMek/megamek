package megamek.common.util;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Utility class for handling digital signatures of canonical files.
 */
public class SignatureUtil {
    private static final Logger logger = LogManager.getLogger(SignatureUtil.class);
    private static final Pattern SIGNATURE_PATTERN = Pattern.compile("<signature>\\s*(.*?)\\s*</signature>$", Pattern.DOTALL);
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private static PublicKey publicKey;

    /**
     * Verifies if a file is a canonical (signed) file.
     *
     * @param file The file to verify
     * @return true if the file has a valid signature, false otherwise
     */
    public static boolean isCanonicalFile(File file) {
        try {
            String content = readFileContent(file);

            // Extract signature if present
            Matcher matcher = SIGNATURE_PATTERN.matcher(content);
            if (!matcher.find()) {
                return false; // No signature found
            }

            String base64Signature = matcher.group(1);
            byte[] signature = Base64.getDecoder().decode(base64Signature);

            // Get content without signature
            String contentWithoutSignature = content.substring(0, matcher.start());

            // Verify signature
            return verifySignature(contentWithoutSignature.getBytes(StandardCharsets.UTF_8), signature);
        } catch (Exception e) {
            logger.error("Error verifying file signature: " + file.getPath(), e);
            return false;
        }
    }

    /**
     * Reads the entire content of a file as a string.
     */
    private static String readFileContent(File file) throws IOException {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        }
        return content.toString();
    }

    /**
     * Verifies a signature using the public key.
     */
    private static boolean verifySignature(byte[] data, byte[] signature) throws GeneralSecurityException, IOException {
        if (publicKey == null) {
            loadPublicKey();
        }

        Signature sig = Signature.getInstance(SIGNATURE_ALGORITHM);
        sig.initVerify(publicKey);
        sig.update(data);
        return sig.verify(signature);
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

        String publicKeyPEM = new String(keyBytes, StandardCharsets.UTF_8);

        // Remove the PEM headers and footers, and any whitespace
        publicKeyPEM = publicKeyPEM.replace("-----BEGIN PUBLIC KEY-----", "");
        publicKeyPEM = publicKeyPEM.replace("-----END PUBLIC KEY-----", "");
        publicKeyPEM = publicKeyPEM.replaceAll("\\s+", "");

        // Decode the Base64 encoded key
        byte[] decodedKey = Base64.getDecoder().decode(publicKeyPEM);

        try {
            X509EncodedKeySpec spec = new X509EncodedKeySpec(decodedKey);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            publicKey = kf.generatePublic(spec);
        } catch (InvalidKeySpecException e) {
            throw new GeneralSecurityException("Invalid public key specification", e);
        }
    }


    /**
     * Extracts content without the signature block.
     * Useful when loading files to avoid having signature data in memory.
     *
     * @param file The file to read
     * @return The file content without the signature block
     */
    public static String getContentWithoutSignature(File file) throws IOException {
        String content = readFileContent(file);
        return content.replaceAll(SIGNATURE_PATTERN.pattern(), "");
    }
}
