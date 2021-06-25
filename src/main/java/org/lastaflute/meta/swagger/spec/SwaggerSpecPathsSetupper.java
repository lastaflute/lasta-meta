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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.validation.constraints.Size;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;
import org.hibernate.validator.constraints.Length;
import org.lastaflute.core.json.control.JsonControlMeta;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.di.helper.misc.ParameterizedRef;
import org.lastaflute.meta.SwaggerOption;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.swagger.spec.parts.annotation.SwaggerSpecAnnotationHandler;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataType;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataTypeHandler;
import org.lastaflute.meta.swagger.spec.parts.defaultvalue.SwaggerSpecDefaultValueHandler;
import org.lastaflute.meta.swagger.spec.parts.definition.SwaggerSpecDefinitionHandler;
import org.lastaflute.meta.swagger.spec.parts.encoding.SwaggerSpecEncodingHandler;
import org.lastaflute.meta.swagger.spec.parts.enumtype.SwaggerSpecEnumHandler;
import org.lastaflute.meta.swagger.spec.parts.httpmethod.SwaggerSpecHttpMethodHandler;
import org.lastaflute.meta.swagger.spec.parts.produces.SwaggerSpecProducesHandler;
import org.lastaflute.meta.swagger.spec.parts.property.SwaggerSpecPropertyHandler;
import org.lastaflute.meta.swagger.spec.zone.form.SwaggerSpecFormSetupper;
import org.lastaflute.meta.swagger.spec.zone.jsonbody.SwaggerSpecJsonBodySetupper;
import org.lastaflute.meta.swagger.spec.zone.responses.SwaggerSpecResponsesSetupper;
import org.lastaflute.web.api.JsonParameter;
import org.lastaflute.web.response.ActionResponse;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/23 Wednesday at roppongi japanese)
 */
public class SwaggerSpecPathsSetupper {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                        Mutable Output
    //                                        --------------
    // mutable, regsitered in this class, these are output of this class
    protected final Map<String, Map<String, Object>> pathsMap;
    protected final Map<String, Map<String, Object>> definitionsMap;
    protected final List<Map<String, Object>> tagsList;

    // -----------------------------------------------------
    //                                    Resource for Setup
    //                                    ------------------
    protected final SwaggerOption swaggerOption; // not null
    protected final RealJsonEngine swaggeruseJsonEngine; // for swagger process, not null
    protected final JsonControlMeta appJsonControlMeta; // from application, not null
    protected final List<Class<?>> nativeDataTypeList; // standard native types of e.g. parameter, not null, not empty

    // -----------------------------------------------------
    //                                         Parts Handler
    //                                         -------------
    protected final SwaggerSpecAnnotationHandler annotationHandler;
    protected final SwaggerSpecEnumHandler enumHandler;
    protected final SwaggerSpecDataTypeHandler dataTypeHandler;
    protected final SwaggerSpecDefaultValueHandler defaultValueHandler;
    protected final SwaggerSpecHttpMethodHandler httpMethodHandler;
    protected final SwaggerSpecPropertyHandler propertyHandler;

    protected final SwaggerSpecDefinitionHandler definitionHandler;
    protected final SwaggerSpecEncodingHandler encodingHandler;
    protected final SwaggerSpecProducesHandler producesHandler;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecPathsSetupper(SwaggerSpecPathsMutableOutput pathMutableOutput, SwaggerOption swaggerOption,
            RealJsonEngine swaggeruseJsonEngine, JsonControlMeta appJsonControlMeta, List<Class<?>> nativeDataTypeList) {
        // #hope jflute keep pathMutableOutput and handle them by output object (2021/06/23)
        this.pathsMap = pathMutableOutput.getPathsMap();
        this.definitionsMap = pathMutableOutput.getDefinitionsMap();
        this.tagsList = pathMutableOutput.getTagsList();

        this.swaggerOption = swaggerOption;
        this.swaggeruseJsonEngine = swaggeruseJsonEngine;
        this.appJsonControlMeta = appJsonControlMeta;
        this.nativeDataTypeList = nativeDataTypeList;

        this.annotationHandler = newSwaggerSpecAnnotationHandler();
        this.enumHandler = newSwaggerSpecEnumHandler();
        this.dataTypeHandler = newSwaggerSpecDataTypeHandler(appJsonControlMeta);
        this.defaultValueHandler = newSwaggerSpecDefaultValueHandler(dataTypeHandler, enumHandler);
        this.httpMethodHandler = newSwaggerSpecHttpMethodHandler(swaggerOption);
        this.propertyHandler = newSwaggerSpecPropertyHandler(annotationHandler);

        this.definitionHandler = newSwaggerSpecDefinitionHandler();
        this.encodingHandler = newSwaggerSpecEncodingHandler();
        this.producesHandler = newSwaggerSpecProducesHandler(dataTypeHandler);
    }

