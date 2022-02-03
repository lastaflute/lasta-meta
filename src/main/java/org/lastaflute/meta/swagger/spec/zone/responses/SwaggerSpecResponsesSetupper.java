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
package org.lastaflute.meta.swagger.spec.zone.responses;

import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.Srl;
import org.lastaflute.meta.SwaggerOption;
import org.lastaflute.meta.SwaggerOption.SwaggerFailureHttpStatusResource;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.swagger.spec.parts.produces.SwaggerSpecProducesHandler;
import org.lastaflute.web.response.ApiResponse;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.response.StreamResponse;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/25 Friday at roppongi japanese)
 */
public class SwaggerSpecResponsesSetupper {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final SwaggerOption swaggerOption;
    protected final SwaggerSpecProducesHandler producesHandler;
    protected final Function<TypeDocMeta, Map<String, Object>> parameterMapProvider; // for return type

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecResponsesSetupper(SwaggerOption swaggerOption, SwaggerSpecProducesHandler producesHandler,
            Function<TypeDocMeta, Map<String, Object>> parameterMapProvider) {
        this.swaggerOption = swaggerOption;
        this.producesHandler = producesHandler;
        this.parameterMapProvider = parameterMapProvider;
    }

    // ===================================================================================
    //                                                                             Prepare
    //                                                                             =======
    // e.g.
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
    public void prepareResponses(Map<String, Object> swaggerHttpMethodMap, ActionDocMeta actionDocMeta) {
        final Map<String, Object> responseMap = DfCollectionUtil.newLinkedHashMap();
        swaggerHttpMethodMap.put("responses", responseMap);
        producesHandler.deriveProduces(actionDocMeta).ifPresent(produces -> {
            swaggerHttpMethodMap.put("produces", produces);
        });
        prepareResponseSuccess(responseMap, actionDocMeta);
        prepareResponseFailure(responseMap, actionDocMeta);
    }

    // ===================================================================================
    //                                                                             Success
    //                                                                             =======
    protected void prepareResponseSuccess(Map<String, Object> responseMap, ActionDocMeta actionDocMeta) {
        final Integer httpStatus = findHttpStatus(actionDocMeta);
        final String description = findDescription(actionDocMeta);
        final Map<String, Object> contentMap = buildSuccessResponseContentMap(actionDocMeta, description);
        registerResponse(responseMap, httpStatus, contentMap);
    }

    protected Integer findHttpStatus(ActionDocMeta actionDocMeta) {
        return swaggerOption.getSuccessHttpStatusLambda().map(callback -> {
            return callback.apply(actionDocMeta); // null allowed
        }).orElseGet(() -> {
            return actionDocMeta.getActionExecute().getSuccessHttpStatus().map(specified -> {
                return specified.getStatusValue();
            }).orElse(200); // as default
        });
    }

    protected String findDescription(ActionDocMeta actionDocMeta) {
        return actionDocMeta.getActionExecute().getSuccessHttpStatus().map(specified -> {
            final String specifiedDesc = specified.getDescription();
            return Srl.is_NotNull_and_NotTrimmedEmpty(specifiedDesc) ? specifiedDesc : null;
        }).orElse("success"); // as default
    }

    protected Map<String, Object> buildSuccessResponseContentMap(ActionDocMeta actionDocMeta, String description) {
        final Map<String, Object> contentMap = newContentMapWithDescription(description);
        final TypeDocMeta returnTypeDocMeta = actionDocMeta.getReturnTypeDocMeta();
        if (!Arrays.asList(HtmlResponse.class, StreamResponse.class)
                .stream()
                .anyMatch(clazz -> clazz.isAssignableFrom(returnTypeDocMeta.getType()))
                && !Arrays.asList(void.class, Void.class).contains(returnTypeDocMeta.getGenericType())) {
            final Map<String, Object> parameterMap = parameterMapProvider.apply(returnTypeDocMeta);
            parameterMap.remove("name");
            parameterMap.remove("required");
            if (parameterMap.containsKey("schema")) {
                contentMap.putAll(parameterMap);
            } else {
                contentMap.put("schema", parameterMap);
            }
        }
        return contentMap;
    }

    // ===================================================================================
    //                                                                             Failure
    //                                                                             =======
    protected void prepareResponseFailure(Map<String, Object> responseMap, ActionDocMeta actionDocMeta) {
        if (!ApiResponse.class.isAssignableFrom(actionDocMeta.getReturnTypeDocMeta().getType())) {
            return;
        }
        // ApiResponse only here, ApiFailureHook returns HTTP status as client error but HtmlResponse not
        // #for_now jflute is it OK? as swagger-spec (what should we do?) (2021/06/25)
        Map<Integer, List<Class<?>>> statusCauseMap = swaggerOption.getFailureHttpStatusLambda().map(callback -> {
            final SwaggerFailureHttpStatusResource resource = callback.apply(actionDocMeta);
            return resource != null ? resource.getFailureStatusCauseMap() : null;
        }).orElseGet(() -> Collections.emptyMap());
        if (statusCauseMap.isEmpty()) {
            final int httpStatus = 400; // as default
            final String description = "client error"; // as default
            final Map<String, Object> contentMap = newContentMapWithDescription(description);
            registerResponse(responseMap, httpStatus, contentMap);
        } else { // specified
            statusCauseMap.forEach((httpStatus, causeTypeList) -> {
                final String description = causeTypeList.stream().map(tp -> tp.getSimpleName()).collect(Collectors.joining(", "));
                final Map<String, Object> contentMap = newContentMapWithDescription(description);
                registerResponse(responseMap, httpStatus, contentMap);
            });
        }
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected LinkedHashMap<String, Object> newContentMapWithDescription(final String description) {
        return DfCollectionUtil.newLinkedHashMap("description", description);
    }

    protected void registerResponse(Map<String, Object> responseMap, int httpStatus, Map<String, Object> contentMap) {
        final String statusExp = String.valueOf(httpStatus);
        responseMap.put(statusExp, contentMap);
    }
}
