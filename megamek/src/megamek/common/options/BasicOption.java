/*
 * MegaMek - Copyright (C) 2000-2002 Ben Mazur (bmazur@sev.org)
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
package megamek.common.options;

import java.io.Serializable;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.adapters.XmlJavaTypeAdapter;

/**
 *
 * @author nderwin
 */
@XmlAccessorType(XmlAccessType.NONE)
public class BasicOption implements IBasicOption, Serializable {

    private static final long serialVersionUID = 916639704995096673L;

    @XmlElement(name = "optionname")
    private String name;

    @XmlElement(name = "optionvalue")
    @XmlJavaTypeAdapter(ToStringAdapter.class)
    private Object value;

    public BasicOption(final String name, final Object value) {
        this.name = name;
        this.value = value;
    }

    /**
     * Constructor that satisfies JAXB.
     */
    public BasicOption() {
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public Object getValue() {
        return value;
    }

}
