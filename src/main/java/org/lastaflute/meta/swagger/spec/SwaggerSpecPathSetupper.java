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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
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
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.di.helper.misc.ParameterizedRef;
import org.lastaflute.meta.SwaggerOption;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.swagger.spec.parts.annotation.SwaggerSpecAnnotationHandler;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataType;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataTypeHandler;
import org.lastaflute.meta.swagger.spec.parts.defaultvalue.SwaggerSpecDefaultValueHandler;
import org.lastaflute.meta.swagger.spec.parts.enumtype.SwaggerSpecEnumHandler;
import org.lastaflute.meta.swagger.spec.parts.httpmethod.SwaggerSpecHttpMethodHandler;
import org.lastaflute.web.api.JsonParameter;
import org.lastaflute.web.response.ActionResponse;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.response.StreamResponse;
import org.lastaflute.web.response.XmlResponse;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/23 Wednesday at roppongi japanese)
 */
public class SwaggerSpecPathSetupper {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // -----------------------------------------------------
    //                                        Mutable Output
    //                                        --------------
    // mutable, regsitered in this class, these are output of this class
    protected final Map<String, Map<String, Object>> swaggerPathMap;
    protected final Map<String, Map<String, Object>> swaggerDefinitionsMap;
    protected final List<Map<String, Object>> swaggerTagList;

    // -----------------------------------------------------
    //                                    Resource for Setup
    //                                    ------------------
    protected final SwaggerOption swaggerOption; // not null
    protected final RealJsonEngine swaggeruseJsonEngine; // for swagger process, not null
    protected final OptionalThing<JsonMappingOption> applicationJsonMappingOption; // from application, not null
    protected final List<Class<?>> nativeDataTypeList; // standard native types of e.g. parameter, not null, not empty

    // -----------------------------------------------------
    //                                         Parts Handler
    //                                         -------------
    protected final SwaggerSpecAnnotationHandler annotationHandler;
    protected final SwaggerSpecEnumHandler enumHandler;
    protected final SwaggerSpecDataTypeHandler dataTypeHandler;
    protected final SwaggerSpecDefaultValueHandler defaultValueHandler;
    protected final SwaggerSpecHttpMethodHandler httpMethodHandler;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecPathSetupper(SwaggerSpecPathMutableOutput pathMutableOutput, SwaggerOption swaggerOption,
            RealJsonEngine swaggeruseJsonEngine, OptionalThing<JsonMappingOption> applicationJsonMappingOption,
            List<Class<?>> nativeDataTypeList) {
        // #hope jflute keep pathMutableOutput and handle them by output object (2021/06/23)
        this.swaggerPathMap = pathMutableOutput.getSwaggerPathMap();
        this.swaggerDefinitionsMap = pathMutableOutput.getSwaggerDefinitionsMap();
        this.swaggerTagList = pathMutableOutput.getSwaggerTagList();

        this.swaggerOption = swaggerOption;
        this.swaggeruseJsonEngine = swaggeruseJsonEngine;
        this.applicationJsonMappingOption = applicationJsonMappingOption;
        this.nativeDataTypeList = nativeDataTypeList;

        this.annotationHandler = newSwaggerSpecAnnotationHandler();
        this.enumHandler = newSwaggerSpecEnumHandler();
        this.dataTypeHandler = newSwaggerSpecDataTypeHandler(applicationJsonMappingOption);
        this.defaultValueHandler = newSwaggerSpecDefaultValueHandler(dataTypeHandler, enumHandler);
        this.httpMethodHandler = newSwaggerSpecHttpMethodHandler(swaggerOption);
    }

    protected SwaggerSpecAnnotationHandler newSwaggerSpecAnnotationHandler() {
        return new SwaggerSpecAnnotationHandler();
    }

    protected SwaggerSpecEnumHandler newSwaggerSpecEnumHandler() {
        return new SwaggerSpecEnumHandler();
    }

    protected SwaggerSpecDataTypeHandler newSwaggerSpecDataTypeHandler(OptionalThing<JsonMappingOption> applicationJsonMappingOption) {
        return new SwaggerSpecDataTypeHandler(applicationJsonMappingOption);
    }

