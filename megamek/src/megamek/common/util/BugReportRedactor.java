/*
 * Copyright (C) 2026 The MegaMek Team. All Rights Reserved.
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

package megamek.common.util;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Function;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import jakarta.annotation.Nullable;
import megamek.logging.MMLogger;

/**
 * Removes details that identify the player or their machine from text that is about to leave it in a bug report.
 *
 * <p>Four things are replaced, in this order:</p>
 * <ul>
 *     <li>the player's home folder, such as {@code C:\Users\Dave}, becomes {@value #HOME_TOKEN}</li>
 *     <li>the folder name under any {@code Users}, {@code home} or {@code Documents and Settings} folder, and the
 *     player's login name wherever it is a whole folder name, become {@value #USER_TOKEN}</li>
 *     <li>the computer's network name becomes {@value #HOST_TOKEN}</li>
 *     <li>IPv4 and IPv6 addresses become {@value #IP_TOKEN}, apart from loopback and the "any" address, which
 *     identify nobody and tell a maintainer that a game was hosted locally</li>
 * </ul>
 *
 * <p>The tokens use square brackets rather than angle brackets because the combat report is HTML, where a token
 * such as {@code <home>} would be read as a tag and disappear from the page.</p>
 *
 * <p>Only text is redacted. Save games are XML inside a gzip stream and are copied untouched, since a replaced value
 * could leave them unloadable.</p>
 *
 * <p>An instance counts what it replaced, so the bundle can log one summary line. It is not thread-safe; use one
 * instance per archive.</p>
 */
public final class BugReportRedactor {
    private static final MMLogger LOGGER = MMLogger.create(BugReportRedactor.class);

    /** Replaces the player's home folder. */
    public static final String HOME_TOKEN = "[home]";
    /** Replaces the player's login name inside a path. */
    public static final String USER_TOKEN = "[user]";
    /** Replaces the computer's network name. */
    public static final String HOST_TOKEN = "[host]";
    /** Replaces an IP address. */
    public static final String IP_TOKEN = "[ip]";

    /** Shorter names than this are too likely to match ordinary words, so they are not redacted on their own. */
    private static final int MINIMUM_NAME_LENGTH = 2;

    /** A path separator on any of the three platforms, as a regex fragment. */
    private static final String SEPARATOR = "[\\\\/]";

    /** Stops a name matching the start of a longer one, so the home folder {@code Al} leaves {@code Alice} alone. */
    private static final String NOT_FOLLOWED_BY_NAME_CHARACTER = "(?![\\p{L}\\p{N}_-])";

    /**
     * The folder under a profile root. A name with spaces ({@code John Smith}) is taken only when a separator follows
     * it, so a path at the end of a sentence does not swallow the rest of the sentence.
     */
    private static final Pattern PROFILE_FOLDER = Pattern.compile(
          "(?i)(" + SEPARATOR + "(?:Users|home|Documents and Settings)" + SEPARATOR + "+)"
                + "((?:[^\\\\/\\r\\n:*?\"<>|]+?(?=" + SEPARATOR + "))|[^\\\\/\\s:*?\"<>|']+)");

    /**
     * Four dotted numbers. Each octet is range-checked after matching, so a version such as 1.2.3 is never hit. A
     * number written right after {@code driver } is a graphics driver version (old ones look like
     * {@code 6.14.10.13}), so it is left alone.
     */
    private static final Pattern IPV4_ADDRESS = Pattern.compile(
          "(?<![\\w.])(?<!driver )(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})(?!\\w|\\.\\d)");

    /**
     * Colon-separated hex groups with an optional zone suffix. A match is only replaced if it is shaped like a real
     * address (see {@link #isIpv6Address(String)}), so log timestamps such as {@code 21:20:07} are left alone.
     */
    private static final Pattern IPV6_CANDIDATE = Pattern.compile(
          "(?<![\\w:.])((?:[0-9A-Fa-f]{0,4}:){2,7}[0-9A-Fa-f]{0,4})(%[\\w.]+)?(?![\\w:])");

    /** IPv6 has eight groups, so a full address with no {@code ::} shortening has seven colons. */
    private static final int FULL_IPV6_COLON_COUNT = 7;
    private static final int MAXIMUM_OCTET = 255;
    private static final String LOOPBACK_IPV4_PREFIX = "127.";
    private static final String ANY_IPV4_ADDRESS = "0.0.0.0";
    private static final List<String> NON_IDENTIFYING_IPV6 = List.of("::1", "::");

    private final @Nullable Pattern homeFolder;
    private final @Nullable Pattern userNameFolder;
    private final List<Pattern> hostNames;

