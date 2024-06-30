/*
 * Copyright 2015-2024 the original author or authors.
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
        final List<TypeDocMeta> nestTypeDocMetaList = actionDocMeta.getFormTypeDocMeta().getNestTypeDocMetaList();
        nestTypeDocMetaList.stream()
                .map(typeDocMeta -> setupFormParameter(httpMethod, typeDocMeta))
                .forEach(parameterMap -> parameterMapList.add(parameterMap));

        // should be after adjustment of parameterMapList to use form information for determination
        setupConsumesIfFormData(httpMethodContentMap, parameterMapList);
    }

    // ===================================================================================
    //                                                                      Form Parameter
    //                                                                      ==============
    protected Map<String, Object> setupFormParameter(String httpMethod, TypeDocMeta typeDocMeta) {
        //       {
        //         "name": "account",
        //         "type": "string",
        //         "in": "formData"
        //       },
        // basic attributes are set up here
        final Map<String, Object> parameterMap = parameterMapProvider.apply(typeDocMeta);

        // override and adjust them as form parameter
        adjustDollarRef(httpMethod, parameterMap);
        adjustItems(httpMethod, parameterMap);
        adjustObjectType(httpMethod, parameterMap);
        adjustParameterName(httpMethod, parameterMap, typeDocMeta);
        adjustIn(httpMethod, parameterMap);
        adjustExample(httpMethod, parameterMap);
        adjustRequired(httpMethod, parameterMap, typeDocMeta);
        adjustCollectionFormat(httpMethod, parameterMap);

        return parameterMap;
    }

    protected void adjustDollarRef(String httpMethod, Map<String, Object> parameterMap) {
        // #for_now p1us2er0 mapping to string until analysis is correct (2018/10/03)
        // #thinking jflute certainly rough logic but no problem? (2022/04/21)
        if (parameterMap.containsKey("$ref")) {
            parameterMap.remove("$ref");
            parameterMap.put("type", "string");
        }
    }

    protected void adjustItems(String httpMethod, Map<String, Object> parameterMap) {
        if (parameterMap.containsKey("items")) {
            @SuppressWarnings("unchecked")
            final Map<String, Object> items = (Map<String, Object>) parameterMap.get("items");
            adjustDollarRef(httpMethod, items);
            adjustObjectType(httpMethod, items);
        }
    }

    protected void adjustObjectType(String httpMethod, Map<String, Object> parameterMap) {
        if ("object".equals(parameterMap.get("type"))) {
            parameterMap.put("type", "string");
        }
    }

    protected void adjustParameterName(String httpMethod, Map<String, Object> parameterMap, TypeDocMeta typeDocMeta) {
        parameterMap.put("name", typeDocMeta.getPublicName());
    }

    protected void adjustIn(String httpMethod, Map<String, Object> parameterMap) {
        parameterMap.put("in", isQueryParameter(httpMethod) ? "query" : "formData");
    }

    protected boolean isQueryParameter(String httpMethod) {
        return Arrays.asList("get", "delete").contains(httpMethod);
    }

    protected void adjustExample(String httpMethod, Map<String, Object> parameterMap) {
        if (parameterMap.containsKey("example")) {
            parameterMap.put("default", parameterMap.get("example"));
            parameterMap.remove("example");
        }
    }

    protected void adjustRequired(String httpMethod, Map<String, Object> parameterMap, TypeDocMeta typeDocMeta) {
        parameterMap.put("required", typeDocMeta.getAnnotationTypeList().stream().anyMatch(annoType -> {
            return annotationHandler.getRequiredAnnotationList()
                    .stream()
                    .anyMatch(requiredAnno -> requiredAnno.isAssignableFrom(annoType.getClass()));
        }));
    }

    protected void adjustCollectionFormat(String httpMethod, Map<String, Object> parameterMap) {
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // as Array and Multi-Value Parameters specification of Swagger-2.0
        // https://swagger.io/docs/specification/2-0/describing-parameters/
        // collectionFormat default is csv, which is unmatched with LastaFlute form mapping logic
        // so explicitly set it up here by jflute (2022/02/03)
        // _/_/_/_/_/_/_/_/_/_/
        if (needsCollectionFormatMulti(parameterMap)) {
            parameterMap.put("collectionFormat", "multi");
        }
    }

    protected boolean needsCollectionFormatMulti(Map<String, Object> parameterMap) {
        // "multi" is only supported for the following inputs as Swagger specification
        // (and also that is enough for LastaFlute form mapping)
        return isParameterInputQuery(parameterMap) || isParameterInputFormData(parameterMap);
    }

    // ===================================================================================
    //                                                                            Consumes
    //                                                                            ========
    protected void setupConsumesIfFormData(Map<String, Object> httpMethodContentMap, List<Map<String, Object>> parameterMapList) {
        if (hasFormDataInput(parameterMapList)) {
            if (hasFileTypeParameter(parameterMapList)) {
                adjustConsumesAsMultipart(httpMethodContentMap);
            } else {
                adjustConsumesAsFormUrlencoded(httpMethodContentMap);
            }
        }
    }

    protected boolean hasFormDataInput(List<Map<String, Object>> parameterMapList) {
        return parameterMapList.stream().anyMatch(parameterMap -> isParameterInputFormData(parameterMap));
    }

    protected boolean hasFileTypeParameter(List<Map<String, Object>> parameterMapList) {
        return parameterMapList.stream().anyMatch(parameterMap -> isParameterTypeFile(parameterMap));
    }

    protected void adjustConsumesAsMultipart(Map<String, Object> httpMethodContentMap) {
        httpMethodContentMap.put("consumes", Arrays.asList("multipart/form-data"));
    }

    protected void adjustConsumesAsFormUrlencoded(Map<String, Object> httpMethodContentMap) {
        httpMethodContentMap.put("consumes", Arrays.asList("application/x-www-form-urlencoded"));
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean isParameterInputFormData(Map<String, Object> parameterMap) {
        return "formData".equals(parameterMap.get("in"));
    }

    protected boolean isParameterInputQuery(Map<String, Object> parameterMap) {
        return "query".equals(parameterMap.get("in"));
    }

    protected boolean isParameterTypeFile(Map<String, Object> parameterMap) {
        return "file".equals(parameterMap.get("type"));
    }
}
