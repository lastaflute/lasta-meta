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
package org.lastaflute.meta.swagger.diff.node;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.stream.IntStream;

import org.dbflute.util.DfStringUtil;
import org.dbflute.util.Srl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerDiffOption (2021/06/29 Tuesday at roppongi japanese)
 */
public class SwaggerDiffNodeHandler {

    private static final Logger logger = LoggerFactory.getLogger(SwaggerDiffNodeHandler.class);

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

    // ===================================================================================
    //                                                                      Trailing Slash
    //                                                                      ==============
    public void filterPathTrailingSlash(JsonNode rootNode) { // precondition: the JSON is paths style format
        final JsonNode foundValue = rootNode.findValue("paths");
        if (foundValue == null || !foundValue.isObject()) { // other version?
            // 'paths' node exists in almost versions
            // however warning-continue only here just in case if not found
            logger.warn("cannot find 'paths' node in the JSON: rootNode=" + rootNode);
            return;
        }
        final ObjectNode pathsObjectNode = (ObjectNode) foundValue;
        final List<String> pathStrList = new ArrayList<>();
        pathsObjectNode.fieldNames().forEachRemaining(fieldName -> {
            pathStrList.add(fieldName);
        });
        for (String path : pathStrList) {
            // all remove/set to keep definition order in JSON just in case
            final JsonNode pathNode = pathsObjectNode.findPath(path);
            final String newPath = path.endsWith("/") ? Srl.rtrim(path, "/") : path;
            pathsObjectNode.remove(path); // by old path
            pathsObjectNode.set(newPath, pathNode); // by new path
        }
    }
}