    private int pathCount;
    private int hostNameCount;
    private int ipAddressCount;

    /**
     * Creates a redactor for given values. Tests use this directly; the game uses {@link #forThisMachine()}.
     *
     * @param homeDirectory the player's home folder, or {@code null} to skip that rule
     * @param userName      the player's login name, or {@code null} to skip that rule
     * @param hostNames     the computer's network names; blank and {@code localhost} entries are ignored
     */
    public BugReportRedactor(@Nullable String homeDirectory, @Nullable String userName, List<String> hostNames) {
        this.homeFolder = buildHomeFolderPattern(homeDirectory);
        this.userNameFolder = buildUserNameFolderPattern(userName);
        this.hostNames = buildHostNamePatterns(hostNames);
    }

    /**
     * Creates a redactor for the player running this program, reading the home folder and login name from the JVM
     * and the computer name from the environment and the network stack.
     *
     * <p>The network lookup can take a moment on a badly configured machine, so call this off the event dispatch
     * thread when that matters.</p>
     *
     * @return a redactor for this machine
     */
    public static BugReportRedactor forThisMachine() {
        List<String> hostNames = new ArrayList<>();
        addIfPresent(hostNames, System.getenv("COMPUTERNAME"));
        addIfPresent(hostNames, System.getenv("HOSTNAME"));
        try {
            addIfPresent(hostNames, InetAddress.getLocalHost().getHostName());
        } catch (Exception exception) {
            LOGGER.debug("[BugReport] Could not look up this computer's network name: {}", exception.getMessage());
        }

        BugReportRedactor redactor = new BugReportRedactor(System.getProperty("user.home"),
              System.getProperty("user.name"), hostNames);
        LOGGER.info("[BugReport] Redaction rules: home folder {}, login name {}, {} host name(s)",
              (redactor.homeFolder == null) ? "not set" : "set",
              (redactor.userNameFolder == null) ? "not set" : "set",
              redactor.hostNames.size());
        return redactor;
    }

    /**
     * Replaces every identifying detail in the text.
     *
     * @param text the text to clean; may span many lines
     *
     * @return the cleaned text
     */
    public String redact(String text) {
        String result = text;

        if (homeFolder != null) {
            result = replaceCounting(homeFolder, result, match -> {
                pathCount++;
                return HOME_TOKEN;
            });
        }

        result = replaceCounting(PROFILE_FOLDER, result, match -> {
            pathCount++;
            return match.group(1) + USER_TOKEN;
        });

        if (userNameFolder != null) {
            result = replaceCounting(userNameFolder, result, match -> {
                pathCount++;
                return USER_TOKEN;
            });
        }

        for (Pattern hostName : hostNames) {
            result = replaceCounting(hostName, result, match -> {
                hostNameCount++;
                return HOST_TOKEN;
            });
        }

        result = replaceCounting(IPV4_ADDRESS, result, match -> {
            if (!isIdentifyingIpv4(match)) {
                return match.group();
            }
            ipAddressCount++;
            return IP_TOKEN;
        });

        result = replaceCounting(IPV6_CANDIDATE, result, match -> {
            if (!isIpv6Address(match.group(1))) {
                return match.group();
            }
            ipAddressCount++;
            return IP_TOKEN;
        });

        return result;
    }

    /** @return how many home-folder and login-name path pieces have been replaced so far */
    public int pathCount() {
        return pathCount;
    }

    /** @return how many computer names have been replaced so far */
    public int hostNameCount() {
        return hostNameCount;
    }

    /** @return how many IP addresses have been replaced so far */
    public int ipAddressCount() {
        return ipAddressCount;
    }

    /**
     * Runs one rule over the text. The replacement is quoted, so a {@code $} or {@code \} in it is kept literally.
     */
    private static String replaceCounting(Pattern pattern, String text,
          Function<MatchResult, String> replacement) {
        Matcher matcher = pattern.matcher(text);
        return matcher.replaceAll(match -> Matcher.quoteReplacement(replacement.apply(match)));
    }

    /**
     * @param match an {@link #IPV4_ADDRESS} match
     *
     * @return {@code true} if every octet is in range and the address is neither loopback nor {@code 0.0.0.0}
     */
    private static boolean isIdentifyingIpv4(MatchResult match) {
        for (int group = 1; group <= 4; group++) {
            if (Integer.parseInt(match.group(group)) > MAXIMUM_OCTET) {
                return false;
            }
        }
        String address = match.group();
        return !address.startsWith(LOOPBACK_IPV4_PREFIX) && !address.equals(ANY_IPV4_ADDRESS);
    }

