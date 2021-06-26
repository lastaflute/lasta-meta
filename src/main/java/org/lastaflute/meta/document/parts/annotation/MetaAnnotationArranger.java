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
package org.lastaflute.meta.document.parts.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.stream.Collector;
import java.util.stream.Collectors;

import org.dbflute.util.DfReflectionUtil;
import org.lastaflute.meta.document.parts.type.MetaTypeNameAdjuster;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from BaseDocumentGenerator (2021/01/19 Friday at roppongi japanese)
 */
public class MetaAnnotationArranger {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    private final MetaTypeNameAdjuster metaTypeNameAdjuster;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public MetaAnnotationArranger(MetaTypeNameAdjuster metaTypeNameAdjuster) {
        this.metaTypeNameAdjuster = metaTypeNameAdjuster;
    }

    // ===================================================================================
    //                                                                             Arrange
    //                                                                             =======
    public List<String> arrangeAnnotationList(List<Annotation> annotationList) {
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // e.g.
        //  public @interface SeaPark {
        //      String dockside() default "over";
        //      String hangar() default "mystic";
        //  }
        //
        //  @SeaPark(hangar="shadow")
        //  public String maihama;
        // _/_/_/_/_/_/_/_/_/_/

        // no re-order for specifed annotation list, order responsibility are on extractor as Lasta Meta policy 
        return annotationList.stream().map(annotation -> {
            // annotation attributes are treated as methods in reflection world 
            final Map<String, Object> methodMap = extractAnnotationMethodMap(annotation); // means attributes
            final String typeName = adjustSimpleTypeName(annotation.annotationType()); // e.g. SeaPark
            if (methodMap.isEmpty()) { // no attribute
                return typeName; // e.g. SeaPark
            }
            return connectTypeAndAttribute(typeName, methodMap); // e.g. "SeaPark{dockside=over, hangar=mystic}"
        }).collect(Collectors.toList());
    }

    protected Map<String, Object> extractAnnotationMethodMap(Annotation annotation) {
        // you can get method of concrete annotation by getDeclaredMethods()
        return Arrays.stream(annotation.annotationType().getDeclaredMethods())
                .filter(method -> isValueSpecifiedAttribute(annotation, method))
                .collect(toMap(annotation));
    }

    protected boolean isValueSpecifiedAttribute(Annotation annotation, Method method) {
        final Object value = DfReflectionUtil.invoke(method, annotation, (Object[]) null); // e.g. shadow (of hangar)
        final Object defaultValue = method.getDefaultValue(); // e.g. mystic (of hangar)
        if (isAttributeValueSimpleDefault(value, defaultValue)) { // means non-specified attribute
            return false;
        }
        if (isArrayAttributeValueDefault(method, value, defaultValue)) { // means non-specified attribute
            return false;
        }
        return true; // specified attributes only here
    }

    protected boolean isAttributeValueSimpleDefault(Object value, Object defaultValue) {
        return Objects.equals(value, defaultValue);
    }

    protected boolean isArrayAttributeValueDefault(Method method, Object value, Object defaultValue) {
        return method.getReturnType().isArray() && Arrays.equals((Object[]) value, (Object[]) defaultValue);
    }

    protected Collector<Method, ?, TreeMap<String, Object>> toMap(Annotation annotation) {
        return Collectors.toMap(method -> method.getName(), method -> {
            return extractAttributeValue(annotation, method);
        }, (u, v) -> v, TreeMap::new); // should be ordered to suppress differences between executions
    }

    protected Object extractAttributeValue(Annotation annotation, Method method) {
        Object data = DfReflectionUtil.invoke(method, annotation, (Object[]) null); // e.g. shadow (of hangar)
        if (data != null && data.getClass().isArray()) {
            // convert array to list and treat classes as simple names
            final List<?> dataList = Arrays.asList((Object[]) data);
            if (dataList.isEmpty()) {
                return "";
            }
            data = dataList.stream().map(element -> {
                return element instanceof Class<?> ? adjustSimpleTypeName(((Class<?>) element)) : element;
            }).collect(Collectors.toList());
        }
        return data;
    }

    protected String connectTypeAndAttribute(String typeName, Map<String, Object> methodMap) {
        return typeName + methodMap; // e.g. "SeaPark{dockside=over, hangar=mystic}"
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
}
