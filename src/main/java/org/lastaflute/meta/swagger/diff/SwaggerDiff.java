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
package org.lastaflute.meta.swagger.diff;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.function.Consumer;
import java.util.function.Function;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
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
public class SwaggerDiff {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final SwaggerDiffOption swaggerDiffOption; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerDiff() {
        this(op -> {});
    }

    public SwaggerDiff(Consumer<SwaggerDiffOption> opLambda) {
        this.swaggerDiffOption = this.createSwaggerDiffOption(opLambda);
    }

    protected SwaggerDiffOption createSwaggerDiffOption(Consumer<SwaggerDiffOption> opLambda) {
        final SwaggerDiffOption swaggerDiffOption = new SwaggerDiffOption();
        opLambda.accept(swaggerDiffOption);
        return swaggerDiffOption;
    }

    // ===================================================================================
    //                                                                                Diff
    //                                                                                ====
    public String diffFromLocations(String leftSwaggerLocation, String rightSwaggerLocation) {
        try {
            final ChangedOpenApi changedOpenApi = diffFromLocationsInChangedOpenApi(leftSwaggerLocation, rightSwaggerLocation);
            return swaggerDiffOption.getDiffResultRender().render(changedOpenApi);
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
        String value = swaggerDiffOption.getDiffResultRender().render(changedOpenApi);
        return value;
    }

    // ===================================================================================
    //                                                                Diff(ChangedOpenApi)
    //                                                                ====================
    protected ChangedOpenApi diffFromLocationsInChangedOpenApi(String leftSwaggerLocation, String rightSwaggerLocation) {
        final Charset charset = getSwaggerDiffOption().getSwaggerContentCharset();
        try (InputStream leftIns = getInputStream(leftSwaggerLocation);
                Reader leftReader = new InputStreamReader(leftIns, charset);
                InputStream rightIns = getInputStream(rightSwaggerLocation);
                Reader rightReader = new InputStreamReader(rightIns, charset);) {
            final String leftSwaggerContent = DfResourceUtil.readText(leftReader);
            final String rightSwaggerContent = DfResourceUtil.readText(rightReader);
            return diffFromContentsInChangedOpenApi(leftSwaggerContent, rightSwaggerContent);
        } catch (IOException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Failed to read the swagger file.");
            br.addItem("leftSwaggerLocation");
            br.addElement(leftSwaggerLocation);
            br.addItem("rightSwaggerLocation");
            br.addElement(rightSwaggerLocation);
            final String msg = br.buildExceptionMessage();
            throw new LastaMetaIOException(msg, e);
        }
    }

    protected ChangedOpenApi diffFromContentsInChangedOpenApi(String leftSwaggerContent, String rightSwaggerContent) {
        // parsed content
        final String leftParsedContent = prepareParsedContent(leftSwaggerContent, swaggerDiffOption.getLeftContentFilter());
        final String rightParsedContent = prepareParsedContent(rightSwaggerContent, swaggerDiffOption.getRightContentFilter());

        // parse/compare
        final OpenAPI leftOpenAPI = parseOpenApiContent(leftParsedContent);
        final OpenAPI rightOpenAPI = parseOpenApiContent(rightParsedContent);
        return compareOpenAPILeftRight(leftOpenAPI, rightOpenAPI);
    }

    // -----------------------------------------------------
    //                                        Parsed Content
    //                                        --------------
    protected String prepareParsedContent(String swaggerContent, OptionalThing<Function<String, String>> contentFilter) {
        final String resolvedContent = resolveSwaggerContentNode(swaggerContent);
        return contentFilter.map(filter -> filter.apply(resolvedContent)).orElse(resolvedContent);
    }

    protected String resolveSwaggerContentNode(String swaggerContent) {
        try {
            final String encoding = getSwaggerDiffOption().getSwaggerContentCharset().name();
            final String decoded = decodeContent(swaggerContent, encoding);
            final ObjectMapper objectMapper = new ObjectMapper();
            final JsonNode jsonNode = objectMapper.readTree(decoded);
            getSwaggerDiffOption().getDiffAdjustmentNode().accept("", jsonNode);
            return objectMapper.writeValueAsString(jsonNode);
        } catch (IOException e) {
            throwSwaggerDiffContentReadIOException(swaggerContent, e);
            return null; // unreachable
        }
    }

    protected String decodeContent(String swaggerContent, String encoding) throws UnsupportedEncodingException {
        // TODO awaawa fix later, about '%24' problem by jflute (2021/06/08)
        String filtered = swaggerContent;
        filtered = Srl.replace(filtered, "%24", "$");
        filtered = Srl.replace(filtered, "%3C", "<");
        filtered = Srl.replace(filtered, "%3E", ">");
        filtered = Srl.replace(filtered, "%40", "@");
        return filtered;
        // old code: failed by regular expression keyword e.g. '%&'
        //try {
        //    return URLDecoder.decode(swaggerContent, encoding);
        //} catch (RuntimeException e) {
        //    String msg = "Failed to decode the swagger content: encoding=" + encoding + ", content=" + swaggerContent;
        //    throw new IllegalStateException(msg, e);
        //}
    }

    protected void throwSwaggerDiffContentReadIOException(String swaggerContent, IOException e) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Failed to parse the swagger");
        br.addItem("swaggerContent");
        br.addElement(swaggerContent);
        final String msg = br.buildExceptionMessage();
        throw new LastaMetaIOException(msg, e);
    }

    // -----------------------------------------------------
    //                                         Parse/Compare
    //                                         -------------
    protected OpenAPI parseOpenApiContent(final String leftParsedContent) {
        return new OpenAPIParser().readContents(leftParsedContent, null, null).getOpenAPI();
    }

    protected ChangedOpenApi compareOpenAPILeftRight(OpenAPI leftOpenAPI, OpenAPI rightOpenAPI) {
        return OpenApiCompare.fromSpecifications(leftOpenAPI, rightOpenAPI);
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    protected InputStream getInputStream(String location) {
        try {
            if (location.contains(":")) {
                return new URL(location).openStream();
            }
            final InputStream inputStream = getClass().getResourceAsStream(location);
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

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    protected SwaggerDiffOption getSwaggerDiffOption() {
        return this.swaggerDiffOption;
    }
}
