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
package org.lastaflute.meta.swagger.spec.parts.httpmethod;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.lastaflute.meta.SwaggerOption;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/23 Wednesday at roppongi japanese)
 */
public class SwaggerSpecHttpMethodHandler {

    protected static final Pattern HTTP_METHOD_PATTERN = Pattern.compile("(.+)\\$.+");

    protected final SwaggerOption swaggerOption; // not null

    public SwaggerSpecHttpMethodHandler(SwaggerOption swaggerOption) {
        this.swaggerOption = swaggerOption;
    }

    public String extractHttpMethod(ActionDocMeta actionDocMeta) {
        final Matcher matcher = HTTP_METHOD_PATTERN.matcher(actionDocMeta.getMethodName());
        if (matcher.find()) {
            return matcher.group(1);
        }
        if (actionDocMeta.getFormTypeDocMeta() != null && !actionDocMeta.getFormTypeDocMeta().getTypeName().endsWith("Form")) {
            return "post";
        }
        return swaggerOption.getDefaultFormHttpMethod().apply(actionDocMeta);
    }
}
