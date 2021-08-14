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
package org.lastaflute.meta.swagger.spec.parts.property;

import java.util.List;
import java.util.stream.Collectors;

import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.swagger.spec.parts.annotation.SwaggerSpecAnnotationHandler;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/25 Friday at roppongi japanese)
 */
public class SwaggerSpecPropertyHandler {

    protected final SwaggerSpecAnnotationHandler annotationHandler;

    public SwaggerSpecPropertyHandler(SwaggerSpecAnnotationHandler annotationHandler) {
        this.annotationHandler = annotationHandler;
    }

    public List<String> deriveRequiredPropertyNameList(TypeDocMeta typeDocMeta) {
        return typeDocMeta.getNestTypeDocMetaList().stream().filter(nesttypeDocMeta -> {
            return nesttypeDocMeta.getAnnotationTypeList().stream().anyMatch(annotationType -> {
                return annotationHandler.getRequiredAnnotationList()
                        .stream()
                        .anyMatch(requiredAnnotation -> requiredAnnotation.isAssignableFrom(annotationType.getClass()));
            });
        }).map(nesttypeDocMeta -> nesttypeDocMeta.getPublicName()).collect(Collectors.toList());
    }
}
