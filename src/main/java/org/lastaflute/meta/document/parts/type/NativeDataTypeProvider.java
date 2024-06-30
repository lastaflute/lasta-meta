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
package org.lastaflute.meta.document.parts.type;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;

import org.lastaflute.web.ruts.multipart.MultipartFormFile;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from ActionDocumentAnalyzer (2021/06/23 Wednesday at roppongi japanese)
 */
public class NativeDataTypeProvider {

    protected static final List<Class<?>> NATIVE_DATA_TYPE_LIST =
            Arrays.asList(void.class, boolean.class, byte.class, int.class, long.class, float.class, double.class, Void.class, Byte.class,
                    Boolean.class, Integer.class, Byte.class, Long.class, Float.class, Double.class, String.class, Map.class, byte[].class,
                    Byte[].class, Date.class, LocalDate.class, LocalDateTime.class, LocalTime.class, MultipartFormFile.class);

    public List<Class<?>> provideNativeDataTypeList() {
        return NATIVE_DATA_TYPE_LIST;
    }
}