    /**
     * Whether colon-separated hex groups form a real IPv6 address rather than a timestamp or a {@code Class::method}
     * reference: it must be shortened with {@code ::} or have all eight groups, must contain a digit, and must not be
     * loopback.
     *
     * @param candidate the address part of an {@link #IPV6_CANDIDATE} match, without any zone suffix
     *
     * @return {@code true} if it should be replaced
     */
    static boolean isIpv6Address(String candidate) {
        if (NON_IDENTIFYING_IPV6.contains(candidate)) {
            return false;
        }

        int colonCount = 0;
        boolean hasDigit = false;
        for (int index = 0; index < candidate.length(); index++) {
            char character = candidate.charAt(index);
            if (character == ':') {
                colonCount++;
            } else if (Character.isDigit(character)) {
                hasDigit = true;
            }
        }

        boolean isShortened = candidate.contains("::");
        return hasDigit && (isShortened || (colonCount == FULL_IPV6_COLON_COUNT));
    }

    /**
     * Builds a pattern for the home folder that accepts either slash direction and doubled (escaped) backslashes, so
     * {@code C:\Users\Dave}, {@code C:/Users/Dave} and {@code C:\\Users\\Dave} all match.
     *
     * @param homeDirectory the home folder, or {@code null}
     *
     * @return the pattern, or {@code null} if there is no usable home folder
     */
    private static @Nullable Pattern buildHomeFolderPattern(@Nullable String homeDirectory) {
        if (homeDirectory == null) {
            return null;
        }

        List<String> segments = new ArrayList<>();
        for (String segment : homeDirectory.split("[\\\\/]+")) {
            if (!segment.isBlank()) {
                segments.add(Pattern.quote(segment));
            }
        }
        // A home folder at the root (or a single segment such as "C:") would redact far more than intended.
        if (segments.size() < 2) {
            return null;
        }

        String leadingSeparator = homeDirectory.startsWith("/") || homeDirectory.startsWith("\\")
              ? SEPARATOR + "+"
              : "";
        String body = String.join(SEPARATOR + "+", segments);
        return Pattern.compile(leadingSeparator + body + NOT_FOLLOWED_BY_NAME_CHARACTER,
              Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);
    }

    /**
     * Builds a pattern for the login name as a whole folder name, as in {@code D:\Dave\Games}. A name that only
     * appears inside other text, such as a unit called "Dave's Lance", is left alone.
     *
     * @param userName the login name, or {@code null}
     *
     * @return the pattern, or {@code null} if there is no usable login name
     */
    private static @Nullable Pattern buildUserNameFolderPattern(@Nullable String userName) {
        if ((userName == null) || (userName.strip().length() < MINIMUM_NAME_LENGTH)) {
            return null;
        }
        return Pattern.compile("(?<=" + SEPARATOR + ")" + Pattern.quote(userName.strip()) + "(?=" + SEPARATOR + "|$)",
              Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.MULTILINE);
    }

    /**
     * Builds one pattern per distinct host name, longest first, so a full name such as {@code dave-pc.lan} is
     * replaced whole before its short form {@code dave-pc} is tried.
     *
     * @param hostNames the candidate names
     *
     * @return the patterns, possibly empty
     */
    private static List<Pattern> buildHostNamePatterns(List<String> hostNames) {
        List<String> distinctNames = new ArrayList<>();
        for (String hostName : hostNames) {
            if (hostName == null) {
                continue;
            }
            String trimmed = hostName.strip();
            String lowerCase = trimmed.toLowerCase(Locale.ROOT);
            if ((trimmed.length() < MINIMUM_NAME_LENGTH) || lowerCase.equals("localhost")) {
                continue;
            }
            boolean alreadyListed = false;
            for (String listed : distinctNames) {
                if (listed.equalsIgnoreCase(trimmed)) {
                    alreadyListed = true;
                    break;
                }
            }
            if (!alreadyListed) {
                distinctNames.add(trimmed);
            }
        }
        distinctNames.sort((first, second) -> Integer.compare(second.length(), first.length()));

        List<Pattern> patterns = new ArrayList<>();
        for (String name : distinctNames) {
            patterns.add(Pattern.compile("(?<![\\p{L}\\p{N}_-])" + Pattern.quote(name) + NOT_FOLLOWED_BY_NAME_CHARACTER,
                  Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
        }
        return patterns;
    }

    private static void addIfPresent(List<String> names, @Nullable String name) {
        if ((name != null) && !name.isBlank()) {
            names.add(name);
        }
    }
}
