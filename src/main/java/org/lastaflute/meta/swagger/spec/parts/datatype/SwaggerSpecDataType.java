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
package org.lastaflute.meta.swagger.spec.parts.datatype;

import java.util.function.BiFunction;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.exception.SwaggerDefaultValueTypeConversionFailureException;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from SwaggerGenerator (2021/06/23 Wednesday at roppongi japanese)
 */
public class SwaggerSpecDataType {

    public final String type;
    public final String format;
    public final BiFunction<TypeDocMeta, Object, Object> defaultValueFunction;

    public SwaggerSpecDataType(String type, String format, BiFunction<TypeDocMeta, Object, Object> defaultValueFunction) {
        this.type = type;
        this.format = format;
        this.defaultValueFunction = (typeDocMeta, value) -> {
            try {
                return defaultValueFunction.apply(typeDocMeta, value);
            } catch (RuntimeException e) {
                final String msg = buildDefaultValueTypeConversionFailureMessage(typeDocMeta, value);
                throw new SwaggerDefaultValueTypeConversionFailureException(msg, e);
            }
        };
    }

    protected String buildDefaultValueTypeConversionFailureMessage(TypeDocMeta typeDocMeta, Object value) {
        final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
        br.addNotice("Cannot convert the \"e.g. default value\" to property type");
        br.addItem("Advice");
        br.addElement("Make sure your \"e.g. default value\" in javadoc's comment.");
        br.addElement("Mismatched type? or Broken expression?");
        br.addElement("For example:");
        br.addElement("  (x): (mismatched type)");
        br.addElement("    /** Sea Count e.g. over */ *Bad");
        br.addElement("    public Integer seaCount;");
        br.addElement("  (o):");
        br.addElement("    /** Sea Count e.g. 1 */ OK");
        br.addElement("    public Integer seaCount;");
        br.addElement("");
        br.addElement("  (x): (broken expression)");
        br.addElement("    /** Sea Date e.g. 2022@04-18 */ *Bad");
        br.addElement("    public LocalDate seaDate;");
        br.addElement("  (o):");
        br.addElement("    /** Sea Date e.g. 2022-04-18 */ OK");
        br.addElement("    public LocalDate seaDate;");
        br.addItem("typeDocMeta");
        br.addElement(typeDocMeta);
        br.addItem("Property Name");
        br.addElement(typeDocMeta.getName());
        br.addItem("Property Type");
        br.addElement(typeDocMeta.getType());
        final String simpleTypeName = typeDocMeta.getSimpleTypeName();
        if (simpleTypeName != null) {
            br.addElement("(type name: " + simpleTypeName + ")");
        }
        br.addItem("Javadoc");
        br.addElement(typeDocMeta.getComment());
        br.addItem("Default Value");
        br.addElement(value);
        return br.buildExceptionMessage();
    }
}
