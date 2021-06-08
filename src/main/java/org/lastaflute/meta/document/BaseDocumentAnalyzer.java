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
package org.lastaflute.meta.document;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;

import org.lastaflute.meta.document.annotation.MetaAnnotationArranger;
import org.lastaflute.meta.document.type.MetaTypeNameAdjuster;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.6.9 (2075/03/05 Sunday) // what? by jflute
 */
public class BaseDocumentAnalyzer {

    private final MetaTypeNameAdjuster metaTypeNameAdjuster = newMetaTypeNameAdjuster();

    protected MetaTypeNameAdjuster newMetaTypeNameAdjuster() {
        return new MetaTypeNameAdjuster();
    }

    private final MetaAnnotationArranger metaAnnotationArranger = newMetaAnnotationArranger();

    protected MetaAnnotationArranger newMetaAnnotationArranger() {
        return new MetaAnnotationArranger(metaTypeNameAdjuster);
    }

    // ===================================================================================
    //                                                                     Adjust TypeName
    //                                                                     ===============
    protected String adjustTypeName(Type type) {
        return metaTypeNameAdjuster.adjustTypeName(type);
    }

    protected String adjustTypeName(String typeName) {
        return metaTypeNameAdjuster.adjustTypeName(typeName);
    }

    protected String adjustSimpleTypeName(Type type) {
        return metaTypeNameAdjuster.adjustSimpleTypeName(type);
    }

    protected String adjustSimpleTypeName(String typeName) {
        return metaTypeNameAdjuster.adjustSimpleTypeName(typeName);
    }

    // ===================================================================================
    //                                                                          Annotation
    //                                                                          ==========
    protected List<String> arrangeAnnotationList(List<Annotation> annotationList) {
        return metaAnnotationArranger.arrangeAnnotationList(annotationList);
    }
}
