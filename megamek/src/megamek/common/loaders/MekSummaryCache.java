/*
  Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright (C) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2002-2025 The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License (GPL),
 * version 3 or (at your option) any later version,
 * as published by the Free Software Foundation.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * A copy of the GPL should have been included with this project;
 * if not, see <https://www.gnu.org/licenses/>.
 *
 * NOTICE: The MegaMek organization is a non-profit group of volunteers
 * creating free software for the BattleTech community.
 *
 * MechWarrior, BattleMech, `Mech and AeroTech are registered trademarks
 * of The Topps Company, Inc. All Rights Reserved.
 *
 * Catalyst Game Labs and the Catalyst Game Labs logo are trademarks of
 * InMediaRes Productions, LLC.
 *
 * MechWarrior Copyright Microsoft Corporation. MegaMek was created under
 * Microsoft's "Game Content Usage Rules"
 * <https://www.xbox.com/en-US/developers/rules> and it is not endorsed by or
 * affiliated with Microsoft.
 */

package megamek.common.loaders;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import megamek.common.Configuration;
import megamek.common.MPCalculationSetting;
import megamek.common.TechConstants;
import megamek.common.alphaStrike.ASUnitType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.annotations.Nullable;
import megamek.common.battleArmor.BattleArmor;
import megamek.common.battleArmor.BattleArmorHandles;
import megamek.common.bays.*;
import megamek.common.equipment.DockingCollar;
import megamek.common.equipment.EquipmentType;
import megamek.common.equipment.Transporter;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.Aero;
import megamek.common.units.DropShuttleBay;
import megamek.common.units.Entity;
import megamek.common.units.EntityWeightClass;
import megamek.common.units.Infantry;
import megamek.common.units.Mek;
import megamek.common.units.NavalRepairFacility;
import megamek.common.units.Tank;
import megamek.common.units.UnitType;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.TestEntity;
import megamek.logging.MMLogger;

/**
 * Cache of the Mek summary information. Implemented as Singleton so a client and server running in the same process can
 * share it
 *
 * @author arlith
 */
public class MekSummaryCache {
    private static final MMLogger logger = MMLogger.create(MekSummaryCache.class);

    private enum LoadOperation {
        INITIAL_LOAD,
        REFRESH,
        REBUILD
    }

    public interface Listener {
        void doneLoading();
    }

    public static final String FILENAME_UNITS_CACHE = "units.cache";
    public static final String FILENAME_LOOKUP = "name_changes.txt";

    private static final List<String> SUPPORTED_FILE_EXTENSIONS = List.of(".mtf", ".blk", ".mep", ".hmv", ".tdb",
          ".hmp", ".zip");

    private static MekSummaryCache instance;
    private static volatile boolean disposeInstance = false;

    private volatile boolean initialized = false;
    private volatile boolean initializing = false;

    private MekSummary[] data;
    private final Map<String, MekSummary> nameMap;
    private final Map<String, MekSummary> fileNameMap;
    private Map<String, String> failedFiles;
    private int cacheCount;
    private int fileCount;
    private int zipCount;

    private final List<Listener> listeners = new ArrayList<>();

    private StringBuffer loadReport = new StringBuffer();
    private volatile Thread loader;

    private LoadOperation queuedLoadOperation;
    private boolean queuedIgnoreUnofficial;
    private static final Object lock = new Object();

    public static synchronized MekSummaryCache getInstance() {
        return getInstance(false);
    }

    public static synchronized MekSummaryCache getInstance(boolean ignoreUnofficial) {
        if (instance == null) {
            instance = new MekSummaryCache();
        }

        instance.ensureInitialized(ignoreUnofficial);
        return instance;
    }

    /**
     * Checks the unit files directory for any changes since the last time the unit cache was loaded. If there are any
     * updates, the new cache is saved.
     *
     * @param ignoreUnofficial If true, skips unofficial directories
     */
    public static synchronized void refreshUnitData(boolean ignoreUnofficial) {
        if (instance == null) {
            instance = new MekSummaryCache();
        }

        instance.requestLoad(LoadOperation.REFRESH, ignoreUnofficial);
    }

    /**
     * Rebuilds the unit cache from scratch, ignoring any existing on-disk cache data.
     *
     * @param ignoreUnofficial If true, skips unofficial directories
     */
    public static synchronized void rebuildUnitData(boolean ignoreUnofficial) {
        if (instance == null) {
            instance = new MekSummaryCache();
        }

        instance.requestLoad(LoadOperation.REBUILD, ignoreUnofficial);
    }

    public static void dispose() {
        if (instance != null) {
            synchronized (lock) {
                disposeInstance = true;
                instance.queuedLoadOperation = null;
                if (instance.initializing && (instance.loader != null)) {
                    instance.loader.interrupt();
                } else {
                    instance = null;
                }
            }
        }
    }

    private void ensureInitialized(boolean ignoreUnofficial) {
        synchronized (lock) {
            if (!initialized && !initializing) {
                startLoadLocked(LoadOperation.INITIAL_LOAD, ignoreUnofficial);
            }
        }
    }