    protected SwaggerSpecDefaultValueHandler newSwaggerSpecDefaultValueHandler(SwaggerSpecDataTypeHandler dataTypeHandler,
            SwaggerSpecEnumHandler enumHandler) {
        return new SwaggerSpecDefaultValueHandler(dataTypeHandler, enumHandler);
    }

    protected SwaggerSpecHttpMethodHandler newSwaggerSpecHttpMethodHandler(SwaggerOption swaggerOption) {
        return new SwaggerSpecHttpMethodHandler(swaggerOption);
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
    public void setupSwaggerPathMap(List<ActionDocMeta> actionDocMetaList) { // top-level tags
        // output this process is registration of mutable attributes
        actionDocMetaList.stream().forEach(actiondocMeta -> {
            doSetupSwaggerPathMap(actiondocMeta);
        });
    }

    protected void doSetupSwaggerPathMap(ActionDocMeta actionDocMeta) {
        final String actionUrl = actionDocMeta.getUrl();

        // arrange swaggerUrlMap in swaggerPathMap if needs
        if (!swaggerPathMap.containsKey(actionUrl)) { // first action for the URL
            final Map<String, Object> swaggerUrlMap = DfCollectionUtil.newLinkedHashMap();
            swaggerPathMap.put(actionUrl, swaggerUrlMap);
        }

        // "/signin/": {
        //   "post": {
        final String httpMethod = httpMethodHandler.extractHttpMethod(actionDocMeta);
        final Map<String, Object> swaggerHttpMethodMap = DfCollectionUtil.newLinkedHashMap();
        swaggerPathMap.get(actionUrl).put(httpMethod, swaggerHttpMethodMap);

        //     "summary": "@author jflute",
        //     "description": "@author jflute",
        swaggerHttpMethodMap.put("summary", actionDocMeta.getDescription());
        swaggerHttpMethodMap.put("description", actionDocMeta.getDescription());

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
                //       {
                //         "name": "account",
                //         "type": "string",
                //         "in": "formData"
                //       },
                //       ...
                //     ],
                parameterMapList.addAll(actionDocMeta.getFormTypeDocMeta().getNestTypeDocMetaList().stream().map(typeDocMeta -> {
                    final Map<String, Object> parameterMap = toParameterMap(typeDocMeta);
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
                        swaggerHttpMethodMap.put("consumes", Arrays.asList("multipart/form-data"));
                    } else {
                        swaggerHttpMethodMap.put("consumes", Arrays.asList("application/x-www-form-urlencoded"));
                    }
                }
            } else {
                //     "consumes": [
                //       "application/json"
                //     ],
                swaggerHttpMethodMap.put("consumes", Arrays.asList("application/json"));
                final Map<String, Object> parameterMap = DfCollectionUtil.newLinkedHashMap();
                parameterMap.put("name", actionDocMeta.getFormTypeDocMeta().getSimpleTypeName());
                parameterMap.put("in", "body");
                parameterMap.put("required", true);
                final Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
                schema.put("type", "object");
                final List<String> requiredPropertyNameList = derivedRequiredPropertyNameList(actionDocMeta.getFormTypeDocMeta());
                if (!requiredPropertyNameList.isEmpty()) {
                    schema.put("required", requiredPropertyNameList);
                }
                schema.put("properties", actionDocMeta.getFormTypeDocMeta().getNestTypeDocMetaList().stream().map(propertyDocMeta -> {
                    return toParameterMap(propertyDocMeta);
                }).collect(Collectors.toMap(key -> key.get("name"), value -> {
                    final LinkedHashMap<String, Object> propertyMap = DfCollectionUtil.newLinkedHashMap(value);
                    propertyMap.remove("name");
                    return propertyMap;
                }, (u, v) -> v, LinkedHashMap::new)));

                // Form or Body's definition
                //   "definitions": {
                //     "org.docksidestage.app.web.signin.SigninBody": {
                swaggerDefinitionsMap.put(derivedDefinitionName(actionDocMeta.getFormTypeDocMeta()), schema);

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
                LinkedHashMap<String, String> schemaMap =
                        DfCollectionUtil.newLinkedHashMap("$ref", prepareSwaggerMapRefDefinitions(actionDocMeta));
                if (!Iterable.class.isAssignableFrom(actionDocMeta.getFormTypeDocMeta().getType())) {
                    parameterMap.put("schema", schemaMap);
                } else {
                    parameterMap.put("schema", DfCollectionUtil.newLinkedHashMap("type", "array", "items", schemaMap));
                }
                parameterMapList.add(parameterMap);
            }
        }
        // Query, Header, Body, Form
        swaggerHttpMethodMap.put("parameters", parameterMapList);

        //     "tags": [
        //       "signin"
        //     ],
        swaggerHttpMethodMap.put("tags", prepareSwaggerMapTags(actionDocMeta));
        final String tag = DfStringUtil.substringFirstFront(actionUrl.replaceAll("^/", ""), "/");

        // reflect the tags to top-level tags
        if (swaggerTagList.stream().noneMatch(swaggerTag -> swaggerTag.containsValue(tag))) {
            swaggerTagList.add(DfCollectionUtil.newLinkedHashMap("name", tag));
        }

        //     "responses": {
        //       ...
        prepareSwaggerMapResponseMap(swaggerHttpMethodMap, actionDocMeta);

        if (!optionalPathNameList.isEmpty()) {
            doSetupSwaggerPathMapForOptionalPath(actionDocMeta, optionalPathNameList);
        }
    }

