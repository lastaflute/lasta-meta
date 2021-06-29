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
import java.util.function.BiPredicate;
import java.util.function.Function;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.meta.swagger.diff.render.LastaMetaMarkdownRender;
import org.openapitools.openapidiff.core.output.Render;

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
    protected boolean pathTrailingSlashIgnored;
    protected Function<String, String> leftContentFilter; // null allowed
    protected Function<String, String> rightContentFilter; // null allowed

    // -----------------------------------------------------
    //                                             Targeting
    //                                             ---------
    protected BiPredicate<String, String> targetNodeLambda = prepareDefaultTargetItem(); // not null
    // done (by jflute) awaawa hope that Jackson class is closed, wrap the JsonNode by jflute (2021/06/08)
    // moved to handler for now, not needs to be option, overriding extension is enough
    //protected BiConsumer<String, JsonNode> diffAdjustmentNodeLambda = getDefaultDiffAdjustmentNode();

    protected BiPredicate<String, String> prepareDefaultTargetItem() {
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
        if (swaggerContentCharset == null) {
            throw new IllegalArgumentException("The argument 'swaggerContentCharset' should not be null.");
        }
        this.swaggerContentCharset = swaggerContentCharset;
    }

    public void setDiffResultRender(Render diffResultRender) {
        if (diffResultRender == null) {
            throw new IllegalArgumentException("The argument 'diffResultRender' should not be null.");
        }
        this.diffResultRender = diffResultRender;
    }

    // ===================================================================================
    //                                                                          Diff Logic
    //                                                                          ==========
    public SwaggerDiffOption ignorePathTrailingSlash() { // best effort logic
        pathTrailingSlashIgnored = true;
        return this;
    }

    public SwaggerDiffOption filterLeftContent(Function<String, String> leftContentFilter) {
        if (leftContentFilter == null) {
            throw new IllegalArgumentException("The argument 'leftContentFilter' should not be null.");
        }
        this.leftContentFilter = leftContentFilter;
        return this;
    }

    public SwaggerDiffOption filterRightContent(Function<String, String> rightContentFilter) {
        if (rightContentFilter == null) {
            throw new IllegalArgumentException("The argument 'rightContentFilter' should not be null.");
        }
        this.rightContentFilter = rightContentFilter;
        return this;
    }

    // ===================================================================================
    //                                                                           Targeting
    //                                                                           =========
    public void deriveTargetNodeAnd(BiPredicate<String, String> targetNodeLambda) {
        if (targetNodeLambda == null) {
            throw new IllegalArgumentException("The argument 'targetNodeLambda' should not be null.");
        }
        this.targetNodeLambda = this.targetNodeLambda.and(targetNodeLambda);
    }

    public void switchTargetNodeDeterminer(BiPredicate<String, String> targetNodeLambda) {
        if (targetNodeLambda == null) {
            throw new IllegalArgumentException("The argument 'targetNodeLambda' should not be null.");
        }
        this.targetNodeLambda = targetNodeLambda;
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    public Charset getSwaggerContentCharset() {
        return this.swaggerContentCharset;
    }

    public Render getDiffResultRender() {
        return this.diffResultRender;
    }

    // -----------------------------------------------------
    //                                            Diff Logic
    //                                            ----------
    public boolean isPathTrailingSlashIgnored() {
        return pathTrailingSlashIgnored;
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

    // -----------------------------------------------------
    //                                             Targeting
    //                                             ---------
    public BiPredicate<String, String> getTargetNodeLambda() {
        return this.targetNodeLambda;
    }
}
