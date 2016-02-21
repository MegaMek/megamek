package megamek.common.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import megamek.common.preference.PreferenceManager;

public class MegaMekUtil {
	
	public static String VERSION = "0.41.15-git"; //$NON-NLS-1$
	
    public static long TIMESTAMP = new File(PreferenceManager
            .getClientPreferences().getLogDirectory()
            + File.separator
            + "timestamp").lastModified(); //$NON-NLS-1$

	/**
     * Calculates the SHA-256 hash of the MegaMek.jar file
     * Used primarily for purposes of checksum comparison when
     * connecting a new client.
     * @return String representing the SHA-256 hash
     */
    public static String getMegaMekSHA256() {
        StringBuilder sb = new StringBuilder();
        byte[] buffer = new byte[8192];
        DigestInputStream in = null;

        // Assume UNIX/Linux, which has the jar in the root folder
        String filename = "MegaMek.jar";
        // If it isn't UNIX/Linux, maybe it's Windows where we've stashed it in the lib folder
        if (new File("lib/"+filename).exists()) {
            filename = "lib/"+filename;
        // And if it isn't either UNIX/Linux or Windows it's got to be Mac, where it's buried inside the app
        } else if (new File("MegaMek.app/Contents/Resources/Java/"+filename).exists()) {
            filename = "MegaMek.app/Contents/Resources/Java/"+filename;
        }

        // Calculate the digest for the given file.
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            in = new DigestInputStream(new FileInputStream(filename), md);
            while (0 < in.read(buffer)) {}
            // gets digest
            byte[] digest = md.digest();
            // convert the byte to hex format
            for (byte d : digest) {
                sb.append(String.format("%02x", d));
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } catch (NoSuchAlgorithmException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            return null;
        } finally {
            try {
                in.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                return null;
            }
        }
        return sb.toString();
    }

    /**
     * This function returns the memory used in the heap (heap memory - free
     * memory).
     *
     * @return memory used in kB
     */
    public static String getMemoryUsed() {
        long heap = Runtime.getRuntime().totalMemory();
        long free = Runtime.getRuntime().freeMemory();
        long used = (heap - free) / 1024;
        return (used) + " kB"; //$NON-NLS-1$
    }
}