    protected SwaggerSpecAnnotationHandler newSwaggerSpecAnnotationHandler() {
        return new SwaggerSpecAnnotationHandler();
    }

    protected SwaggerSpecEnumHandler newSwaggerSpecEnumHandler() {
        return new SwaggerSpecEnumHandler();
    }

    protected SwaggerSpecDataTypeHandler newSwaggerSpecDataTypeHandler(JsonControlMeta appJsonControlMeta) {
        return new SwaggerSpecDataTypeHandler(appJsonControlMeta);
    }

    protected SwaggerSpecDefaultValueHandler newSwaggerSpecDefaultValueHandler(SwaggerSpecDataTypeHandler dataTypeHandler,
            SwaggerSpecEnumHandler enumHandler) {
        return new SwaggerSpecDefaultValueHandler(dataTypeHandler, enumHandler);
    }

    protected SwaggerSpecHttpMethodHandler newSwaggerSpecHttpMethodHandler(SwaggerOption swaggerOption) {
        return new SwaggerSpecHttpMethodHandler(swaggerOption);
    }

    protected SwaggerSpecPropertyHandler newSwaggerSpecPropertyHandler(SwaggerSpecAnnotationHandler annotationHandler) {
        return new SwaggerSpecPropertyHandler(annotationHandler);
    }

    protected SwaggerSpecDefinitionHandler newSwaggerSpecDefinitionHandler() {
        return new SwaggerSpecDefinitionHandler();
    }

    protected SwaggerSpecEncodingHandler newSwaggerSpecEncodingHandler() {
        return new SwaggerSpecEncodingHandler();
    }

    protected SwaggerSpecProducesHandler newSwaggerSpecProducesHandler(SwaggerSpecDataTypeHandler dataTypeHandler) {
        return new SwaggerSpecProducesHandler(dataTypeHandler);
    }

    // ===================================================================================
    //                                                                         Set up Path 
    //                                                                         ===========
    // e.g. HTML
    // "/signin/signin": {
    //   "post": {
    //     "summary": "@author jflute",
    //     "description": "@author jflute",
    //     "consumes": [
    //       "application/x-www-form-urlencoded"
    //     ],
    //     "parameters": [
    //       {
    //         "name": "account",
    //         "type": "string",
    //         "in": "formData"
    //       },
    //       {
    //         "name": "password",
    //         "type": "string",
    //         "in": "formData"
    //       },
    //       {
    //         "name": "rememberMe",
    //         "type": "boolean",
    //         "in": "formData"
    //       }
    //     ],
    //     "tags": [
    //       "signin"
    //     ],
    //     "responses": {
    //       "200": {
    //         "description": "success",
    //         "schema": {
    //           "type": "object"
    //         }
    //       },
    //       "400": {
    //         "description": "client error"
    //       }
    //     },
    //     "produces": [
    //       "text/html"
    //     ]
    //   },
    //   "parameters": [
    //     {
    //       "in": "header",
    //       "type": "string",
    //       "required": true,
    //       "name": "hangar",
    //       "default": "mystic"
    //     }
    //   ]
    // },
    //
    // e.g. JSON
    // "/signin/": {
    //   "post": {
    //     "summary": "@author jflute",
    //     "description": "@author jflute",
    //     "consumes": [
    //       "application/json"
    //     ],
    //     "parameters": [
    //       {
    //         "name": "SigninBody",
    //         "in": "body",
    //         "required": true,
    //         "schema": {
    //           "$ref": "#/definitions/org.docksidestage.app.web.signin.SigninBody"
    //         }
    //       }
    //     ],
    //     "tags": [
    //       "signin"
    //     ],
    //     "responses": {
    //       "200": {
    //         "description": "success",
    //         "schema": {
    //           "$ref": "#/definitions/org.docksidestage.app.web.signin.SigninResult"
    //         }
    //       },
    //       "400": {
    //         "description": "client error"
    //       }
    //     },
    //     "produces": [
    //       "application/json"
    //     ]
    //   }
    // },
    //
    public void setupSwaggerPathsMap(List<ActionDocMeta> actionDocMetaList) { // top-level tags
        // output this process is registration of mutable attributes
        actionDocMetaList.stream().forEach(actionDocMeta -> {
            doSetupSwaggerPathsMap(actionDocMeta);
        });
    }

