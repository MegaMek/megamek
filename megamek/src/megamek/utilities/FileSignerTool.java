package megamek.utilities;

import megamek.common.loaders.BLKFile;
import megamek.logging.MMLogger;

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
public class FileSignerTool {
    private static final MMLogger logger = MMLogger.create(FileSignerTool.class);

    private static final String SIGNATURE_START = "<signature>\n";
    private static final String SIGNATURE_END = "\n</signature>";
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";

    private int fileCounter = 0;
    private int errorCounter = 0;
    private List<String> errors = new ArrayList<>();

    /**
     * Main method to run the signer from command line or Gradle.
     *
     * @param args Expected arguments:
     *             [0] - Private key file path
     *             [1...n] - Directories to scan for files
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: FileSignerTool <private_key_path> <directory1> [<directory2> ...]");
            System.exit(1);
        }

        File privateKeyFile = new File(args[0]);
        if (!privateKeyFile.exists()) {
            System.out.println("Private key file not found: " + privateKeyFile.getAbsolutePath());
            System.exit(1);
        }

        List<File> directories = new ArrayList<>();
        for (int i = 1; i < args.length; i++) {
            File dir = new File(args[i]);
            if (dir.exists() && dir.isDirectory()) {
                directories.add(dir);
            } else {
                System.out.println("Warning: Directory not found or not a directory: " + dir.getAbsolutePath());
            }
        }

        if (directories.isEmpty()) {
            System.out.println("No valid directories provided");
            System.exit(1);
        }

        FileSignerTool signer = new FileSignerTool();
        boolean success = signer.signDirectories(privateKeyFile, directories);

        System.exit(success ? 0 : 1);
    }

    /**
     * Signs all applicable files in the given directories.
     *
     * @param privateKeyFile The private key file
     * @param directories List of directories to scan
     * @return true if signing succeeded, false if there were errors
     */
    public boolean signDirectories(File privateKeyFile, List<File> directories) {
        long startTime = System.currentTimeMillis();

        try {
            // Load the private key
            PrivateKey privateKey = loadPrivateKey(privateKeyFile);
            System.out.println("Private key loaded successfully.");

            // Process each directory
            for (File dir : directories) {
                processDirectory(dir, privateKey);
            }

            // Print summary
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;

            System.out.println("\nSigning summary:");
            System.out.println("- Total files processed: " + fileCounter);
            System.out.println("- Signing errors: " + errorCounter);
            System.out.println("- Time taken: " + duration + " seconds");

            if (errorCounter > 0) {
                System.out.println("\nError details:");
                for (String error : errors) {
                    System.out.println(error);
                }
                return false;
            }

            return true;
        } catch (Exception e) {
            System.out.println("Fatal error: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Process a directory recursively, signing all applicable files.
     */
    private void processDirectory(File dir, PrivateKey privateKey) {
        System.out.println("Processing directory: " + dir.getPath());

        // Get all files in this directory and subdirectories
        List<File> filesToSign = new ArrayList<>();
        findFilesToSign(dir, filesToSign);

        // Sign each file
        for (File file : filesToSign) {
            try {
                signFile(file, privateKey);
                fileCounter++;

                if (fileCounter % 500 == 0) {
                    System.out.println("Signed " + fileCounter + " files...");
                }
            } catch (Exception e) {
                errorCounter++;
                String message = "Failed to sign " + file.getPath() + ": " + e.getMessage();
                errors.add(message);

                if (errorCounter <= 10) { // Only print the first 10 errors to avoid cluttering the console
                    System.out.println(message);
                } else if (errorCounter == 11) {
                    System.out.println("More errors found. All errors will be listed in the summary.");
                }
            }
        }
    }

    /**
     * Find all files that should be signed in a directory and its subdirectories.
     */
    private void findFilesToSign(File dir, List<File> result) {
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    findFilesToSign(file, result);
                } else if (shouldSignFile(file)) {
                    result.add(file);
                }
            }
        }
    }

    /**
     * Determines if a file should be signed based on its extension.
     */
    private boolean shouldSignFile(File file) {
        String name = file.getName().toLowerCase();
        return name.endsWith(".mtf") || name.endsWith(".blk");
    }

    /**
     * Sign a single file.
     */
    private void signFile(File file, PrivateKey privateKey) throws Exception {

        if (verifyFile(file, new File("/Users/coppio/Projects/megamek/megamek/resources/public.key"))) {
            return;
        }

        // Read the file content
        String content = Files.readString(file.toPath());

        // Remove existing signature if present
        int signaturePos = content.lastIndexOf(SIGNATURE_START);
        if (signaturePos != -1) {
            content = content.substring(0, signaturePos);
        }

        // Ensure content ends with a single newline
        if (!content.endsWith("\n")) {
            content += "\n";
        }

        // Sign the content
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initSign(privateKey);
        signature.update(content.getBytes(StandardCharsets.UTF_8));
        byte[] signatureBytes = signature.sign();

        // Encode the signature in Base64
        String encodedSignature = Base64.getEncoder().encodeToString(signatureBytes);

        // Add the signature to the file
        content += SIGNATURE_START + encodedSignature + SIGNATURE_END;

        // Write back to the file
        Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Load a private key from a file.
     */
    private PrivateKey loadPrivateKey(File keyFile) throws Exception {
        byte[] keyBytes = Files.readAllBytes(keyFile.toPath());

        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory factory = KeyFactory.getInstance("RSA");
        return factory.generatePrivate(spec);
    }

    /**
     * Verify a file's signature.
     */
    public static boolean verifyFile(File file, File publicKeyFile) throws Exception {
        // Read the file content

        // Read public key
        byte[] keyBytes = Files.readAllBytes(publicKeyFile.toPath());
        X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
        KeyFactory keyFactory = KeyFactory.getInstance("RSA");
        PublicKey publicKey = keyFactory.generatePublic(keySpec);

        return verifyFile(file, publicKey);
    }

    /**
     * Verify a file's signature.
     */
    public static boolean verifyFile(File file, PublicKey publicKey) throws Exception {
        // Read the file content
        String content = Files.readString(file.toPath());
        return verifyFile(content, publicKey);
    }

    /**
     * Verify a file's signature.
     */
    public static boolean verifyFile(String content, PublicKey publicKey) throws Exception {
        // Find signature
        int signatureStartPos = content.lastIndexOf(SIGNATURE_START);
        if (signatureStartPos == -1) {
            logger.debug("No signature found in the content");
            return false;
        }

        int signatureEndPos = content.lastIndexOf(SIGNATURE_END);
        if (signatureEndPos == -1 || signatureEndPos < signatureStartPos) {
            logger.debug("Invalid signature format");
            return false;
        }

        // Extract signature
        String encodedSignature = content.substring(
            signatureStartPos + SIGNATURE_START.length(),
            signatureEndPos
        );

        byte[] signatureBytes = Base64.getDecoder().decode(encodedSignature);

        // Get content without signature
        String contentWithoutSignature = content.substring(0, signatureStartPos);

        // Verify the signature
        Signature signature = Signature.getInstance(SIGNATURE_ALGORITHM);
        signature.initVerify(publicKey);
        signature.update(contentWithoutSignature.getBytes(StandardCharsets.UTF_8));

        boolean isValid = signature.verify(signatureBytes);
        return isValid;
    }
}
