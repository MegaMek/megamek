/*
 * MegaMek - Copyright (C) 2018 - The MegaMek Team
 *
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 */
package megamek.common.templates;

import freemarker.template.Configuration;
import freemarker.template.TemplateExceptionHandler;

/**
 * Configuration for FreeMarker templates
 *
 * @author Neoancient
 */
public final class TemplateConfiguration {

    private static Configuration configuration = null;

    public static Configuration getInstance() {
        if (null == configuration) {
            configuration = createConfiguration();
        }
        return configuration;
    }

    private static Configuration createConfiguration() {
        final Configuration cfg = new Configuration(Configuration.VERSION_2_3_31);
        cfg.setClassForTemplateLoading(TemplateConfiguration.class, "/megamek/common/templates"); // TODO : Remove inline file path
        cfg.setDefaultEncoding("UTF-8");
        cfg.setTemplateExceptionHandler(TemplateExceptionHandler.RETHROW_HANDLER);
        cfg.setLogTemplateExceptions(false);
        cfg.setWrapUncheckedExceptions(true);
        return cfg;
    }
}
