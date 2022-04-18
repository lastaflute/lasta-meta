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

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.dbflute.optional.OptionalThing;
import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.core.json.control.JsonControlMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.exception.SwaggerDefaultValueParseFailureException;
import org.lastaflute.meta.swagger.spec.parts.datatype.SwaggerSpecDataTypeHandler;
import org.lastaflute.meta.swagger.spec.parts.enumtype.SwaggerSpecEnumHandler;
import org.lastaflute.meta.unit.mock.dbflute.MockCDef;

/**
 * @author jflute
 * @since 0.5.4 (2022/04/18 Monday at roppongi japanese)
 */
public class SwaggerSpecDefaultValueHandlerTest extends PlainTestCase {

    // ===================================================================================
    //                                                                              String
    //                                                                              ======
    public void test_deriveDefaultValue_string_basic() {
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. SeaOfDreams")).get());
        assertEquals("1", handler().deriveDefaultValue(stringMeta("Sea Name e.g. 1")).get());
        assertEquals("1.2", handler().deriveDefaultValue(stringMeta("Sea Name e.g. 1.2")).get());

        // #hope jflute allow this case for easy comment (2022/04/18)
        assertFalse(handler().deriveDefaultValue(stringMeta("e.g. Sea of Dreams")).isPresent());
    }