    private void requestLoad(LoadOperation loadOperation, boolean ignoreUnofficial) {
        synchronized (lock) {
            if (initializing) {
                queuedLoadOperation = loadOperation;
                queuedIgnoreUnofficial = ignoreUnofficial;
                if (loader != null) {
                    loader.interrupt();
                }
                return;
            }

            startLoadLocked(loadOperation, ignoreUnofficial);
        }
    }

    private void startLoadLocked(LoadOperation loadOperation, boolean ignoreUnofficial) {
        initializing = true;
        initialized = false;
        disposeInstance = false;
        queuedLoadOperation = null;
        resetLoadStats();

        Thread nextLoader = new Thread(() -> runLoad(loadOperation, ignoreUnofficial), getThreadName(loadOperation));
        nextLoader.setPriority(Thread.NORM_PRIORITY - 1);
        loader = nextLoader;
        nextLoader.start();
    }

    private void runLoad(LoadOperation loadOperation, boolean ignoreUnofficial) {
        switch (loadOperation) {
            case INITIAL_LOAD:
                loadMekData(ignoreUnofficial);
                break;
            case REFRESH:
                refreshCache(ignoreUnofficial);
                break;
            case REBUILD:
                rebuildCache(ignoreUnofficial);
                break;
            default:
                throw new IllegalStateException("Unexpected load operation: " + loadOperation);
        }
    }

    private String getThreadName(LoadOperation loadOperation) {
        return switch (loadOperation) {
            case REBUILD -> "Mek Cache Rebuilder";
            case REFRESH -> "Mek Cache Refresher";
            default -> "Mek Cache Loader";
        };
    }

    private boolean shouldStopLoading() {
        Thread currentThread = Thread.currentThread();
        return disposeInstance || currentThread.isInterrupted() || ((loader != null) && (currentThread != loader));
    }

    /**
     * Get the directory for the unit cache file.
     *
     * @return The path to the directory containing the unit cache.
     */
    public static File getUnitCacheDir() {
        return Configuration.unitsDir();
    }

    public boolean isInitialized() {
        return initialized;
    }

