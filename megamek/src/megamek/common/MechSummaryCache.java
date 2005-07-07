/*
 * MegaMek - Copyright (C) 2000,2001,2002,2003,2004 Ben Mazur (bmazur@sev.org)
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */

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
import megamek.common.preference.PreferenceManager;
import megamek.common.verifier.EntityVerifier;
import megamek.common.verifier.TestEntity;
import megamek.common.verifier.TestMech;
import megamek.common.verifier.TestTank;

import com.sun.java.util.collections.*;

/**
 * Cache of the Mech summary information.
 * Implemented as Singleton so a client and server running in the same
 * process can share it
 */
public class MechSummaryCache {

    public static interface Listener {
        void doneLoading();
    }

    private static final MechSummaryCache m_instance = new MechSummaryCache();

    private boolean initialized = false;
    private boolean initializing = false;
    private ArrayList listeners = new ArrayList();

    private StringBuffer loadReport = new StringBuffer();
    private final static String CONFIG_FILENAME = "data/mechfiles/UnitVerifierOptions.xml"; //should be a client option?
    private EntityVerifier entityVerifier = null;

    public static synchronized MechSummaryCache getInstance() {
        if (!m_instance.initialized && !m_instance.initializing) {
            m_instance.initializing = true;
            Thread t = new Thread(new Runnable() {
                public void run() {
                    m_instance.loadMechData();
                }
            }, "Mech Cach Loader");
            t.setPriority(Thread.NORM_PRIORITY - 1);
            t.start();
        }
        return m_instance;
    }

    public boolean isInitialized() {
        return initialized;
    }

    public void addListener(Listener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(Listener listener) {
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
    private static final File ROOT = new File(PreferenceManager.getClientPreferences().getMechDirectory());
    private static final File CACHE = new File(ROOT, "units.cache");

    private MechSummaryCache() {
        m_nameMap = new HashMap();
    }

    public MechSummary[] getAllMechs() {
        block();
        return m_data;
    }

    private void block() {
        if (!initialized) {
            synchronized (m_instance) {
                try {
                    m_instance.wait();
                } catch (Exception e) {

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
        entityVerifier = new EntityVerifier(new File(CONFIG_FILENAME));
        hFailedFiles = new Hashtable();

        EquipmentType.initializeTypes(); // load master equipment lists

        loadReport.append("\n");
        loadReport.append("Reading unit files:\n");

        // check the cache
        try {
            if (CACHE.exists() && CACHE.lastModified() >= megamek.MegaMek.TIMESTAMP) {
                loadReport.append("  Reading from unit cache file...\n");
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
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 + 1);
                    ms.setBV(Integer.parseInt(s.substring(nIndex1 + 1, nIndex2)));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 +1);
                    ms.setLevel(s.substring(nIndex1 + 1, nIndex2));
                    nIndex1 = nIndex2;
                    nIndex2 = s.indexOf(SEPARATOR, nIndex1 +1);
                    ms.setCost(Integer.parseInt(s.substring(nIndex1 + 1,nIndex2)));
                    ms.setCanon(s.substring(nIndex2+1).equals("T")? true : false);

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
            loadReport.append("  Unable to load unit cache: ")
                .append(e.getMessage()).append("\n");
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
                loadReport.append("  Unable to save mech cache\n");
            }
        }

        loadReport.append(m_data.length).append(" units loaded.\n");

        if (hFailedFiles.size() > 0) {
            loadReport.append("  ").append(hFailedFiles.size())
                .append(" units failed to load...\n");
        }
        /*
        Enumeration failedUnits = hFailedFiles.keys();
        Enumeration failedUnitsDesc = hFailedFiles.elements();
        while (failedUnits.hasMoreElements()) {
            loadReport.append("    ").append(failedUnits.nextElement())
                .append("\n");
            loadReport.append("    --")
                .append(failedUnitsDesc.nextElement()).append("\n");
        }
        */

        System.out.print(loadReport.toString());

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
        loadReport.append("Saving unit cache.\n");
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
                    + SEPARATOR
                    + m_data[x].getLevel()
                    + SEPARATOR
                    + m_data[x].getCost()
                    + SEPARATOR
                    + (m_data[x].isCanon()? 'T' : 'F')
                    + "\r\n");
        }
        wr.flush();
        wr.close();
    }

    private MechSummary getSummary(Entity e, File f, String entry) {
        MechSummary ms = new MechSummary();
        ms.setName(e.getShortName());
        ms.setChassis(e.getChassis());
        ms.setModel(e.getModel());
        ms.setUnitType(MechSummary.determineUnitType(e));
        ms.setSourceFile(f);
        ms.setEntryName(entry);
        ms.setYear(e.getYear());
        ms.setType(e.getTechLevel());
        ms.setTons((int) e.getWeight());
        ms.setBV(e.calculateBattleValue());
        ms.setLevel(TechConstants.T_SIMPLE_LEVEL[e.getTechLevel()]);
        ms.setCost((int)e.getCost());
        ms.setCanon(e.isCanon());
        // we can only test meks and vehicles right now
        if (e instanceof Mech || e instanceof Tank) {
            TestEntity testEntity = null;
            if (e instanceof Mech)
                testEntity = new TestMech((Mech)e, entityVerifier.mechOption, null);
            else
                testEntity = new TestTank((Tank)e, entityVerifier.tankOption, null);
            if (!testEntity.correctEntity(new StringBuffer())) {
                ms.setLevel("F");
            }
        }
        return ms;
    }

    // Loading a complete mech object for each summary is a bear and should be 
    // changed, but it lets me use the existing parsers
    private boolean loadMechsFromDirectory(Vector vMechs, Set sKnownFiles, long lLastCheck, File fDir) {
        boolean bNeedsUpdate = false;
        loadReport.append("  Looking in ").append(fDir.getPath())
            .append("...\n");
        int thisDirectoriesFileCount = 0;
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
            if (f.getName().equals("UnitVerifierOptions.xml")) {
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
                MechFileParser mfp = new MechFileParser(f);
                Entity e = mfp.getEntity();
                MechSummary ms = getSummary(e, f, null);
                vMechs.addElement(ms);
                sKnownFiles.add(f.toString());
                bNeedsUpdate = true;
                thisDirectoriesFileCount++;
                fileCount++;
                Enumeration failedEquipment = e.getFailedEquipment();
                if (failedEquipment.hasMoreElements()) {
                    loadReport.append("    Loading from ").append(f)
                        .append("\n");
                    while (failedEquipment.hasMoreElements()) {
                        loadReport.append("      Failed to load equipment: ")
                            .append(failedEquipment.nextElement())
                            .append("\n");
                    }
                }
            } catch (EntityLoadingException ex) {
                loadReport.append("    Loading from ")
                    .append(f).append("\n");
                loadReport.append("***   Unable to load file: ")
                    .append(ex.getMessage()).append("\n");
                hFailedFiles.put(f.toString(), ex.getMessage());
                continue;
            }
        }

        loadReport.append("  ...loaded ").append(thisDirectoriesFileCount)
            .append(" files.\n");

        return bNeedsUpdate;
    }

