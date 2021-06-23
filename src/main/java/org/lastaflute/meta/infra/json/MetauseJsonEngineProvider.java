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
package org.lastaflute.meta.infra.json;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.SimpleJsonManager;
import org.lastaflute.core.json.engine.GsonJsonEngine;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.core.util.ContainerUtil;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from DocumentAnalyzerFactory (2021/06/23 Wednesday at roppongi japanese)
 */
public class MetauseJsonEngineProvider {

    // ===================================================================================
    //                                                                         JSON Engine
    //                                                                         ===========
    public RealJsonEngine createJsonEngine() {
        return new GsonJsonEngine(builder -> {
            builder.serializeNulls().setPrettyPrinting();
        }, op -> {});
        // not to depend on application settings
        //return ContainerUtil.getComponent(JsonManager.class);
    }

    public OptionalThing<JsonMappingOption> getApplicationJsonMappingOption() {
        // #hope jflute use pulloutControlMeta() instead of downcast (2021/06/23)
        JsonManager jsonManager = ContainerUtil.getComponent(JsonManager.class);
        if (jsonManager instanceof SimpleJsonManager) {
            return ((SimpleJsonManager) jsonManager).getJsonMappingOption();
        }
        return OptionalThing.empty();
    }
}