    protected void doSetupSwaggerPathsMap(ActionDocMeta actionDocMeta) {
        final String actionUrl = actionDocMeta.getUrl();

        // arrange swaggerUrlMap in swaggerPathMap if needs
        if (!pathsMap.containsKey(actionUrl)) { // first action for the URL
            final Map<String, Object> swaggerUrlMap = DfCollectionUtil.newLinkedHashMap();
            pathsMap.put(actionUrl, swaggerUrlMap);
        }

        // "/signin/": {
        //   "post": {
        final String httpMethod = httpMethodHandler.extractHttpMethod(actionDocMeta);
        final Map<String, Object> httpMethodContentMap = DfCollectionUtil.newLinkedHashMap();
        pathsMap.get(actionUrl).put(httpMethod, httpMethodContentMap);

        //     "summary": "@author jflute",
        //     "description": "@author jflute",
        httpMethodContentMap.put("summary", actionDocMeta.getDescription());
        httpMethodContentMap.put("description", actionDocMeta.getDescription());

        //     "parameters": [
        //       {
        //         "name": "SigninBody",
        //         "in": "body",
        //         "required": true,
        //         "schema": {
        //           "$ref": "#/definitions/org.docksidestage.app.web.signin.SigninBody"
        //         }
        //       }
        //     ],
        final List<Map<String, Object>> parameterMapList = DfCollectionUtil.newArrayList();
        final List<String> optionalPathNameList = DfCollectionUtil.newArrayList();
        parameterMapList.addAll(actionDocMeta.getParameterTypeDocMetaList().stream().map(typeDocMeta -> {
            final Map<String, Object> parameterMap = toParameterMap(typeDocMeta);
            parameterMap.put("in", "path");
            if (parameterMap.containsKey("example")) {
                parameterMap.put("default", parameterMap.get("example"));
                parameterMap.remove("example");
            }
            // p1us2er0 Swagger path parameters are always required. (2017/10/12)
            // If path parameter is Option, define Path separately.
            // https://stackoverflow.com/questions/45549663/swagger-schema-error-should-not-have-additional-properties
            parameterMap.put("required", true);
            if (OptionalThing.class.isAssignableFrom(typeDocMeta.getType())) {
                optionalPathNameList.add(typeDocMeta.getPublicName());
            }
            return parameterMap;
        }).collect(Collectors.toList()));

        if (actionDocMeta.getFormTypeDocMeta() != null) {
            if (actionDocMeta.getFormTypeDocMeta().getTypeName().endsWith("Form")) {
                //     "consumes": [
                //       "application/x-www-form-urlencoded"
                //     ],
                //     "parameters": [
                //       ...
                //     ],
                prepareForm(actionDocMeta, httpMethod, httpMethodContentMap, parameterMapList);
            } else {
                //     "consumes": [
                //       "application/json"
                //     ],
                prepareJsonBody(actionDocMeta, httpMethodContentMap, parameterMapList);
            }
        }
        // Query, Header, Body, Form
        httpMethodContentMap.put("parameters", parameterMapList);

        //     "tags": [
        //       "signin"
        //     ],
        httpMethodContentMap.put("tags", prepareSwaggerMapTags(actionDocMeta));
        final String tag = DfStringUtil.substringFirstFront(actionUrl.replaceAll("^/", ""), "/");

        // reflect the tags to top-level tags
        if (tagsList.stream().noneMatch(swaggerTag -> swaggerTag.containsValue(tag))) {
            tagsList.add(DfCollectionUtil.newLinkedHashMap("name", tag));
        }

        //     "responses": {
        //       ...
        prepareResponses(httpMethodContentMap, actionDocMeta);

        if (!optionalPathNameList.isEmpty()) {
            doSetupSwaggerPathsMapForOptionalPath(actionDocMeta, optionalPathNameList);
        }
    }

