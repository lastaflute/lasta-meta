/*
 * Copyright 2015-2024 the original author or authors.
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
package org.lastaflute.meta.agent;

import org.dbflute.optional.OptionalThing;
import org.lastaflute.meta.infra.maven.MavenVersionFinder;
import org.lastaflute.web.response.HtmlResponse;
import org.lastaflute.web.servlet.request.RequestManager;

/**
 * It means SwaggerUIAgent but no rename to keep auto-import compatibility.
 * @author p1us2er0
 * @author jflute
 * @since 0.2.3 (2017/04/20 Thursday)
 */
public class SwaggerAgent { // used by application's SwaggerAction

    protected final RequestManager requestManager;

    public SwaggerAgent(RequestManager requestManager) {
        this.requestManager = requestManager;
    }

    public HtmlResponse prepareSwaggerUiResponse(String swaggerJsonUrl) {
        return toHtmlResponse(buildSwaggerUiUrl(swaggerJsonUrl));
    }

    public String buildSwaggerUiUrl(String swaggerJsonUrl) {
        final String requestUrl = requestManager.getContextPath() + swaggerJsonUrl;
        return findSwaggerUiVersion().map(version -> {
            return String.format("../webjars/swagger-ui/%s/index.html?validatorUrl=&url=%s", version, requestUrl);
        }).orElseTranslatingThrow(cause -> {
            return new IllegalStateException("Not found the Swagger UI dependency in your classpath.", cause);
        });
    }

    protected OptionalThing<String> findSwaggerUiVersion() {
        return createMavenVersionFinder().findVersion("org.webjars", "swagger-ui");
    }

    protected MavenVersionFinder createMavenVersionFinder() {
        return new MavenVersionFinder();
    }

    protected HtmlResponse toHtmlResponse(String swaggerUiUrl) {
        // swagger-ui's index.html defines css, javascript links as relative path so needs to redirect
        // (needs deep adjustment if forward: but prefer forward for url on address bar)
        return HtmlResponse.fromRedirectPath(swaggerUiUrl);
    }
}
