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
package org.lastaflute.meta.swagger.spec.parts.definition;

import org.lastaflute.meta.document.docmeta.TypeDocMeta;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/25 Friday at roppongi japanese)
 */
public class SwaggerSpecDefinitionHandler {

    public String deriveDefinitionName(TypeDocMeta typeDocMeta) {
        if (typeDocMeta.getTypeName().matches("^[^<]+<(.+)>$")) {
            return typeDocMeta.getTypeName().replaceAll("^[^<]+<(.+)>$", "$1").replaceAll(" ", "");
        }
        return typeDocMeta.getTypeName().replaceAll(" ", "");
    }
}
