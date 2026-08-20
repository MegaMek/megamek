/*
 * Copyright (c) 2009 Jay Lawson
 * Copyright (c) 2013 Edward Cullen (eddy@obsessedcomputers.co.uk)
 * Copyright (C) 2024-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.client.ui.util;

import java.awt.Image;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.battlefieldSupport.BFSAssetType;
import megamek.common.battlefieldSupport.BattlefieldSupportAsset;
import megamek.common.loaders.MekSummary;
import megamek.common.preference.PreferenceManager;
import megamek.common.units.BTObject;
import megamek.common.units.Mek;
import megamek.logging.MMLogger;

/**
 * This class provides methods for retrieving fluff images, for use in MM, MML and MHQ; also for record sheets (where
 * the fallback image "hud.png" is used).
 */
public final class FluffImageHelper {

    private static final MMLogger LOGGER = MMLogger.create(FluffImageHelper.class);

    public static final String DIR_NAME_BA = "BattleArmor";
    public static final String DIR_NAME_ASSET = "Asset";
    public static final String DIR_NAME_CONV_FIGHTER = "ConvFighter";
    public static final String DIR_NAME_DROPSHIP = "DropShip";
    public static final String DIR_NAME_FIGHTER = "Fighter";
    public static final String DIR_NAME_INFANTRY = "Infantry";
    public static final String DIR_NAME_JUMPSHIP = "JumpShip";
    public static final String DIR_NAME_MEK = "Mek";
    public static final String DIR_NAME_PROTOMEK = "ProtoMek";
    public static final String DIR_NAME_SMALLCRAFT = "Small Craft";
    public static final String DIR_NAME_SPACE_STATION = "Space Station";
    public static final String DIR_NAME_VEHICLE = "Vehicle";
    public static final String DIR_NAME_WARSHIP = "WarShip";
    public static final String[] EXTENSIONS_FLUFF_IMAGE_FORMATS = { ".PNG", ".png", ".JPG",
                                                                    ".JPEG", ".jpg", ".jpeg", ".GIF", ".gif" };

    /** The extensions above as a set, for membership tests when scanning a chassis or model directory. */
    private static final Set<String> EXTENSIONS_FLUFF_IMAGE_FORMAT_SET = Set.of(EXTENSIONS_FLUFF_IMAGE_FORMATS);

    /** The model subdirectory name that units with an empty model match, e.g. fluff/Mek/Chassis/Chassis ---empty---. */
    static final String EMPTY_MODEL_DIR_NAME = "---empty---";

    /**
     * Returns a fluff image for the given unit/object to be shown e.g. in the unit summary.
     *
     * <p>
     * If a fluff image is stored in the unit/object itself, e.g. if it was part of the unit's file or is created by the
     * unit itself, this
     * is returned. Note that this is not used for canon units, but may be used in custom ones by
     * adding a fluff image to the unit in MML.
     *
     * <p>
     * Otherwise, the fluff images directories are searched. First searches the user dir, then the internal dir. Tries
     * to match the image by
     * chassis + model or chassis alone. Chassis and model names are cleaned from " and /
     * characters before matching. For Meks with clan
     * names, both names and the combinations are searched. The model alone is not used to search.
     * <p>
     * Returns null if no fluff image can be found.
     *
     * @param unit The unit
     *
     * @return a fluff image or null, if no match is found
     */
    public static @Nullable Image getFluffImage(@Nullable BTObject unit) {
        return getFluffImage(unit, false);
    }

    public static @Nullable String getFluffImagePath(@Nullable BTObject unit) {
        if (unit == null) {
            return null;
        }
        File fluffImageFile = findFluffFiles(unit, true).stream().findFirst().orElse(null);
        if (fluffImageFile != null) {
            return fluffImageFile.toString();
        } else {
            return null;
        }
    }

    /**
     * Returns a list of all fluff images for the given unit/object to be shown e.g. in the unit summary.
     *
     * <p>If a fluff image is stored in the unit/object itself, e.g. if it was part of the
     * unit's file or is created by the unit itself, only this is returned. Note that this is not used for canon units, but may be used in
     * custom ones by adding a fluff image to the unit in MML.</p>
     *
     * <p>Otherwise, the fluff image directories are searched. First searches the user dir,
     * then the internal dir. Tries to match the image by chassis + model or chassis alone. Chassis and model names are cleaned from " and /
     * characters before matching. For Meks with clan names, both names and the combinations are searched. The model alone is not used to
     * search.</p>
     *
     * @param unit The unit
     * @return a list of fluff images, or an empty list if none are found
     */
    public static List<Image> getFluffImages(@Nullable BTObject unit) {
        return getFluffImageList(unit, false);
    }

