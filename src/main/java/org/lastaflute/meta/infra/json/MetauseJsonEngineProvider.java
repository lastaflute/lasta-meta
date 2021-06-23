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

import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.json.control.JsonControlMeta;
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
            // starndard option as possible because other-world parser may parse it
            builder.serializeNulls().setPrettyPrinting();
        }, op -> {});
        // not to depend on application settings
        //return ContainerUtil.getComponent(JsonManager.class);
    }

    // ===================================================================================
    //                                                                        Control Meta
    //                                                                        ============
    public JsonControlMeta getAppJsonControlMeta() { // for e.g. FieldNaming, DateTimeFormatter
        final JsonManager jsonManager = ContainerUtil.getComponent(JsonManager.class);
        return jsonManager.pulloutControlMeta();
    }
}
