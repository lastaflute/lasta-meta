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
package org.lastaflute.meta.agent.yourswagger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Predicate;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.Srl;
import org.lastaflute.meta.swagger.diff.SwaggerDiffOption;

/**
 * @author jflute
 * @since 0.5.1 (2021/06/27 Sunday)
 */
public class YourSwaggerSyncOption { // used by e.g. UTFlute and application directly

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final List<Consumer<SwaggerDiffOption>> swaggerDiffOptionSetupperList = new ArrayList<>();
    protected Predicate<String> exceptionDeterminer;

    // ===================================================================================
    //                                                                              Option
    //                                                                              ======
    public YourSwaggerSyncOption deriveTargetNode(BiPredicate<String, String> targetNodeLambda) {
        swaggerDiffOptionSetupperList.add(op -> op.deriveTargetNode(targetNodeLambda));
        return this;
    }

    public YourSwaggerSyncOption removeLastaTrailingSlash() {
        swaggerDiffOptionSetupperList.add(op -> {
            op.filterLeftContent(content -> filterTrailingSlash(content));
        });
        return this;
    }

    protected String filterTrailingSlash(String leftSwaggerContent) {
        // #needs_fix jflute depends on Lasta-presents swagger.json format here, can it do by OpenAPI (2021/06/28)
        return Srl.replace(leftSwaggerContent, "/\": {", "\": {");
    }

    public YourSwaggerSyncOption determineException(Predicate<String> exceptionDeterminer) {
        this.exceptionDeterminer = exceptionDeterminer;
        return this;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public List<Consumer<SwaggerDiffOption>> getSwaggerDiffOptionSetupperList() {
        return Collections.unmodifiableList(swaggerDiffOptionSetupperList);
    }

    public OptionalThing<Predicate<String>> getExceptionDeterminer() {
        return OptionalThing.ofNullable(exceptionDeterminer, () -> {
            throw new IllegalStateException("Not found the exceptionDeterminer.");
        });
    }
}
