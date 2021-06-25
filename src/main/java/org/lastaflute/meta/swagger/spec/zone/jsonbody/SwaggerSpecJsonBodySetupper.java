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
package org.lastaflute.meta.swagger.spec.zone.jsonbody;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.swagger.spec.parts.annotation.SwaggerSpecAnnotationHandler;
import org.lastaflute.meta.swagger.spec.parts.definition.SwaggerSpecDefinitionHandler;
import org.lastaflute.meta.swagger.spec.parts.encoding.SwaggerSpecEncodingHandler;
import org.lastaflute.meta.swagger.spec.parts.property.SwaggerSpecPropertyHandler;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/25 Friday at roppongi japanese)
 */
public class SwaggerSpecJsonBodySetupper {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final SwaggerSpecAnnotationHandler annotationHandler;
    protected final SwaggerSpecPropertyHandler propertyHandler;
    protected final SwaggerSpecDefinitionHandler definitionHandler;
    protected final SwaggerSpecEncodingHandler encodingHandler;
    protected final Function<TypeDocMeta, Map<String, Object>> parameterMapProvider; // for return type

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecJsonBodySetupper(SwaggerSpecAnnotationHandler annotationHandler, SwaggerSpecPropertyHandler propertyHandler,
            SwaggerSpecDefinitionHandler definitionHandler, SwaggerSpecEncodingHandler encodingHandler,
            Function<TypeDocMeta, Map<String, Object>> parameterMapProvider) {
        this.annotationHandler = annotationHandler;
        this.propertyHandler = propertyHandler;
        this.definitionHandler = definitionHandler;
        this.encodingHandler = encodingHandler;
        this.parameterMapProvider = parameterMapProvider;
    }

    // ===================================================================================
    //                                                                             Prepare
    //                                                                             =======
    public void prepareJsonBody(ActionDocMeta actionDocMeta, Map<String, Map<String, Object>> definitionsMap,
            Map<String, Object> httpMethodContentMap, List<Map<String, Object>> parameterMapList) {
        //     "consumes": [
        //       "application/json"
        //     ],
        httpMethodContentMap.put("consumes", Arrays.asList("application/json"));
        final Map<String, Object> parameterMap = DfCollectionUtil.newLinkedHashMap();
        parameterMap.put("name", actionDocMeta.getFormTypeDocMeta().getSimpleTypeName());
        parameterMap.put("in", "body");
        parameterMap.put("required", true);
        final Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
        schema.put("type", "object");
        final List<String> requiredPropertyNameList = propertyHandler.deriveRequiredPropertyNameList(actionDocMeta.getFormTypeDocMeta());
        if (!requiredPropertyNameList.isEmpty()) {
            schema.put("required", requiredPropertyNameList);
        }
        schema.put("properties", actionDocMeta.getFormTypeDocMeta().getNestTypeDocMetaList().stream().map(propertyDocMeta -> {
            return parameterMapProvider.apply(propertyDocMeta);
        }).collect(Collectors.toMap(key -> key.get("name"), value -> {
            final LinkedHashMap<String, Object> propertyMap = DfCollectionUtil.newLinkedHashMap(value);
            propertyMap.remove("name");
            return propertyMap;
        }, (u, v) -> v, LinkedHashMap::new)));

        // Form or Body's definition
        //   "definitions": {
        //     "org.docksidestage.app.web.signin.SigninBody": {
        registerSchemaToDefinitionsMap(actionDocMeta, definitionsMap, schema);

        //         "schema": {
        //           "$ref": "#/definitions/org.docksidestage.app.web.signin.SigninBody"
        //         }
        // or
        //         "schema": {
        //           "type": "array",
        //           "items": {
        //             "$ref": "#/definitions/org.docksidestage.app.web.wx.remogen.bean.simple.SuperSimpleBody"
        //           }
        //         }
        LinkedHashMap<String, String> schemaMap = DfCollectionUtil.newLinkedHashMap("$ref", prepareSwaggerMapRefDefinitions(actionDocMeta));
        if (!Iterable.class.isAssignableFrom(actionDocMeta.getFormTypeDocMeta().getType())) {
            parameterMap.put("schema", schemaMap);
        } else {
            parameterMap.put("schema", DfCollectionUtil.newLinkedHashMap("type", "array", "items", schemaMap));
        }
        parameterMapList.add(parameterMap);
    }

    protected void registerSchemaToDefinitionsMap(ActionDocMeta actionDocMeta, Map<String, Map<String, Object>> definitionsMap,
            Map<String, Object> schema) {
        final String definitionName = definitionHandler.deriveDefinitionName(actionDocMeta.getFormTypeDocMeta());
        definitionsMap.put(definitionName, schema);
    }

    protected String prepareSwaggerMapRefDefinitions(ActionDocMeta actiondocMeta) {
        final String definitionName = definitionHandler.deriveDefinitionName(actiondocMeta.getFormTypeDocMeta());
        return "#/definitions/" + encodingHandler.encode(definitionName);
    }
}
