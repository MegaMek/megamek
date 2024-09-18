/*
 * MegaMek - Copyright (C) 2000-2004 Ben Mazur (bmazur@sev.org)
 * Copyright © 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (c) 2022 - The MegaMek Team. All Rights Reserved.
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 2 of the License, or (at your option)
 * any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 * for more details.
 */
package megamek.common;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import megamek.common.alphaStrike.ASUnitType;
import megamek.common.alphaStrike.AlphaStrikeElement;
import megamek.common.alphaStrike.conversion.ASConverter;
import megamek.common.preference.PreferenceManager;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.common.verifier.TestEntity;
import megamek.logging.MMLogger;

/**
 * Cache of the Mek summary information. Implemented as Singleton so a client
 * and server running in the same process can share it
 *
 * @author arlith
 */
public class MekSummaryCache {
    private static final MMLogger logger = MMLogger.create(MekSummaryCache.class);

    public interface Listener {
        void doneLoading();
    }

    public static final String FILENAME_UNITS_CACHE = "units.cache";
    public static final String FILENAME_LOOKUP = "name_changes.txt";

    private static final List<String> SUPPORTED_FILE_EXTENSIONS = List.of(".mtf", ".blk", ".mep", ".hmv", ".tdb",
            ".hmp", ".zip");

    private static MekSummaryCache instance;
    private static boolean disposeInstance = false;
    private static boolean interrupted = false;

    private boolean initialized = false;
    private boolean initializing = false;

    private MekSummary[] data;
    private final Map<String, MekSummary> nameMap;
    private final Map<String, MekSummary> fileNameMap;
    private Map<String, String> failedFiles;
    private int cacheCount;
    private int fileCount;
    private int zipCount;

    private final List<Listener> listeners = new ArrayList<>();

    private StringBuffer loadReport = new StringBuffer();
    private Thread loader;
    private static final Object lock = new Object();

    public static synchronized MekSummaryCache getInstance() {
        return getInstance(false);
    }

    public static synchronized MekSummaryCache getInstance(boolean ignoreUnofficial) {
        final boolean ignoringUnofficial = ignoreUnofficial;
        if (instance == null) {
            instance = new MekSummaryCache();
        }
        if (!instance.initialized && !instance.initializing) {
            instance.initializing = true;
            interrupted = false;
            disposeInstance = false;
            instance.loader = new Thread(() -> instance.loadMekData(ignoringUnofficial), "Mek Cache Loader");
            instance.loader.setPriority(Thread.NORM_PRIORITY - 1);
            instance.loader.start();
        }
        return instance;
    }

    /**
     * Checks the unit files directory for any changes since the last time the unit
     * cache was
     * loaded. If there are any updates, the new cache is saved.
     *
     * @param ignoreUnofficial If true, skips unofficial directories
     */
    public static void refreshUnitData(boolean ignoreUnofficial) {
        instance.initializing = true;
        instance.initialized = false;
        interrupted = false;
        disposeInstance = false;

        File unit_cache_path = new MegaMekFile(getUnitCacheDir(), FILENAME_UNITS_CACHE).getFile();
        long lastModified = unit_cache_path.exists() ? unit_cache_path.lastModified() : 0L;

        instance.loader = new Thread(() -> instance.refreshCache(lastModified, ignoreUnofficial),
                "Mek Cache Loader");
        instance.loader.setPriority(Thread.NORM_PRIORITY - 1);
        instance.loader.start();
    }

