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
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.core.json.control.JsonControlMeta;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.di.util.LdiSerializeUtil;
import org.lastaflute.meta.SwaggerOption;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.swagger.spec.parts.annotation.SwaggerSpecAnnotationHandler;
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
import org.lastaflute.meta.swagger.spec.zone.parameter.SwaggerSpecParameterSetupper;
import org.lastaflute.meta.swagger.spec.zone.responses.SwaggerSpecResponsesSetupper;

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
        final String tag = deriveActionTag(actionDocMeta);
        httpMethodContentMap.put("tags", Arrays.asList(tag));
        if (isNewTag(tag)) {
            registerNewTagToTopLevel(tag); // reflect the tags to top-level tags
        }

        //     "responses": {
        //       ...
        prepareResponses(httpMethodContentMap, actionDocMeta);

        if (!optionalPathNameList.isEmpty()) {
            prepareOptionalParameterPath(actionDocMeta, optionalPathNameList);
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
    protected String deriveActionTag(ActionDocMeta actionDocMeta) {
        if ("RootAction".equals(actionDocMeta.getType().getSimpleName())) {
            return "root"; // fixedly
        }
        final String actionUrl = actionDocMeta.getUrl();
        return DfStringUtil.substringFirstFront(actionUrl.replaceAll("^/", ""), "/"); // first element
    }

    protected boolean isNewTag(String tag) {
        return tagsList.stream().noneMatch(swaggerTag -> swaggerTag.containsValue(tag));
    }

    protected boolean registerNewTagToTopLevel(final String tag) {
        return tagsList.add(DfCollectionUtil.newLinkedHashMap("name", tag));
    }

    // -----------------------------------------------------
    //                                     for Optional Path
    //                                     -----------------
    protected void prepareOptionalParameterPath(ActionDocMeta actionDocMeta, List<String> optionalPathNameList) {
        // copy optional parameter path to non-parameter path
        //  e.g. /product/list/{pageNumber} to /product/list
        final String actionUrl = actionDocMeta.getUrl(); // e.g. /product/list/{pageNumber}
        final String httpMethod = httpMethodHandler.extractHttpMethod(actionDocMeta);

        // loop: e.g. sea, land, piari, if /maihama/{sea}/{land}/{piari}
        IntStream.range(0, optionalPathNameList.size()).forEach(index -> {
            // [sea, land, piari] if current {sea}
            // [land, piari] if current {land}
            // [piari] if current [piari]
            final List<String> currentOptionalPathNameList = optionalPathNameList.subList(index, optionalPathNameList.size());

            // /maihama if current {sea}
            // /maihama/{sea} if current {land}
            // /maihama/{sea}/{land} if current {piari}
            final String currentUrl = currentOptionalPathNameList.stream().reduce(actionUrl, (workingActionUrl, optionalPathName) -> {
                return workingActionUrl.replaceAll("/\\{" + optionalPathName + "\\}", "");
            });

            // arrange swaggerUrlMap in swaggerPathMap if needs
            if (!pathsMap.containsKey(currentUrl)) { // first action for the URL
                pathsMap.put(currentUrl, DfCollectionUtil.newLinkedHashMap());
            }

            // prepare swaggerHttpMethodMap for current optional path
            @SuppressWarnings("unchecked")
            Map<String, Object> swaggerHttpMethodMap =
                    (Map<String, Object>) LdiSerializeUtil.serialize(pathsMap.get(actionUrl).get(httpMethod));
            prepareOptionalSwaggerHttpMethodMap(swaggerHttpMethodMap, currentOptionalPathNameList);

            // register HTTP Method definition for current optional path
            pathsMap.get(currentUrl).put(httpMethod, swaggerHttpMethodMap);
        });
        final Map<String, Object> swaggerUrlMap = pathsMap.remove(actionUrl);
        pathsMap.put(actionUrl, swaggerUrlMap);
    }

    protected void prepareOptionalSwaggerHttpMethodMap(Map<String, Object> swaggerHttpMethodMap, List<String> currentOptionalPathNameList) {
        final String parametersKey = "parameters";
        @SuppressWarnings("unchecked")
        final List<Map<String, Object>> parametersSnapshotList = (List<Map<String, Object>>) swaggerHttpMethodMap.get(parametersKey);
        final List<Map<String, Object>> filteredParameterMapList = parametersSnapshotList.stream()
                .filter(parameterMap -> !currentOptionalPathNameList.contains(parameterMap.get("name")))
                .collect(Collectors.toList());
        swaggerHttpMethodMap.put(parametersKey, filteredParameterMapList); // overwrite
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
    protected Map<String, Object> toParameterMap(TypeDocMeta typeDocMeta) {
        // #for_now jflute too many arguments, can it move any handers to the setupper?  (2021/08/07)
        final SwaggerSpecParameterSetupper parameterSetupper = new SwaggerSpecParameterSetupper(nativeDataTypeList, enumHandler,
                dataTypeHandler, defaultValueHandler, propertyHandler, definitionHandler, encodingHandler);
        return parameterSetupper.toParameterMap(typeDocMeta, definitionsMap);
    }
}
