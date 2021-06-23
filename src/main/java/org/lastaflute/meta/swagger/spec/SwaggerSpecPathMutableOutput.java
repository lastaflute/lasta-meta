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
package org.lastaflute.meta.swagger.spec;

import java.util.List;
import java.util.Map;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 (2021/06/23 Wedenesday at roppongi japanese)
 */
public class SwaggerSpecPathMutableOutput {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // these are mutable, registered in setupper
    protected final Map<String, Map<String, Object>> pathsMap;
    protected final Map<String, Map<String, Object>> definitionsMap;
    protected final List<Map<String, Object>> tagsList;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecPathMutableOutput(Map<String, Map<String, Object>> pathsMap // map of top-level paths
            , Map<String, Map<String, Object>> definitionsMap // map of top-level definitions
            , List<Map<String, Object>> tagsList) { // top-level tags
        this.pathsMap = pathsMap;
        this.definitionsMap = definitionsMap;
        this.tagsList = tagsList;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // don't wrap as read-only, should be mutable
    public Map<String, Map<String, Object>> getPathsMap() {
        return pathsMap;
    }

    public Map<String, Map<String, Object>> getDefinitionsMap() {
        return definitionsMap;
    }

    public List<Map<String, Object>> getTagsList() {
        return tagsList;
    }
}
