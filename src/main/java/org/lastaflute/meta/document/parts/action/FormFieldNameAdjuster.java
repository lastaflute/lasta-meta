/*
 * Copyright 2015-2021 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. See the License for the specific language
 * governing permissions and limitations under the License.
 */
package org.lastaflute.meta.document.parts.action;

import java.lang.reflect.Field;

import org.lastaflute.core.json.JsonMappingOption.JsonFieldNaming;
import org.lastaflute.core.json.control.JsonControlMeta;
import org.lastaflute.meta.infra.json.MetauseJsonEngineProvider;

import com.google.gson.FieldNamingPolicy;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from ActionDocumentGenerator (2021/06/26 Saturday at ikspiari)
 */
public class FormFieldNameAdjuster {

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    protected final MetauseJsonEngineProvider metauseJsonEngineProvider;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public FormFieldNameAdjuster(MetauseJsonEngineProvider metauseJsonEngineProvider) {
        this.metauseJsonEngineProvider = metauseJsonEngineProvider;
    }

    // ===================================================================================
    //                                                                          Field Name
    //                                                                          ==========
    public String adjustFieldName(Class<?> clazz, Field field) {
        return field.getName();
    }

    public String adjustPublicFieldName(Class<?> clazz, Field field) {
        // done (by jflute 2019/01/17) p1us2er0 judge accurately in adjustFieldName() (2017/04/20)
        if (clazz == null || isActionFormComponentType(clazz)) {
            return field.getName();
        }
        // basically JsonBody or JsonResult here
        // (Thymeleaf beans cannot be analyzed as framework so not here)
        return getAppJsonControlMeta().getMappingControlMeta().flatMap(meta -> {
            return meta.getFieldNaming().map(naming -> {
                if (naming == JsonFieldNaming.IDENTITY) {
                    return FieldNamingPolicy.IDENTITY.translateName(field);
                } else if (naming == JsonFieldNaming.CAMEL_TO_LOWER_SNAKE) {
                    return FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES.translateName(field);
                } else {
                    return field.getName();
                }
            });
        }).orElse(field.getName());
    }

    protected boolean isActionFormComponentType(Class<?> clazz) { // and not JSON body
        // #thinking jflute using Form meta of LastaFlute is better? (2019/01/17)
        return clazz.getSimpleName().endsWith("Form") // just form class
                || clazz.getName().contains("Form$"); // inner class of Form (e.g. Part)
    }

    // ===================================================================================
    //                                                                        JSON Control
    //                                                                        ============
    protected JsonControlMeta getAppJsonControlMeta() {
        return metauseJsonEngineProvider.getAppJsonControlMeta();
    }
}