    // -----------------------------------------------------
    //                                                 Form
    //                                                ------
    protected void prepareForm(ActionDocMeta actionDocMeta, String httpMethod, Map<String, Object> httpMethodContentMap,
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
        final SwaggerSpecFormSetupper formSetupper = new SwaggerSpecFormSetupper(annotationHandler, meta -> toParameterMap(meta));
        formSetupper.prepareForm(actionDocMeta, httpMethod, httpMethodContentMap, parameterMapList);
    }

    // -----------------------------------------------------
    //                                             JSON Body
    //                                             ---------
    protected void prepareJsonBody(ActionDocMeta actionDocMeta, Map<String, Object> httpMethodContentMap,
            List<Map<String, Object>> parameterMapList) {
        //     "consumes": [
        //       "application/json"
        //     ],
        //     ...
        //     "definitions": {
        //     ...
        final SwaggerSpecJsonBodySetupper jsonBodySetupper = new SwaggerSpecJsonBodySetupper(annotationHandler, propertyHandler,
                definitionHandler, encodingHandler, meta -> toParameterMap(meta));
        jsonBodySetupper.prepareJsonBody(actionDocMeta, definitionsMap, httpMethodContentMap, parameterMapList);
    }

    // -----------------------------------------------------
    //                                                 Tags
    //                                                ------
    protected List<String> prepareSwaggerMapTags(ActionDocMeta actiondocMeta) {
        return Arrays.asList(DfStringUtil.substringFirstFront(actiondocMeta.getUrl().replaceAll("^/", ""), "/"));
    }

    // -----------------------------------------------------
    //                                     for Optional Path
    //                                     -----------------
    protected void doSetupSwaggerPathsMapForOptionalPath(ActionDocMeta actionDocMeta, List<String> optionalPathNameList) {
        final String actionUrl = actionDocMeta.getUrl();
        final String httpMethod = httpMethodHandler.extractHttpMethod(actionDocMeta);
        final String json = swaggeruseJsonEngine.toJson(pathsMap.get(actionUrl).get(httpMethod));

        IntStream.range(0, optionalPathNameList.size()).forEach(index -> {
            List<String> deleteOptionalPathNameList = optionalPathNameList.subList(index, optionalPathNameList.size());
            String deleteOptionalPathNameUrl = deleteOptionalPathNameList.stream().reduce(actionUrl, (aactionUrl, optionalPathName) -> {
                return aactionUrl.replaceAll("/\\{" + optionalPathName + "\\}", "");
            });
            // arrange swaggerUrlMap in swaggerPathMap if needs
            if (!pathsMap.containsKey(deleteOptionalPathNameUrl)) { // first action for the URL
                pathsMap.put(deleteOptionalPathNameUrl, DfCollectionUtil.newLinkedHashMap());
            }
            Map<String, Object> swaggerHttpMethodMap =
                    swaggeruseJsonEngine.fromJsonParameteried(json, new ParameterizedRef<Map<String, Object>>() {
                    }.getType());
            @SuppressWarnings("unchecked")
            final List<Map<String, Object>> parameterMapList =
                    ((List<Map<String, Object>>) swaggerHttpMethodMap.get("parameters")).stream().filter(parameter -> {
                        return !deleteOptionalPathNameList.contains(parameter.get("name"));
                    }).collect(Collectors.toList());
            swaggerHttpMethodMap.put("parameters", parameterMapList);
            pathsMap.get(deleteOptionalPathNameUrl).put(httpMethod, swaggerHttpMethodMap);
        });
        final Map<String, Object> swaggerUrlMap = pathsMap.remove(actionUrl);
        pathsMap.put(actionUrl, swaggerUrlMap);
    }

