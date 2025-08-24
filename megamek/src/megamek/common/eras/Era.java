/*
 * Copyright (C) 2022-2025 The MegaMek Team. All Rights Reserved.
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

package megamek.common.eras;

import static megamek.common.eras.EraFlag.EARLY_REPUBLIC;
import static megamek.common.eras.EraFlag.EARLY_SUCCESSION_WARS;
import static megamek.common.eras.EraFlag.LATE_REPUBLIC;
import static megamek.common.eras.EraFlag.LATE_SUCCESSION_WARS_LOS_TECH;
import static megamek.common.eras.EraFlag.LATE_SUCCESSION_WARS_RENAISSANCE;

import java.io.File;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import javax.swing.ImageIcon;

import megamek.codeUtilities.StringUtility;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import megamek.logging.MMLogger;

/**
 * This class represents an Era of the BT Universe such as the Civil War or Star League eras. The eras are read from the
 * XML definition file located at {@link megamek.MMConstants#ERAS_FILE_PATH}.
 * <p>
 * This class is immutable.
 *
 * @author Justin "Windchild" Bowen
 * @author Simon (Juliez)
 */
public final class Era {
    private static final MMLogger LOGGER = MMLogger.create(Era.class);

    /** @return The full name of the Era, e.g. "Clan Invasion". */
    public String name() {
        return name;
    }

    /** @return The short name of the Era, e.g. "CI" or "ILC". */
    public String code() {
        return code;
    }

    /**
     * @return The end date of this Era. Note that the end date is e.g. 3052 when the era ends on 31 Dec 3051.
     */
    public LocalDate end() {
        return end;
    }

    /**
     * @return All {@link EraFlag}s of this era. Returns a copy that may be safely modified.
     */
    public Set<EraFlag> flags() {
        return Collections.unmodifiableSet(flags);
    }

    /** @return The MUL ID of this era or -1 when it has no entry in the MUL. */
    public int mulId() {
        return mulId;
    }

    /** @return The MUL ID of this era or -1 when it has no entry in the MUL. */
    public String iconFilePath() {
        return iconFilePath;
    }

    /** @return True when this Era has an icon file. */
    public boolean hasIcon() {
        return !iconFilePath.isBlank();
    }

    public ImageIcon getIcon() {
        try {
            MegaMekFile iconFile = new MegaMekFile(Configuration.universeImagesDir(), iconFilePath);
            return new ImageIcon(iconFile.getFile().getPath());
        } catch (Exception exception) {
            return new ImageIcon(ImageUtil.failStandardImage());
        }
    }

    /** @return True when this Era has at least one of the given flags. */
    public boolean hasAnyFlagOf(final EraFlag... flags) {
        return Stream.of(flags).anyMatch(this.flags::contains);
    }

    /** @return True when this Era is part of the Succession Wars. */
    public boolean isSuccessionWars() {
        return hasAnyFlagOf(EARLY_SUCCESSION_WARS, LATE_SUCCESSION_WARS_LOS_TECH, LATE_SUCCESSION_WARS_RENAISSANCE);
    }

    /** @return True when this Era is part of the Republic eras. */
    public boolean isRepublic() {
        return hasAnyFlagOf(LATE_REPUBLIC, EARLY_REPUBLIC);
    }

    /** @return True when the given date is part of this era. */
    public boolean isThisEra(LocalDate date) {
        return Eras.isThisEra(date, this);
    }

    /**
     * @return True when this is the last Era (it has no end date in the xml file).
     */
    public boolean isLastEra() {
        return end.equals(LocalDate.MAX);
    }

    /**
     * @return True when this is the last Era (it has no end date in the xml file).
     */
    public boolean isFirstEra() {
        return Eras.isFirstEra(this);
    }

    public LocalDate start() {
        return Eras.startDate(this);
    }

    @Override
    public String toString() {
        return "Era: " + name;
    }

    @Override
    public boolean equals(final @Nullable Object object) {
        if (this == object) {
            return true;
        } else if (!(object instanceof Era)) {
            return false;
        } else {
            return code.equals(((Era) object).code);
        }
    }

    @Override
    public int hashCode() {
        return code.hashCode();
    }

    // region Non-public

    private final String code;
    private final String name;
    private final LocalDate end;
    private final Set<EraFlag> flags = new HashSet<>();
    private final int mulId;
    private final String iconFilePath;

    /**
     * Constructs a new Era. The code and name cannot be empty or null. The code is the database key and must be unique.
     * This constructor is for use from {@link Eras} only.
     *
     * @param code         The code (short name) of the Era, such as CW or ILC.
     * @param name         The full name of the Era, such as "Civil War".
     * @param end          The inclusive end date of the Era, such as 3051-31-12 (next Era starts 3052-1-1)
     * @param flags        {@link EraFlag}s of this Era
     * @param mulId        The MUL ID for this Era; used to create hyperlinks to the MUL website, set to -1 if negative
     * @param iconFilePath The file path of the icon, if any, relative to {@link Configuration#universeImagesDir()}
     */
    Era(String code, String name, @Nullable LocalDate end, @Nullable Collection<EraFlag> flags,
          int mulId, @Nullable String iconFilePath) {

        if (StringUtility.isNullOrBlank(code) || StringUtility.isNullOrBlank(name)) {
            throw new IllegalArgumentException("The code and name for a new Era must not be null or empty.");
        }

        this.code = code;
        this.name = name;
        this.end = (end != null) ? end : LocalDate.MAX;
        if (flags != null) {
            this.flags.addAll(flags);
        }
        this.mulId = mulId > 0 ? mulId : -1;

        if (iconFilePath != null) {
            File iconFile = new File(Configuration.universeImagesDir(), iconFilePath);
            if (!iconFile.exists()) {
                LOGGER.warn("Icon file at {} not found.", iconFilePath);
                this.iconFilePath = "";
            } else {
                this.iconFilePath = iconFilePath;
            }
        } else {
            this.iconFilePath = "";
        }
    }
    // endregion non-public
}
