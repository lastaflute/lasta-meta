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
package org.lastaflute.meta;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.core.util.Lato;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.reference.ActionDocReference;
import org.lastaflute.web.api.BusinessFailureMapping;

/**
 * The option for swagger process e.g. SwaggerGenerator.
 * @author p1us2er0
 * @author jflute
 */
public class SwaggerOption {

    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
    // #for_now jflute derivedXxx() means deriveXxx() but keep compatible and uniformed (2024/02/22)
    // #hope jflute use ActionDocReference (read-only interface) instead of ActionDocMeta (2024/02/22)
    // _/_/_/_/_/_/_/_/_/_/

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    protected Function<String, String> basePathLambda; // null allowed
    protected Supplier<String> applicationVersionOnUrlLambda; // null allowed

    // -----------------------------------------------------
    //                                            Meta Infra
    //                                            ----------
    protected Consumer<List<String>> additionalSourceDirectoriesLambda; // null allowed

    // -----------------------------------------------------
    //                                       Action Handling
    //                                       ---------------
    protected Predicate<ActionDocMeta> targetActionDocMetaLambda; // null allowed

    // -----------------------------------------------------
    //                                         HTTP Handling
    //                                         -------------
    protected Function<ActionDocMeta, String> defaultFormHttpMethodLambda; // null allowed
    protected Function<ActionDocMeta, Integer> successHttpStatusLambda; // null allowed
    protected Function<ActionDocMeta, SwaggerFailureHttpStatusResource> failureHttpStatusLambda; // null allowed

    // -----------------------------------------------------
    //                                             Path Item
    //                                             ---------
    protected SwaggerPathSummaryDeriver pathSummaryDeriver; // null allowed
    protected SwaggerPathDescriptionDeriver pathDescriptionDeriver; // null allowed

    // -----------------------------------------------------
    //                                                Header
    //                                                ------
    protected List<Map<String, Object>> headerParameterList; // null allowed, lazy-loaded

    // -----------------------------------------------------
    //                                              Security
    //                                              --------
    protected List<Map<String, Object>> securityDefinitionList; // null allowed, lazy-loaded

    // ===================================================================================
    //                                                                               Basic
    //                                                                               =====
    /**
     * Derive application base path (e.g. /showbase/) by filter.
     * <pre>
     * op.derivedBasePath(basePath -&gt; basePath + "api/");
     * </pre>
     * @param oneArgLambda The callback of base path filter. (NotNull)
     */
    public void derivedBasePath(Function<String, String> oneArgLambda) {
        this.basePathLambda = oneArgLambda;
    }

    /**
     * Supply your application version on URL. e.g. /showbase/v1/
     * <pre>
     * op.supplyApplicationVersionOnUrl(() -&gt; "v1");
     * </pre>
     * @param noArgLambda The callback to supply your application version on URL. (NotNull)
     */
    public void supplyApplicationVersionOnUrl(Supplier<String> noArgLambda) {
        this.applicationVersionOnUrlLambda = noArgLambda;
    }

    // ===================================================================================
    //                                                                          Meta Infra
    //                                                                          ==========
    // -----------------------------------------------------
    //                                      Source Directory
    //                                      ----------------
    /**
     * Register additional source directories to parse for swagger. <br>
     * Basically the path is supposed to be relative from your project root.
     * <pre>
     * op.registerAdditionalSourceDirectory(dirList -&gt; dirList.add("../../yourlib/src/main/java/"));
     * </pre>
     * @param oneArgLambda The callback to add your source directories to managed list. (NotNull)
     */
    public void registerAdditionalSourceDirectory(Consumer<List<String>> oneArgLambda) {
        this.additionalSourceDirectoriesLambda = oneArgLambda;
    }

    // ===================================================================================
    //                                                                     Action Handling
    //                                                                     ===============
    // -----------------------------------------------------
    //                                         Action Target
    //                                         -------------
    /**
     * Derive action execute that can be target for swagger-spec by filter.
     * <pre>
     * op.derivedTargetActionDocMeta(meta -&gt; {
     *     return ...; // true if the action execute is target
     * });
     * </pre>
     * @param oneArgLambda The callback of target to determine it. (NotNull)
     */
    public void derivedTargetActionDocMeta(Predicate<ActionDocMeta> oneArgLambda) {
        this.targetActionDocMetaLambda = oneArgLambda;
    }

