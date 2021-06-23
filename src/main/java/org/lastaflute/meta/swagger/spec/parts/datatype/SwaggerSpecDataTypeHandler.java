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
package org.lastaflute.meta.swagger.spec.parts.datatype;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Optional;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfTypeUtil;
import org.lastaflute.core.json.JsonMappingOption;
import org.lastaflute.core.json.annotation.JsonDatePattern;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.web.ruts.multipart.MultipartFormFile;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/23 Wednesday at roppongi japanese)
 */
public class SwaggerSpecDataTypeHandler {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final OptionalThing<JsonMappingOption> applicationJsonMappingOption; // from application, not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public SwaggerSpecDataTypeHandler(OptionalThing<JsonMappingOption> applicationJsonMappingOption) {
        this.applicationJsonMappingOption = applicationJsonMappingOption;
    }

    // ===================================================================================
    //                                                                        DataType Map
    //                                                                        ============
    public Map<Class<?>, SwaggerSpecDataType> createSwaggerDataTypeMap() {
        final Map<Class<?>, SwaggerSpecDataType> typeMap = DfCollectionUtil.newLinkedHashMap();
        typeMap.put(boolean.class, new SwaggerSpecDataType("boolean", null, (typeDocMeta, value) -> DfTypeUtil.toBoolean(value)));
        typeMap.put(byte.class, new SwaggerSpecDataType("byte", null, (typeDocMeta, value) -> DfTypeUtil.toByte(value)));
        typeMap.put(int.class, new SwaggerSpecDataType("integer", "int32", (typeDocMeta, value) -> DfTypeUtil.toInteger(value)));
        typeMap.put(long.class, new SwaggerSpecDataType("integer", "int64", (typeDocMeta, value) -> DfTypeUtil.toLong(value)));
        typeMap.put(float.class, new SwaggerSpecDataType("integer", "float", (typeDocMeta, value) -> DfTypeUtil.toFloat(value)));
        typeMap.put(double.class, new SwaggerSpecDataType("integer", "double", (typeDocMeta, value) -> DfTypeUtil.toDouble(value)));
        typeMap.put(Boolean.class, new SwaggerSpecDataType("boolean", null, (typeDocMeta, value) -> DfTypeUtil.toBoolean(value)));
        typeMap.put(Byte.class, new SwaggerSpecDataType("boolean", null, (typeDocMeta, value) -> DfTypeUtil.toByte(value)));
        typeMap.put(Integer.class, new SwaggerSpecDataType("integer", "int32", (typeDocMeta, value) -> DfTypeUtil.toInteger(value)));
        typeMap.put(Long.class, new SwaggerSpecDataType("integer", "int64", (typeDocMeta, value) -> DfTypeUtil.toLong(value)));
        typeMap.put(Float.class, new SwaggerSpecDataType("number", "float", (typeDocMeta, value) -> DfTypeUtil.toFloat(value)));
        typeMap.put(Double.class, new SwaggerSpecDataType("number", "double", (typeDocMeta, value) -> DfTypeUtil.toDouble(value)));
        typeMap.put(BigDecimal.class, new SwaggerSpecDataType("integer", "double", (typeDocMeta, value) -> DfTypeUtil.toBigDecimal(value)));
        typeMap.put(String.class, new SwaggerSpecDataType("string", null, (typeDocMeta, value) -> value));
        typeMap.put(byte[].class, new SwaggerSpecDataType("string", "byte", (typeDocMeta, value) -> value));
        typeMap.put(Byte[].class, new SwaggerSpecDataType("string", "byte", (typeDocMeta, value) -> value));
        typeMap.put(Date.class, new SwaggerSpecDataType("string", "date", (typeDocMeta, value) -> {
            return value == null ? getLocalDateFormatter(typeDocMeta).format(getDefaultLocalDate()) : value;
        }));
        typeMap.put(LocalDate.class, new SwaggerSpecDataType("string", "date", (typeDocMeta, value) -> {
            return value == null ? getLocalDateFormatter(typeDocMeta).format(getDefaultLocalDate()) : value;
        }));
        typeMap.put(LocalDateTime.class, new SwaggerSpecDataType("string", "date-time", (typeDocMeta, value) -> {
            return value == null ? getLocalDateTimeFormatter(typeDocMeta).format(getDefaultLocalDateTime()) : value;
        }));
        typeMap.put(LocalTime.class, new SwaggerSpecDataType("string", null, (typeDocMeta, value) -> {
            return value == null ? getLocalTimeFormatter(typeDocMeta).format(getDefaultLocalTime()) : value;
        }));
        typeMap.put(MultipartFormFile.class, new SwaggerSpecDataType("file", null, (typeDocMeta, value) -> value));
        return typeMap;
    }

    // ===================================================================================
    //                                                                           Date Time
    //                                                                           =========
    protected LocalDate getDefaultLocalDate() {
        return LocalDate.ofYearDay(2000, 1);
    }

    protected LocalDateTime getDefaultLocalDateTime() {
        return getDefaultLocalDate().atStartOfDay();
    }

    protected LocalTime getDefaultLocalTime() {
        return LocalTime.from(getDefaultLocalDateTime());
    }

    protected DateTimeFormatter getLocalDateFormatter(TypeDocMeta typeDocMeta) {
        Optional<DateTimeFormatter> jsonDatePatternDateTimeFormatter = getJsonDatePatternDateTimeFormatter(typeDocMeta);
        if (jsonDatePatternDateTimeFormatter.isPresent()) {
            return jsonDatePatternDateTimeFormatter.get();
        }
        return applicationJsonMappingOption.flatMap(applicationJsonMappingOption -> applicationJsonMappingOption.getLocalDateFormatter())
                .orElseGet(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd"));
    }

    protected DateTimeFormatter getLocalDateTimeFormatter(TypeDocMeta typeDocMeta) {
        Optional<DateTimeFormatter> jsonDatePatternDateTimeFormatter = getJsonDatePatternDateTimeFormatter(typeDocMeta);
        if (jsonDatePatternDateTimeFormatter.isPresent()) {
            return jsonDatePatternDateTimeFormatter.get();
        }
        return applicationJsonMappingOption
                .flatMap(applicationJsonMappingOption -> applicationJsonMappingOption.getLocalDateTimeFormatter())
                .orElseGet(() -> DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSS"));
    }

    protected DateTimeFormatter getLocalTimeFormatter(TypeDocMeta typeDocMeta) {
        Optional<DateTimeFormatter> jsonDatePatternDateTimeFormatter = getJsonDatePatternDateTimeFormatter(typeDocMeta);
        if (jsonDatePatternDateTimeFormatter.isPresent()) {
            return jsonDatePatternDateTimeFormatter.get();
        }
        return applicationJsonMappingOption.flatMap(applicationJsonMappingOption -> applicationJsonMappingOption.getLocalTimeFormatter())
                .orElseGet(() -> DateTimeFormatter.ofPattern("HH:mm:ss.SSS"));
    }

    protected Optional<DateTimeFormatter> getJsonDatePatternDateTimeFormatter(TypeDocMeta typeDocMeta) {
        return typeDocMeta.getAnnotationTypeList()
                .stream()
                .filter(annotationType -> annotationType instanceof JsonDatePattern)
                .findFirst()
                .map(jsonDatePattern -> {
                    return DateTimeFormatter.ofPattern(((JsonDatePattern) jsonDatePattern).value());
                });
    }
}
