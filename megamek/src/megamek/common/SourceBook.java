/*
 * Copyright (C) 2025-2026 The MegaMek Team. All Rights Reserved.
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

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import jakarta.annotation.Nullable;

/**
 * This class is used to store information about an individual sourcebook such as TRO: 3039.
 *
 * <p>Unknown fields in a sourcebook file are ignored, so a field added to the data files later cannot stop an
 * older client from loading the book.</p>
 */
@SuppressWarnings("unused") // Methods used by Jackson during deserialization
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonPropertyOrder({ "id", "sku", "title", "abbrev", "image", "url", "ispublished", "canon", "published",
                     "description", "mul_url" })
public class SourceBook {

    private String title;
    private int id;
    private String sku;
    private String abbrev;
    private String image;
    private String url;
    private boolean ispublished = false;
    private boolean canon = false;
    private String published;
    private String description;
    private String mul_url;

    /**
     * @return The full title of the book, such as "Technical Readout: 3039"
     */
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    /**
     * @return The mul id of the book.
     */
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return The product code of the book, such as "CAT35121c"
     */
    public String getSku() {
        return sku;
    }

    public void setSku(String sku) {
        this.sku = sku;
    }

    /**
     * @return The abbreviation of the book, such as "TR: 3039"
     */
    public String getAbbrev() {
        return abbrev;
    }

    public void setAbbrev(String abbrev) {
        this.abbrev = abbrev;
    }

    // image URLs, currently bad quality
    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    // shop page URls
    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    // some books are apparently unpublished or were at some point
    public boolean getIspublished() {
        return ispublished;
    }

    public void setIspublished(boolean ispublished) {
        this.ispublished = ispublished;
    }

    /**
     * @return true if this sourcebook is canon
     */
    public boolean isCanon() {
        return canon;
    }

    public void setCanon(boolean canon) {
        this.canon = canon;
    }

    /**
     * @return The date this book was first published, as precise as is known: "2016", "2016-02" or "2016-02-10".
     *       A reprint keeps the date of its first printing. {@code null} if unknown.
     */
    public @Nullable String getPublished() {
        return published;
    }

    public void setPublished(String published) {
        this.published = published;
    }

    /**
     * @return The link to show for this book: the shop page if there is one, else the Master Unit List page.
     *       {@code null} if the book has neither.
     */
    @JsonIgnore
    public @Nullable String getLinkUrl() {
        if ((url != null) && !url.isBlank()) {
            return url;
        }
        if ((mul_url != null) && !mul_url.isBlank()) {
            return mul_url;
        }
        return null;
    }

    /**
     * @return A description of the book with HTML tags.
     */
    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * @return A complete Master Unit List URL for this book
     */
    public String getMul_url() {
        return mul_url;
    }

    public void setMul_url(String mul_url) {
        this.mul_url = mul_url;
    }
}