    public void test_deriveDefaultValue_string_space() {
        // #thinking jflute non quoted value may be treated as value until comment end... (2022/04/18)
        assertEquals("Sea", handler().deriveDefaultValue(stringMeta("Sea Name e.g. Sea of Dreams")).get());

        // #hope jflute too strict so allow these cases for easy comment (2022/04/18)
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.  Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.  \"Sea of Dreams\"")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Namee.g. Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Namee.g.Sea of Dreams")).isPresent());
    }

    public void test_deriveDefaultValue_string_quoted() {
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Sea of Dreams\"")).get());
        assertEquals("Sea of Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Sea of Dreams")).get());
        assertEquals("Sea", handler().deriveDefaultValue(stringMeta("Sea Name e.g. Sea of Dreams\"")).get());
        assertEquals("Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Dreams\"")).get());
        assertEquals("Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Dreams")).get());
        assertEquals("Dreams\"", handler().deriveDefaultValue(stringMeta("Sea Name e.g. Dreams\"")).get());
        assertEquals("Sea", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Sea\" of Dreams")).get());
    }

    public void test_deriveDefaultValue_string_none() {
        assertFalse(handler().deriveDefaultValue(stringMeta("e.g. Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g. null")).isPresent());
        assertEquals("NULL", handler().deriveDefaultValue(stringMeta("Sea Name e.g. NULL")).get());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.gSea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.\"Sea of Dreams\"")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.  Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.  \"Sea of Dreams\"")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Namee.g. Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name eg. Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name eg Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g . Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e. g. Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e .g. Sea of Dreams")).isPresent());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.\tSeaOfDreams")).isPresent());
        assertEquals("SeaOfDreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g.\nSeaOfDreams")).get());
        assertFalse(handler().deriveDefaultValue(stringMeta("Sea Name e.g.\n\nSeaOfDreams")).isPresent());
    }

    public void test_deriveDefaultValue_string_various() {
        assertEquals("Sea.of.Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. Sea.of.Dreams")).get());
        assertEquals("Sea.e.g.Dreams", handler().deriveDefaultValue(stringMeta("Sea Name e.g. \"Sea.e.g.Dreams\"")).get());
        assertEquals(Arrays.asList("Sea of Dreams"), handler().deriveDefaultValue(stringMeta("Sea Name e.g. [Sea of Dreams]")).get());
        assertEquals("{Sea", handler().deriveDefaultValue(stringMeta("Sea Name e.g. {Sea of Dreams}")).get());
    }

    private TypeDocMeta stringMeta(String comment) {
        return createMeta("seaName", String.class, null, comment);
    }

    // ===================================================================================
    //                                                                              Number
    //                                                                              ======
    public void test_deriveDefaultValue_number_basic() {
        assertEquals(1, handler().deriveDefaultValue(integerMeta("Sea Count e.g. 1")).get());
        assertEquals(123456789, handler().deriveDefaultValue(integerMeta("Sea Count e.g. 123456789")).get());
        assertEquals(1234567890123456L, handler().deriveDefaultValue(longMeta("Sea Count e.g. 1234567890123456")).get());
        assertEquals(BigDecimal.ONE, handler().deriveDefaultValue(decimalMeta("Sea Count e.g. 1")).get());
        assertEquals(new BigDecimal("1.2"), handler().deriveDefaultValue(decimalMeta("Sea Count e.g. 1.2")).get());

        // none
        assertException(SwaggerDefaultValueParseFailureException.class, () -> {
            handler().deriveDefaultValue(integerMeta("Sea Count e.g. 1.0")); // integer is not decimal
        });
        assertException(SwaggerDefaultValueParseFailureException.class, () -> {
            handler().deriveDefaultValue(integerMeta("Sea Count e.g. mystic"));
        });
        assertException(SwaggerDefaultValueParseFailureException.class, () -> {
            handler().deriveDefaultValue(integerMeta("Sea Count e.g. 1234567890123456")); // too big for integer
        });
    }

    private TypeDocMeta integerMeta(String comment) {
        return createMeta("seaCount", Integer.class, null, comment);
    }

    private TypeDocMeta longMeta(String comment) {
        return createMeta("seaMoney", Long.class, null, comment);
    }

    private TypeDocMeta decimalMeta(String comment) {
        return createMeta("seaPercent", BigDecimal.class, null, comment);
    }

    // ===================================================================================
    //                                                                               Date
    //                                                                              ======
    // scalar types depend on SwaggerSpecDataTypeHandler so enough here

    // ===================================================================================
    //                                                                            Iterable
    //                                                                            ========
    public void test_deriveDefaultValue_iterable_basic() {
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [dockside, hangar]")).get());
        assertEquals(Arrays.asList("mystic"), handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [mystic]")).get());
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"dockside\", \"hangar\"]")).get());
        assertEquals(Arrays.asList("mystic"), handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"mystic\"]")).get());

        // comma space
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"dockside\",\"hangar\"]")).get());
        assertFalse(handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g.[\"dockside\", \"hangar\"]")).isPresent());

        // value space
        assertEquals(Arrays.asList("dock side", "han gar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"dock side\", \"han gar\"]")).get());
        assertEquals(Arrays.asList("dock side", "han gar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [dock side, han gar]")).get());
        assertEquals(Arrays.asList("\"dockside", "hangar\""), // comma is prior
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"dockside, hangar\"]")).get());

        // number
        assertEquals(Arrays.asList(1, 2), handler().deriveDefaultValue(listMeta(Integer.class, "Sea List e.g. [1, 2]")).get());
        assertEquals(Arrays.asList(1, 2), handler().deriveDefaultValue(listMeta(Integer.class, "Sea List e.g. [\"1\", \"2\"]")).get());

        assertException(SwaggerDefaultValueParseFailureException.class, () -> {
            handler().deriveDefaultValue(listMeta(Integer.class, "Sea List e.g. [\"dockside\", \"hangar\"]"));
        });
        assertException(SwaggerDefaultValueParseFailureException.class, () -> {
            handler().deriveDefaultValue(listMeta(Integer.class, "Sea List e.g. [\"1\", \"hangar\"]"));
        });

        // non generic (treated as string)
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(null, "Sea List e.g. [\"dockside\", \"hangar\"]")).get());

        // various
        assertFalse(handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. \"dockside\", \"hangar\"")).isPresent());
        assertEquals(Arrays.asList("dockside", "hangar"),
                handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. [\"dockside\", \"hangar\"")).get());
        assertFalse(handler().deriveDefaultValue(listMeta(String.class, "Sea List e.g. \"dockside\", \"hangar\"]")).isPresent());

        // non
        assertFalse(handler().deriveDefaultValue(listMeta(String.class, "Sea List")).isPresent());
    }

    private TypeDocMeta listMeta(Class<?> genericType, String comment) {
        return createMeta("seaList", List.class, genericType, comment);
    }

    // ===================================================================================
    //                                                                                Map
    //                                                                               =====
    public void test_deriveDefaultValue_map_basic() { // unsupported until 0.5.3
        String basicComment = "Sea Map e.g. {\"dockside\" = \"over\", \"hangar\" = \"mystic\"}";
        assertFalse(handler().deriveDefaultValue(mapMeta(basicComment)).isPresent());
    }

    private TypeDocMeta mapMeta(String comment) {
        return createMeta("seaMap", Map.class, null, comment);
    }

    // ===================================================================================
    //                                                                               Enum
    //                                                                              ======
    public void test_deriveDefaultValue_enum_basic() {
        assertEquals("FML", handler().deriveDefaultValue(statusMeta("Sea Status e.g. FML")).get());
        assertEquals("FML", handler().deriveDefaultValue(statusMeta("Sea Status e.g. \"FML\"")).get());
        assertEquals("Formalized", handler().deriveDefaultValue(statusMeta("Sea Status e.g. \"Formalized\"")).get());
        assertEquals("PRV", handler().deriveDefaultValue(statusMeta("Sea Status e.g. PRV")).get());
        assertEquals("NON", handler().deriveDefaultValue(statusMeta("Sea Status e.g. \"NON\"")).get());

        // none (first element of enum)
        assertEquals("FML", handler().deriveDefaultValue(statusMeta("Sea Status")).get());
        assertEquals("DOCKSIDE", handler().deriveDefaultValue(enumMeta(TestBasicEnum.class, "Seanum")).get());
        assertFalse(handler().deriveDefaultValue(enumMeta(TestEmptyEnum.class, "Seanum")).isPresent());
    }

    private TypeDocMeta statusMeta(String comment) {
        return createMeta("seaType", MockCDef.MemberStatus.class, null, comment);
    }

    private TypeDocMeta enumMeta(Class<?> enumType, String comment) {
        return createMeta("seaNum", enumType, null, comment);
    }

    private enum TestBasicEnum {
        DOCKSIDE, HANGAR;
    }

    private enum TestEmptyEnum {
        ;
    }

    // ===================================================================================
    //                                                                        Small Helper
    //                                                                        ============
    private SwaggerSpecDefaultValueHandler handler() {
        JsonControlMeta appJsonControlMeta = new JsonControlMeta(OptionalThing.empty(), OptionalThing.empty());
        SwaggerSpecDataTypeHandler dataTypeHandler = new SwaggerSpecDataTypeHandler(appJsonControlMeta);
        SwaggerSpecEnumHandler enumHandler = new SwaggerSpecEnumHandler();
        return new SwaggerSpecDefaultValueHandler(dataTypeHandler, enumHandler);
    }

    private TypeDocMeta createMeta(String name, Class<?> type, Class<?> genericType, String comment) {
        // only used in the handler (minimum set)
        TypeDocMeta typeDocMeta = new TypeDocMeta();
        typeDocMeta.setName(name);
        typeDocMeta.setType(type);
        typeDocMeta.setGenericType(genericType);
        typeDocMeta.setComment(comment);
        return typeDocMeta;
    }
}
