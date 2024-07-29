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
  public final static Pattern[] filterList = new Pattern[] {
      // Arrays of Core Types
      Pattern.compile("[C", Pattern.LITERAL),
      Pattern.compile("[I", Pattern.LITERAL),
      Pattern.compile("[Z", Pattern.LITERAL),

      // Arrays of java.lang items.
      Pattern.compile("[Ljava.lang.Enum", Pattern.LITERAL),
      Pattern.compile("[Ljava.lang.Object", Pattern.LITERAL),

      // File IO
      Pattern.compile("java.io.File", Pattern.LITERAL),

      // Java Lang
      Pattern.compile("java.lang.Boolean", Pattern.LITERAL),
      Pattern.compile("java.lang.Enum", Pattern.LITERAL),
      Pattern.compile("java.lang.Integer", Pattern.LITERAL),
      Pattern.compile("java.lang.Double", Pattern.LITERAL),
      Pattern.compile("java.lang.Long", Pattern.LITERAL),
      Pattern.compile("java.lang.Number", Pattern.LITERAL),
      Pattern.compile("java.lang.StringBuffer", Pattern.LITERAL),
      Pattern.compile("java.lang.[I", Pattern.LITERAL),
      Pattern.compile("java.lang.Object", Pattern.LITERAL),
      Pattern.compile("java.lang.String", Pattern.LITERAL),

      // Java Util
      Pattern.compile("java.util.ArrayList", Pattern.LITERAL),
      Pattern.compile("java.util.Collections$SetFromMap", Pattern.LITERAL),
      Pattern.compile("java.util.Collections$UnmodifiableCollection", Pattern.LITERAL),
      Pattern.compile("java.util.Collections$UnmodifiableList", Pattern.LITERAL),
      Pattern.compile("java.util.Collections$UnmodifiableRandomAccessList", Pattern.LITERAL),
      Pattern.compile("java.util.concurrent.ConcurrentHashMap", Pattern.LITERAL),
      Pattern.compile("java.util.concurrent.ConcurrentHashMap$Segment", Pattern.LITERAL),
      Pattern.compile("java.util.concurrent.CopyOnWriteArrayList", Pattern.LITERAL),
      Pattern.compile("java.util.concurrent.locks.AbstractOwnableSynchronizer", Pattern.LITERAL),
      Pattern.compile("java.util.concurrent.locks.AbstractQueuedSynchronizer", Pattern.LITERAL),
      Pattern.compile("java.util.concurrent.locks.ReentrantLock", Pattern.LITERAL),
      Pattern.compile("java.util.concurrent.locks.ReentrantLock$NonfairSync", Pattern.LITERAL),
      Pattern.compile("java.util.concurrent.locks.ReentrantLock$Sync", Pattern.LITERAL),
      Pattern.compile("java.util.UUID", Pattern.LITERAL),
      Pattern.compile("java.util.EnumMap", Pattern.LITERAL),
      Pattern.compile("java.util.EnumSet", Pattern.LITERAL),
      Pattern.compile("java.util.HashMap", Pattern.LITERAL),
      Pattern.compile("java.util.HashSet", Pattern.LITERAL),
      Pattern.compile("java.util.Hashtable", Pattern.LITERAL),
      Pattern.compile("java.util.LinkedHashMap", Pattern.LITERAL),
      Pattern.compile("java.util.LinkedHashSet", Pattern.LITERAL),
      Pattern.compile("java.util.LinkedList", Pattern.LITERAL),
      Pattern.compile("java.util.RegularEnumSet", Pattern.LITERAL),
      Pattern.compile("java.util.TreeMap", Pattern.LITERAL),
      Pattern.compile("java.util.Map", Pattern.LITERAL),
      Pattern.compile("java.util.TreeSet", Pattern.LITERAL),
      Pattern.compile("java.util.Vector", Pattern.LITERAL),

      // MegaMek Related
      Pattern.compile("megamek.", Pattern.LITERAL),
      Pattern.compile("mekhq.", Pattern.LITERAL),
      Pattern.compile("megameklab.", Pattern.LITERAL),
  };

  private final static MMLogger logger = MMLogger.create(SanityInputFilter.class);

  @Override
  public Status checkInput(FilterInfo filterInfo) {
    Class<?> serialClass = filterInfo.serialClass();
    if (serialClass == null) {
      return Status.UNDECIDED;
    }

    String className = serialClass.getName();

    for (Pattern pattern : filterList) {
      Matcher match = pattern.matcher(className);
      if (match.matches()) {
        return Status.ALLOWED;
      }

      if (className.contains(pattern.toString())) {
        return Status.ALLOWED;
      }
    }

    logger.info("Class is Undecided: {}", className);

    return Status.UNDECIDED;
  }
}