    protected void doSetupSwaggerPathMapForOptionalPath(ActionDocMeta actionDocMeta, List<String> optionalPathNameList) {
        final String actionUrl = actionDocMeta.getUrl();
        final String httpMethod = httpMethodHandler.extractHttpMethod(actionDocMeta);
        String json = swaggeruseJsonEngine.toJson(swaggerPathMap.get(actionUrl).get(httpMethod));

        IntStream.range(0, optionalPathNameList.size()).forEach(index -> {
            List<String> deleteOptionalPathNameList = optionalPathNameList.subList(index, optionalPathNameList.size());
            String deleteOptionalPathNameUrl = deleteOptionalPathNameList.stream().reduce(actionUrl, (aactionUrl, optionalPathName) -> {
                return aactionUrl.replaceAll("/\\{" + optionalPathName + "\\}", "");
            });
            // arrange swaggerUrlMap in swaggerPathMap if needs
            if (!swaggerPathMap.containsKey(deleteOptionalPathNameUrl)) { // first action for the URL
                swaggerPathMap.put(deleteOptionalPathNameUrl, DfCollectionUtil.newLinkedHashMap());
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
            swaggerPathMap.get(deleteOptionalPathNameUrl).put(httpMethod, swaggerHttpMethodMap);
        });
        Map<String, Object> swaggerUrlMap = swaggerPathMap.remove(actionUrl);
        swaggerPathMap.put(actionUrl, swaggerUrlMap);
    }

    protected String prepareSwaggerMapRefDefinitions(ActionDocMeta actiondocMeta) {
        return "#/definitions/" + encode(derivedDefinitionName(actiondocMeta.getFormTypeDocMeta()));
    }

    protected List<String> prepareSwaggerMapTags(ActionDocMeta actiondocMeta) {
        return Arrays.asList(DfStringUtil.substringFirstFront(actiondocMeta.getUrl().replaceAll("^/", ""), "/"));
    }

    protected void prepareSwaggerMapResponseMap(Map<String, Object> swaggerHttpMethodMap, ActionDocMeta actiondocMeta) {
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
        final Map<String, Object> responseMap = DfCollectionUtil.newLinkedHashMap();
        swaggerHttpMethodMap.put("responses", responseMap);
        derivedProduces(actiondocMeta).ifPresent(produces -> {
            swaggerHttpMethodMap.put("produces", produces);
        });
        final Map<String, Object> response = DfCollectionUtil.newLinkedHashMap("description", "success");
        final TypeDocMeta returnTypeDocMeta = actiondocMeta.getReturnTypeDocMeta();
        if (!Arrays.asList(HtmlResponse.class, StreamResponse.class)
                .stream()
                .anyMatch(clazz -> clazz.isAssignableFrom(returnTypeDocMeta.getType()))
                && !Arrays.asList(void.class, Void.class).contains(returnTypeDocMeta.getGenericType())) {
            final Map<String, Object> parameterMap = toParameterMap(returnTypeDocMeta);
            parameterMap.remove("name");
            parameterMap.remove("required");
            if (parameterMap.containsKey("schema")) {
                response.putAll(parameterMap);
            } else {
                response.put("schema", parameterMap);
            }
        }
        responseMap.put("200", response);
        if (ApiResponse.class.isAssignableFrom(returnTypeDocMeta.getType())) {
            responseMap.put("400", DfCollectionUtil.newLinkedHashMap("description", "client error"));
        }
    }

    // ===================================================================================
    //                                                                       Parameter Map
    //                                                                       =============
    protected Map<String, Object> toParameterMap(TypeDocMeta typeDocMeta) {
        final Map<Class<?>, SwaggerSpecDataType> typeMap = createSwaggerDataTypeMap();
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
        String derivedDefinitionName = derivedDefinitionName(typeDocMeta);
        if (!swaggerDefinitionsMap.containsKey(derivedDefinitionName)) {
            final Map<String, Object> schema = DfCollectionUtil.newLinkedHashMap();
            schema.put("type", "object");
            final List<String> requiredPropertyNameList = derivedRequiredPropertyNameList(typeDocMeta);
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

            swaggerDefinitionsMap.put(derivedDefinitionName, schema);
        }
        return "#/definitions/" + encode(derivedDefinitionName);
    }

    protected List<String> derivedRequiredPropertyNameList(TypeDocMeta typeDocMeta) {
        return typeDocMeta.getNestTypeDocMetaList().stream().filter(nesttypeDocMeta -> {
            return nesttypeDocMeta.getAnnotationTypeList().stream().anyMatch(annotationType -> {
                return annotationHandler.getRequiredAnnotationList()
                        .stream()
                        .anyMatch(requiredAnnotation -> requiredAnnotation.isAssignableFrom(annotationType.getClass()));
            });
        }).map(nesttypeDocMeta -> nesttypeDocMeta.getPublicName()).collect(Collectors.toList());
    }

    protected String derivedDefinitionName(TypeDocMeta typeDocMeta) {
        if (typeDocMeta.getTypeName().matches("^[^<]+<(.+)>$")) {
            return typeDocMeta.getTypeName().replaceAll("^[^<]+<(.+)>$", "$1").replaceAll(" ", "");
        }
        return typeDocMeta.getTypeName().replaceAll(" ", "");
    }

    protected OptionalThing<List<String>> derivedProduces(ActionDocMeta actiondocMeta) {
        if (Arrays.asList(void.class, Void.class).contains(actiondocMeta.getReturnTypeDocMeta().getGenericType())) {
            return OptionalThing.empty();
        }
        if (createSwaggerDataTypeMap().containsKey(actiondocMeta.getReturnTypeDocMeta().getGenericType())) {
            return OptionalThing.of(Arrays.asList("text/plain;charset=UTF-8"));
        }
        final Map<Class<?>, List<String>> produceMap = DfCollectionUtil.newHashMap();
        produceMap.put(JsonResponse.class, Arrays.asList("application/json"));
        produceMap.put(XmlResponse.class, Arrays.asList("application/xml"));
        produceMap.put(HtmlResponse.class, Arrays.asList("text/html"));
        produceMap.put(StreamResponse.class, Arrays.asList("application/octet-stream"));
        final Class<?> produceType = actiondocMeta.getReturnTypeDocMeta().getType();
        final List<String> produceList = produceMap.get(produceType);
        return OptionalThing.ofNullable(produceList, () -> {
            String msg = "Not found the produce: type=" + produceType + ", keys=" + produceMap.keySet();
            throw new IllegalStateException(msg);
        });
    }

    // ===================================================================================
    //                                                                    Swagger DataType
    //                                                                    ================
    protected Map<Class<?>, SwaggerSpecDataType> createSwaggerDataTypeMap() {
        // #hope jflute should be cached? because of many calls (2021/06/23)
        return dataTypeHandler.createSwaggerDataTypeMap();
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    // MUST be in the form of a URI. p1us2er0  (2021/06/08)
    // https://swagger.io/docs/specification/using-ref/
    // https://spec.openapis.org/oas/v3.1.0#reference-object
    protected String encode(String value) {
        try {
            return URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new IllegalArgumentException(e);
        }
    }
}