    // -----------------------------------------------------
    //                                             Responses
    //                                             ---------
    protected void prepareResponses(Map<String, Object> swaggerHttpMethodMap, ActionDocMeta actionDocMeta) {
        //     "responses": {
        //       "200": {
        //         "description": "success",
        //         "schema": {
        //           "$ref": "#/definitions/org.docksidestage.app.web.signin.SigninResult"
        //         }
        //       },
        //       "400": {
        //         "description": "client error"
        //       }
        //     },
        final SwaggerSpecResponsesSetupper responsesSetupper = new SwaggerSpecResponsesSetupper(swaggerOption, producesHandler, meta -> {
            return toParameterMap(meta);
        });
        responsesSetupper.prepareResponses(swaggerHttpMethodMap, actionDocMeta);
    }

    // ===================================================================================
    //                                                                       Parameter Map
    //                                                                       =============
    // #hope jflute split this to zone (2021/06/25)
    protected Map<String, Object> toParameterMap(TypeDocMeta typeDocMeta) {
        // #hope jflute should be cached? because of many calls (2021/06/23)
        final Map<Class<?>, SwaggerSpecDataType> typeMap = dataTypeHandler.createSwaggerDataTypeMap();
        final Class<?> keepType = typeDocMeta.getType();
        if (typeDocMeta.getGenericType() != null && (ActionResponse.class.isAssignableFrom(typeDocMeta.getType())
                || OptionalThing.class.isAssignableFrom(typeDocMeta.getType()))) {
            typeDocMeta.setType(typeDocMeta.getGenericType());
        }

        final Map<String, Object> parameterMap = DfCollectionUtil.newLinkedHashMap();
        parameterMap.put("name", typeDocMeta.getPublicName());
        if (DfStringUtil.is_NotNull_and_NotEmpty(typeDocMeta.getDescription())) {
            parameterMap.put("description", typeDocMeta.getDescription());
        }
        if (typeMap.containsKey(typeDocMeta.getType())) {
            final SwaggerSpecDataType swaggerType = typeMap.get(typeDocMeta.getType());
            parameterMap.put("type", swaggerType.type);
            final String format = swaggerType.format;
            if (DfStringUtil.is_NotNull_and_NotEmpty(format)) {
                parameterMap.put("format", format);
            }
        } else if (typeDocMeta.getAnnotationTypeList().stream().anyMatch(annotationType -> {
            return JsonParameter.class.isAssignableFrom(annotationType.getClass());
        })) {
            parameterMap.put("type", "string");
            // TODO p1us2er0 set description and example. (2018/09/30)
        } else if (Iterable.class.isAssignableFrom(typeDocMeta.getType())) {
            setupBeanList(typeDocMeta, typeMap, parameterMap);
        } else if (typeDocMeta.getType().equals(Object.class) || Map.class.isAssignableFrom(typeDocMeta.getType())) {
            parameterMap.put("type", "object");
        } else if (Enum.class.isAssignableFrom(typeDocMeta.getType())) {
            parameterMap.put("type", "string");
            @SuppressWarnings("unchecked")
            final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) typeDocMeta.getType();
            final List<Map<String, String>> enumMap = enumHandler.buildEnumMapList(enumClass);
            parameterMap.put("enum", enumMap.stream().map(e -> e.get("code")).collect(Collectors.toList()));
            String description = typeDocMeta.getDescription();
            if (DfStringUtil.is_Null_or_Empty(description)) {
                description = typeDocMeta.getPublicName();
            }
            description += ":" + enumMap.stream().map(e -> {
                return String.format(" * `%s` - %s, %s.", e.get("code"), e.get("name"), e.get("alias"));
            }).collect(Collectors.joining());
            parameterMap.put("description", description);
        } else if (!nativeDataTypeList.contains(typeDocMeta.getType())) {
            String definition = putDefinition(typeDocMeta);
            parameterMap.clear();
            parameterMap.put("name", typeDocMeta.getPublicName());
            parameterMap.put("$ref", definition);
        } else {
            parameterMap.put("type", "object");
        }