    /**
     * Returns a list of all fluff image records for the given unit/object to be shown e.g. in the unit summary.
     *
     * <p>If a fluff image is stored in the unit/object itself, e.g. if it was part of the
     * unit's file or is created by the unit itself, only this is returned. Note that this is not used for canon units, but may be used in
     * custom ones by adding a fluff image to the unit in MML.</p>
     *
     * <p>Otherwise, the fluff image directories are searched. First searches the user dir,
     * then the internal dir. Tries to match the image by chassis + model or chassis alone. Chassis and model names are cleaned from " and /
     * characters before matching. For Meks with clan names, both names and the combinations are searched. The model alone is not used to
     * search.</p>
     *
     * @param unit The unit
     * @return a list of fluff image records, or an empty list if none are found
     */
    public static List<FluffImageRecord> getFluffRecords(@Nullable BTObject unit) {
        return getFluffImageRecords(unit, false);
    }

    /**
     * Returns a fluff image for the given unit for the record sheet, with a fallback file named "hud.png" if that is
     * present in the right
     * fluff directory, or {@code null} if nothing can be found. See {@link #getFluffImage(BTObject)} for
     * further comments on how the fluff image is
     * searched.
     *
     * @param unit The unit
     *
     * @return a fluff image or null, if no match is found
     */
    public static @Nullable Image getRecordSheetFluffImage(@Nullable BTObject unit) {
        return getFluffImage(unit, true);
    }

    private static @Nullable Image getFluffImage(@Nullable BTObject unit, boolean recordSheet) {
        List<Image> fluffImages = getFluffImageList(unit, recordSheet);
        if (!fluffImages.isEmpty()) {
            return fluffImages.get(0);
        } else {
            return null;
        }
    }

    /**
     * Returns a list of available fluff images. If a fluff image is embedded in the unit file,
     * only that image is returned, even if others are available from the fluff directories. The returned
     * list may be empty, but not {@code null}.
     *
     * @param unit The unit
     * @param recordSheet True if this image search is meant for a record sheet (used in MML)
     * @return Available fluff images or the embedded fluff image
     */
    private static List<FluffImageRecord> getFluffImageRecords(@Nullable BTObject unit, boolean recordSheet) {
        if (unit == null) {
            return new ArrayList<>();
        }
        Image embeddedFluffImage = unit.getFluffImage();
        if (embeddedFluffImage != null) {
            return List.of(new FluffImageRecord(embeddedFluffImage, null));
        } else {
            return findFluffFiles(unit, recordSheet).stream().map(FluffImageRecord::toRecord).toList();
        }
    }

    /**
     * Returns a list of available fluff images. If a fluff image is embedded in the unit file,
     * only that image is returned, even if others are available from the fluff directories. The returned
     * list may be empty, but not {@code null}.
     *
     * @param unit The unit
     * @param recordSheet True if this image search is meant for a record sheet
     * @return Available fluff images or the embedded fluff image
     */
    private static List<Image> getFluffImageList(@Nullable BTObject unit, boolean recordSheet) {
        if (unit == null) {
            return new ArrayList<>();
        }
        Image embeddedFluffImage = unit.getFluffImage();
        if (embeddedFluffImage != null) {
            return List.of(embeddedFluffImage);
        } else {
            return findFluffFiles(unit, recordSheet).stream()
                    .map(File::toString)
                    .map(ImageIcon::new)
                    .map(ImageIcon::getImage)
                    .collect(Collectors.toList());
        }
    }

