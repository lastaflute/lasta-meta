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
package org.lastaflute.meta.sourceparser.javaparser;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.dbflute.optional.OptionalThing;
import org.dbflute.util.DfCollectionUtil;
import org.dbflute.util.DfStringUtil;
import org.lastaflute.meta.document.docmeta.ActionDocMeta;
import org.lastaflute.meta.document.docmeta.JobDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.sourceparser.SourceParserReflector;
import org.lastaflute.meta.sourceparser.javaparser.assist.JavaparserMethodIdentityDeterminer;
import org.lastaflute.meta.sourceparser.javaparser.parsing.JavaparserSourceMethodHandler;
import org.lastaflute.meta.sourceparser.javaparser.parsing.JavaparserSourceTypeHandler;
import org.lastaflute.meta.sourceparser.javaparser.visiting.JavaparserActionDocMetaVisitorAdapter;

import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithJavadoc;
import com.github.javaparser.ast.visitor.VoidVisitorAdapter;
import com.github.javaparser.javadoc.Javadoc;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class JavaparserSourceParserReflector implements SourceParserReflector {

    // ===================================================================================
    //                                                                          Definition
    //                                                                          ==========
    /** to pick up first line from javadoc comment of class and method. (NotNull) */
    protected static final Pattern CLASS_METHOD_COMMENT_END_PATTERN = Pattern.compile("(.+)[.。]?.*(\r?\n)?");

    /** to pick up first statement (line) from javadoc comment of field. (NotNull) */
    protected static final Pattern FIELD_COMMENT_END_PATTERN = Pattern.compile("([^.。\\*]+).* ?\\*?");

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    protected final JavaparserMethodIdentityDeterminer methodIdentityDeterminer; // not null
    protected final JavaparserSourceTypeHandler sourceTypeHandler; // not null, has srcDirList
    protected final JavaparserSourceMethodHandler sourceMethodHandler; // not null

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JavaparserSourceParserReflector(List<String> srcDirList) {
        this.methodIdentityDeterminer = newJavaparserMethodIdentityDeterminer();
        this.sourceTypeHandler = newJavaparserSourceTypeHandler(srcDirList);
        this.sourceMethodHandler = newJavaparserSourceMethodHandler(this.sourceTypeHandler, this.methodIdentityDeterminer);
    }

    protected JavaparserMethodIdentityDeterminer newJavaparserMethodIdentityDeterminer() {
        return new JavaparserMethodIdentityDeterminer();
    }

    protected JavaparserSourceTypeHandler newJavaparserSourceTypeHandler(List<String> srcDirList) {
        return new JavaparserSourceTypeHandler(srcDirList);
    }

    protected JavaparserSourceMethodHandler newJavaparserSourceMethodHandler(JavaparserSourceTypeHandler sourceTypeHandler,
            JavaparserMethodIdentityDeterminer methodIdentityDeterminer) {
        return new JavaparserSourceMethodHandler(sourceTypeHandler, methodIdentityDeterminer);
    }

    // ===================================================================================
    //                                                                         Method List
    //                                                                         ===========
    @Override
    public List<Method> getMethodListOrderByDefinition(Class<?> clazz) {
        return sourceMethodHandler.getMethodListOrderByDefinition(clazz);
    }

    // ===================================================================================
    //                                                               Reflect ActionDocMeta
    //                                                               =====================
    @Override
    public void reflect(ActionDocMeta meta, Method method) {
        parseClass(method.getDeclaringClass()).ifPresent(compilationUnit -> {
            final Map<String, List<String>> returnMap = DfCollectionUtil.newLinkedHashMap();

            // you need to execute this before the setting process
            readActionDocSourceDrivenMetaByJavaparser(meta, method, compilationUnit, returnMap);

            // prepare description (summary-like)
            final List<String> descriptionElementList = extractDescriptionElementList(meta, method, compilationUnit, returnMap);
            if (!descriptionElementList.isEmpty()) {
                final String summarylikeDescription = String.join(", ", descriptionElementList);
                meta.setDescription(summarylikeDescription);
            }

            // resolve parameter names of reflection by actual variable names
            final List<TypeDocMeta> parameterTypeDocMetaList = meta.getParameterTypeDocMetaList();
            final Parameter[] parameters = method.getParameters(); // method arguments
            for (int parameterIndex = 0; parameterIndex < parameters.length; parameterIndex++) {
                if (parameterIndex < parameterTypeDocMetaList.size()) {
                    final Parameter parameter = parameters[parameterIndex];
                    final TypeDocMeta typeDocMeta = parameterTypeDocMetaList.get(parameterIndex);
                    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
                    // /products/{arg0}/purchases/{arg1}/showbase-oneman/
                    //  ↓↓↓
                    // /products/{productId}/purchases/{purchaseId}/showbase-oneman/
                    // _/_/_/_/_/_/_/_/_/_/
                    final String plainUrl = meta.getUrl();
                    final String resolvedUrl = plainUrl.replace("{" + parameter.getName() + "}", "{" + typeDocMeta.getName() + "}");
                    meta.setUrl(resolvedUrl);
                }
            }

            // prepare return statement information
            final String methodName = method.getName();
            if (returnMap.containsKey(methodName) && !returnMap.get(methodName).isEmpty()) {
                String returnExp = String.join(",", returnMap.get(methodName)); // e.g. asJson(listResult)
                meta.getReturnTypeDocMeta().setValue(returnExp);
            }
        });
    }

    // -----------------------------------------------------
    //                                      SourceDrive Meta
    //                                      ----------------
    protected void readActionDocSourceDrivenMetaByJavaparser(ActionDocMeta meta, Method method, CompilationUnit compilationUnit,
            Map<String, List<String>> returnMap) {
        final VoidVisitorAdapter<ActionDocMeta> adapter = createActionDocMetaVisitorAdapter(method, returnMap);
        adapter.visit(compilationUnit, meta);
    }

    protected VoidVisitorAdapter<ActionDocMeta> createActionDocMetaVisitorAdapter(Method method, Map<String, List<String>> returnMap) {
        return new JavaparserActionDocMetaVisitorAdapter(method, returnMap, nodeWithJavadoc -> {
            return adjustComment(nodeWithJavadoc);
        }, methodIdentityDeterminer);
    }

    // -----------------------------------------------------
    //                                           Description
    //                                           -----------
    protected List<String> extractDescriptionElementList(ActionDocMeta meta, Method method, CompilationUnit compilationUnit,
            Map<String, List<String>> returnMap) {
        final List<String> descriptionElementList = DfCollectionUtil.newArrayList();
        final String typeComment = meta.getTypeComment(); // class javadoc, null allowed
        final String methodComment = meta.getMethodComment(); // execute method javadoc, null allowed
        Arrays.asList(typeComment, methodComment).forEach(comment -> {
            if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                final Matcher matcher = CLASS_METHOD_COMMENT_END_PATTERN.matcher(comment);
                if (matcher.find()) {
                    final String fisrtLine = matcher.group(1); // first line
                    descriptionElementList.add(fisrtLine);
                }
            }
        });
        return descriptionElementList; // class javadoc first line + method javadoc first line
    }

    // ===================================================================================
    //                                                                  Reflect JobDocMeta
    //                                                                  ==================
    @Override
    public void reflect(JobDocMeta jobDocMeta, Class<?> clazz) {
        // #needs_fix jflute wants to refactor JobDocMeta (2024/02/21)
        parseClass(clazz).ifPresent(compilationUnit -> {
            VoidVisitorAdapter<JobDocMeta> adapter = createJobDocMetaVisitorAdapter();
            adapter.visit(compilationUnit, jobDocMeta);
            List<String> descriptionList = DfCollectionUtil.newArrayList();
            Arrays.asList(jobDocMeta.getTypeComment(), jobDocMeta.getMethodComment()).forEach(comment -> {
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    Matcher matcher = CLASS_METHOD_COMMENT_END_PATTERN.matcher(comment);
                    if (matcher.find()) {
                        descriptionList.add(matcher.group(1));
                    }
                }
            });
            if (!descriptionList.isEmpty()) {
                jobDocMeta.setDescription(String.join(", ", descriptionList));
            }
        });
    }

    protected VoidVisitorAdapter<JobDocMeta> createJobDocMetaVisitorAdapter() {
        return new JobDocMetaVisitorAdapter();
    }

    public class JobDocMetaVisitorAdapter extends VoidVisitorAdapter<JobDocMeta> {

        @Override
        public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, JobDocMeta jobDocMeta) {
            classOrInterfaceDeclaration.getBegin().ifPresent(begin -> {
                classOrInterfaceDeclaration.getEnd().ifPresent(end -> {
                    jobDocMeta.setFileLineCount(end.line - begin.line);
                });
            });
            String comment = adjustComment(classOrInterfaceDeclaration);
            if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                jobDocMeta.setTypeComment(comment);
            }
            super.visit(classOrInterfaceDeclaration, jobDocMeta);
        }

        @Override
        public void visit(MethodDeclaration methodDeclaration, JobDocMeta jobDocMeta) {
            if (!methodDeclaration.getNameAsString().equals(jobDocMeta.getMethodName())) {
                return;
            }

            methodDeclaration.getBegin().ifPresent(begin -> {
                methodDeclaration.getEnd().ifPresent(end -> {
                    jobDocMeta.setMethodLineCount(end.line - begin.line);
                });
            });
            String comment = adjustComment(methodDeclaration);
            if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                jobDocMeta.setMethodComment(comment);
            }
            super.visit(methodDeclaration, jobDocMeta);
        }
    }

    // ===================================================================================
    //                                                                 Reflect TypeDocMeta
    //                                                                 ===================
    @Override
    public void reflect(TypeDocMeta typeDocMeta, Class<?> clazz) {
        List<Class<?>> classList = DfCollectionUtil.newArrayList();
        for (Class<?> targetClass = clazz; targetClass != null; targetClass = targetClass.getSuperclass()) {
            if (!targetClass.isPrimitive() && !Number.class.isAssignableFrom(targetClass)
                    && !Arrays.asList(Object.class, String.class).contains(targetClass)) {
                classList.add(targetClass);
            }
        }
        Collections.reverse(classList);
        classList.forEach(targetClass -> {
            parseClass(targetClass).ifPresent(compilationUnit -> {
                VoidVisitorAdapter<TypeDocMeta> adapter = createTypeDocMetaVisitorAdapter(clazz);
                adapter.visit(compilationUnit, typeDocMeta);
            });
        });
    }

    protected VoidVisitorAdapter<TypeDocMeta> createTypeDocMetaVisitorAdapter(Class<?> clazz) {
        return new TypeDocMetaVisitorAdapter(clazz);
    }

    public class TypeDocMetaVisitorAdapter extends VoidVisitorAdapter<TypeDocMeta> {

        private Class<?> clazz;

        public TypeDocMetaVisitorAdapter(Class<?> clazz) {
            this.clazz = clazz;
        }

        @Override
        public void visit(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDocMeta typeDocMeta) {
            prepareClassComment(classOrInterfaceDeclaration, typeDocMeta);
            super.visit(classOrInterfaceDeclaration, typeDocMeta);
        }

        protected void prepareClassComment(ClassOrInterfaceDeclaration classOrInterfaceDeclaration, TypeDocMeta typeDocMeta) {
            if (DfStringUtil.is_Null_or_Empty(typeDocMeta.getComment())
                    && classOrInterfaceDeclaration.getNameAsString().equals(typeDocMeta.getSimpleTypeName())) {
                final String comment = adjustComment(classOrInterfaceDeclaration);
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    typeDocMeta.setComment(comment);
                    final Matcher matcher = CLASS_METHOD_COMMENT_END_PATTERN.matcher(comment);
                    if (matcher.find()) {
                        typeDocMeta.setDescription(matcher.group(1));
                    }
                }
            }
        }

        @Override
        public void visit(FieldDeclaration fieldDeclaration, TypeDocMeta typeDocMeta) {
            prepareFieldComment(fieldDeclaration, typeDocMeta);
            super.visit(fieldDeclaration, typeDocMeta);
        }

        protected void prepareFieldComment(FieldDeclaration fieldDeclaration, TypeDocMeta typeDocMeta) {
            if (fieldDeclaration.getVariables().stream().anyMatch(variable -> variable.getNameAsString().equals(typeDocMeta.getName()))) {
                String comment = adjustComment(fieldDeclaration);
                if (DfStringUtil.is_NotNull_and_NotEmpty(comment)) {
                    if (DfStringUtil.is_Null_or_Empty(typeDocMeta.getComment()) || fieldDeclaration.getParentNode().map(parentNode -> {
                        @SuppressWarnings("unchecked")
                        TypeDeclaration<TypeDeclaration<?>> typeDeclaration = (TypeDeclaration<TypeDeclaration<?>>) parentNode;
                        return typeDeclaration.getNameAsString().equals(clazz.getSimpleName());
                    }).orElse(false)) {
                        typeDocMeta.setComment(comment);
                        Matcher matcher = FIELD_COMMENT_END_PATTERN.matcher(saveFieldCommentSpecialExp(comment));
                        if (matcher.find()) {
                            String description = matcher.group(1).trim();
                            typeDocMeta.setDescription(restoreFieldCommentSpecialExp(description));
                        }
                    }
                }
            }
        }

        protected String saveFieldCommentSpecialExp(String comment) {
            return comment.replace("e.g.", "$$edotgdot$$");
        }

        protected String restoreFieldCommentSpecialExp(String comment) {
            return comment.replace("$$edotgdot$$", "e.g.");
        }
    }

    // ===================================================================================
    //                                                                      Adjust Comment
    //                                                                      ==============
    protected String adjustComment(NodeWithJavadoc<?> nodeWithJavadoc) {
        try {
            // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
            // remove first/last line separators
            //
            // e.g.
            //  (first line separators)
            //  @param <DATA> The type of data form.
            //  @author jflute
            //  (last line separators)
            //   ↓↓↓
            //  @param <DATA> The type of data form.
            //  @author jflute
            // _/_/_/_/_/_/_/_/_/_/
            final Optional<Javadoc> optJavadoc = nodeWithJavadoc.getJavadoc();
            return optJavadoc.map(javadoc -> javadoc.toText().replaceAll("(^\r?\n|\r?\n$)", "")).orElse(null);
        } catch (Throwable t) {
            return "javadoc parse error. error messge=" + t.getMessage();
        }
    }

    // ===================================================================================
    //                                                                         Parse Class
    //                                                                         ===========
    protected OptionalThing<CompilationUnit> parseClass(Class<?> clazz) {
        return sourceTypeHandler.parseClass(clazz);
    }
}
