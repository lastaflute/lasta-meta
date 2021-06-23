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

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Pattern;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.AccessibleConfig;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.outputmeta.OutputMetaSerializer;
import org.lastaflute.meta.document.type.NativeDataTypeProvider;
import org.lastaflute.meta.infra.json.MetauseJsonEngineProvider;
import org.lastaflute.meta.swagger.json.SwaggerJsonReader;
import org.lastaflute.meta.swagger.spec.SwaggerSpecCreator;
import org.lastaflute.meta.swagger.spec.SwaggerSpecPathMutableOutput;
import org.lastaflute.meta.swagger.spec.SwaggerSpecPathSetupper;
import org.lastaflute.meta.swagger.web.LaActionSwaggerable;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.util.LaRequestUtil;

/**
 * @author p1us2er0
 * @author jflute
 */
public class SwaggerGenerator {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final Pattern HTTP_METHOD_PATTERN = Pattern.compile("(.+)\\$.+");

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final MetauseJsonEngineProvider jsonEngineProvider = newMetaJsonEngineProvider();

    protected MetauseJsonEngineProvider newMetaJsonEngineProvider() {
        return new MetauseJsonEngineProvider();
    }

    protected final OutputMetaSerializer outputMetaSerializer = newOutputMetaSerializer();

    protected OutputMetaSerializer newOutputMetaSerializer() {
        return new OutputMetaSerializer();
    }

    protected final NativeDataTypeProvider dataNativeTypeProvider = newDataNativeTypeProvider();

    protected NativeDataTypeProvider newDataNativeTypeProvider() {
        return new NativeDataTypeProvider();
    }

    // ===================================================================================
    //                                                                            Generate
    //                                                                            ========
    // basically called by action
    /**
     * Generate swagger map. (no option)
     * @return The map of swagger information. (NotNull)
     */
    public Map<String, Object> generateSwaggerMap() {
        return generateSwaggerMap(op -> {});
    }

    /**
     * Generate swagger map with option.
     * <pre>
     * new SwaggerGenerator().generateSwaggerMap(op -&gt; {
     *     op.deriveBasePath(basePath -&gt; basePath + "api/");
     * });
     * </pre>
     * @param opLambda The callback for settings of option. (NotNull)
     * @return The map of swagger information. (NotNull)
     */
    public Map<String, Object> generateSwaggerMap(Consumer<SwaggerOption> opLambda) {
        final OptionalThing<Map<String, Object>> swaggerJson = readSwaggerJson();
        if (swaggerJson.isPresent()) { // e.g. war world
            final Map<String, Object> swaggerMap = swaggerJson.get();
            swaggerMap.put("schemes", prepareSwaggerMapSchemes()); // #thinking jflute why? (2021/06/21)
            return swaggerMap;
        }
        return createSwaggerMap(createSwaggerOption(opLambda)); // basically here if local development
    }

    // -----------------------------------------------------
    //                                 Existing swagger.json
    //                                 ---------------------
    protected OptionalThing<Map<String, Object>> readSwaggerJson() { // for war world
        return newSwaggerJsonReader(createJsonEngine()).readSwaggerJson();
    }

    protected SwaggerJsonReader newSwaggerJsonReader(RealJsonEngine jsonEngine) {
        return new SwaggerJsonReader(jsonEngine);
    }

    // -----------------------------------------------------
    //                                       Request Schemes
    //                                       ---------------
    protected List<String> prepareSwaggerMapSchemes() { // // #for_now jflute copied from SwaggerMapCreator (2021/06/21)
        return Arrays.asList(getRequest().getScheme());
    }

    // -----------------------------------------------------
    //                                        Swagger Option
    //                                        --------------
    protected SwaggerOption createSwaggerOption(Consumer<SwaggerOption> opLambda) {
        final SwaggerOption swaggerOption = new SwaggerOption();
        opLambda.accept(swaggerOption);
        return swaggerOption;
    }

    // ===================================================================================
    //                                                                         Swagger Map
    //                                                                         ===========
    protected Map<String, Object> createSwaggerMap(SwaggerOption swaggerOption) {
        final SwaggerSpecCreator creator = newSwaggerSpecCreator(getAccessibleConfig(), getRequest());
        return creator.createSwaggerMap(swaggerOption, (swaggerPathMap, swaggerDefinitionsMap, swaggerTagList) -> {
            setupSwaggerPathMap(swaggerPathMap, swaggerDefinitionsMap, swaggerTagList, swaggerOption);
        });
    }