    private static Set<File> findFluffFiles(BTObject unit, boolean recordSheet) {
        // A LinkedHashSet keeps the search order while removing duplicates. The order is significant:
        // getFluffImage(BTObject) shows the first entry, and the directories below are searched from
        // most to least specific so that the most specific art wins.
        Set<File> fileCandidates = new LinkedHashSet<>();

        List<String> nameCandidates = nameCandidates(unit);

        String userDir = PreferenceManager.getClientPreferences().getUserDir();
        boolean hasUserDir = !userDir.isBlank() && new File(userDir).isDirectory();

        // Assets search their own folder first, then the folder of the corresponding TW unit type; all other units
        // search a single folder. The folders are searched in order so that the most specific art wins.
        for (String fluffPath : getFluffPaths(unit)) {
            var fluffDir = new File(Configuration.fluffImagesDir(), fluffPath);

            // UserDir matches
            // For internal use: in [user dir]/data/images/rs/<type> images for record sheets can be placed; these
            // will be preferentially loaded when the recordSheet parameter is true (i.e. when called from RS printing)
            if (hasUserDir) {
                var fluffUserDir = userFluffDir(userDir, false, fluffPath);
                var rsFluffUserDir = userFluffDir(userDir, true, fluffPath);

                if (recordSheet) {
                    fileCandidates.addAll(findMatchingFiles(rsFluffUserDir, nameCandidates));
                }
                fileCandidates.addAll(findMatchingFiles(fluffUserDir, nameCandidates));
                fileCandidates.addAll(getFluffInChassisDirs(unit, fluffUserDir));
            }

            // Internal fluff path matches
            fileCandidates.addAll(findMatchingFiles(fluffDir, nameCandidates));
            fileCandidates.addAll(getFluffInChassisDirs(unit, fluffDir));

            // Fallback for units other than HHWs.
            // The HHW fallback image is embedded into the RS template.
            if (recordSheet && !unit.isHandheldWeapon()) {
                File hudFile = findMatchingFile(fluffDir, "hud.png");
                if (hudFile != null) {
                    fileCandidates.add(hudFile);
                }
            }
        }

        fileCandidates.removeIf(candidate -> !candidate.exists() || candidate.isDirectory());
        return fileCandidates;
    }

    /**
     * With the addition of multiple fluff images, file matching depends on the directory a file is in.
     * <BR>- In the main fluff/[unittype]/ directory the old rules apply, i.e. a file is valid if it
     * matches the model exactly or if the filename is only the chassis and matches the unit's chassis.
     * The filename may now contain additional information after an underscore (atlas_xyz.jpg matches for
     * any Atlas mek).
     * <BR>- In a chassis subdirectory fluff/[unittype]/[chassis], all files match if [chassis]
     * matches the unit's chassis (even if the filename has the wrong model) AND if there is no
     * [model] subdirectory matching the unit's model. Empty models match the directory "---empty---".
     * The filename doesn't matter for matching.
     * <BR>- In a model subdirectory fluff/[unittype]/[chassis]/[model], all files match if the
     * unit's chassis and model match [chassis] and [model]. The filename doesn't matter for matching.
     */
    static List<File> getFluffInChassisDirs(BTObject unit, File unitTypeFluffDir) {
        List<File> result = new ArrayList<>();
        for (String nameCandidate : chassisNameCandidates(unit)) {
            var chassisDir = new File(unitTypeFluffDir, nameCandidate);
            if (chassisDir.isDirectory()) {
                result.addAll(getFluffInChassisDir(unit, chassisDir));
            }
        }
        return result;
    }

    /**
     * @return For the unit, returns the possible chassis lookup strings, which is simply the chassis
     * (the list has only one entry) for all units except Clan Meks with a double name, where the list
     * includes the four variations on Timber Wolf (Mad Cat), Mad Cat (Timber Wolf), Mad Cat and
     * Timber Wolf. Note that a few units have X (Y) chassis that are not clan double names. Those
     * will return only the full chassis X (Y).
     */
    private static List<String> chassisNameCandidates(BTObject unit) {
        List<String> result = new ArrayList<>();
        String sanitizedChassis = sanitize(unit.generalName());
        result.add(sanitizedChassis);
        if ((unit instanceof Mek) && !((Mek) unit).getClanChassisName().isBlank()) {
            String sanitizedClanChassis = sanitize(((Mek) unit).getClanChassisName());
            result.add(sanitizedClanChassis + " (" + sanitizedChassis + ")");
            result.add(sanitizedChassis + " (" + sanitizedClanChassis + ")");
            result.add(sanitizedClanChassis);
        }
        return result;
    }