        typeDocMeta.getAnnotationTypeList().forEach(annotation -> {
            if (annotation instanceof Size) {
                final Size size = (Size) annotation;
                parameterMap.put("minimum", size.min());
                parameterMap.put("maximum", size.max());
            }
            if (annotation instanceof Length) {
                final Length length = (Length) annotation;
                parameterMap.put("minLength", length.min());
                parameterMap.put("maxLength", length.max());
            }
            // pattern, maxItems, minItems
        });

        defaultValueHandler.deriveDefaultValue(typeDocMeta).ifPresent(defaultValue -> {
            parameterMap.put("example", defaultValue);
        });

        typeDocMeta.setType(keepType);
        return parameterMap;
    }

    protected void setupBeanList(TypeDocMeta typeDocMeta, Map<Class<?>, SwaggerSpecDataType> dataTypeMap, Map<String, Object> schemaMap) {
        schemaMap.put("type", "array");
        if (!typeDocMeta.getNestTypeDocMetaList().isEmpty()) {
            final String definition = putDefinition(typeDocMeta);
            schemaMap.put("items", DfCollectionUtil.newLinkedHashMap("$ref", definition));
        } else {
            final Map<String, String> items = DfCollectionUtil.newLinkedHashMap();
            final Class<?> genericType = typeDocMeta.getGenericType();
            if (genericType != null) {
                final SwaggerSpecDataType swaggerDataType = dataTypeMap.get(genericType);
                if (swaggerDataType != null) {
                    items.put("type", swaggerDataType.type);
                    final String format = swaggerDataType.format;
                    if (DfStringUtil.is_NotNull_and_NotEmpty(format)) {
                        items.put("format", format);
                    }
                }
            }
            if (!items.containsKey("type")) {
                items.put("type", "object");
            }
            schemaMap.put("items", items);
        }
        if (typeDocMeta.getSimpleTypeName().matches(".*List<.*List<.*")) {
            schemaMap.put("items", DfCollectionUtil.newLinkedHashMap("type", "array", "items", schemaMap.get("items")));
        }
    }

    protected String putDefinition(TypeDocMeta typeDocMeta) {
        //     "org.docksidestage.app.web.mypage.MypageResult": {
        //       "type": "object",
        //       "required": [
        //         ...
        //       ],
        //       "properties": {
        //         ...
        //       ],
        //
        //     ...
        //
        //     "org.docksidestage.app.web.base.paging.SearchPagingResult\u003corg.docksidestage.app.web.products.ProductsRowResult\u003e": {
        //       "type": "object",
        //       ...
        String derivedDefinitionName = definitionHandler.deriveDefinitionName(typeDocMeta);
        if (!definitionsMap.containsKey(derivedDefinitionName)) {
            final Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
            schema.put("type", "object");
            final List<String> requiredPropertyNameList = propertyHandler.deriveRequiredPropertyNameList(typeDocMeta);
            if (!requiredPropertyNameList.isEmpty()) {
                schema.put("required", requiredPropertyNameList);
            }
            schema.put("properties", typeDocMeta.getNestTypeDocMetaList().stream().map(nestTypeDocMeta -> {
                return toParameterMap(nestTypeDocMeta);
            }).collect(Collectors.toMap(key -> key.get("name"), value -> {
                // TODO p1us2er0 remove name. refactor required. (2017/10/12)
                final LinkedHashMap<String, Object> property = DfCollectionUtil.newLinkedHashMap(value);
                property.remove("name");
                return property;
            }, (u, v) -> v, LinkedHashMap::new)));

            definitionsMap.put(derivedDefinitionName, schema);
        }
        return "#/definitions/" + encodingHandler.encode(derivedDefinitionName);
    }
}
