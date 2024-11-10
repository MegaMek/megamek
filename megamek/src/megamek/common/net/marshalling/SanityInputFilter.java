/*
 * Copyright (c) 2014-2024 - The MegaMek Team. All Rights Reserved.
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
package megamek.common.net.marshalling;

import java.io.ObjectInputFilter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import megamek.logging.MMLogger;

public class SanityInputFilter implements ObjectInputFilter {
    protected static final Pattern[] filterList = new Pattern[] {
            // Arrays of Core Types
            Pattern.compile("\\[C"),
            Pattern.compile("\\[I"),
            Pattern.compile("\\[Z"),

            // Arrays of java.lang items.
            Pattern.compile("\\[Ljava\\.lang\\.Enum"),
            Pattern.compile("\\[Ljava\\.lang\\.Object"),

            // File IO
            Pattern.compile("java\\.io\\.File"),

            // Java Lang
            Pattern.compile("java\\.lang\\.Boolean"),
            Pattern.compile("java\\.lang\\.Enum"),
            Pattern.compile("java\\.lang\\.Integer"),
            Pattern.compile("java\\.lang\\.Double"),
            Pattern.compile("java\\.lang\\.Long"),
            Pattern.compile("java\\.lang\\.Number"),
            Pattern.compile("java\\.lang\\.StringBuffer"),
            Pattern.compile("java\\.lang\\.\\[I"),
            Pattern.compile("java\\.lang\\.Object"),
            Pattern.compile("java\\.lang\\.String"),

            // Java Net
            Pattern.compile("java\\.net\\.URI"),

            // Java Util
            Pattern.compile("java\\.util\\.AbstractMap"),
            Pattern.compile("java\\.util\\.ArrayList"),
            Pattern.compile("java\\.util\\.Collections\\$SetFromMap"),
            Pattern.compile("java\\.util\\.Collections\\$UnmodifiableCollection"),
            Pattern.compile("java\\.util\\.Collections\\$UnmodifiableList"),
            Pattern.compile("java\\.util\\.Collections\\$UnmodifiableRandomAccessList"),
            Pattern.compile("java\\.util\\.concurrent\\.ConcurrentHashMap"),
            Pattern.compile("java\\.util\\.concurrent\\.ConcurrentHashMap$Segment"),
            Pattern.compile("java\\.util\\.concurrent\\.CopyOnWriteArrayList"),
            Pattern.compile("java\\.util\\.concurrent\\.locks\\.AbstractOwnableSynchronizer"),
            Pattern.compile("java\\.util\\.concurrent\\.locks\\.AbstractQueuedSynchronizer"),
            Pattern.compile("java\\.util\\.concurrent\\.locks\\.ReentrantLock"),
            Pattern.compile("java\\.util\\.concurrent\\.locks\\.ReentrantLock\\$NonfairSync"),
            Pattern.compile("java\\.util\\.concurrent\\.locks\\.ReentrantLock\\$Sync"),
            Pattern.compile("java\\.util\\.UUID"),
            Pattern.compile("java\\.util\\.EnumMap"),
            Pattern.compile("java\\.util\\.EnumSet"),
            Pattern.compile("java\\.util\\.HashMap"),
            Pattern.compile("java\\.util\\.HashSet"),
            Pattern.compile("java\\.util\\.Hashtable"),
            Pattern.compile("java\\.util\\.LinkedHashMap"),
            Pattern.compile("java\\.util\\.LinkedHashSet"),
            Pattern.compile("java\\.util\\.LinkedList"),
            Pattern.compile("java\\.util\\.RegularEnumSet"),
            Pattern.compile("java\\.util\\.Set"),
            Pattern.compile("java\\.util\\.TreeMap"),
            Pattern.compile("java\\.util\\.Map"),
            Pattern.compile("java\\.util\\.TreeSet"),
            Pattern.compile("java\\.util\\.Vector"),

            // Fonts
            Pattern.compile("org\\.apache\\.fop\\.fonts\\.*"),

            // MegaMek Related
            Pattern.compile("megamek.*"),
            Pattern.compile("mekhq.*"),
            Pattern.compile("megameklab.*"),
    };

    private static final MMLogger logger = MMLogger.create(SanityInputFilter.class);

    @Override
    public Status checkInput(FilterInfo filterInfo) {
        Class<?> serialClass = filterInfo.serialClass();
        if (serialClass == null) {
            return Status.UNDECIDED;
        }

        String className = serialClass.getName();

        for (Pattern pattern : filterList) {
            Matcher match = pattern.matcher(className);

            if (match.find()) {
                return Status.ALLOWED;
            }

            if (className.contains(pattern.toString())) {
                return Status.ALLOWED;
            }
        }

        logger.info("Class is Rejected: {}", className);

        return Status.REJECTED;
    }

    public static Pattern[] getFilterList() {
        return filterList;
    }
}
