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
package megamek.common;

import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

/**
 * A structured reference to one page in a sourcebook. The page is {@code null} when the source has no page reference.
 */
@JsonInclude(JsonInclude.Include.ALWAYS)
@JsonPropertyOrder({ "book", "page" })
public record RulesRef(SourceBookCode book, Integer page) {

    public RulesRef {
        Objects.requireNonNull(book, "book");
        if ((page != null) && (page < 1)) {
            throw new IllegalArgumentException("page must be positive");
        }
    }

    /**
     * @return this reference formatted for display as {@code abbrev, page}, or only {@code abbrev} when its page is
     *       unknown.
     */
    public String toDisplayString() {
        return page == null ? book.getAbbrev() : book.getAbbrev() + ", " + page;
    }

    /** Formats references for display, separated by semicolons. */
    public static String formatForDisplay(Collection<RulesRef> references) {
        Objects.requireNonNull(references, "references");
        return references.stream().map(RulesRef::toDisplayString).collect(Collectors.joining("; "));
    }

    /**
     * @return serializer-independent structured export data using the sourcebook abbreviation, not the enum name.
     */
    public Map<String, Object> toYamlData() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("book", book.getAbbrev());
        data.put("page", page);
        return data;
    }

    @Override
    public String toString() {
        return page == null ? book.getAbbrev() : page + ", " + book.getAbbrev();
    }
}