    // ===================================================================================
    //                                                                       HTTP Handling
    //                                                                       =============
    // -----------------------------------------------------
    //                                      Form HTTP Method
    //                                      ----------------
    /**
     * Derive form http method by callback. <br>
     * In the following cases, the following judgment has priority. <br>
     * <ul>
     * <li>methods that use the HTTP method restriction style (get$index, post$index).</li>
     * <li>Request is body (will be post).</li>
     * </ul>
     * @param oneArgLambda The callback of http method deriving. (NotNull)
     */
    public void derivedDefaultFormHttpMethod(Function<ActionDocMeta, String> oneArgLambda) {
        this.defaultFormHttpMethodLambda = oneArgLambda;
    }

    // -----------------------------------------------------
    //                                           HTTP Status
    //                                           -----------
    /**
     * Derive success HTTP status for the execute method by filter.
     * <pre>
     * op.derivedSuccessHttpStatus(meta -&gt; {
     *     return ...; // e.g. 200 or 201 or 204
     * });
     * </pre>
     * @param oneArgLambda The callback of HTTP status deriving, returning null means no filter. (NotNull)
     */
    public void derivedSuccessHttpStatus(Function<ActionDocMeta, Integer> oneArgLambda) {
        this.successHttpStatusLambda = oneArgLambda;
    }

    /**
     * Derive success HTTP status for the execute method by filter.
     * <pre>
     * op.derivedFailureHttpStatus(meta -&gt; {
     *     SwaggerFailureHttpStatusResource resource = new SwaggerFailureHttpStatusResource();
     *     resource.addMapping(404, EntityAlreadyDeletedException.class);
     *     return resource;
     * });
     * </pre>
     * @param oneArgLambda The callback of HTTP status deriving, returning null means no filter. (NotNull)
     */
    public void derivedFailureHttpStatus(Function<ActionDocMeta, SwaggerFailureHttpStatusResource> oneArgLambda) {
        this.failureHttpStatusLambda = oneArgLambda;
    }

    /**
     * @author jflute
     */
    public static class SwaggerFailureHttpStatusResource {

        protected final Map<Integer, List<Class<?>>> failureStatusCauseMap = DfCollectionUtil.newLinkedHashMap();

        public void addMapping(int failureStatus, Class<?> causeType) {
            List<Class<?>> existingList = failureStatusCauseMap.get(failureStatus);
            if (existingList == null) {
                existingList = DfCollectionUtil.newArrayList();
                failureStatusCauseMap.put(failureStatus, existingList);
            }
            existingList.add(causeType);
        }

        public void acceptFailureStatusMap(BusinessFailureMapping<Integer> failureMapping) { // for e.g. faicli pattern
            final Map<Class<?>, Integer> failureMap = failureMapping.getFailureMap();
            failureMap.forEach((causeType, httpStatus) -> { // translate
                addMapping(httpStatus, causeType);
            });
        }

        public Map<Integer, List<Class<?>>> getFailureStatusCauseMap() {
            return Collections.unmodifiableMap(failureStatusCauseMap);
        }
    }

    // ===================================================================================
    //                                                                           Path Item
    //                                                                           =========
    // -----------------------------------------------------
    //                                          Path Summary
    //                                          ------------
    /**
     * Derive path summary to fit your requirement.
     * <pre>
     * op.derivePathSummary((actionDocReference, defaultSummary) -&gt; {
     *     return ... // filtering defaultSummary
     * });
     * </pre>
     * @param pathSummaryDeriver The callback to derive path summary. (NotNull)
     */
    public void derivedPathSummary(SwaggerPathSummaryDeriver pathSummaryDeriver) {
        this.pathSummaryDeriver = pathSummaryDeriver;
    }

    /**
     * @author jflute
     */
    public static interface SwaggerPathSummaryDeriver {

        /**
         * @param actionDocReference The doc reference of current action, basically read-only. (NotNull)
         * @param defaultSummary The swagger's "summary" of the action as default. (NullAllowed) 
         * @return The new derived summary by your process. (NullAllowed: if null, means no summary)
         */
        String derive(ActionDocReference actionDocReference, String defaultSummary);
    }

