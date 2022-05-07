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
    public Map<String, Object> createSwaggerSpecMap(SwaggerOption swaggerOption, SwaggerPathCall swaggerPathCall) {
        // process order is order in swagger.json is here
        final Map<String, Object> specMap = DfCollectionUtil.newLinkedHashMap();
        specMap.put("swagger", "2.0");
        specMap.put("info", createSwaggerInfoMap());
        specMap.put("schemes", prepareSwaggerMapSchemes());
        specMap.put("basePath", deriveBasePath(swaggerOption));

        final List<Map<String, Object>> tagsList = DfCollectionUtil.newArrayList();
        specMap.put("tags", tagsList);

        // security has no constraint of order but should be before paths for swagger.json view
        swaggerOption.getSecurityDefinitionList().ifPresent(securityDefinitionList -> {
            adaptSecurityDefinitions(specMap, securityDefinitionList);
        });

        //  "paths": {
        //    "/root/": {
        final Map<String, Map<String, Object>> pathsMap = DfCollectionUtil.newLinkedHashMap();
        specMap.put("paths", pathsMap);

        //   "definitions": {
        //     "org.docksidestage.app.web.signin.SigninBody": {
        final Map<String, Map<String, Object>> definitionsMap = DfCollectionUtil.newLinkedHashMap();
        specMap.put("definitions", definitionsMap);

        // #hope jflute not map, handle them as object (2021/06/21)
        swaggerPathCall.callback(pathsMap, definitionsMap, tagsList);

        // header is under paths so MUST be after paths setup
        swaggerOption.getHeaderParameterList().ifPresent(headerParameterList -> {
            adaptHeaderParameters(specMap, headerParameterList); // needs paths in swaggerMap
        });
        return specMap;
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
        swaggerInfoMap.put("description", deriveSwaggerInfoDescription(title));
        swaggerInfoMap.put("version", deriveSwaggerInfoVersion());
        return swaggerInfoMap;
    }

    protected String findSwaggerInfoTitle() {
        // application always defines in [app]_config.properties
        final String domainTitle = accessibleConfig.get("domain.title");
        final String domainName = accessibleConfig.get("domain.name");
        return Objects.toString(domainTitle, domainName);
    }

    protected String deriveSwaggerInfoDescription(String title) {
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

    protected String deriveSwaggerInfoVersion() {
        return "1.0.0";
    }

    protected List<String> prepareSwaggerMapSchemes() {
        return Arrays.asList(currentRequest.getScheme());
    }

    protected String deriveBasePath(SwaggerOption swaggerOption) {
        final StringBuilder basePathSb = new StringBuilder();
        basePathSb.append(currentRequest.getContextPath() + "/"); // e.g. /showbase/
        swaggerOption.getApplicationVersionOnUrl().ifPresent(supplier -> {
            basePathSb.append(supplier.get() + "/"); // e.g. /showbase/v1/
        });
        final String currentPath = basePathSb.toString();
        return swaggerOption.getDerivedBasePath().map(derivedBasePath -> {
            return derivedBasePath.apply(currentPath); // filtered by application
        }).orElse(currentPath);
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
                final String key = "parameters";
                if (!pathDataMap.containsKey(key)) {
                    pathDataMap.put(key, DfCollectionUtil.newArrayList());
                }
                final Object parameters = pathDataMap.get(key);
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