    protected SwaggerSpecCreator newSwaggerSpecCreator(AccessibleConfig accessibleConfig, HttpServletRequest currentRequest) {
        return new SwaggerSpecCreator(accessibleConfig, currentRequest);
    }

    // ===================================================================================
    //                                                                    Swagger Path Map
    //                                                                    ================
    protected void setupSwaggerPathMap(Map<String, Map<String, Object>> swaggerPathMap // map of top-level paths
            , Map<String, Map<String, Object>> swaggerDefinitionsMap // map of top-level definitions
            , List<Map<String, Object>> swaggerTagList, SwaggerOption swaggerOption) { // top-level tags
        final SwaggerSpecPathSetupper pathSetupper =
                createSwaggerSpecPathSetupper(swaggerPathMap, swaggerDefinitionsMap, swaggerTagList, swaggerOption);
        pathSetupper.setupSwaggerPathMap(generateActionDocMetaList());
    }

    // -----------------------------------------------------
    //                                         Path Setupper
    //                                         -------------
    protected SwaggerSpecPathSetupper createSwaggerSpecPathSetupper(Map<String, Map<String, Object>> swaggerPathMap,
            Map<String, Map<String, Object>> swaggerDefinitionsMap, List<Map<String, Object>> swaggerTagList, SwaggerOption swaggerOption) {
        // prepare mutable output (registered in setupper) here
        final SwaggerSpecPathMutableOutput pathMutableOutput =
                new SwaggerSpecPathMutableOutput(swaggerPathMap, swaggerDefinitionsMap, swaggerTagList);

        // prepare resources for setup here
        final RealJsonEngine swaggeruseJsonEngine = createJsonEngine();
        final OptionalThing<JsonMappingOption> applicationJsonMappingOption = getApplicationJsonMappingOption();
        final List<Class<?>> nativeDataTypeList = dataNativeTypeProvider.provideNativeDataTypeList();

        return newSwaggerSpecPathSetupper(pathMutableOutput, swaggerOption, swaggeruseJsonEngine, applicationJsonMappingOption,
                nativeDataTypeList);
    }

    protected SwaggerSpecPathSetupper newSwaggerSpecPathSetupper(SwaggerSpecPathMutableOutput pathMutableOutput,
            SwaggerOption swaggerOption, RealJsonEngine swaggeruseJsonEngine, OptionalThing<JsonMappingOption> applicationJsonMappingOption,
            List<Class<?>> nativeDataTypeList) {
        return new SwaggerSpecPathSetupper(pathMutableOutput, swaggerOption, swaggeruseJsonEngine, applicationJsonMappingOption,
                nativeDataTypeList);
    }

    // -----------------------------------------------------
    //                                         ActionDocMeta
    //                                         -------------
    protected List<ActionDocMeta> generateActionDocMetaList() {
        return new DocumentGenerator().createActionDocumentAnalyzer().generateActionDocMetaList();
    }

    // ===================================================================================
    //                                                                               Save
    //                                                                              ======
    public void saveSwaggerMeta(LaActionSwaggerable swaggerable) { // basically called by unit test
        final String json = extractActionJson(swaggerable);
        outputMetaSerializer.saveSwaggerMeta(json);
    }

    protected String extractActionJson(LaActionSwaggerable swaggerable) {
        final JsonResponse<Map<String, Object>> jsonResponse = swaggerable.json();
        return createJsonEngine().toJson(jsonResponse.getJsonResult());
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected AccessibleConfig getAccessibleConfig() {
        return ContainerUtil.getComponent(AccessibleConfig.class);
    }

    protected HttpServletRequest getRequest() {
        return LaRequestUtil.getRequest();
    }

    protected RealJsonEngine createJsonEngine() {
        return jsonEngineProvider.createJsonEngine();
    }

    protected OptionalThing<JsonMappingOption> getApplicationJsonMappingOption() {
        return jsonEngineProvider.getApplicationJsonMappingOption();
    }
}