    // -----------------------------------------------------
    //                                      Path Description
    //                                      ----------------
    /**
     * Derive path description to fit your requirement.
     * <pre>
     * op.switchPathDescription((actionDocReference, defaultDescription) -&gt; {
     *     return ... // filtering defaultDescription
     * });
     * </pre>
     * @param pathDescriptionDeriver The callback to derive path description. (NotNull)
     */
    public void derivedPathDescription(SwaggerPathDescriptionDeriver pathDescriptionDeriver) {
        this.pathDescriptionDeriver = pathDescriptionDeriver;
    }

    /**
     * @author jflute
     */
    public static interface SwaggerPathDescriptionDeriver {

        /**
         * @param actionDocReference The doc reference of current action, basically read-only. (NotNull)
         * @param defaultDescription The swagger's "description" of the action as default. (NullAllowed) 
         * @return The new derived description by your process. (NullAllowed: if null, means no summary)
         */
        String derive(ActionDocReference actionDocReference, String defaultDescription);
    }

    // ===================================================================================
    //                                                                              Header
    //                                                                              ======
    public void addHeaderParameter(String name, String value) {
        if (headerParameterList == null) {
            headerParameterList = DfCollectionUtil.newArrayList();
        }
        headerParameterList.add(createHeaderParameterMap(name, value));
    }

    public void addHeaderParameter(String name, String value, Consumer<SwaggerHeaderParameterResource> resourceLambda) {
        final Map<String, Object> parameterMap = createHeaderParameterMap(name, value);
        resourceLambda.accept(new SwaggerHeaderParameterResource(parameterMap));
        if (headerParameterList == null) {
            headerParameterList = DfCollectionUtil.newArrayList();
        }
        headerParameterList.add(parameterMap);
    }

    protected Map<String, Object> createHeaderParameterMap(String name, String value) {
        // #hope jflute move this logic depending to swagger-spec to setupper (2021/06/25)
        final Map<String, Object> parameterMap = DfCollectionUtil.newLinkedHashMap();
        parameterMap.put("in", "header");
        parameterMap.put("type", "string");
        parameterMap.put("required", true);
        parameterMap.put("name", name);
        parameterMap.put("default", value);
        return parameterMap;
    }

    public static class SwaggerHeaderParameterResource {

        protected final Map<String, Object> headerParameterMap;

        public SwaggerHeaderParameterResource(Map<String, Object> headerParameterMap) {
            this.headerParameterMap = headerParameterMap;
        }

        public void registerAttribute(String key, Object value) {
            if (key == null) {
                throw new IllegalArgumentException("The argument 'key' should not be null.");
            }
            if (value == null) {
                throw new IllegalArgumentException("The argument 'value' should not be null.");
            }
            if (key.equalsIgnoreCase("name") || key.equalsIgnoreCase("default")) {
                throw new IllegalArgumentException("Cannot add '" + key + "' key here: " + key + ", " + value);
            }
            headerParameterMap.put(key, value);
        }
    }

    // ===================================================================================
    //                                                                            Security
    //                                                                            ========
    public void addSecurityDefinition(String name) {
        if (securityDefinitionList == null) {
            securityDefinitionList = DfCollectionUtil.newArrayList();
        }
        securityDefinitionList.add(createSecurityDefinitionMap(name));
    }

    public void addSecurityDefinition(String name, Consumer<SwaggerSecurityDefinitionResource> resourceLambda) {
        final Map<String, Object> definitionMap = createSecurityDefinitionMap(name);
        resourceLambda.accept(new SwaggerSecurityDefinitionResource(definitionMap));
        if (securityDefinitionList == null) {
            securityDefinitionList = DfCollectionUtil.newArrayList();
        }
        securityDefinitionList.add(definitionMap);
    }

    protected Map<String, Object> createSecurityDefinitionMap(String name) {
        // #hope jflute move this logic depending to swagger-spec to setupper (2021/06/25)
        final Map<String, Object> definitionMap = DfCollectionUtil.newLinkedHashMap();
        definitionMap.put("in", "header");
        definitionMap.put("type", "apiKey");
        definitionMap.put("name", name);
        return definitionMap;
    }

