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
package org.lastaflute.meta.swagger.web;

import java.io.InputStream;
import java.util.Map;

import org.dbflute.helper.filesystem.FileTextIO;
import org.lastaflute.core.json.JsonEngineResource;
import org.lastaflute.core.json.JsonManager;
import org.lastaflute.core.json.engine.RealJsonEngine;
import org.lastaflute.web.response.JsonResponse;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.2.6 (2017/06/11 Sunday)
 */
public interface LaActionSwaggerable { // used by application's SwaggerAction

    /**
     * Prepare JSON for e.g. SwaggerUI content. <br>
     * Also used to save Lasta-presents swagger.json.
     * <pre>
     * &#064Execute
     * public JsonResponse<Map<String, Object>> json() { // using Lasta-presents json
     *     verifySwaggerAllowed();
     *     Map&lt;String, Object&gt; swaggerMap = new SwaggerGenerator().generateSwaggerMap(op -&gt; {});
     *     return asJson(swaggerMap).switchMappingOption(op -> {}); // not to depend on application settings
     * }
     * </pre>
     * @return The JSON response as map from swagger.json. (NotNull)
     */
    JsonResponse<Map<String, Object>> json();

    // e.g. SwaggerAction implementation
    //@AllowAnyoneAccess
    //public class SwaggerAction extends FortressBaseAction implements LaActionSwaggerable {
    //
    //    // ===================================================================================
    //    //                                                                           Attribute
    //    //                                                                           =========
    //    @Resource
    //    private RequestManager requestManager;
    //    @Resource
    //    private FortressConfig config;
    //
    //    // ===================================================================================
    //    //                                                                             Execute
    //    //                                                                             =======
    //    @Execute
    //    public HtmlResponse index() {
    //        verifySwaggerAllowed();
    //        String swaggerJsonUrl = toActionUrl(SwaggerAction.class, moreUrl("json"));
    //        return new SwaggerAgent(requestManager).prepareSwaggerUiResponse(swaggerJsonUrl);
    //    }
    //
    //    @Execute
    //    public JsonResponse<Map<String, Object>> json() {
    //        verifySwaggerAllowed();
    //        return asJson(new SwaggerGenerator().generateSwaggerMap());
    //    }
    //
    //    private void verifySwaggerAllowed() { // also check in ActionAdjustmentProvider
    //        verifyOrClientError("Swagger is not enabled.", config.isSwaggerEnabled());
    //    }
    //}
    // e.g. LastaMetaTest implementation
    //public class ShowbaseLastaMetaTest extends UnitShowbaseTestCase {
    //
    //    @Override
    //    protected String prepareMockContextPath() {
    //        return ShowbaseBoot.CONTEXT; // basically for swagger
    //    }
    //
    //    public void test_document() throws Exception {
    //        saveLastaMeta();
    //    }
    //
    //    public void test_swaggerJson() throws Exception {
    //        saveSwaggerMeta(new SwaggerAction());
    //    }
    //}

    /**
     * Read swagger.json in classpath resource, basically for application swagger.json. <br>
     * The swagger.json file should be UTF-8.
     * <pre>
     * &#064Execute
     * public JsonResponse<Map<String, Object>> appjson() { // using application json
     *     verifySwaggerAllowed();
     *     Map&lt;String, Object&gt; swaggerMap = readResourceJson(jsonManager, "/swagger/your-swagger.json");
     *     return asJson(swaggerMap).switchMappingOption(op -> {}); // not to depend on application settings
     * }
     * </pre>
     * @param jsonManager The JSON manager of LastaFlute, to use new ruled engine. (NotNull)
     * @param jsonResourcePath The resource path to swagger.json. (NotNull)
     * @return The map from the swagger.json for SwaggerUI. (NotNull)
     */
    default Map<String, Object> readResourceJson(JsonManager jsonManager, String jsonResourcePath) {
        final RealJsonEngine simpleEngine = jsonManager.newRuledEngine(new JsonEngineResource());
        final InputStream ins = getClass().getClassLoader().getResourceAsStream(jsonResourcePath);
        if (ins == null) {
            throw new IllegalStateException("Not found the swagger JSON file: " + jsonResourcePath);
        }
        final String json = new FileTextIO().encodeAsUTF8().read(ins);
        @SuppressWarnings("unchecked")
        final Map<String, Object> swaggerMap = simpleEngine.fromJson(json, Map.class);
        return swaggerMap;
    }
}
