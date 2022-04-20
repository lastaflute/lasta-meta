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
package org.lastaflute.meta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.direction.AccessibleConfig;
import org.lastaflute.core.json.control.JsonControlMeta;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.outputmeta.OutputMetaSerializer;
import org.lastaflute.meta.document.parts.type.NativeDataTypeProvider;
import org.lastaflute.meta.infra.json.MetauseJsonEngineProvider;
import org.lastaflute.meta.swagger.json.SwaggerJsonReader;
import org.lastaflute.meta.swagger.spec.SwaggerSpecCreator;
import org.lastaflute.meta.swagger.spec.SwaggerSpecPathsMutableOutput;
import org.lastaflute.meta.swagger.spec.SwaggerSpecPathsSetupper;
import org.lastaflute.meta.swagger.web.LaActionSwaggerable;
import org.lastaflute.web.response.JsonResponse;
import org.lastaflute.web.util.LaRequestUtil;

/**
 * @author p1us2er0
 * @author jflute
 */
public class SwaggerGenerator {

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
        return createSwaggerSpecMap(createSwaggerOption(opLambda)); // basically here if local development
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
        customizeSwaggerOption(swaggerOption);
        opLambda.accept(swaggerOption);
        return swaggerOption;
    }

    protected void customizeSwaggerOption(SwaggerOption swaggerOption) { // you can override
        // do nothing as default
    }

    // ===================================================================================
    //                                                                         Swagger Map
    //                                                                         ===========
    protected Map<String, Object> createSwaggerSpecMap(SwaggerOption swaggerOption) {
        final SwaggerSpecCreator creator = newSwaggerSpecCreator(getAccessibleConfig(), getRequest());
        return creator.createSwaggerSpecMap(swaggerOption, (pathsMap, definitionsMap, tagsList) -> {
            setupSwaggerPathsMap(pathsMap, definitionsMap, tagsList, swaggerOption);
        });
    }

    protected SwaggerSpecCreator newSwaggerSpecCreator(AccessibleConfig accessibleConfig, HttpServletRequest currentRequest) {
        return new SwaggerSpecCreator(accessibleConfig, currentRequest);
    }

    // ===================================================================================
    //                                                                    Swagger Path Map
    //                                                                    ================
    protected void setupSwaggerPathsMap(Map<String, Map<String, Object>> pathsMap // map of top-level paths
            , Map<String, Map<String, Object>> definitionsMap // map of top-level definitions
            , List<Map<String, Object>> tagsList, SwaggerOption swaggerOption) { // top-level tags
        final SwaggerSpecPathsSetupper pathsSetupper = createSwaggerSpecPathsSetupper(pathsMap, definitionsMap, tagsList, swaggerOption);
        pathsSetupper.setupSwaggerPathsMap(filterActionDocMetaList(generateActionDocMetaList(swaggerOption)));
    }

    // -----------------------------------------------------
    //                                         Path Setupper
    //                                         -------------
    protected SwaggerSpecPathsSetupper createSwaggerSpecPathsSetupper(Map<String, Map<String, Object>> pathsMap,
            Map<String, Map<String, Object>> definitionsMap, List<Map<String, Object>> tagsList, SwaggerOption swaggerOption) {
        // prepare mutable output (registered in setupper) here
        final SwaggerSpecPathsMutableOutput pathMutableOutput = new SwaggerSpecPathsMutableOutput(pathsMap, definitionsMap, tagsList);

        // prepare resources for setup here
        final RealJsonEngine swaggeruseJsonEngine = createJsonEngine();
        final JsonControlMeta appJsonControlMeta = getAppJsonControlMeta();
        final List<Class<?>> nativeDataTypeList = dataNativeTypeProvider.provideNativeDataTypeList();

        return newSwaggerSpecPathsSetupper(pathMutableOutput, swaggerOption, swaggeruseJsonEngine, appJsonControlMeta, nativeDataTypeList);
    }

    protected SwaggerSpecPathsSetupper newSwaggerSpecPathsSetupper(SwaggerSpecPathsMutableOutput pathMutableOutput,
            SwaggerOption swaggerOption, RealJsonEngine swaggeruseJsonEngine, JsonControlMeta appJsonControlMeta,
            List<Class<?>> nativeDataTypeList) {
        return new SwaggerSpecPathsSetupper(pathMutableOutput, swaggerOption, swaggeruseJsonEngine, appJsonControlMeta, nativeDataTypeList);
    }

    // -----------------------------------------------------
    //                                         ActionDocMeta
    //                                         -------------
    protected List<ActionDocMeta> generateActionDocMetaList(SwaggerOption swaggerOption) {
        final DocumentGenerator documentGenerator = newDocumentGenerator();
        swaggerOption.getAdditionalSourceDirectories().ifPresent(consumer -> {
            final List<String> dirList = new ArrayList<>();
            consumer.accept(dirList);
            for (String dir : dirList) {
                documentGenerator.addSrcDir(dir);
            }
        });
        customizeActionDocumentGenerator(documentGenerator);
        return documentGenerator.createActionDocumentAnalyzer().analyzeAction();
    }

    protected DocumentGenerator newDocumentGenerator() {
        return new DocumentGenerator();
    }

    protected void customizeActionDocumentGenerator(DocumentGenerator documentGenerator) { // you can override
        // do nothing as default
    }

    protected List<ActionDocMeta> filterActionDocMetaList(List<ActionDocMeta> actionDocMetaList) {
        // the SwaggerAction is unneeded in swagger.json (avoid noise of SwaggerDiff, RemoteApiGen)
        return actionDocMetaList.stream().filter(meta -> !isSwaggerAction(meta)).collect(Collectors.toList());
    }

    protected boolean isSwaggerAction(ActionDocMeta meta) {
        return LaActionSwaggerable.class.isAssignableFrom(meta.getType());
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

    protected JsonControlMeta getAppJsonControlMeta() {
        return jsonEngineProvider.getAppJsonControlMeta();
    }
}