    public static class SwaggerSecurityDefinitionResource {

        protected final Map<String, Object> securityDefinitionMap;

        public SwaggerSecurityDefinitionResource(Map<String, Object> securityDefinitionMap) {
            this.securityDefinitionMap = securityDefinitionMap;
        }

        public void registerAttribute(String key, Object value) {
            if (key == null) {
                throw new IllegalArgumentException("The argument 'key' should not be null.");
            }
            if (value == null) {
                throw new IllegalArgumentException("The argument 'value' should not be null.");
            }
            if (key.equalsIgnoreCase("name")) {
                throw new IllegalArgumentException("Cannot add '" + key + "' key here: " + key + ", " + value);
            }
            securityDefinitionMap.put(key, value);
        }
    }

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return Lato.string(this);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    public OptionalThing<Function<String, String>> getDerivedBasePath() {
        return OptionalThing.ofNullable(basePathLambda, () -> {
            throw new IllegalStateException("Not set basePathLambda.");
        });
    }

    public OptionalThing<Supplier<String>> getApplicationVersionOnUrl() {
        return OptionalThing.ofNullable(applicationVersionOnUrlLambda, () -> {
            throw new IllegalStateException("Not set applicationVersionOnUrlLambda.");
        });
    }

    // -----------------------------------------------------
    //                                            Meta Infra
    //                                            ----------
    public OptionalThing<Consumer<List<String>>> getAdditionalSourceDirectories() {
        return OptionalThing.ofNullable(additionalSourceDirectoriesLambda, () -> {
            throw new IllegalStateException("Not set additionalSourceDirectoriesLambda.");
        });
    }

    // -----------------------------------------------------
    //                                       Action Handling
    //                                       ---------------
    public Predicate<ActionDocMeta> getTargetActionDocMeta() {
        if (targetActionDocMetaLambda == null) {
            return (actionDocMeta) -> true;
        }
        return targetActionDocMetaLambda;
    }

    // -----------------------------------------------------
    //                                         HTTP Handling
    //                                         -------------
    public Function<ActionDocMeta, String> getDefaultFormHttpMethod() {
        if (defaultFormHttpMethodLambda == null) {
            // #hope jflute move this default logic to setupper (2021/06/25)
            return meta -> "get"; // as default
        }
        return defaultFormHttpMethodLambda;
    }

    public OptionalThing<Function<ActionDocMeta, Integer>> getSuccessHttpStatusLambda() {
        return OptionalThing.ofNullable(successHttpStatusLambda, () -> {
            throw new IllegalStateException("Not set successHttpStatusLambda.");
        });
    }

    public OptionalThing<Function<ActionDocMeta, SwaggerFailureHttpStatusResource>> getFailureHttpStatusLambda() {
        return OptionalThing.ofNullable(failureHttpStatusLambda, () -> {
            throw new IllegalStateException("Not set failureHttpStatusLambda.");
        });
    }

    // -----------------------------------------------------
    //                                             Path Item
    //                                             ---------
    public OptionalThing<SwaggerPathSummaryDeriver> getPathSummaryDeriver() {
        return OptionalThing.ofNullable(pathSummaryDeriver, () -> {
            throw new IllegalStateException("Not found the pathSummaryDeriver.");
        });
    }

    public OptionalThing<SwaggerPathDescriptionDeriver> getPathDescriptionDeriver() {
        return OptionalThing.ofNullable(pathDescriptionDeriver, () -> {
            throw new IllegalStateException("Not found the pathDescriptionDeriver.");
        });
    }

    // -----------------------------------------------------
    //                                                Header
    //                                                ------
    public OptionalThing<List<Map<String, Object>>> getHeaderParameterList() {
        return OptionalThing.ofNullable(headerParameterList, () -> {
            throw new IllegalStateException("Not set headerParameterList.");
        });
    }

    // -----------------------------------------------------
    //                                              Security
    //                                              --------
    public OptionalThing<List<Map<String, Object>>> getSecurityDefinitionList() {
        return OptionalThing.ofNullable(securityDefinitionList, () -> {
            throw new IllegalStateException("Not set securityDefinitionList.");
        });
    }
}