    private boolean loadMechsFromZipFile(Vector vMechs, Set sKnownFiles, long lLastCheck, File fZipFile) {
        boolean bNeedsUpdate = false;
        ZipFile zFile;
        int thisZipFileCount = 0;
        try {
            zFile = new ZipFile(fZipFile);
        } catch (Exception ex) {
            loadReport.append("  Unable to load file ")
                .append(fZipFile.getName()).append(": ")
                .append(ex.getMessage()).append("\n");
            return false;
        }
        loadReport.append("  Looking in zip file ")
            .append(fZipFile.getPath()).append("...\n");

        for (java.util.Enumeration i = zFile.entries(); i.hasMoreElements();) {
            ZipEntry zEntry = (ZipEntry) i.nextElement();

            if (zEntry.isDirectory()) {
                if (zEntry.getName().toLowerCase().equals("unsupported")) {
                    loadReport.append("  Do not place special 'unsupported' type folders in zip files, they must\n    be uncompressed directories to work properly.  Note that you may place\n    zip files inside of 'unsupported' type folders, though.\n");
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
                MechFileParser mfp = new MechFileParser(zFile.getInputStream(zEntry), zEntry.getName());
                Entity e = mfp.getEntity();
                MechSummary ms = getSummary(e, fZipFile, zEntry.getName());
                vMechs.addElement(ms);
                sKnownFiles.add(zEntry.getName());
                bNeedsUpdate = true;
                thisZipFileCount++;
                zipCount++;
                Enumeration failedEquipment = e.getFailedEquipment();
                if (failedEquipment.hasMoreElements()) {
                    loadReport.append("    Loading from zip file")
                        .append(" >> ").append(zEntry.getName()).append("\n");
                    while (failedEquipment.hasMoreElements()) {
                        loadReport.append("      Failed to load equipment: ")
                            .append(failedEquipment.nextElement())
                            .append("\n");
                    }
                }
            } catch (Exception ex) {
                loadReport.append("    Loading from zip file")
                    .append(" >> ").append(zEntry.getName()).append("\n");
                loadReport.append("      Unable to load file: ")
                    .append(ex.getMessage()).append("\n");
                hFailedFiles.put(zEntry.getName(), ex.getMessage());
                continue;
            }
        }

        try {
            zFile.close();
        } catch (Exception ex) {
            // whatever.
        }

        loadReport.append("  ...loaded ").append(thisZipFileCount)
            .append(" files.\n");

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
