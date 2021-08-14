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
package org.lastaflute.meta.swagger.spec.zone.form;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.swagger.spec.parts.annotation.SwaggerSpecAnnotationHandler;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/25 Friday at roppongi japanese)
 */
public class SwaggerSpecFormSetupper {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final SwaggerSpecAnnotationHandler annotationHandler;
    protected final Function<TypeDocMeta, Map<String, Object>> parameterMapProvider; // for return type

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecFormSetupper(SwaggerSpecAnnotationHandler annotationHandler,
            Function<TypeDocMeta, Map<String, Object>> parameterMapProvider) {
        this.annotationHandler = annotationHandler;
        this.parameterMapProvider = parameterMapProvider;
    }

    // ===================================================================================
    //                                                                             Prepare
    //                                                                             =======
    public void prepareForm(ActionDocMeta actionDocMeta, String httpMethod, Map<String, Object> httpMethodContentMap,
            List<Map<String, Object>> parameterMapList) {
        //     "consumes": [
        //       "application/x-www-form-urlencoded"
        //     ],
        //     "parameters": [
        //       {
        //         "name": "account",
        //         "type": "string",
        //         "in": "formData"
        //       },
        //       ...
        //     ],
        parameterMapList.addAll(actionDocMeta.getFormTypeDocMeta().getNestTypeDocMetaList().stream().map(typeDocMeta -> {
            final Map<String, Object> parameterMap = parameterMapProvider.apply(typeDocMeta);
            // TODO p1us2er0 mapping to string until analysis is correct (2018/10/03)
            if (parameterMap.containsKey("$ref")) {
                parameterMap.remove("$ref");
                parameterMap.put("type", "string");
            }
            if (parameterMap.containsKey("items")) {
                @SuppressWarnings("unchecked")
                Map<String, Object> items = (Map<String, Object>) parameterMap.get("items");
                if (items.containsKey("$ref")) {
                    items.remove("$ref");
                    items.put("type", "string");
                }
                if ("object".equals(items.get("type"))) {
                    items.put("type", "string");
                }
            }
            if ("object".equals(parameterMap.get("type"))) {
                parameterMap.put("type", "string");
            }
            parameterMap.put("name", typeDocMeta.getPublicName());
            parameterMap.put("in", Arrays.asList("get", "delete").contains(httpMethod) ? "query" : "formData");
            if (parameterMap.containsKey("example")) {
                parameterMap.put("default", parameterMap.get("example"));
                parameterMap.remove("example");
            }
            parameterMap.put("required", typeDocMeta.getAnnotationTypeList().stream().anyMatch(annotationType -> {
                return annotationHandler.getRequiredAnnotationList()
                        .stream()
                        .anyMatch(requiredAnnotation -> requiredAnnotation.isAssignableFrom(annotationType.getClass()));
            }));
            return parameterMap;
        }).collect(Collectors.toList()));
        if (parameterMapList.stream().anyMatch(parameterMap -> "formData".equals(parameterMap.get("in")))) {
            if (parameterMapList.stream().anyMatch(parameterMap -> "file".equals(parameterMap.get("type")))) {
                httpMethodContentMap.put("consumes", Arrays.asList("multipart/form-data"));
            } else {
                httpMethodContentMap.put("consumes", Arrays.asList("application/x-www-form-urlencoded"));
            }
        }
    }
}