    private static List<File> getFluffInChassisDir(BTObject unit, File chassisDir) {
        String sanitizedModel = sanitize(unit.specificName());
        if (sanitizedModel.isBlank()) {
            sanitizedModel = EMPTY_MODEL_DIR_NAME;
        }
        List<File> result = new ArrayList<>();
        for (String chassisNameCandidate : chassisNameCandidates(unit)) {
            var modelDir = new File(chassisDir, chassisNameCandidate + " " + sanitizedModel);
            if (modelDir.isDirectory()) {
                result.addAll(getFluffInDir(modelDir));
            }
        }
        if (result.isEmpty()) {
            result.addAll(getFluffInDir(chassisDir));
        }
        return result;
    }

    private static List<File> getFluffInDir(File dir) {
        List<File> result = new ArrayList<>();
        // Files.list is a shallow listing and, unlike Files.walk(dir, 1), does not include the directory itself
        try (Stream<Path> entries = Files.list(dir.toPath())) {
            result.addAll(entries.map(Path::toFile).toList());
            result.removeIf(FluffImageHelper::isNoImageFile);
        } catch (IOException exception) {
            LOGGER.warn("Error while reading files from {}", dir, exception);
        }
        result.sort(Comparator.comparing(File::getName, String.CASE_INSENSITIVE_ORDER));
        return result;
    }

    static File userFluffDir(String userDir, boolean recordSheet, String fluffPath) {
        File imageTypeDir = new File(new File(new File(userDir, "data"), "images"),
              recordSheet ? "rs" : "fluff");
        return new File(imageTypeDir, fluffPath);
    }

    private static List<File> findMatchingFiles(File directory, List<String> nameCandidates) {
        List<File> matches = new ArrayList<>();
        if (!directory.exists() || !directory.isDirectory()) {
            return matches;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return matches;
        }

        for (String nameCandidate : nameCandidates) {
            for (String ext : EXTENSIONS_FLUFF_IMAGE_FORMATS) {
                String searchName = nameCandidate + ext;
                for (File file : files) {
                    if (file.getName().equalsIgnoreCase(searchName)) {
                        matches.add(file);
                        break;
                    }
                }
            }
        }
        return matches;
    }

    private static @Nullable File findMatchingFile(File directory, String fileName) {
        if (!directory.exists() || !directory.isDirectory()) {
            return null;
        }

        File[] files = directory.listFiles();
        if (files == null) {
            return null;
        }

        for (File file : files) {
            if (file.getName().equalsIgnoreCase(fileName)) {
                return file;
            }
        }
        return null;
    }

    private static String sanitize(String original) {
        return original.replace("\"", "").replace("/", "");
    }

    private static List<String> nameCandidates(BTObject unit) {
        List<String> candidates = new ArrayList<>();
        String sanitizedChassis = sanitize(unit.generalName());
        String sanitizedModel = sanitize(unit.specificName());
        // Check for an empty model so the order more specific -> less specific name candidate is always kept
        if (!sanitizedModel.isBlank()) {
            candidates.add((sanitizedChassis + " " + sanitizedModel).trim());
        }
        if (unit instanceof Mek mek && !mek.getClanChassisName().isBlank()) {
            addClanChassisVariants(mek.getFullChassis(), candidates, sanitizedModel, mek.getClanChassisName());
        } else if (unit instanceof MekSummary mekSummary && mekSummary.isMek()
              && !mekSummary.getClanChassisName().isBlank()) {
            addClanChassisVariants(mekSummary.getFullChassis(), candidates, sanitizedModel,
                  mekSummary.getClanChassisName());
        }
        candidates.addAll(chassisNameCandidates(unit));
        return candidates;
    }

    private static void addClanChassisVariants(String fullChassis, List<String> candidates, String sanitizedModel,
          String clanChassis) {

        String sanitizedFullChassis = sanitize(fullChassis);
        String sanitizedClanChassis = sanitize(clanChassis);
        if (!sanitizedModel.isBlank()) {
            candidates.add((sanitizedFullChassis + " " + sanitizedModel).trim());
            candidates.add((sanitizedClanChassis + " " + sanitizedModel).trim());
        }
        candidates.add(sanitizedFullChassis);
        candidates.add(sanitizedClanChassis);
    }

    private FluffImageHelper() {}

