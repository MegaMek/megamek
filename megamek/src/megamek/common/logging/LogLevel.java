/*
 * MegaMek - Copyright (C) 2000-2011 Ben Mazur (bmazur@sev.org)
 *           Copyright (C) 2018 The MegaMek team
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
package megamek.common.logging;

import java.util.ArrayList;
import java.util.List;

import org.apache.logging.log4j.Level;

import megamek.common.annotations.Nullable;

/**
 * @author: Deric "Netzilla" Page (deric dot page at usa dot net)
 */
public enum LogLevel {

    OFF(Level.OFF),
    FATAL(Level.FATAL),
    ERROR(Level.ERROR),
    WARNING(Level.WARN),
    INFO(Level.INFO),
    DEBUG(Level.DEBUG),
    TRACE(Level.TRACE);

    private final Level level;

    LogLevel(Level level) {
        this.level = level;
    }

    public Level getLevel() {
        return level;
    }

    public int toInt() {
        return level.intLevel();
    }

    public boolean willLog(final LogLevel logLevel) {
        return logLevel.getLevel().compareTo(level) >= 0;
    }

    public static String[] getLogLevelNames() {
        List<String> out = new ArrayList<>();
        for (LogLevel l : values()) {
            out.add(l.toString());
        }
        return out.toArray(new String[out.size()]);
    }

    public static LogLevel getLogLevel(final String levelName) {
        for (LogLevel l : values()) {
            if (l.toString().equalsIgnoreCase(levelName)) {
                return l;
            }
        }
        return null;
    }

    @Nullable
    public static LogLevel getFromLog4jLevel(final int level) {
        if (Level.FATAL.intLevel() == level) {
            return FATAL;
        } else if (Level.ERROR.intLevel() == level) {
            return ERROR;
        } else if (Level.WARN.intLevel() == level) {
            return WARNING;
        } else if (Level.INFO.intLevel() == level) {
            return INFO;
        } else if (Level.DEBUG.intLevel() == level) {
            return DEBUG;
        } else if (Level.TRACE.intLevel() == level) {
            return TRACE;
        } else if (Level.OFF.intLevel() == level) {
            return OFF;
        }
        return null;
    }

}
