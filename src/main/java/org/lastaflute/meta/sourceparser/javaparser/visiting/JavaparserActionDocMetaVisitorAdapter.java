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
package org.lastaflute.meta.sourceparser.javaparser.visiting;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.IntStream;

import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.sourceparser.javaparser.assist.JavaparserMethodIdentityDeterminer;

import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.stmt.ReturnStmt;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.1 split from JavaparserSourceParserReflector (2021/06/04 Friday)
 */
public class JavaparserActionDocMetaVisitorAdapter extends VoidVisitorAdapter<ActionDocMeta> {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    protected static final Pattern RETURN_STMT_PATTERN = Pattern.compile("^[^)]+\\)");

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final Method method;
    protected final Map<String, List<String>> returnMap;
    protected final Function<NodeWithJavadoc<?>, String> commentAdjuster;
    protected final JavaparserMethodIdentityDeterminer methodIdentityDeterminer;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JavaparserActionDocMetaVisitorAdapter(Method method, Map<String, List<String>> returnMap,
            Function<NodeWithJavadoc<?>, String> commentAdjuster, JavaparserMethodIdentityDeterminer methodIdentityDeterminer) {
        this.method = method;
        this.returnMap = returnMap;
        this.commentAdjuster = commentAdjuster;
        this.methodIdentityDeterminer = methodIdentityDeterminer;
    }

    // ===================================================================================
    //                                                                         Visit Class
    //                                                                         ===========
    @Override
    public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, ActionDocMeta actionDocMeta) {
        classOrInterfaceDeclaration.getBegin().ifPresent(begin -> {
            classOrInterfaceDeclaration.getEnd().ifPresent(end -> {
                actionDocMeta.setFileLineCount(end.line - begin.line);
            });
        });
        String comment = commentAdjuster.apply(classOrInterfaceDeclaration);
        if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
            actionDocMeta.setTypeComment(comment);
        }
        super.visit(classOrInterfaceDeclaration, actionDocMeta);
    }

    // ===================================================================================
    //                                                                        Visit Method
    //                                                                        ============
    @Override
    public void visit(MethodDeclaration methodDeclaration, ActionDocMeta actionDocMeta) {
        if (!matchesMethod(methodDeclaration)) {
            return;
        }

        methodDeclaration.getBegin().ifPresent(begin -> {
            methodDeclaration.getEnd().ifPresent(end -> {
                actionDocMeta.setMethodLineCount(end.line - begin.line);
            });
        });
        String comment = commentAdjuster.apply(methodDeclaration);
        if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
            actionDocMeta.setMethodComment(comment);
        }
        IntStream.range(0, actionDocMeta.getParameterTypeDocMetaList().size()).forEach(parameterIndex -> {
            if (parameterIndex < methodDeclaration.getParameters().size()) {
                TypeDocMeta typeDocMeta = actionDocMeta.getParameterTypeDocMetaList().get(parameterIndex);
                com.github.javaparser.ast.body.Parameter parameter = methodDeclaration.getParameters().get(parameterIndex);
                typeDocMeta.setName(parameter.getNameAsString());
                typeDocMeta.setPublicName(parameter.getNameAsString());
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    // parse parameter comment
                    Pattern pattern = Pattern.compile(".*@param\\s?" + parameter.getNameAsString() + "\\s?(.*)\r?\n.*", Pattern.DOTALL);
                    Matcher matcher = pattern.matcher(comment);
                    if (matcher.matches()) {
                        typeDocMeta.setComment(matcher.group(1).replaceAll("\r?\n.*", ""));
                        typeDocMeta.setDescription(typeDocMeta.getComment().replaceAll(" ([^\\p{Alnum}]|e\\.g\\. )+.*", ""));
                    }
                }
            }
        });

        methodDeclaration.accept(new VoidVisitorAdapter<ActionDocMeta>() {
            @Override
            public void visit(ReturnStmt returnStmt, ActionDocMeta actionDocMeta) {
                prepareReturnStmt(methodDeclaration, returnStmt);
                super.visit(returnStmt, actionDocMeta);
            }
        }, actionDocMeta);
        super.visit(methodDeclaration, actionDocMeta);
    }

    // ===================================================================================
    //                                                                        Assist Logic
    //                                                                        ============
    protected boolean matchesMethod(MethodDeclaration methodDeclaration) {
        return methodIdentityDeterminer.matchesMethod(method, methodDeclaration);
    }

    protected void prepareReturnStmt(MethodDeclaration methodDeclaration, ReturnStmt returnStmt) {
        returnStmt.getExpression().ifPresent(expression -> {
            String returnStmtStr = expression.toString();
            Matcher matcher = RETURN_STMT_PATTERN.matcher(returnStmtStr);
            if (!returnMap.containsKey(methodDeclaration.getNameAsString())) {
                returnMap.put(methodDeclaration.getNameAsString(), DfCollectionUtil.newArrayList());
            }
            returnMap.get(methodDeclaration.getNameAsString()).add(matcher.find() ? matcher.group(0) : "##unanalyzable##");
        });
    }
}
