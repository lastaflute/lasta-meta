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
package org.lastaflute.meta.swagger.diff;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.IntStream;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.meta.swagger.diff.render.LastaMetaMarkdownRender;
import org.openapitools.openapidiff.core.output.Render;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * @author p1us2er0
 * @author jflute
 */
public class SwaggerDiffOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    protected Charset swaggerContentCharset = StandardCharsets.UTF_8; // as default
    protected Render diffResultRender = newLastaMetaMarkdownRender(); // as default

    protected LastaMetaMarkdownRender newLastaMetaMarkdownRender() {
        return new LastaMetaMarkdownRender();
    }

    // -----------------------------------------------------
    //                                            Diff Logic
    //                                            ----------
    protected Function<String, String> leftContentFilter;
    protected Function<String, String> rightContentFilter;

    // -----------------------------------------------------
    //                                         Node Handling
    //                                         -------------
    protected BiConsumer<String, JsonNode> diffAdjustmentNodeLambda = getDefaultDiffAdjustmentNode();
    protected BiPredicate<String, String> targetNodeLambda = getDefaultTargetNode();

    // TODO awaawa hope that default logic is moved from here by jflute (2021/06/08)
    protected BiConsumer<String, JsonNode> getDefaultDiffAdjustmentNode() {
        return (fieldName, node) -> {
            if (node.isArray()) {
                IntStream.range(0, node.size()).forEach(index -> {
                    this.getDefaultDiffAdjustmentNode().accept(fieldName, node.get(index));
                });
            } else if (node.isObject()) {
                List<String> fieldNameList = new ArrayList<String>();
                node.fieldNames().forEachRemaining(fieldNameList::add);
                fieldNameList.forEach(name -> {
                    String path = DfStringUtil.isEmpty(fieldName) ? name : fieldName + "." + name;
                    if (this.getTargetNodeLambda().test(path, name)) {
                        this.getDefaultDiffAdjustmentNode().accept(path, node.get(name));
                    } else {
                        ((ObjectNode) node).remove(name);
                    }
                });
            }
        };
    }

    protected BiPredicate<String, String> getDefaultTargetNode() {
        return (path, name) -> {
            if (DfCollectionUtil.newArrayList("summary", "description", "examples").contains(name)) {
                return false;
            }
            if (path.matches(".+\\.responses\\.[^.]+$")) {
                return name.equals("200");
            }
            return true;
        };
    }

    // ===================================================================================
    //                                                                               Basic
    //                                                                               =====
    public void setSwaggerContentCharset(Charset swaggerContentCharset) {
        this.swaggerContentCharset = swaggerContentCharset;
    }

    public void setDiffResultRender(Render diffResultRender) {
        this.diffResultRender = diffResultRender;
    }

    // ===================================================================================
    //                                                                          Diff Logic
    //                                                                          ==========
    public SwaggerDiffOption filterLeftContent(Function<String, String> leftContentFilter) {
        this.leftContentFilter = leftContentFilter;
        return this;
    }

    public SwaggerDiffOption filterRightContent(Function<String, String> leftContentFilter) {
        this.leftContentFilter = leftContentFilter;
        return this;
    }

    // ===================================================================================
    //                                                                       Node Handling
    //                                                                       =============
    // TODO awaawa hope that Jackson class is closed, wrap the JsonNode by jflute (2021/06/08)
    public void deriveDiffAdjustmentNode(BiConsumer<String, JsonNode> diffAdjustmentNodeLambda) {
        this.diffAdjustmentNodeLambda = diffAdjustmentNodeLambda;
    }

    public void deriveTargetNode(BiPredicate<String, String> targetNodeLambda) {
        this.targetNodeLambda = targetNodeLambda;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    public Charset getSwaggerContentCharset() {
        return this.swaggerContentCharset;
    }

    public Render getDiffResultRender() {
        return this.diffResultRender;
    }

    public OptionalThing<Function<String, String>> getLeftContentFilter() {
        return OptionalThing.ofNullable(leftContentFilter, () -> {
            throw new IllegalStateException("Not found the leftContentFilter.");
        });
    }

    public OptionalThing<Function<String, String>> getRightContentFilter() {
        return OptionalThing.ofNullable(rightContentFilter, () -> {
            throw new IllegalStateException("Not found the rightContentFilter.");
        });
    }

    public BiConsumer<String, JsonNode> getDiffAdjustmentNode() {
        return this.diffAdjustmentNodeLambda;
    }

    public BiPredicate<String, String> getTargetNodeLambda() {
        return this.targetNodeLambda;
    }
}
