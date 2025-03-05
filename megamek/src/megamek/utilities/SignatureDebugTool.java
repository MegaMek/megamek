package megamek.utilities;

import megamek.common.util.SignatureUtil;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Debug utility for troubleshooting signature verification issues.
 * This can be run from the command line.
 */
public class SignatureDebugTool {

    private static final Pattern SIGNATURE_PATTERN =
        Pattern.compile("<signature>\\s*(.*?)\\s*</signature>$", Pattern.DOTALL);

    public static void main(String[] args) {
        if (args.length != 1) {
            System.out.println("Usage: SignatureDebugTool <file_path>");
            return;
        }

        File file = new File(args[0]);
        if (!file.exists()) {
            System.out.println("File not found: " + file.getAbsolutePath());
            return;
        }

        try {
            // Read the file content
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            System.out.println("File exists with size: " + content.length() + " bytes");

            // Extract signature if present
            Matcher matcher = SIGNATURE_PATTERN.matcher(content);
            if (!matcher.find()) {
                System.out.println("ERROR: No signature found in the file");
                return;
            }

            String base64Signature = matcher.group(1).trim();
            System.out.println("Signature found: " + base64Signature.substring(0, 20) + "...");

            // Get content without signature
            String contentWithoutSignature = content.substring(0, matcher.start());

            // Calculate hash of content for debugging
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] contentHash = digest.digest(contentWithoutSignature.getBytes(StandardCharsets.UTF_8));
            System.out.println("Content Hash: " + Base64.getEncoder().encodeToString(contentHash));

            // Try to verify using SignatureUtil
            boolean verified = SignatureUtil.isCanonicalFile(file);
            System.out.println("Verification result: " + (verified ? "SUCCESS" : "FAILED"));

            if (!verified) {
                System.out.println("\nDiagnostic info:");
                System.out.println("- Last 50 chars before signature: " +
                    contentWithoutSignature.substring(Math.max(0, contentWithoutSignature.length() - 50)));
                System.out.println("- Check for hidden characters or incorrect line endings");
                System.out.println("- Ensure the public key matches the private key used for signing");
                System.out.println("- Character encoding issues might be present");
            }

        } catch (Exception e) {
            System.out.println("Error during verification: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
