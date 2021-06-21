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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.core.direction.AccessibleConfig;
import org.lastaflute.meta.SwaggerOption;
import org.lastaflute.meta.infra.maven.MavenVersionFinder;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/21 Monday)
 */
public class SwaggerSpecCreator {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final AccessibleConfig accessibleConfig; // not null
    protected final HttpServletRequest currentRequest; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecCreator(AccessibleConfig accessibleConfig, HttpServletRequest currentRequest) {
        this.accessibleConfig = accessibleConfig;
        this.currentRequest = currentRequest;
    }

    // ===================================================================================
    //                                                                              Create
    //                                                                              ======
    public Map<String, Object> createSwaggerMap(SwaggerOption swaggerOption, SwaggerPathCall swaggerPathCall) {
        // process order is order in swagger.json is here
        final Map<String, Object> swaggerMap = DfCollectionUtil.newLinkedHashMap();
        swaggerMap.put("swagger", "2.0");
        swaggerMap.put("info", createSwaggerInfoMap());
        swaggerMap.put("schemes", prepareSwaggerMapSchemes());
        swaggerMap.put("basePath", derivedBasePath(swaggerOption));

        final List<Map<String, Object>> swaggerTagList = DfCollectionUtil.newArrayList();
        swaggerMap.put("tags", swaggerTagList);

        // security has no constraint of order but should be before paths for swagger.json view
        swaggerOption.getSecurityDefinitionList().ifPresent(securityDefinitionList -> {
            adaptSecurityDefinitions(swaggerMap, securityDefinitionList);
        });

        //  "paths": {
        //    "/root/": {
        final Map<String, Map<String, Object>> swaggerPathMap = DfCollectionUtil.newLinkedHashMap();
        swaggerMap.put("paths", swaggerPathMap);

        //   "definitions": {
        //     "org.docksidestage.app.web.signin.SigninBody": {
        final Map<String, Map<String, Object>> swaggerDefinitionsMap = DfCollectionUtil.newLinkedHashMap();
        swaggerMap.put("definitions", swaggerDefinitionsMap);

        // #hope jflute not map, handle them as object (2021/06/21)
        swaggerPathCall.callback(swaggerPathMap, swaggerDefinitionsMap, swaggerTagList);

        // header is under paths so MUST be after paths setup
        swaggerOption.getHeaderParameterList().ifPresent(headerParameterList -> {
            adaptHeaderParameters(swaggerMap, headerParameterList); // needs paths in swaggerMap
        });
        return swaggerMap;
    }

    public static interface SwaggerPathCall {

        void callback(Map<String, Map<String, Object>> swaggerPathMap, Map<String, Map<String, Object>> swaggerDefinitionsMap,
                List<Map<String, Object>> swaggerTagList);
    }

    // ===================================================================================
    //                                                                    Required Element
    //                                                                    ================
    protected Map<String, String> createSwaggerInfoMap() {
        final Map<String, String> swaggerInfoMap = DfCollectionUtil.newLinkedHashMap();
        final String title = findSwaggerInfoTitle();
        swaggerInfoMap.put("title", title);
        swaggerInfoMap.put("description", derivedSwaggerInfoDescription(title));
        swaggerInfoMap.put("version", derivedSwaggerInfoVersion());
        return swaggerInfoMap;
    }

    protected String findSwaggerInfoTitle() {
        // application always defines in [app]_config.properties
        final String domainTitle = accessibleConfig.get("domain.title");
        final String domainName = accessibleConfig.get("domain.name");
        return Objects.toString(domainTitle, domainName);
    }

    protected String derivedSwaggerInfoDescription(String title) {
        final StringBuilder description = new StringBuilder();
        description.append(title);
        description.append(". generated by lasta-meta");
        findLastaMetaVersion().ifPresent(version -> {
            description.append("-");
            description.append(version);
            description.append(".");
        });
        return description.toString();
    }

    protected String derivedSwaggerInfoVersion() {
        return "1.0.0";
    }

    protected List<String> prepareSwaggerMapSchemes() {
        return Arrays.asList(currentRequest.getScheme());
    }

    protected String derivedBasePath(SwaggerOption swaggerOption) {
        StringBuilder basePath = new StringBuilder();
        basePath.append(currentRequest.getContextPath() + "/");
        prepareApplicationVersion().ifPresent(applicationVersion -> {
            basePath.append(applicationVersion + "/");
        });
        return swaggerOption.getDerivedBasePath().map(derivedBasePath -> {
            return derivedBasePath.apply(basePath.toString());
        }).orElse(basePath.toString());
    }

    protected OptionalThing<String> prepareApplicationVersion() {
        return OptionalThing.empty();
    }

    // ===================================================================================
    //                                                                      Option Element
    //                                                                      ==============
    protected void adaptSecurityDefinitions(Map<String, Object> swaggerMap, List<Map<String, Object>> securityDefinitionList) {
        final Map<Object, Object> securityDefinitions = DfCollectionUtil.newLinkedHashMap();
        final Map<Object, Object> security = DfCollectionUtil.newLinkedHashMap();
        swaggerMap.put("securityDefinitions", securityDefinitions);
        swaggerMap.put("security", security);
        securityDefinitionList.forEach(securityDefinition -> {
            securityDefinitions.put(securityDefinition.get("name"), securityDefinition);
            security.put(securityDefinition.get("name"), Arrays.asList());
        });
    }

    protected void adaptHeaderParameters(Map<String, Object> swaggerMap, List<Map<String, Object>> headerParameterList) {
        if (headerParameterList.isEmpty()) {
            return;
        }
        final Object paths = swaggerMap.get("paths");
        if (!(paths instanceof Map<?, ?>)) {
            return;
        }
        @SuppressWarnings("unchecked")
        final Map<Object, Object> pathMap = (Map<Object, Object>) paths;
        pathMap.forEach((path, pathData) -> {
            if (!(pathData instanceof Map<?, ?>)) {
                return;
            }
            @SuppressWarnings("unchecked")
            final Map<Object, Object> pathDataMap = (Map<Object, Object>) pathData;

            headerParameterList.forEach(headerParameter -> {
                if (!pathDataMap.containsKey("parameters")) {
                    pathDataMap.put("parameters", DfCollectionUtil.newArrayList());
                }
                final Object parameters = pathDataMap.get("parameters");
                if (parameters instanceof List<?>) {
                    @SuppressWarnings("all")
                    final List<Object> parameterList = (List<Object>) parameters;
                    parameterList.add(headerParameter);
                }
            });
        });
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected OptionalThing<String> findLastaMetaVersion() {
        return createMavenVersionFinder().findVersion("org.lastaflute.meta", "lasta-meta");
    }

    protected MavenVersionFinder createMavenVersionFinder() {
        return new MavenVersionFinder();
    }
}