    /**
     * Returns the subdirectory in the fluff images directory suitable for the given unit, i.e. "ConvFighter" for CF and
     * FWS units.
     *
     * @param unit The unit
     *
     * @return The unit type subdirectory for fluff images
     */
    public static String getFluffPath(BTObject unit) {
        if (unit.isBattlefieldSupportAsset()) {
            return DIR_NAME_ASSET;
        } else if (unit.isWarShip()) {
            return DIR_NAME_WARSHIP;
        } else if (unit.isSpaceStation()) {
            return DIR_NAME_SPACE_STATION;
        } else if (unit.isJumpShip()) {
            return DIR_NAME_JUMPSHIP;
        } else if (unit.isConventionalFighter() || unit.isFixedWingSupport()) {
            return DIR_NAME_CONV_FIGHTER;
        } else if (unit.isDropShip()) {
            return DIR_NAME_DROPSHIP;
        } else if (unit.isSmallCraft()) {
            return DIR_NAME_SMALLCRAFT;
        } else if (unit.isFighter()) {
            return DIR_NAME_FIGHTER;
        } else if (unit.isBattleArmor()) {
            return DIR_NAME_BA;
        } else if (unit.isConventionalInfantry()) {
            return DIR_NAME_INFANTRY;
        } else if (unit.isProtoMek()) {
            return DIR_NAME_PROTOMEK;
        } else if (unit.isVehicle()) {
            return DIR_NAME_VEHICLE;
        } else {
            return DIR_NAME_MEK;
        }
    }

    /**
     * Returns the ordered list of fluff image subdirectories to search for the given unit. Most units search a single
     * folder (see {@link #getFluffPath(BTObject)}). Battlefield Support Assets rarely have their own art, so they
     * search the "Asset" folder first (for asset-specific art such as Emplacements, which have no standard-unit folder)
     * and then fall back to the folder of the corresponding TW unit type, letting a linked asset share its base unit's
     * art by chassis/model.
     *
     * @param unit The unit
     *
     * @return the ordered fluff image subdirectories to search
     */
    public static List<String> getFluffPaths(BTObject unit) {
        if (unit instanceof BattlefieldSupportAsset asset) {
            List<String> paths = new ArrayList<>();
            paths.add(DIR_NAME_ASSET);
            String twFolder = twFolderForAssetType(asset.getAssetType());
            if ((twFolder != null) && !paths.contains(twFolder)) {
                paths.add(twFolder);
            }
            return paths;
        }
        return List.of(getFluffPath(unit));
    }

    /**
     * @param assetType a Battlefield Support Asset type
     *
     * @return the fluff image folder of the corresponding TW unit type, or {@code null} if the asset type has no
     *       standard-unit folder of its own (e.g. Emplacements)
     */
    private static @Nullable String twFolderForAssetType(BFSAssetType assetType) {
        return switch (assetType) {
            case VEHICLE -> DIR_NAME_VEHICLE;
            case CONV_INFANTRY -> DIR_NAME_INFANTRY;
            case BATTLE_ARMOR -> DIR_NAME_BA;
            case EMPLACEMENT -> null;
        };
    }

    /**
     * A fluff image that is either already loaded (when it was embedded in the unit file) or still on disk, in which
     * case it is only read when it is actually shown. Exactly one of the two is set.
     *
     * @param image The already loaded image, or {@code null} when the image must be read from {@code file}
     * @param file  The file holding the image, or {@code null} when {@code image} is already loaded
     */
    public record FluffImageRecord(@Nullable Image image, @Nullable File file) {

        /**
         * @param file The image file to read when the image is shown
         *
         * @return A record for a fluff image that has not been loaded yet
         */
        public static FluffImageRecord toRecord(File file) {
            return new FluffImageRecord(null, file);
        }

        /**
         * Returns the fluff image, reading it from disk if it has not been loaded yet.
         *
         * @return The fluff image, or {@code null} if this record has neither an image nor a file
         *
         * @throws IOException When the image file is present but cannot be read
         */
        public @Nullable Image getImage() throws IOException {
            if (image != null) {
                return image;
            } else if (file != null) {
                return ImageIO.read(file);
            } else {
                return null;
            }
        }
    }

    private static boolean isNoImageFile(File file) {
        Optional<String> extension = getExtension(file.toString());
        return extension.isEmpty() || !EXTENSIONS_FLUFF_IMAGE_FORMAT_SET.contains(extension.get());
    }

    /**
     * Returns the file extension of a given filename.
     * source: baeldung.com/java-file-extension
     *
     * @param filename The filename, potentially with directories
     * @return The extension, including the dot
     */
    public static Optional<String> getExtension(String filename) {
        return Optional.ofNullable(filename)
                .filter(name -> name.contains("."))
                .map(name -> name.substring(name.lastIndexOf(".")));
    }
}
