/*
 * Copyright (c) 2022-2023 - The MegaMek Team. All Rights Reserved.
 *
 * This file is part of MegaMek.
 *
 * MegaMek is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MegaMek is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MegaMek. If not, see <http://www.gnu.org/licenses/>.
 */
package megamek.common.eras;

import megamek.codeUtilities.StringUtility;
import megamek.common.Configuration;
import megamek.common.annotations.Nullable;
import megamek.common.util.ImageUtil;
import megamek.common.util.fileUtils.MegaMekFile;
import org.apache.logging.log4j.LogManager;

import javax.swing.*;
import java.io.File;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

import static megamek.common.eras.EraFlag.*;

/**
 * This class represents an Era of the BT Universe such as the Civil War or Star League eras.
 * The eras are read from the XML definition file located at {@link megamek.MMConstants#ERAS_FILE_PATH}.
 *
 * @implNote This class is immutable.
 *
 * @author Justin "Windchild" Bowen
 * @author Simon (Juliez)
 */
public final class Era {

    /** @return The full name of the Era, e.g. "Clan Invasion". */
    public String name() {
        return name;
    }

    /** @return The short name of the Era, e.g. "CI" or "ILC". */
    public String code() {
        return code;
    }

    /** @return The end date of this Era. Note that the end date is e.g. 3052 when the era ends on 31 Dec 3051. */
    public LocalDate end() {
        return end;
    }

    /** @return All {@link EraFlag}s of this era. Returns a copy that may be safely modified. */
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
            MegaMekFile iconfile = new MegaMekFile(Configuration.universeImagesDir(), iconFilePath);
            return new ImageIcon(iconfile.getFile().getPath());
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
        return hasAnyFlagOf(EARLY_SUCCESSION_WARS, LATE_SUCCESSION_WARS_LOSTECH, LATE_SUCCESSION_WARS_RENAISSANCE);
    }

    /** @return True when this Era is part of the Republic eras. */
    public boolean isRepublic() {
        return hasAnyFlagOf(LATE_REPUBLIC, EARLY_REPUBLIC);
    }

    /** @return True when the given date is part of this era. */
    public boolean isThisEra(LocalDate date) {
        return Eras.isThisEra(date, this);
    }

    /** @return True when this is the last Era (it has no end date in the xml file). */
    public boolean isLastEra() {
        return end.equals(LocalDate.MAX);
    }

    /** @return True when this is the last Era (it has no end date in the xml file). */
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

    //region Non-public

    private final String code;
    private final String name;
    private final LocalDate end;
    private final Set<EraFlag> flags = new HashSet<>();
    private final int mulId;
    private final String iconFilePath;

    /**
     * Constructs a new Era. The code and name cannot be empty or null. The code is the database key and must
     * be unique.
     * This constructor is for use from {@link Eras} only.
     *
     * @param code The code (short name) of the Era, such as CW or ILC.
     * @param name The full name of the Era, such as "Civil War".
     * @param end The inclusive end date of the Era, such as 3051-31-12 (next Era starts 3052-1-1)
     * @param flags {@link EraFlag}s of this Era
     * @param mulId The MUL ID for this Era; used to create hyperlinks to the MUL website, set to -1 if negative
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
                LogManager.getLogger().warn("Icon file at " + iconFilePath + " not found.");
                this.iconFilePath = "";
            } else {
                this.iconFilePath = iconFilePath;
            }
        } else {
            this.iconFilePath = "";
        }
    }
    //endregion non-public
}