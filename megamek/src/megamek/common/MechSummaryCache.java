package megamek.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import megamek.common.loaders.EntityLoadingException;

import com.sun.java.util.collections.*;

/*
 * Setting this up as static so a client and server running in the same
 * process can share it
 */

public class MechSummaryCache {

    public static interface Listener {
        void doneLoading();
    }

    private static final MechSummaryCache m_instance = new MechSummaryCache();

    private static boolean initialized = false;
    private static boolean initializing = false;
    private static ArrayList listeners = new ArrayList();

    public static synchronized MechSummaryCache getInstance() {
        if (!initialized && !initializing) {
            initializing = true;
            Thread t = new Thread(new Runnable() {
                public void run() {
                    m_instance.loadMechData();
                }
            });
            t.setPriority(Thread.NORM_PRIORITY - 1);
            t.start();
        }
        return m_instance;
    }

    public static boolean isInitialized() {
        return initialized;
    }

    public static void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public static void removeListener(Listener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private MechSummary[] m_data;
    private Map m_nameMap;
    private Hashtable hFailedFiles;
    private int cacheCount;
    private int fileCount;
    private int zipCount;

    private static final char SEPARATOR = '|';
    private static final File ROOT = new File(Settings.mechDirectory);
    private static final File CACHE = new File(ROOT, "units.cache");

    private MechSummaryCache() {
        m_nameMap = new HashMap();
    }

    public MechSummary[] getAllMechs() {
        block();
        return m_data;
    }

    private static void block() {
        if (!initialized) {
            synchronized (m_instance) {
                try {
                    m_instance.wait();
                } catch (Exception e) {
                    ;
                }
            }
        }
    }

    public MechSummary getMech(String sRef) {
        block();
        return (MechSummary) m_nameMap.get(sRef);
    }

    public Hashtable getFailedFiles() {
        block();
        return hFailedFiles;
    }

    private void loadMechData() {
        Vector vMechs = new Vector();
        Set sKnownFiles = new HashSet();
        long lLastCheck = 0;

        hFailedFiles = new Hashtable();

        System.out.println("");
        System.out.println("Reading unit files:");

        // check the cache
        try {
            if (CACHE.exists() && CACHE.lastModified() >= megamek.MegaMek.TIMESTAMP) {
                System.out.println("Reading from unit cache file");
                lLastCheck = CACHE.lastModified();
                BufferedReader br = new BufferedReader(new FileReader(CACHE));
                String s;
                while ((s = br.readLine()) != null) {
                    MechSummary ms = new MechSummary();
                    // manually do a string tokenizer.  Much faster
                    int nIndex1 = s.indexOf(SEPARATOR);
                    ms.setName(s.substring(0, nIndex1));
                    int nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setChassis(s.substring(nIndex1 + 1, nIndex2));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setModel(s.substring(nIndex1 + 1, nIndex2));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setUnitType(s.substring(nIndex1 + 1, nIndex2));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setSourceFile(new File(s.substring(nIndex1 + 1, nIndex2)));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setEntryName(s.substring(nIndex1 + 1, nIndex2));
                    // have to translate "null" to null
                    if (ms.getEntryName().equals("null")) {
                        ms.setEntryName(null);
                    }
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setYear(Integer.parseInt(s.substring(nIndex1 + 1, nIndex2)));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setType(Integer.parseInt(s.substring(nIndex1 + 1, nIndex2)));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setTons(Integer.parseInt(s.substring(nIndex1 + 1, nIndex2)));
                    ms.setBV(Integer.parseInt(s.substring(nIndex2 + 1)));

                    // Verify that this file still exists and is older than
                    //  the cache.
                    File fSource = ms.getSourceFile();
                    if (fSource.exists() && fSource.lastModified() < lLastCheck) {
                        vMechs.addElement(ms);
                        sKnownFiles.add(ms.getSourceFile().toString());
                        cacheCount++;
                    }
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to load unit cache: " + e.getMessage());
            e.printStackTrace();
        }

        // load any changes since the last check time
        boolean bNeedsUpdate = loadMechsFromDirectory(vMechs, sKnownFiles, lLastCheck, ROOT);

        // convert to array
        m_data = new MechSummary[vMechs.size()];
        vMechs.copyInto(m_data);

        // store map references
        for (int x = 0; x < m_data.length; x++) {
            m_nameMap.put(m_data[x].getName(), m_data[x]);
        }

        // save updated cache back to disk
        if (bNeedsUpdate) {
            try {
                saveCache();
            } catch (Exception e) {
                System.out.println("Unable to save mech cache");
            }
        }

        System.out.println(m_data.length + " units loaded.");
        if (hFailedFiles.size() > 0) {
            System.out.println(hFailedFiles.size() + " units failed to load...");
        }
        Enumeration failedUnits = hFailedFiles.keys();
        Enumeration failedUnitsDesc = hFailedFiles.elements();
        while (failedUnits.hasMoreElements()) {
            System.out.println("--" + failedUnits.nextElement());
            System.out.println("---" + failedUnitsDesc.nextElement());
        }

        System.out.println("");
        done();
    }

    private void done() {
        initialized = true;
        synchronized (m_instance) {
            m_instance.notifyAll();
        }
        synchronized (listeners) {
            for (int i = 0; i < listeners.size(); i++) {
				((Listener)listeners.get(i)).doneLoading();
            }
        }
    }

    private void saveCache() throws Exception {
        System.out.println("Saving unit cache");
        FileWriter wr = new FileWriter(CACHE);
        for (int x = 0; x < m_data.length; x++) {
            wr.write(
                m_data[x].getName()
                    + SEPARATOR
                    + m_data[x].getChassis()
                    + SEPARATOR
                    + m_data[x].getModel()
                    + SEPARATOR
                    + m_data[x].getUnitType()
                    + SEPARATOR
                    + m_data[x].getSourceFile().getPath()
                    + SEPARATOR
                    + m_data[x].getEntryName()
                    + SEPARATOR
                    + m_data[x].getYear()
                    + SEPARATOR
                    + m_data[x].getType()
                    + SEPARATOR
                    + m_data[x].getTons()
                    + SEPARATOR
                    + m_data[x].getBV()
                    + "\r\n");
        }
        wr.flush();
        wr.close();
    }

    // Loading a complete mech object for each summary is a bear and should be 
    // changed, but it lets me use the existing parsers
    private boolean loadMechsFromDirectory(Vector vMechs, Set sKnownFiles, long lLastCheck, File fDir) {
        boolean bNeedsUpdate = false;
        System.out.println("Looking in " + fDir.getPath());
        String[] sa = fDir.list();

        for (int x = 0; x < sa.length; x++) {
            File f = new File(fDir, sa[x]);
            if (f.equals(CACHE)) {
                continue;
            }
            if (f.isDirectory()) {
                if (f.getName().toLowerCase().equals("unsupported")) {
                    // Mechs in this directory are ignored because
                    //  they have features not implemented in MM yet.
                    continue;
                }
                // recursion is fun
                bNeedsUpdate |= loadMechsFromDirectory(vMechs, sKnownFiles, lLastCheck, f);
                continue;
            }
            if (f.getName().indexOf('.') == -1) {
                continue;
            }
            if (f.getName().toLowerCase().endsWith(".txt")) {
                continue;
            }
            if (f.getName().toLowerCase().endsWith(".log")) {
                continue;
            }
            if (f.getName().toLowerCase().endsWith(".zip")) {
                bNeedsUpdate |= loadMechsFromZipFile(vMechs, sKnownFiles, lLastCheck, f);
                continue;
            }
            if (f.lastModified() < lLastCheck && sKnownFiles.contains(f.toString())) {
                continue;
            }
            try {
                System.out.println("Loading from " + f);
                MechFileParser mfp = new MechFileParser(f);
                Entity m = mfp.getEntity();
                MechSummary ms = new MechSummary();
                ms.setName(m.getShortName());
                ms.setChassis(m.getChassis());
                ms.setModel(m.getModel());
                ms.setUnitType(MechSummary.determineUnitType(m));
                ms.setSourceFile(f);
                ms.setEntryName(null);
                ms.setYear(m.getYear());
                ms.setType(m.getTechLevel());
                ms.setTons((int) m.getWeight());
                ms.setBV(m.calculateBattleValue());
                vMechs.addElement(ms);
                sKnownFiles.add(f.toString());
                bNeedsUpdate = true;
                fileCount++;
            } catch (EntityLoadingException ex) {
                System.err.println("Unable to load file " + f.getName() + " : " + ex.getMessage());
                hFailedFiles.put(f.toString(), ex.getMessage());
                continue;
            }
        }

        return bNeedsUpdate;
    }

    private boolean loadMechsFromZipFile(Vector vMechs, Set sKnownFiles, long lLastCheck, File fZipFile) {
        boolean bNeedsUpdate = false;
        ZipFile zFile;
        try {
            zFile = new ZipFile(fZipFile);
        } catch (Exception ex) {
            System.err.println("Unable to load file " + fZipFile.getName() + " : " + ex.getMessage());
            return false;
        }
        System.out.println("Looking in zip file " + fZipFile.getPath());

        for (java.util.Enumeration i = zFile.entries(); i.hasMoreElements();) {
            ZipEntry zEntry = (ZipEntry) i.nextElement();

            if (zEntry.isDirectory()) {
                if (zEntry.getName().toLowerCase().equals("unsupported")) {
                    System.err.println(
                        "Do not place special 'unsupported' type folders in zip files, they must be uncompressed directories to work properly.  Note that you may place zip files inside of 'unsupported' type folders, though.");
                }
                continue;
            }
            if (zEntry.getName().toLowerCase().endsWith(".txt")) {
                continue;
            }
            if (Math.max(fZipFile.lastModified(), zEntry.getTime()) < lLastCheck
                && sKnownFiles.contains(fZipFile.toString())) {
                continue;
            }

            try {
                System.out.println("Loading from " + fZipFile.getPath() + " >> " + zEntry.getName());
                MechFileParser mfp = new MechFileParser(zFile.getInputStream(zEntry), zEntry.getName());
                Entity m = mfp.getEntity();
                MechSummary ms = new MechSummary();
                ms.setName(m.getShortName());
                ms.setChassis(m.getChassis());
                ms.setModel(m.getModel());
                ms.setUnitType(MechSummary.determineUnitType(m));
                ms.setSourceFile(fZipFile);
                ms.setEntryName(zEntry.getName());
                ms.setYear(m.getYear());
                ms.setType(m.getTechLevel());
                ms.setTons((int) m.getWeight());
                ms.setBV(m.calculateBattleValue());
                vMechs.addElement(ms);
                sKnownFiles.add(zEntry.getName());
                bNeedsUpdate = true;
                zipCount++;
            } catch (Exception ex) {
                System.err.println("Unable to load file " + zEntry.getName() + " : " + ex.getMessage());
                hFailedFiles.put(zEntry.getName(), ex.getMessage());
                continue;
            }
        }

        try {
            zFile.close();
        } catch (Exception ex) {
            // whatever.
        }

        return bNeedsUpdate;
    }

    public int getCacheCount() {
        return cacheCount;
    }

    public int getFileCount() {
        return fileCount;
    }

    public int getZipCount() {
        return zipCount;
    }

}
