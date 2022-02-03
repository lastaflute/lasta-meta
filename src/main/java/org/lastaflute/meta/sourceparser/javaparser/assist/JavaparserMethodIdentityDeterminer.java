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
package org.lastaflute.meta.sourceparser.javaparser.assist;

import java.lang.reflect.Method;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.dbflute.util.Srl;

import com.github.javaparser.ast.body.MethodDeclaration;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from JavaparserSourceParserReflector (2021/06/04 Friday)
 */
public class JavaparserMethodIdentityDeterminer {

    // ===================================================================================
    //                                                                             Matches
    //                                                                             =======
    public boolean matchesMethod(Method method, MethodDeclaration dec) {
        return buildMethodIdentityNative(method).equals(buildMethodIdentitySource(dec));
    }

    // ===================================================================================
    //                                                                            Identity
    //                                                                            ========
    public String buildMethodIdentityNative(Method method) { // e.g. get$index(ProductsSearchForm)
        final String methodName = method.getName(); // e.g. get$index
        final String paramExp = Stream.of(method.getParameters())
                .map(pr -> pr.getType().getSimpleName()) // e.g. ProductsSearchForm, non generic
                .collect(Collectors.joining(getParameterDelimiter()));
        return doBuildMethodIdentity(methodName, paramExp);
    }

    public String buildMethodIdentitySource(MethodDeclaration dec) { // e.g. get$index(ProductsSearchForm)
        final String methodName = dec.getNameAsString(); // e.g. get$index
        final String paramExp = dec.getParameters().stream().map(pr -> {
            final String plainExp = pr.getTypeAsString(); // e.g. ProductsSearchForm, may have generic type
            return filterSourceParamExpIfNeeds(plainExp); // adjust source definition to compare with reflection expression
        }).collect(Collectors.joining(getParameterDelimiter()));
        return doBuildMethodIdentity(methodName, paramExp);
    }

    // -----------------------------------------------------
    //                                         Filter Source
    //                                         -------------
    protected String filterSourceParamExpIfNeeds(String oneParamExp) { // may have generic type
        return removeCDefPrefixIfNeeds(removeGenericDefinitionIfNeeds(oneParamExp));
    }

    protected String removeGenericDefinitionIfNeeds(String oneParamExp) { // may have generic type
        // generic uneeded for method identity as Java specification
        return Srl.substringFirstFront(oneParamExp, "<"); // remove it if exists
    }

    protected String removeCDefPrefixIfNeeds(String oneParamExp) {
        // #for_now jflute is there more smart way? (2021/06/21)
        if (oneParamExp.contains("CDef.")) { // e.g. CDef.MemberStatus
            return Srl.substringFirstRear(oneParamExp, "CDef."); // e.g. MemberStatus
        } else {
            return oneParamExp;
        }
    }

    // -----------------------------------------------------
    //                                          Assist Logic
    //                                          ------------
    protected String getParameterDelimiter() {
        return ", ";
    }

    protected String doBuildMethodIdentity(String methodName, String paramExp) {
        return methodName + "(" + paramExp + ")";
    }
}
