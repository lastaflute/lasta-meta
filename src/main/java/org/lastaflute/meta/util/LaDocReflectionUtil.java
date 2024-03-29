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
package org.lastaflute.meta.util;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.2.3 (2017/04/20 Thursday)
 */
public class LaDocReflectionUtil {

    // e.g. (actually FQCN)
    //  JsonResponse<List<String>>, 1 to String
    //  JsonResponse<List<SeaLandPiari>>. 1 to SeaLandPiari
    //  JsonResponse<List<Sea<Land>>>, 1 to Sea
    //  JsonResponse<List<Map<String, Object>>>, 1 to Map
    // #hope p1us2er0 Refactor. (2017/10/13)
    public static Class<?> extractElementType(Type type, int depth) {
        if (type instanceof ParameterizedType) {
            Type actualType = ((ParameterizedType) type).getActualTypeArguments()[0];
            if (depth == 0) {
                if (actualType instanceof ParameterizedType) {
                    return (Class<?>) ((ParameterizedType) actualType).getRawType();
                }
                if (actualType instanceof Class<?>) {
                    return (Class<?>) actualType;
                }
                if (actualType instanceof TypeVariable) {
                    return (Class<?>) ((TypeVariable<?>) actualType).getBounds()[0];
                }
                // WildcardType etc.
                return Object.class;
            }
            return (Class<?>) extractElementType(actualType, --depth);
        }
        if (depth != 0) {
            throw new IllegalArgumentException("depth=" + depth);
        }
        return (Class<?>) type;
    }
}
