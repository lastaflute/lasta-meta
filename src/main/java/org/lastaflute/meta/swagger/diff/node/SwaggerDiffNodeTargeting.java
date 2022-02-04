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
package org.lastaflute.meta.swagger.diff.node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

import org.dbflute.util.DfStringUtil;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerDiffOption (2021/06/29 Tuesday at roppongi japanese)
 */
public class SwaggerDiffNodeTargeting {

    // ===================================================================================
    //                                                                           Targeting
    //                                                                           =========
    public BiConsumer<String, JsonNode> prepareNodeTargeting(BiPredicate<String, String> targetNodeLambda) {
        return (fieldName, node) -> {
            if (node.isArray()) {
                IntStream.range(0, node.size()).forEach(index -> {
                    prepareNodeTargeting(targetNodeLambda).accept(fieldName, node.get(index));
                });
            } else if (node.isObject()) {
                final List<String> fieldNameList = new ArrayList<String>();
                node.fieldNames().forEachRemaining(fieldNameList::add);
                fieldNameList.forEach(name -> {
                    final String path = DfStringUtil.isEmpty(fieldName) ? name : fieldName + "." + name;
                    if (targetNodeLambda.test(path, name)) {
                        prepareNodeTargeting(targetNodeLambda).accept(path, node.get(name));
                    } else {
                        ((ObjectNode) node).remove(name);
                    }
                });
            }
        };
    }
}
