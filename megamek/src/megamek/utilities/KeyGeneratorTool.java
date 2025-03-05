package megamek.utilities;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

/**
 * Utility tool for signing canonical files in MegaMek.
 * This tool can be called from Gradle to handle the file signing process.
 */
public class KeyGeneratorTool {
    /**
     * Main method to run the key generator tool from command line or Gradle.
     *
     * @param args Expected arguments:
     *             [0] - Private key file path
     *             [1] - Public key file path
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: KeyGeneratorTool <private_key_path> <public_key_path>");
            System.exit(1);
        }
        generateKeyPair(new File(args[0]), new File(args[1]));
        System.exit(0);
    }

    /**
     * Generate a new key pair and save to files.
     */
    public static void generateKeyPair(File privateKeyFile, File publicKeyFile) throws Exception {
        java.security.KeyPairGenerator keyGen = java.security.KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        java.security.KeyPair pair = keyGen.generateKeyPair();

        // Save private key
        Files.write(privateKeyFile.toPath(), pair.getPrivate().getEncoded());

        // Save public key
        Files.write(publicKeyFile.toPath(), pair.getPublic().getEncoded());
    }

}
