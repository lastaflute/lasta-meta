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
package org.lastaflute.meta.diff;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.function.Consumer;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.util.DfResourceUtil;
import org.dbflute.util.Srl;
import org.lastaflute.meta.exception.LastaMetaIOException;
import org.openapitools.openapidiff.core.OpenApiCompare;
import org.openapitools.openapidiff.core.model.ChangedOpenApi;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.swagger.parser.OpenAPIParser;
import io.swagger.v3.oas.models.OpenAPI;

/**
 * You need to set the "openapi-diff-core" library at your build settings.
 * @author p1us2er0
 * @author jflute
 */
public class SwaggerDiffGenerator {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final SwaggerDiffOption swaggerDiffOption;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerDiffGenerator() {
        this(op -> {});
    }

    public SwaggerDiffGenerator(Consumer<SwaggerDiffOption> opLambda) {
        this.swaggerDiffOption = this.createSwaggerDiffOption(opLambda);
    }

    protected SwaggerDiffOption createSwaggerDiffOption(Consumer<SwaggerDiffOption> opLambda) {
        SwaggerDiffOption swaggerDiffOption = new SwaggerDiffOption();
        opLambda.accept(swaggerDiffOption);
        return swaggerDiffOption;
    }

    protected SwaggerDiffOption getSwaggerDiffOption() {
        return this.swaggerDiffOption;
    }

    // ===================================================================================
    //                                                                                Diff
    //                                                                                ====
    public String diffFromLocations(String leftSwaggerLocation, String rightSwaggerLocation) {
        try {
            ChangedOpenApi changedOpenApi = diffFromLocationsInChangedOpenApi(leftSwaggerLocation, rightSwaggerLocation);
            return swaggerDiffOption.getRender().render(changedOpenApi);
        } catch (RuntimeException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to diff the swagger files.");
            br.addItem("leftSwaggerLocation");
            br.addElement(leftSwaggerLocation);
            br.addItem("rightSwaggerLocation");
            br.addElement(rightSwaggerLocation);
            final String msg = br.buildExceptionMessage();
            throw new IllegalStateException(msg, e);
        }
    }

    public String diffFromContents(String leftSwaggerContent, String rightSwaggerContent) {
        ChangedOpenApi changedOpenApi = diffFromContentsInChangedOpenApi(leftSwaggerContent, rightSwaggerContent);
        String value = swaggerDiffOption.getRender().render(changedOpenApi);
        return value;
    }

    // ===================================================================================
    //                                                                Diff(ChangedOpenApi)
    //                                                                ====================
    protected ChangedOpenApi diffFromLocationsInChangedOpenApi(String leftSwaggerLocation, String rightSwaggerLocation) {
        try (InputStream leftSwaggerInputStream = getInputStream(leftSwaggerLocation);
                Reader leftSwaggerReader = new InputStreamReader(leftSwaggerInputStream, this.getSwaggerDiffOption().getCharset());
                InputStream rightSwaggerInputStream = getInputStream(rightSwaggerLocation);
                Reader rightSwaggerReader = new InputStreamReader(rightSwaggerInputStream, this.getSwaggerDiffOption().getCharset());) {
            String leftSwaggerContent = DfResourceUtil.readText(leftSwaggerReader);
            String rightSwaggerContent = DfResourceUtil.readText(rightSwaggerReader);
            return diffFromContentsInChangedOpenApi(leftSwaggerContent, rightSwaggerContent);
        } catch (IOException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to parse the swagger");
            br.addItem("leftSwaggerLocation");
            br.addElement(leftSwaggerLocation);
            br.addItem("rightSwaggerLocation");
            br.addElement(rightSwaggerLocation);
            final String msg = br.buildExceptionMessage();
            throw new LastaMetaIOException(msg, e);
        }
    }

    protected ChangedOpenApi diffFromContentsInChangedOpenApi(String leftSwaggerContent, String rightSwaggerContent) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            final String encoding = this.getSwaggerDiffOption().getCharset().name();
            {
                final String decoded = decodeContent(leftSwaggerContent, encoding);
                final JsonNode jsonNode = objectMapper.readTree(decoded);
                this.getSwaggerDiffOption().getDiffAdjustmentNode().accept("", jsonNode);
                leftSwaggerContent = objectMapper.writeValueAsString(jsonNode);
            }
            {
                final String decoded = decodeContent(rightSwaggerContent, encoding);
                final JsonNode jsonNode = objectMapper.readTree(decoded);
                this.getSwaggerDiffOption().getDiffAdjustmentNode().accept("", jsonNode);
                rightSwaggerContent = objectMapper.writeValueAsString(jsonNode);
            }
        } catch (IOException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to parse the swagger");
            br.addItem("leftSwaggerContent");
            br.addElement(leftSwaggerContent);
            br.addItem("rightSwaggerContent");
            br.addElement(rightSwaggerContent);
            final String msg = br.buildExceptionMessage();
            throw new LastaMetaIOException(msg, e);
        }

        OpenAPI leftOpenAPI = new OpenAPIParser().readContents(leftSwaggerContent, null, null).getOpenAPI();
        OpenAPI rightOpenAPI = new OpenAPIParser().readContents(rightSwaggerContent, null, null).getOpenAPI();
        ChangedOpenApi diff = OpenApiCompare.fromSpecifications(leftOpenAPI, rightOpenAPI);
        return diff;
    }

    protected String decodeContent(String swaggerContent, String encoding) throws UnsupportedEncodingException {
        // TODO awaawa fix later, about '%24' problem by jflute (2021/06/08)
        return Srl.replace(swaggerContent, "%24", "$");
        // old code: failed by regular expression keyword e.g. '%&'
        //try {
        //    return URLDecoder.decode(swaggerContent, encoding);
        //} catch (RuntimeException e) {
        //    String msg = "Failed to decode the swagger content: encoding=" + encoding + ", content=" + swaggerContent;
        //    throw new IllegalStateException(msg, e);
        //}
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected InputStream getInputStream(String location) {
        try {
            if (location.contains(":")) {
                return new URL(location).openStream();
            }
            InputStream inputStream = getClass().getResourceAsStream(location);
            if (inputStream != null) {
                return inputStream;
            }
            return new FileInputStream(location);
        } catch (IOException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to read location");
            br.addItem("location");
            br.addElement(location);
            final String msg = br.buildExceptionMessage();
            throw new LastaMetaIOException(msg, e);
        }
    }
}
