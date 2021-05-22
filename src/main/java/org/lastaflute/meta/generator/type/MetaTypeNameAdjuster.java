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
package org.lastaflute.meta.generator.type;

import java.lang.reflect.Type;

import org.dbflute.jdbc.Classification;
import org.dbflute.util.DfTypeUtil;
import org.dbflute.util.Srl;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from BaseDocumentGenerator (2021/01/19 Friday at roppongi japanese)
 */
public class MetaTypeNameAdjuster {

    // ===================================================================================
    //                                                                     Adjust TypeName
    //                                                                     ===============
    public String adjustTypeName(Type type) {
        return adjustTypeName(type.getTypeName());
    }

    public String adjustTypeName(String typeName) { // may be overridden
        return typeName; // no filter as default
    }

    public String adjustSimpleTypeName(Type type) {
        if (type instanceof Class<?>) {
            final Class<?> clazz = ((Class<?>) type);
            final String typeName;
            if (Classification.class.isAssignableFrom(clazz)) {
                typeName = Srl.replace(DfTypeUtil.toClassTitle(clazz), "CDef$", "CDef."); // e.g. CDef.MemberStatus
            } else {
                typeName = clazz.getSimpleName();
            }
            return typeName;
        } else {
            return adjustSimpleTypeName(adjustTypeName(type));
        }
    }

    public String adjustSimpleTypeName(String typeName) {
        return typeName.replaceAll("[a-z0-9]+\\.", ""); // e.g. org.docksidestage.Sea => Sea
    }
}
