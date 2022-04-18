/*
 * Copyright 2015-2022 the original author or authors.
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
import org.dbflute.util.DfCollectionUtil;
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
    /**
     * @param typeDocMeta The meta of property on bean class. (NotNull)
     * @return The optional default value derived from comment. (NotNull, EmptyAllowed: e.g. not found)
     */
    public OptionalThing<Object> deriveDefaultValue(TypeDocMeta typeDocMeta) {
        final Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap = dataTypeHandler.createSwaggerDataTypeMap();
        if (swaggerDataTypeMap.containsKey(typeDocMeta.getType())) { // scalar e.g. String, Integer, LocalDate
            // e.g.
            //  /** Sea Name e.g. SeaOfDreams */ => SeaOfDreams
            //  /** Sea Name e.g. \"Sea of Dreams\"*/ => Sea of Dreams
            return doDeriveScalarDefalutValue(typeDocMeta, swaggerDataTypeMap);
        } else if (isNonNestIterable(typeDocMeta)) { // e.g. List<String>, ImmutableList<Integer> (not List<SeaResult>)
            // e.g.
            //  /** Sea List e.g. [dockside, hangar] */ => ["dockside", "hangar"]
            //  /** Sea List e.g. ["dockside", "hangar"] */ => ["dockside", "hangar"]
            return doDeriveListDefalutValue(typeDocMeta, swaggerDataTypeMap);
        } else if (isNonNestMap(typeDocMeta)) { // e.g. Map<String, String>
            // e.g.
            //  /** Sea Map e.g. {dockside:over, hangar:mystic] */ => {"dockside" = "over", "hangar" = "mystic"}
            return doDeriveMapDefalutValue(typeDocMeta, swaggerDataTypeMap);
        } else if (Enum.class.isAssignableFrom(typeDocMeta.getType())) { // e.g. CDef
            // e.g.
            //  /** Sea Status e.g. FML */ => FML
            return doDeriveEnumDefaultValue(typeDocMeta, swaggerDataTypeMap);
        }
        return OptionalThing.empty();
    }

    // -----------------------------------------------------
    //                                                Scalar
    //                                                ------
    protected OptionalThing<Object> doDeriveScalarDefalutValue(TypeDocMeta typeDocMeta,
            Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap) {
        final SwaggerSpecDataType swaggerType = swaggerDataTypeMap.get(typeDocMeta.getType());
        final Object extracted = extractDefaultValueFromComment(typeDocMeta.getComment());
        final Object defaultValue = swaggerType.defaultValueFunction.apply(typeDocMeta, extracted);
        return OptionalThing.ofNullable(defaultValue, () -> {
            throw new IllegalStateException("Not found the default value: " + typeDocMeta);
        });
    }

    // -----------------------------------------------------
    //                                              Iterable
    //                                              --------
    protected boolean isNonNestIterable(TypeDocMeta typeDocMeta) {
        return Iterable.class.isAssignableFrom(typeDocMeta.getType()) && typeDocMeta.getNestTypeDocMetaList().isEmpty();
    }

    protected OptionalThing<Object> doDeriveListDefalutValue(TypeDocMeta typeDocMeta,
            Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap) {
        final Object defaultValue = extractDefaultValueFromComment(typeDocMeta.getComment());
        if (!(defaultValue instanceof List)) {
            return OptionalThing.empty();
        }
        @SuppressWarnings("unchecked")
        final List<Object> defaultValueList = (List<Object>) defaultValue;
        Class<?> genericType = typeDocMeta.getGenericType();
        if (genericType == null) {
            genericType = String.class;
        }
        final SwaggerSpecDataType swaggerType = swaggerDataTypeMap.get(genericType);
        if (swaggerType != null) {
            return OptionalThing.of(defaultValueList.stream().map(value -> {
                return swaggerType.defaultValueFunction.apply(typeDocMeta, value);
            }).collect(Collectors.toList()));
        }
        return OptionalThing.empty();
    }

    // -----------------------------------------------------
    //                                                  Map
    //                                                 -----
    protected boolean isNonNestMap(TypeDocMeta typeDocMeta) {
        return Map.class.isAssignableFrom(typeDocMeta.getType()) && typeDocMeta.getNestTypeDocMetaList().isEmpty();
    }

    protected OptionalThing<Object> doDeriveMapDefalutValue(TypeDocMeta typeDocMeta,
            Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap) {
        // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
        // contributed by U-NEXT, thanks.
        // for now, migrated as plain logic first by jflute (2022/04/18)
        // _/_/_/_/_/_/_/_/_/_/

        // java.util.Map<String, String> convert to [String, String]
        final String[] keyValueArray = extractMapGenericTypeAsArray(typeDocMeta); // e.g. [String, String]
        if (keyValueArray.length <= 1) { // non generic?
            return OptionalThing.empty();
        }
        final String keyTypeName = keyValueArray[0].trim();
        final String valueTypeName = keyValueArray[1].trim();
        return OptionalThing.ofNullable(deriveMapDefaultValueByComment(typeDocMeta.getComment(), keyTypeName, valueTypeName), () -> {
            throw new IllegalStateException("not found default value.");
        });
    }

    protected String[] extractMapGenericTypeAsArray(TypeDocMeta typeDocMeta) {
        // java.util.Map<String, String> convert to [String, String]
        return DfStringUtil.substringFirstFront(DfStringUtil.substringFirstRear(typeDocMeta.getTypeName(), "<"), ">").split(",");
    }

    protected Object deriveMapDefaultValueByComment(String comment, String keyTypeName, String valueTypeName) {
        if (DfStringUtil.is_Null_or_Empty(comment)) {
            return null;
        }
        final String egMark = "e.g. {";
        if (!comment.contains(egMark)) {
            return null;
        }
        final String defaultValue = DfStringUtil.substringFirstFront(DfStringUtil.substringFirstRear(comment, egMark), "}");
        Map<Object, Object> map = DfCollectionUtil.newHashMap();
        for (String keyValueEntry : defaultValue.split(",")) {
            String[] entry = keyValueEntry.split(":");
            Object key = entry[0];
            Object value = entry[1];
            if ("String".equals(keyTypeName)) {
                key = egStringToJavaString(key.toString().trim());
            }
            if ("String".equals(valueTypeName)) {
                value = egStringToJavaString(value.toString().trim());
            }
            map.put(key, value);
        }
        return map;
    }

    private String egStringToJavaString(String egString) {
        if (egString.startsWith("\"") && egString.endsWith("\"")) {
            return egString.substring(1, egString.length() - 1);
        }
        return egString;
    }

    // -----------------------------------------------------
    //                                                 Enum
    //                                                ------
    protected OptionalThing<Object> doDeriveEnumDefaultValue(TypeDocMeta typeDocMeta,
            Map<Class<?>, SwaggerSpecDataType> swaggerDataTypeMap) {
        final Object defaultValue = extractDefaultValueFromComment(typeDocMeta.getComment());
        if (defaultValue != null) {
            return OptionalThing.of(defaultValue);
        }
        // use first Enum element as default
        @SuppressWarnings("unchecked")
        final Class<? extends Enum<?>> enumClass = (Class<? extends Enum<?>>) typeDocMeta.getType();
        final List<Map<String, String>> enumMapList = enumHandler.buildEnumMapList(enumClass);
        final Optional<Object> firstEnumElement = enumMapList.stream().map(e -> (Object) e.get("code")).findFirst();
        return OptionalThing.migratedFrom(firstEnumElement, () -> {
            throw new IllegalStateException("not found enum value.");
        });
    }

    // ===================================================================================
    //                                                                   Extract egDefault
    //                                                                   =================
    /**
     * Extract default value from comment plainly in spite of property type. 
     * @param comment The plain comment on property JavaDoc, may contain default value. (NullAllowed)
     * @return The extracted default value from the comment simply. (NullAllowed: null comment or "null")
     */
    protected Object extractDefaultValueFromComment(String comment) {
        if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
            final String commentWithoutLine = comment.replaceAll("\r?\n", " ");
            if (commentWithoutLine.contains(" e.g. \"")) {
                return DfStringUtil.substringFirstFront(DfStringUtil.substringFirstRear(commentWithoutLine, " e.g. \""), "\"");
            }
            if (commentWithoutLine.contains(" e.g. [")) {
                final String defaultValue =
                        DfStringUtil.substringFirstFront(DfStringUtil.substringFirstRear(commentWithoutLine, " e.g. ["), "]");
                return Arrays.stream(defaultValue.split(", *")).map(value -> {
                    if (value.startsWith("\"") && value.endsWith("\"")) {
                        return value.substring(1, value.length() - 1);
                    }
                    return "null".equals(value) ? null : value;
                }).collect(Collectors.toList());
            }
            final Pattern pattern = Pattern.compile(" e\\.g\\. ([^ ]+)");
            final Matcher matcher = pattern.matcher(commentWithoutLine);
            if (matcher.find()) {
                final String value = matcher.group(1);
                return "null".equals(value) ? null : value;
            }
        }
        return null;
    }
}
