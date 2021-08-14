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
package org.lastaflute.meta.swagger.spec.parts.defaultvalue;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataType;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataTypeHandler;
import org.lastaflute.meta.swagger.spec.parts.enumtype.SwaggerSpecEnumHandler;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/23 Wednesday at roppongi japanese)
 */
public class SwaggerSpecDefaultValueHandler {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final SwaggerSpecDataTypeHandler dataTypeHandler;
    protected final SwaggerSpecEnumHandler enumHandler;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecDefaultValueHandler(SwaggerSpecDataTypeHandler dataTypeHandler, SwaggerSpecEnumHandler enumHandler) {
        this.dataTypeHandler = dataTypeHandler;
        this.enumHandler = enumHandler;
    }

    // ===================================================================================
    //                                                                       Default Value
    //                                                                       =============
    public OptionalThing<Object> deriveDefaultValue(TypeDocMeta typeDocMeta) {
        final Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap = dataTypeHandler.createSwaggerDataTypeMap();
        if (swaggerDataTypeMap.containsKey(typeDocMeta.getType())) {
            SwaggerSpecDataType swaggerType = swaggerDataTypeMap.get(typeDocMeta.getType());
            Object defaultValue =
                    swaggerType.defaultValueFunction.apply(typeDocMeta, deriveDefaultValueByComment(typeDocMeta.getComment()));
            if (defaultValue != null) {
                return OptionalThing.of(defaultValue);
            }
        } else if (Iterable.class.isAssignableFrom(typeDocMeta.getType()) && typeDocMeta.getNestTypeDocMetaList().isEmpty()) {
            Object defaultValue = deriveDefaultValueByComment(typeDocMeta.getComment());
            if (!(defaultValue instanceof List)) {
                return OptionalThing.empty();
            }
            @SuppressWarnings("unchecked")
            List<Object> defaultValueList = (List<Object>) defaultValue;
            Class<?> genericType = typeDocMeta.getGenericType();
            if (genericType == null) {
                genericType = String.class;
            }
            SwaggerSpecDataType swaggerType = swaggerDataTypeMap.get(genericType);
            if (swaggerType != null) {
                return OptionalThing.of(defaultValueList.stream().map(value -> {
                    return swaggerType.defaultValueFunction.apply(typeDocMeta, value);
                }).collect(Collectors.toList()));
            }
        } else if (Enum.class.isAssignableFrom(typeDocMeta.getType())) {
            final Object defaultValue = deriveDefaultValueByComment(typeDocMeta.getComment());
            if (defaultValue != null) {
                return OptionalThing.of(defaultValue);
            } else { // use first Enum element as default
                @SuppressWarnings("unchecked")
                final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) typeDocMeta.getType();
                final List<Map<String, String>> enumMapList = enumHandler.buildEnumMapList(enumClass);
                final Optional<Object> firstEnumElement = enumMapList.stream().map(e -> (Object) e.get("code")).findFirst();
                return OptionalThing.migratedFrom(firstEnumElement, () -> {
                    throw new IllegalStateException("not found enum value.");
                });
            }
        }
        return OptionalThing.empty();
    }

    protected Object deriveDefaultValueByComment(String comment) {
        if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
            String commentWithoutLine = comment.replaceAll("\r?\n", " ");
            if (commentWithoutLine.contains(" e.g. \"")) {
                return DfStringUtil.substringFirstFront(DfStringUtil.substringFirstRear(commentWithoutLine, " e.g. \""), "\"");
            }
            if (commentWithoutLine.contains(" e.g. [")) {
                String defaultValue = DfStringUtil.substringFirstFront(DfStringUtil.substringFirstRear(commentWithoutLine, " e.g. ["), "]");
                return Arrays.stream(defaultValue.split(", *")).map(value -> {
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        return value.substring(1, value.length() - 1);
                    }
                    return "null".equals(value) ? null : value;
                }).collect(Collectors.toList());
            }
            Pattern pattern = Pattern.compile(" e\\.g\\. ([^ ]+)");
            Matcher matcher = pattern.matcher(commentWithoutLine);
            if (matcher.find()) {
                String value = matcher.group(1);
                return "null".equals(value) ? null : value;
            }
        }
        return null;
    }
}