    public static void dispose() {
        if (instance != null) {
            synchronized (lock) {
                interrupted = true;
                instance.loader.interrupt();
                // We can't do this, otherwise we can't notifyAll()
                // instance = null;
                disposeInstance = true;
            }
        }
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
                try {
                    lock.wait();
                } catch (Exception ignored) {

                }
            }
        }
    }

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
        Vector<MekSummary> vMeks = new Vector<>();
        Set<String> sKnownFiles = new HashSet<>();
        long lLastCheck = 0;
        failedFiles = new HashMap<>();

        EquipmentType.initializeTypes(); // load master equipment lists

        loadReport.append("\n");
        loadReport.append("Reading unit files:\n");

        if (!ignoreUnofficial) {
            File unit_cache_path = new MegaMekFile(getUnitCacheDir(),
                    FILENAME_UNITS_CACHE).getFile();
            // check the cache
            try {
                if (unit_cache_path.exists()) {
                    loadReport.append("  Reading from unit cache file...\n");
                    lLastCheck = unit_cache_path.lastModified();
                    InputStream inputStream = new BufferedInputStream(new FileInputStream(unit_cache_path));
                    ObjectInputStream fin = new ObjectInputStream(inputStream);
                    Integer newUnits = (Integer) fin.readObject();
                    for (int i = 0; i < newUnits; i++) {
                        if (interrupted) {
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
                loadReport.append("  Unable to load unit cache: ")
                        .append(ex.getMessage()).append("\n");
                logger.error(loadReport.toString(), ex);
            }
        }

        checkForChanges(ignoreUnofficial, vMeks, sKnownFiles, lLastCheck);
        updateData(vMeks);
        addLookupNames();
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
            File userDataUnits = new File(Configuration.userdataDir(), Configuration.unitsDir().toString());
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
            File storyarcsDir = Configuration.storyarcsDir();
            if (storyarcsDir.exists() && storyarcsDir.isDirectory()) {
                File[] storyArcsFiles = storyarcsDir.listFiles();
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
        if (bNeedsUpdate) {
            saveCache(vMeks);
        }
    }

    private void updateData(Vector<MekSummary> vMeks) {
        // convert to array
        data = new MekSummary[vMeks.size()];
        vMeks.copyInto(data);
        nameMap.clear();
        fileNameMap.clear();

        // store map references
        for (MekSummary element : data) {
            if (interrupted) {
                done();
                return;
            }
            nameMap.put(element.getName(), element);
            String entryName = element.getEntryName();
            if (entryName == null) {
                fileNameMap.put(element.getSourceFile().getName(), element);
            } else {
                String unitName = entryName;

                if (unitName.contains("\\")) {
                    unitName = unitName.substring(unitName.lastIndexOf("\\") + 1);
                }

                if (unitName.contains("/")) {
                    unitName = unitName.substring(unitName.lastIndexOf("/") + 1);
                }

                fileNameMap.put(unitName, element);
            }
        }
    }

    private void logReport() {
        loadReport.append(data.length).append(" units loaded.\n");

        if (!failedFiles.isEmpty()) {
            loadReport.append("  ").append(failedFiles.size())
                    .append(" units failed to load...\n");
        }

        logger.debug(loadReport.toString());
    }

    private void done() {
        synchronized (lock) {
            lock.notifyAll();

            initialized = true;

            for (Listener listener : listeners) {
                listener.doneLoading();
            }

            if (disposeInstance) {
                instance = null;
                initialized = false;
            }
        }
    }

    private void saveCache(List<MekSummary> data) {
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

    private void refreshCache(long lastCheck, boolean ignoreUnofficial) {
        loadReport = new StringBuffer();
        loadReport.append("Refreshing unit cache:\n");
        Vector<MekSummary> units = new Vector<>();
        Set<String> knownFiles = new HashSet<>();
        // Loop through current contents and make sure the file is still there.
        // Note which files are represented so we can skip them if they haven't changed
        for (MekSummary ms : data) {
            if (interrupted) {
                done();
                return;
            }
            File source = ms.getSourceFile();
            if (source.exists()) {
                units.add(ms);
                if (null == ms.getEntryName()) {
                    knownFiles.add(source.toString());
                } else {
                    knownFiles.add(ms.getEntryName());
                }
            }
        }

        // load any changes since the last check time
        checkForChanges(ignoreUnofficial, units, knownFiles, lastCheck);
        updateData(units);
        addLookupNames();
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
        } else if (e.getTechLevel() == TechConstants.T_INTRO_BOXSET) {
            ms.setAltTypes(new int[] { TechConstants.T_INTRO_BOXSET, TechConstants.T_IS_ADVANCED,
                    TechConstants.T_IS_EXPERIMENTAL });
        } else {
            ms.setAltTypes(new int[] { TechConstants.T_IS_TW_NON_BOX, TechConstants.T_IS_ADVANCED,
                    TechConstants.T_IS_EXPERIMENTAL });
        }
        ms.setTons(e.getWeight());
        if (e instanceof BattleArmor) {
            ms.setTOweight(((BattleArmor) e).getAlternateWeight());
            ms.setTWweight(e.getWeight());
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
        ms.setJumpMp(e.getJumpMP());
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

        // Check to see if this entity has a cockpit, and if so, set it's type
        if ((e instanceof Mek)) {
            ms.setCockpitType(((Mek) e).getCockpitType());
        } else if ((e instanceof Aero)) {
            ms.setCockpitType(((Aero) e).getCockpitType());
        } else {
            // TODO: There's currently no NO_COCKPIT type value, if this value
            // existed, Entity could have a getCockpitType function and this
            // logic could become unnecessary
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
            lowerArms += e.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_RARM) ? 1 : 0;
            lowerArms += e.hasSystem(Mek.ACTUATOR_LOWER_ARM, Mek.LOC_LARM) ? 1 : 0;
            hands += e.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_RARM) ? 1 : 0;
            hands += e.hasSystem(Mek.ACTUATOR_HAND, Mek.LOC_LARM) ? 1 : 0;
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
            if (t instanceof DropshuttleBay) {
                dBays++;
                dDoors += ((DropshuttleBay) t).getCurrentDoors();
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
        ms.setDropshuttleBays(dBays);
        ms.setDropshuttleDoors(dDoors);
        ms.setDropshuttelUnits(dUnits);
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
     * Loading a complete {@link Entity} object for each summary is a bear and
     * should be
     * changed, but it lets me use the existing parsers
     *
     * @param vMeks       List to add units to as they are loaded
     * @param sKnownFiles Files that have been processed so far and can be skipped
     * @param lLastCheck  The timestamp of the last time the cache was updated
     * @param fDir        The directory to load units from
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
                if (interrupted) {
                    done();
                    return false;
                }
                File f = new MegaMekFile(fDir, element).getFile();
                if (f.equals(new MegaMekFile(getUnitCacheDir(), FILENAME_UNITS_CACHE).getFile())) {
                    continue;
                }
                if (f.isDirectory()) {
                    if (f.getName().equalsIgnoreCase("unsupported")) {
                        // Meks in this directory are ignored because
                        // they have features not implemented in MM yet.
                        continue;
                    } else if (f.getName().equalsIgnoreCase("unofficial") && ignoreUnofficial) {
                        // Meks in this directory are ignored because
                        // they are unofficial and we don't want those right
                        // now.
                        continue;
                    } else if (f.getName().equalsIgnoreCase("_svn")
                            || f.getName().equalsIgnoreCase(".svn")) {
                        // This is a Subversion work directory. Lets ignore it.
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

        for (Enumeration<?> i = zFile.entries(); i.hasMoreElements();) {
            if (interrupted) {
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
