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
package org.lastaflute.meta;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.web.api.BusinessFailureMapping;

/**
 * @author p1us2er0
 * @author jflute
 */
public class SwaggerOption {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                                 Basic
    //                                                 -----
    protected Function<String, String> basePathLambda;

    // -----------------------------------------------------
    //                                                Action
    //                                                ------
    protected Function<ActionDocMeta, String> defaultFormHttpMethodLambda;
    protected Predicate<ActionDocMeta> targetActionDocMetaLambda;
    protected Function<ActionDocMeta, Integer> successHttpStatusLambda;
    protected Function<ActionDocMeta, SwaggerFailureHttpStatusResource> failureHttpStatusLambda;

    // -----------------------------------------------------
    //                                                Header
    //                                                ------
    protected List<Map<String, Object>> headerParameterList;

    // -----------------------------------------------------
    //                                              Security
    //                                              --------
    protected List<Map<String, Object>> securityDefinitionList;

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

    // ===================================================================================
    //                                                                              Action
    //                                                                              ======
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
    //                                                                    Header Parameter
    //                                                                    ================
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
    //                                                                 Security Definition
    //                                                                 ===================
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

    // -----------------------------------------------------
    //                                                Action
    //                                                ------
    public Function<ActionDocMeta, String> getDefaultFormHttpMethod() {
        if (defaultFormHttpMethodLambda == null) {
            // #hope jflute move this default logic to setupper (2021/06/25)
            return meta -> "get"; // as default
        }
        return defaultFormHttpMethodLambda;
    }

    public Predicate<ActionDocMeta> getTargetActionDocMeta() {
        if (targetActionDocMetaLambda == null) {
            return (actionDocMeta) -> true;
        }
        return targetActionDocMetaLambda;
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