    public boolean isLoading() {
        return initializing;
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

    private MekSummaryCache() {
        nameMap = new HashMap<>();
        fileNameMap = new HashMap<>();
    }

    public MekSummary[] getAllMeks() {
        block();
        return data;
    }

    private void block() {
        if (!initialized) {
            synchronized (lock) {
                while (!initialized) {
                    try {
                        lock.wait();
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                }
            }
        }
    }

    @Nullable
    public MekSummary getMek(String sRef) {
        block();

        if (nameMap.containsKey(sRef)) {
            return nameMap.get(sRef);
        }

        return fileNameMap.get(sRef);
    }

    public Map<String, String> getFailedFiles() {
        block();
        return failedFiles;
    }

    public void loadMekData() {
        loadMekData(false);
    }

    public void loadMekData(boolean ignoreUnofficial) {
        resetLoadStats();
        Vector<MekSummary> vMeks = new Vector<>();
        Set<String> sKnownFiles = new HashSet<>();
        long lLastCheck = 0;

        EquipmentType.initializeTypes(); // load master equipment lists

        loadReport.append("\n");
        loadReport.append("Reading unit files:\n");

        if (!ignoreUnofficial) {
            File unit_cache_path = new MegaMekFile(getUnitCacheDir(), FILENAME_UNITS_CACHE).getFile();
            // check the cache
            try {
                if (unit_cache_path.exists()) {
                    loadReport.append("  Reading from unit cache file...\n");
                    lLastCheck = unit_cache_path.lastModified();
                    InputStream inputStream = new BufferedInputStream(new FileInputStream(unit_cache_path));
                    ObjectInputStream fin = new ObjectInputStream(inputStream);
                    Integer newUnits = (Integer) fin.readObject();
                    for (int i = 0; i < newUnits; i++) {
                        if (shouldStopLoading()) {
                            done();
                            fin.close();
                            inputStream.close();
                            return;
                        }
                        MekSummary ms = (MekSummary) fin.readObject();
                        // Verify that this file still exists and is older than
                        // the cache.
                        File fSource = ms.getSourceFile();
                        if (fSource.exists()) {
                            vMeks.addElement(ms);
                            if (null == ms.getEntryName()) {
                                sKnownFiles.add(fSource.toString());
                            } else {
                                sKnownFiles.add(ms.getEntryName());
                            }
                            cacheCount++;
                        }
                    }
                    fin.close();
                    inputStream.close();
                }
            } catch (Exception ex) {
                loadReport.append("  Unable to load unit cache: ").append(ex.getMessage()).append("\n");
                logger.error(loadReport.toString(), ex);
            }
        }

        checkForChanges(ignoreUnofficial, vMeks, sKnownFiles, lLastCheck);
        if (shouldStopLoading()) {
            done();
            return;
        }
        if (!updateData(vMeks)) {
            done();
            return;
        }
        if (shouldStopLoading()) {
            done();
            return;
        }
        addLookupNames();
        if (shouldStopLoading()) {
            done();
            return;
        }
        logReport();

        done();
    }

    private void checkForChanges(boolean ignoreUnofficial, Vector<MekSummary> vMeks,
          Set<String> sKnownFiles, long lLastCheck) {
        // load any changes since the last check time
        boolean bNeedsUpdate = loadMeksFromDirectory(vMeks, sKnownFiles,
              lLastCheck, Configuration.unitsDir(), ignoreUnofficial);

        // Official units are in the internal dir, not in the user dirs or story arcs
        // dir
        if (!ignoreUnofficial) {
            // load units from the MM internal user data dir
            File userDataUnits = new File(Configuration.userDataDir(), Configuration.unitsDir().toString());
            if (userDataUnits.isDirectory()) {
                bNeedsUpdate |= loadMeksFromDirectory(vMeks, sKnownFiles, lLastCheck, userDataUnits, false);
            }

            // load units from the external user data dir
            String userDir = PreferenceManager.getClientPreferences().getUserDir();
            File userDataUnits2 = new File(userDir, "");
            if (!userDir.isBlank() && userDataUnits2.isDirectory()) {
                bNeedsUpdate |= loadMeksFromDirectory(vMeks, sKnownFiles, lLastCheck, userDataUnits2, false);
            }

            // load units from story arcs
            File storyArcsDir = Configuration.storyArcsDir();
            if (storyArcsDir.exists() && storyArcsDir.isDirectory()) {
                File[] storyArcsFiles = storyArcsDir.listFiles();
                if (storyArcsFiles != null) {
                    for (File file : storyArcsFiles) {
                        if (file.isDirectory()) {
                            File storyArcUnitsDir = new File(file.getPath() + "/data/mekfiles");
                            if (storyArcUnitsDir.exists() && storyArcUnitsDir.isDirectory()) {
                                bNeedsUpdate |= loadMeksFromDirectory(vMeks, sKnownFiles, lLastCheck, storyArcUnitsDir,
                                      false);
                            }
                        }
                    }
                }
            }
        }

        // save updated cache back to disk
        if (shouldStopLoading()) {
            return;
        }

        if (bNeedsUpdate) {
            saveCache(vMeks);
        }
    }

    private boolean updateData(Vector<MekSummary> vMeks) {
        // convert to array
        MekSummary[] updatedData = new MekSummary[vMeks.size()];
        vMeks.copyInto(updatedData);
        Map<String, MekSummary> updatedNameMap = new HashMap<>();
        Map<String, MekSummary> updatedFileNameMap = new HashMap<>();

        // store map references
        for (MekSummary element : updatedData) {
            if (shouldStopLoading()) {
                return false;
            }
            updatedNameMap.put(element.getName(), element);
            String entryName = element.getEntryName();
            if (entryName == null) {
                updatedFileNameMap.put(element.getSourceFile().getName(), element);
            } else {
                String unitName = entryName;

                if (unitName.contains("\\")) {
                    unitName = unitName.substring(unitName.lastIndexOf("\\") + 1);
                }

                if (unitName.contains("/")) {
                    unitName = unitName.substring(unitName.lastIndexOf("/") + 1);
                }

                updatedFileNameMap.put(unitName, element);
            }
        }

        data = updatedData;
        nameMap.clear();
        nameMap.putAll(updatedNameMap);
        fileNameMap.clear();
        fileNameMap.putAll(updatedFileNameMap);
        return true;
    }

    private void logReport() {
        loadReport.append(data.length).append(" units loaded.\n");

        if (!failedFiles.isEmpty()) {
            loadReport.append("  ").append(failedFiles.size())
                  .append(" units failed to load...\n");
        }

        logger.debug(loadReport.toString());
    }

    private void resetLoadStats() {
        loadReport = new StringBuffer();
        failedFiles = new HashMap<>();
        cacheCount = 0;
        fileCount = 0;
        zipCount = 0;
    }

    private void done() {
        List<Listener> listenersSnapshot;

        synchronized (lock) {
            if ((loader != null) && (Thread.currentThread() != loader)) {
                return;
            }

            if (disposeInstance) {
                initializing = false;
                initialized = false;
                loader = null;
                queuedLoadOperation = null;
                queuedIgnoreUnofficial = false;
                instance = null;
                lock.notifyAll();
                return;
            }

            if (queuedLoadOperation != null) {
                LoadOperation nextOperation = queuedLoadOperation;
                boolean nextIgnoreUnofficial = queuedIgnoreUnofficial;
                startLoadLocked(nextOperation, nextIgnoreUnofficial);
                return;
            }

            loader = null;
            initializing = false;
            initialized = true;
            lock.notifyAll();
        }

        synchronized (listeners) {
            listenersSnapshot = new ArrayList<>(listeners);
        }

        for (Listener listener : listenersSnapshot) {
            listener.doneLoading();
        }
    }

    private void saveCache(List<MekSummary> data) {
        if (shouldStopLoading()) {
            return;
        }

        loadReport.append("Saving unit cache.\n");
        try (FileOutputStream fos = new FileOutputStream(
              new MegaMekFile(getUnitCacheDir(), FILENAME_UNITS_CACHE).getFile());
              BufferedOutputStream bos = new BufferedOutputStream(fos);
              ObjectOutputStream oos = new ObjectOutputStream(bos)) {
            oos.writeObject(data.size());
            for (MekSummary element : data) {
                oos.writeObject(element);
            }
        } catch (Exception ex) {
            loadReport.append(" Unable to save mek cache\n");
            logger.error("", ex);
        }
    }

    private void refreshCache(boolean ignoreUnofficial) {
        if (data == null) {
            rebuildCache(ignoreUnofficial);
            return;
        }

        resetLoadStats();
        loadReport.append("Refreshing unit cache:\n");
        File unitCachePath = new MegaMekFile(getUnitCacheDir(), FILENAME_UNITS_CACHE).getFile();
        long lastCheck = unitCachePath.exists() ? unitCachePath.lastModified() : 0L;
        Vector<MekSummary> units = new Vector<>();
        Set<String> knownFiles = new HashSet<>();
        // Loop through current contents and make sure the file is still there.
        // Note which files are represented so we can skip them if they haven't changed
        for (MekSummary mekSummary : data) {
            if (shouldStopLoading()) {
                done();
                return;
            }
            File source = mekSummary.getSourceFile();
            if (source.exists()) {
                units.add(mekSummary);
                if (null == mekSummary.getEntryName()) {
                    knownFiles.add(source.toString());
                } else {
                    knownFiles.add(mekSummary.getEntryName());
                }
            }
        }

        // load any changes since the last check time
        checkForChanges(ignoreUnofficial, units, knownFiles, lastCheck);
        if (shouldStopLoading()) {
            done();
            return;
        }
        if (!updateData(units)) {
            done();
            return;
        }
        if (shouldStopLoading()) {
            done();
            return;
        }
        addLookupNames();
        if (shouldStopLoading()) {
            done();
            return;
        }
        logReport();

        done();
    }

    private void rebuildCache(boolean ignoreUnofficial) {
        resetLoadStats();
        EquipmentType.initializeTypes();

        loadReport.append("Rebuilding unit cache:\n");
        Vector<MekSummary> units = new Vector<>();
        Set<String> knownFiles = new HashSet<>();

        checkForChanges(ignoreUnofficial, units, knownFiles, 0L);
        if (shouldStopLoading()) {
            done();
            return;
        }
        if (!updateData(units)) {
            done();
            return;
        }
        if (shouldStopLoading()) {
            done();
            return;
        }
        addLookupNames();
        if (shouldStopLoading()) {
            done();
            return;
        }
        logReport();

        done();
    }

    private MekSummary getSummary(Entity e, File f, String entry) {
        MekSummary ms = new MekSummary();
        ms.setName(e.getShortNameRaw());
        ms.setChassis(e.getChassis());
        ms.setClanChassisName(e.getClanChassisName());
        ms.setModel(e.getModel());
        ms.setMulId(e.getMulId());
        ms.setUnitType(UnitType.getTypeName(e.getUnitType()));
        ms.setFullAccurateUnitType(Entity.getEntityTypeName(e.getEntityType()));
        ms.setEntityType(e.getEntityType());
        ms.setOmni(e.isOmni());
        if (e.getFluff().hasEmbeddedFluffImage()) {
            ms.setFluffImage(e.getFluff().getBase64FluffImage().getBase64String());
        }
        ms.setMilitary(e.isMilitary());
        ms.setMountedInfantry((e instanceof Infantry) && ((Infantry) e).getMount() != null);

        int tankTurrets = 0;
        if (e instanceof Tank) {
            tankTurrets = ((Tank) e).getTurretCount();
        }
        ms.setTankTurrets(tankTurrets);
        ms.setSourceFile(f);
        ms.setSource(e.getSource());
        ms.setEntryName(entry);
        ms.setYear(e.getYear());
        ms.setType(e.getTechLevel());
        if (TechConstants.convertFromNormalToSimple(e.getTechLevel()) == TechConstants.T_SIMPLE_UNOFFICIAL) {
            int[] alt = new int[3];
            Arrays.fill(alt, e.getTechLevel());
            ms.setAltTypes(alt);
        } else if (e.isClan()) {
            ms.setAltTypes(new int[] { TechConstants.T_CLAN_TW, TechConstants.T_CLAN_ADVANCED,
                                       TechConstants.T_CLAN_EXPERIMENTAL });
        } else if (e.getTechLevel() == TechConstants.T_INTRO_BOX_SET) {
            ms.setAltTypes(new int[] { TechConstants.T_INTRO_BOX_SET, TechConstants.T_IS_ADVANCED,
                                       TechConstants.T_IS_EXPERIMENTAL });
        } else {
            ms.setAltTypes(new int[] { TechConstants.T_IS_TW_NON_BOX, TechConstants.T_IS_ADVANCED,
                                       TechConstants.T_IS_EXPERIMENTAL });
        }
        ms.setTons(e.getWeight());
        if (e instanceof BattleArmor) {
            ms.setTOWeight(((BattleArmor) e).getAlternateWeight());
            ms.setTWWeight(e.getWeight());
            ms.setSuitWeight(((BattleArmor) e).getTrooperWeight());
        }

        ms.setBV(e.calculateBattleValue(true, true));
        ms.setLevel(TechConstants.T_SIMPLE_LEVEL[e.getTechLevel()]);
        ms.setAdvancedYear(e.getProductionDate(e.isClan()));
        ms.setStandardYear(e.getCommonDate(e.isClan()));
        ms.setExtinctRange(e.getExtinctionRange());
        ms.setCost((long) e.getCost(false));
        ms.setDryCost((long) e.getCost(true));
        ms.setAlternateCost((int) e.getAlternateCost());
        ms.setCanon(e.isCanon());
        ms.setWalkMp(e.getWalkMP());
        ms.setRunMp(e.getRunMP(MPCalculationSetting.NO_GRAVITY));
        ms.setJumpMp(e.getAnyTypeMaxJumpMP());
        ms.setMoveMode(e.getMovementMode());
        ms.setClan(e.isClan());
        if (e.isSupportVehicle()) {
            ms.setSupport(true);
        }
        if (e instanceof Mek) {
            if (((Mek) e).isIndustrial()) {
                ms.setUnitSubType("Industrial");
            } else if (e.isOmni()) {
                ms.setUnitSubType("Omni");
            } else {
                ms.setUnitSubType("BattleMek");
            }
        } else {
            ms.setUnitSubType(e.getMovementModeAsString());
        }
        if (ms.isSupport()) {
            ms.setWeightClass(EntityWeightClass.getSupportWeightClass(ms.getTons(), ms.getUnitSubType()));
        } else {
            double weightClassWeight = ms.isBattleArmor() ? ms.getSuitWeight() : ms.getTons();
            ms.setWeightClass(EntityWeightClass.getWeightClass(weightClassWeight, ms.getUnitType()));
        }

        ms.setEquipment(e.getEquipment());
        ms.setQuirkNames(e.getQuirks());
        ms.setWeaponQuirkNames(e);
        ms.setTotalArmor(e.getTotalArmor());
        ms.setTotalInternal(e.getTotalInternal());
        ms.setInternalsType(e.getStructureType());
        int[] armorTypes = new int[e.locations()];
        int[] armorTechTypes = new int[e.locations()];
        for (int i = 0; i < armorTypes.length; i++) {
            armorTypes[i] = e.getArmorType(i);
            armorTechTypes[i] = e.getArmorTechLevel(i);
        }
        ms.setArmorType(armorTypes);
        ms.setArmorTypes(armorTypes);
        ms.setArmorTechTypes(armorTechTypes);
        ms.setPatchwork(Arrays.stream(ms.getArmorTypes()).distinct().count() > 1);
        ms.setDoomedOnGround(e.doomedOnGround());
        ms.setDoomedInAtmosphere(e.doomedInAtmosphere());
        ms.setDoomedInSpace(e.doomedInSpace());
        ms.setDoomedInExtremeTemp(e.doomedInExtremeTemp());
        ms.setDoomedInVacuum(e.doomedInVacuum());

        // Check to see if this entity has a cockpit, and if so, set its type
        if ((e instanceof Mek)) {
            ms.setCockpitType(((Mek) e).getCockpitType());
        } else if ((e instanceof Aero)) {
            ms.setCockpitType(((Aero) e).getCockpitType());
        } else {
            // TODO: There's currently no NO_COCKPIT type value, if this value existed, Entity could have a
            //  getCockpitType function and this logic could become unnecessary
            ms.setCockpitType(-2);
        }

        TestEntity testEntity = TestEntity.getEntityVerifier(e);
        if (testEntity != null && !testEntity.correctEntity(new StringBuffer())) {
            ms.setLevel("F");
            ms.setInvalid(true);
        } else {
            ms.setInvalid(false);
        }

        ms.setTechLevel(e.getStaticTechLevel().toString());
        ms.setTechLevelCode(e.getStaticTechLevel().ordinal());
        ms.setTechBase(e.getTechBaseDescription());

        ms.setFailedToLoadEquipment(e.getFailedEquipment().hasNext());

        ms.setGyroType(e.getGyroType());
        if (e.hasEngine()) {
            ms.setEngineName(e.getEngine().getEngineName());
            ms.setEngineType(e.getEngine().getEngineType());
        } else {
            ms.setEngineName("None");
            ms.setEngineType(-1);
        }

        if (e instanceof Mek) {
            if (((Mek) e).hasTSM(false)) {
                ms.setMyomerName("Triple-Strength");
            } else if (((Mek) e).hasTSM(true)) {
                ms.setMyomerName("Prototype Triple-Strength");
            } else if (((Mek) e).hasIndustrialTSM()) {
                ms.setMyomerName("Industrial Triple-Strength");
            } else {
                ms.setMyomerName("Standard");
            }
        } else {
            ms.setMyomerName("None");
        }

        int lowerArms = 0;
        int hands = 0;

        if (e instanceof Mek) {
            lowerArms += e.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RIGHT_ARM) ? 1 : 0;
            lowerArms += e.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LEFT_ARM) ? 1 : 0;
            hands += e.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RIGHT_ARM) ? 1 : 0;
            hands += e.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LEFT_ARM) ? 1 : 0;
        }

        ms.setLowerArms(lowerArms);
        ms.setHands(hands);

        double ts = e.getTroopCarryingSpace();
        ts += e.getPodMountedTroopCarryingSpace();
        ms.setTroopCarryingSpace(ts);

        int aBays = 0;
        int aDoors = 0;
        double aUnits = 0;
        int scBays = 0;
        int scDoors = 0;
        double scUnits = 0;
        int dc = 0;
        int mBays = 0;
        int mDoors = 0;
        double mUnits = 0;
        int hvBays = 0;
        int hvDoors = 0;
        double hvUnits = 0;
        int lvBays = 0;
        int lvDoors = 0;
        double lvUnits = 0;
        int pmBays = 0;
        int pmDoors = 0;
        double pmUnits = 0;
        int baBays = 0;
        int baDoors = 0;
        double baUnits = 0;
        int iBays = 0;
        int iDoors = 0;
        double iUnits = 0;
        int shvBays = 0;
        int shvDoors = 0;
        double shvUnits = 0;
        int dBays = 0;
        int dDoors = 0;
        double dUnits = 0;
        double cbUnits = 0;
        int nrf = 0;
        int bah = 0;
        Vector<Transporter> trs = e.getTransports();
        for (Transporter t : trs) {
            if (t instanceof ASFBay) {
                aBays++;
                aDoors += ((ASFBay) t).getCurrentDoors();
                aUnits += t.getUnused();
            }
            if (t instanceof SmallCraftBay) {
                scBays++;
                scDoors += ((SmallCraftBay) t).getCurrentDoors();
                scUnits += t.getUnused();
            }
            if (t instanceof DockingCollar) {
                dc++;
            }
            if (t instanceof MekBay) {
                mBays++;
                mDoors += ((MekBay) t).getCurrentDoors();
                mUnits += t.getUnused();
            }
            if (t instanceof HeavyVehicleBay) {
                hvBays++;
                hvDoors += ((HeavyVehicleBay) t).getCurrentDoors();
                hvUnits += t.getUnused();
            }
            if (t instanceof LightVehicleBay) {
                lvBays++;
                lvDoors += ((LightVehicleBay) t).getCurrentDoors();
                lvUnits += t.getUnused();
            }
            if (t instanceof ProtoMekBay) {
                pmBays++;
                pmDoors += ((ProtoMekBay) t).getCurrentDoors();
                pmUnits += t.getUnused();
            }
            if (t instanceof BattleArmorBay) {
                baBays++;
                baDoors += ((BattleArmorBay) t).getCurrentDoors();
                baUnits += t.getUnused();
            }
            if (t instanceof InfantryBay) {
                iBays++;
                iDoors += ((InfantryBay) t).getCurrentDoors();
                iUnits += ((InfantryBay) t).getUnusedSlots();
            }
            if (t instanceof SuperHeavyVehicleBay) {
                shvBays++;
                shvDoors += ((SuperHeavyVehicleBay) t).getCurrentDoors();
                shvUnits += t.getUnused();
            }
            if (t instanceof DropShuttleBay) {
                dBays++;
                dDoors += ((DropShuttleBay) t).getCurrentDoors();
                dUnits += t.getUnused();
            }
            if (t instanceof BattleArmorHandles) {
                bah++;
            }
            if (t instanceof CargoBay) {
                cbUnits += t.getUnused();
            }
            if (t instanceof NavalRepairFacility) {
                nrf++;
            }
        }
        ms.setASFBays(aBays);
        ms.setASFDoors(aDoors);
        ms.setASFUnits(aUnits);
        ms.setSmallCraftBays(scBays);
        ms.setSmallCraftDoors(scDoors);
        ms.setSmallCraftUnits(scUnits);
        ms.setDockingCollars(dc);
        ms.setMekBays(mBays);
        ms.setMekDoors(mDoors);
        ms.setMekUnits(mUnits);
        ms.setHeavyVehicleBays(hvBays);
        ms.setHeavyVehicleDoors(hvDoors);
        ms.setHeavyVehicleUnits(hvUnits);
        ms.setLightVehicleBays(lvBays);
        ms.setLightVehicleDoors(lvDoors);
        ms.setLightVehicleUnits(lvUnits);
        ms.setProtoMekBays(pmBays);
        ms.setProtoMekDoors(pmDoors);
        ms.setProtoMekUnits(pmUnits);
        ms.setBattleArmorBays(baBays);
        ms.setBattleArmorDoors(baDoors);
        ms.setBattleArmorUnits(baUnits);
        ms.setInfantryBays(iBays);
        ms.setInfantryDoors(iDoors);
        ms.setInfantryUnits(iUnits);
        ms.setSuperHeavyVehicleBays(shvBays);
        ms.setSuperHeavyVehicleDoors(shvDoors);
        ms.setSuperHeavyVehicleUnits(shvUnits);
        ms.setDropShuttleBays(dBays);
        ms.setDropShuttleDoors(dDoors);
        ms.setDropShuttleUnits(dUnits);
        ms.setBattleArmorHandles(bah);
        ms.setCargoBayUnits(cbUnits);
        ms.setNavalRepairFacilities(nrf);

        if (ASConverter.canConvert(e)) {
            AlphaStrikeElement element = ASConverter.convertForMekCache(e);
            ms.setAsUnitType(element.getASUnitType());
            ms.setSize(element.getSize());
            ms.setTmm(element.getTMM());
            ms.setMovement(element.getMovement());
            ms.setPrimaryMovementMode(element.getPrimaryMovementMode());
            ms.setStandardDamage(element.getStandardDamage());
            ms.setOverheat(element.getOV());
            ms.setFrontArc(element.getFrontArc());
            ms.setLeftArc(element.getLeftArc());
            ms.setRightArc(element.getRightArc());
            ms.setRearArc(element.getRearArc());
            ms.setThreshold(element.getThreshold());
            ms.setFullArmor(element.getFullArmor());
            ms.setFullStructure(element.getFullStructure());
            ms.setSquadSize(element.getSquadSize());
            ms.setSpecialAbilities(element.getSpecialAbilities());
            ms.setUnitRole(element.getRole());
            ms.setPointValue(element.getPointValue());
        } else {
            ms.setAsUnitType(ASUnitType.UNKNOWN);
        }

        return ms;
    }

    /**
     * Loading a complete {@link Entity} object for each summary is a bear and should be changed, but it lets me use the
     * existing parsers
     *
     * @param vMeks       List to add units to as they are loaded
     * @param sKnownFiles Files that have been processed so far and can be skipped
     * @param lLastCheck  The timestamp of the last time the cache was updated
     * @param fDir        The directory to load units from
     *
     * @return Whether the list of units has changed, requiring rewriting the cache
     */
    private boolean loadMeksFromDirectory(Vector<MekSummary> vMeks,
          Set<String> sKnownFiles, long lLastCheck, File fDir,
          boolean ignoreUnofficial) {
        boolean bNeedsUpdate = false;
        loadReport.append("  Looking in ").append(fDir.getPath())
              .append("...\n");
        int thisDirectoriesFileCount = 0;
        String[] sa = fDir.list();

        if (sa != null) {
            for (String element : sa) {
                if (shouldStopLoading()) {
                    done();
                    return false;
                }
                File f = new MegaMekFile(fDir, element).getFile();
                if (f.equals(new MegaMekFile(getUnitCacheDir(), FILENAME_UNITS_CACHE).getFile())) {
                    continue;
                }
                if (f.isDirectory()) {
                    if (f.getName().equalsIgnoreCase("unsupported") || f.getName().equalsIgnoreCase(".mml_tmp")) {
                        // Meks in "unsupported" are ignored because
                        // they have features not implemented in MM yet.
                        //
                        // Meks in ".mml_tmp" are created by MML to back up unsaved work,
                        // and should only be loaded by the MML unit recovery process.
                        continue;
                    } else if (f.getName().equalsIgnoreCase("unofficial") && ignoreUnofficial) {
                        // Meks in this directory are ignored because
                        // they are unofficial, and we don't want those right
                        // now.
                        continue;
                    } else if (f.getName().equalsIgnoreCase("_svn")
                          || f.getName().equalsIgnoreCase(".svn")) {
                        // This is a Subversion work directory. Let's ignore it.
                        continue;
                    }
                    // recursion is fun
                    bNeedsUpdate |= loadMeksFromDirectory(vMeks, sKnownFiles, lLastCheck, f, ignoreUnofficial);
                    continue;
                }
                String lowerCaseName = f.getName().toLowerCase();
                if (SUPPORTED_FILE_EXTENSIONS.stream().noneMatch(lowerCaseName::endsWith)) {
                    continue;
                }
                if (lowerCaseName.endsWith(".zip")) {
                    bNeedsUpdate |= loadMeksFromZipFile(vMeks, sKnownFiles, lLastCheck, f);
                    continue;
                }
                if ((f.lastModified() < lLastCheck) && sKnownFiles.contains(f.toString())) {
                    continue;
                }
                try {
                    MekFileParser mfp = new MekFileParser(f);
                    Entity e = mfp.getEntity();
                    MekSummary ms = getSummary(e, f, null);
                    // if this is unit's MekSummary is already known,
                    // remove it first, so we don't get duplicates
                    if (sKnownFiles.contains(f.toString())) {
                        vMeks.removeElement(ms);
                    }
                    vMeks.addElement(ms);
                    sKnownFiles.add(f.toString());
                    bNeedsUpdate = true;
                    thisDirectoriesFileCount++;
                    fileCount++;
                    Iterator<String> failedEquipment = e.getFailedEquipment();
                    if (failedEquipment.hasNext()) {
                        loadReport.append("    Loading from ").append(f).append("\n");
                        while (failedEquipment.hasNext()) {
                            loadReport.append("      Failed to load equipment: ")
                                  .append(failedEquipment.next()).append("\n");
                        }
                    }
                } catch (Exception ex) {
                    loadReport.append("    Loading from ").append(f).append("\n");
                    loadReport.append("***   Unable to load file: ");
                    StringWriter stringWriter = new StringWriter();
                    PrintWriter printWriter = new PrintWriter(stringWriter);
                    ex.printStackTrace(printWriter);
                    loadReport.append(stringWriter.getBuffer()).append("\n");
                    failedFiles.put(f.toString(), ex.getMessage());
                }
            }
        }

        loadReport.append("  ...loaded ").append(thisDirectoriesFileCount).append(" files.\n");
        return bNeedsUpdate;
    }

    private boolean loadMeksFromZipFile(Vector<MekSummary> vMeks,
          Set<String> sKnownFiles, long lLastCheck, File fZipFile) {
        boolean bNeedsUpdate = false;
        ZipFile zFile;
        int thisZipFileCount = 0;
        try {
            zFile = new ZipFile(fZipFile);
        } catch (Exception ex) {
            loadReport.append("  Unable to load file ")
                  .append(fZipFile.getName()).append(": ");
            StringWriter stringWriter = new StringWriter();
            PrintWriter printWriter = new PrintWriter(stringWriter);
            ex.printStackTrace(printWriter);
            loadReport.append(stringWriter.getBuffer()).append("\n");
            return false;
        }
        loadReport.append("  Looking in zip file ").append(fZipFile.getPath())
              .append("...\n");

        for (Enumeration<?> i = zFile.entries(); i.hasMoreElements(); ) {
            if (shouldStopLoading()) {
                done();
                try {
                    zFile.close();
                    return false;
                } catch (Exception ex) {
                    logger.error("", ex);
                }
            }
            ZipEntry zEntry = (ZipEntry) i.nextElement();

            if (zEntry.isDirectory()) {
                if (zEntry.getName().equalsIgnoreCase("unsupported")) {
                    loadReport.append(
                          " Do not place special 'unsupported' type folders in zip files, they must \nbe uncompressed directories to work properly. Note that you may place \nzip files inside of 'unsupported' type folders, though.\n");
                }
                continue;
            }
            String lowerCaseName = zEntry.getName().toLowerCase();
            if (SUPPORTED_FILE_EXTENSIONS.stream().noneMatch(lowerCaseName::endsWith)) {
                continue;
            }
            if ((Math.max(fZipFile.lastModified(), zEntry.getTime()) < lLastCheck)
                  && sKnownFiles.contains(zEntry.getName())) {
                continue;
            }

            try {
                MekFileParser mfp = new MekFileParser(
                      zFile.getInputStream(zEntry), zEntry.getName());
                Entity e = mfp.getEntity();
                MekSummary ms = getSummary(e, fZipFile, zEntry.getName());
                vMeks.addElement(ms);
                sKnownFiles.add(zEntry.getName());
                bNeedsUpdate = true;
                thisZipFileCount++;
                zipCount++;
                Iterator<String> failedEquipment = e.getFailedEquipment();
                if (failedEquipment.hasNext()) {
                    loadReport.append("    Loading from zip file")
                          .append(" >> ").append(zEntry.getName()).append("\n");
                    while (failedEquipment.hasNext()) {
                        loadReport.append("      Failed to load equipment: ")
                              .append(failedEquipment.next()).append("\n");
                    }
                }
            } catch (Exception ex) {
                loadReport.append("    Loading from zip file").append(" >> ")
                      .append(zEntry.getName()).append("\n");
                loadReport.append("      Unable to load file: ");
                StringWriter stringWriter = new StringWriter();
                PrintWriter printWriter = new PrintWriter(stringWriter);
                ex.printStackTrace(printWriter);
                loadReport.append(stringWriter.getBuffer()).append("\n");
                if (!(ex.getMessage() == null)) {
                    failedFiles.put(zEntry.getName(), ex.getMessage());
                }
            }
        }

        try {
            zFile.close();
        } catch (Exception ex) {
            logger.error("", ex);
        }

        loadReport.append("  ...loaded ").append(thisZipFileCount)
              .append(" files.\n");

        return bNeedsUpdate;
    }

    private void addLookupNames() {
        File lookupNames = new MegaMekFile(getUnitCacheDir(), FILENAME_LOOKUP).getFile();
        if (lookupNames.exists()) {
            try (FileInputStream fis = new FileInputStream(lookupNames);
                  InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8);
                  BufferedReader br = new BufferedReader(isr)) {
                String line;
                String lookupName;
                String entryName;
                while (null != (line = br.readLine())) {
                    if (line.startsWith("#")) {
                        continue;
                    }
                    int index = line.indexOf('|');
                    if (index > 0) {
                        lookupName = line.substring(0, index);
                        entryName = line.substring(index + 1);
                        if (!nameMap.containsKey(lookupName)) {
                            MekSummary ms = nameMap.get(entryName);
                            if (null != ms) {
                                nameMap.put(lookupName, ms);
                            }
                        }
                    }
                }
            } catch (Exception ex) {
                logger.error("", ex);
            }
        }
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
