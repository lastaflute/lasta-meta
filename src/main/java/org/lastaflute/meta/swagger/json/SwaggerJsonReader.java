/*
 * Copyright 2015-2022 the original author or authors.
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
package org.lastaflute.meta.swagger.json;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfResourceUtil;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.di.helper.misc.ParameterizedRef;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/21 Monday)
 */
public class SwaggerJsonReader {

    protected final RealJsonEngine jsonEngine; // not null

    public SwaggerJsonReader(RealJsonEngine jsonEngine) {
        this.jsonEngine = jsonEngine;
    }

    public OptionalThing<Map<String, Object>> readSwaggerJson() { // for war world
        String swaggerJsonFilePath = "./swagger.json";
        if (!DfResourceUtil.isExist(swaggerJsonFilePath)) {
            return OptionalThing.empty();
        }

        try (InputStream inputStream = DfResourceUtil.getResourceStream(swaggerJsonFilePath);
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader)) {
            String json = DfResourceUtil.readText(bufferedReader);
            return OptionalThing.of(jsonEngine.fromJsonParameteried(json, new ParameterizedRef<Map<String, Object>>() {
            }.getType()));
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read the json to the file: " + swaggerJsonFilePath, e);
        }
    }
}
