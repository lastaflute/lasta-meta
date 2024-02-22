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
package org.lastaflute.meta.document.docmeta;

import java.lang.annotation.Annotation;
import java.util.List;

import org.dbflute.util.DfCollectionUtil;
import org.lastaflute.core.util.Lato;
import org.lastaflute.meta.document.docmeta.reference.TypeDocReference;

// #hope jflute split this to ParameterTypeDocMeta, ReturnTypeDocMeta for also debug (2022/04/19)
// #hope jflute keep parent information for also debug (2022/04/19)
/**
 * TypeDocMeta holds structure of the Field of the Action class,
 * the Field of the Job class, the Action Method Parameter,
 * the Action Form / Body / Result itself and its Field.<br>
 * <pre>
 * Action's fields
 *  |-nest: target field type's fields (and more nestable...)
 *  |-nest: target generic type's fields (and more nestable...)
 * 
 * Job's fields
 *  |-nest: target field type's fields (and more nestable...)
 *  |-nest: target generic type's fields (and more nestable...)
 * 
 * Action's parameters of execute methods
 *  |-(no nest)
 * 
 * Form/Body itself (*no name)
 *  |-nest: Form/Body's fields
 *     |-nest: target field type's fields (and more nestable...)
 *     |-nest: target generic type's fields (and more nestable...)
 * 
 * Result itself (*no name)
 *  |-nest: Result's fields
 *     |-nest: target field type's fields (and more nestable...)
 *     |-nest: target generic type's fields (and more nestable...)
 * </pre>
 * @author p1us2er0
 * @author jflute
 * @since 0.5.0-sp9 (2015/09/18 Friday)
 */
public class TypeDocMeta implements TypeDocReference {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    // _/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/_/
    // instance variable names are referred by LastaDoc templates in DBFlute Engine
    // (so field name is directly used by Gson)
    //
    // DocumentGenerator@saveLastaDocMeta() (called by e.g. UTFlute)
    //   |
    //   |            (by Gson)
    //   +--(save)--> analyzed-lastadoc.json
    //                    ^
    //                    |            (by DBFlute FreeGen)
    //                    +---(ref)--- LaDocHtml.vm (and others)
    //
    // so don't change easily without migration steps
    // _/_/_/_/_/_/_/_/_/_/
    // -----------------------------------------------------
    //                                            Basic Item
    //                                            ----------
    /**
     * The name pf Action Method Parameter or Action Form / Body / Result field. (NullAllowed)<br>
     * Null for action form or request body or response body.
     */
    private String name;

    /**
     * The public name for the interface of {@link #name}. (NullAllowed)<br>
     * Camel case is common in java, but since there are cases where the interface uses snake case, it is kept separately from name.
     */
    private String publicName;

    /**
     * The java class(type). (NotNull)<br>
     * To transient to exclude with serialize in json.<br>
     * If customize the serialize process with json, can combine {@link #type} and {@link #typeName} into one.
     */
    private transient Class<?> type;

    /**
     * The java class(type) as string. (NotNull)<br>
     * e.g. org.docksidestage.SeaPark -> org.docksidestage.SeaPark
     */
    private String typeName;

    /**
     * The java class(type) as simple string. (NotNull)<br>
     * e.g. org.docksidestage.SeaPark -> SeaPark
     */
    private String simpleTypeName;

    // -----------------------------------------------------
    //                                          Comment Item
    //                                          ------------
    // #thinking jflute the name "value" is too easy so... e.g. valueExpression? (2021/08/06)
    // #for_now jflute only referred at field comment of LastaDoc (swagger extracts type directly) (2021/08/05)
    /** The value expression of the type, for example, enum values, value derived from javadoc comment. (NullAllowed) */
    private String value; // e.g. {FML = Formalized, PRV = Provisinal, ...}

    /**
     * The description of javadoc comment. (NullAllowed)<br>
     * The first sentence of the javadoc comment is the description.
     */
    private String description;

    /**
     * The javadoc comment. (NullAllowed)<br>
     * Analyze the source code using javaparser. If don't have javadoc, or if can't see the source code at runtime, it will be Null.
     */
    private String comment;

    // -----------------------------------------------------
    //                                          Generic Item
    //                                          ------------
    /**
     * The generic type of {@link #type}. (NullAllowed)<br>
     * Only one generic is supported.<br>
     * To transient to exclude with serialize in json.<br>
     * e.g. SeaPark&lt;LandPark&gt; -> LandPark, SeaPark&lt;LandPark, XxxPark&gt; -> LandPark
     */
    private transient Class<?> genericType;

    // -----------------------------------------------------
    //                                       Annotation Item
    //                                       ---------------
    /**
     * The list of annotation type. (NullAllowed, EmptyAllowed)<br>
     * To transient to exclude with serialize in json.<br>
     * If customize the serialize process with json, can combine {@link #annotationTypeList} and {@link #annotationList} into one.
     */
    public transient List<Annotation> annotationTypeList;

    /**
     * The list of annotation name(no package) and annotation parameters as string. (NotNull, EmptyAllowed)<br>
     * e.g. SeaPark、SeaPark{dockside=over, hangar=mystic}<br>
     */
    private List<String> annotationList = DfCollectionUtil.newArrayList(); // as default

    // -----------------------------------------------------
    //                                          Nested Field
    //                                          ------------
    /**
     * The list of nested meta, basically field of part class. (NotNull, EmptyAllowed)<br>
     * Action Form / Body / Result field is the target.
     */
    private List<TypeDocMeta> nestTypeDocMetaList = DfCollectionUtil.newArrayList(); // as default

    // ===================================================================================
    //                                                                      Basic Override
    //                                                                      ==============
    @Override
    public String toString() {
        return Lato.string(this);
    }

    // ===================================================================================
    //                                                                            Accessor
    //                                                                            ========
    // -----------------------------------------------------
    //                                            Basic Item
    //                                            ----------
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPublicName() {
        return publicName;
    }

    public void setPublicName(String publicName) {
        this.publicName = publicName;
    }

    public Class<?> getType() {
        return type;
    }

    public void setType(Class<?> type) {
        this.type = type;
    }

    public String getTypeName() {
        return typeName;
    }

    public void setTypeName(String typeName) {
        this.typeName = typeName;
    }

    public String getSimpleTypeName() {
        return simpleTypeName;
    }

    public void setSimpleTypeName(String simpleTypeName) {
        this.simpleTypeName = simpleTypeName;
    }

    // -----------------------------------------------------
    //                                          Comment Item
    //                                          ------------
    public String getValue() { // used by velocity template
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    // -----------------------------------------------------
    //                                          Generic Item
    //                                          ------------
    public Class<?> getGenericType() {
        return genericType;
    }

    public void setGenericType(Class<?> genericType) {
        this.genericType = genericType;
    }

    // -----------------------------------------------------
    //                                       Annotation Item
    //                                       ---------------
    public List<Annotation> getAnnotationTypeList() {
        return annotationTypeList;
    }

    public void setAnnotationTypeList(List<Annotation> annotationTypeList) {
        this.annotationTypeList = annotationTypeList;
    }

    public List<String> getAnnotationList() { // list of annotation name (without package)
        return annotationList;
    }

    public void setAnnotationList(List<String> annotationList) {
        this.annotationList = annotationList;
    }

    // -----------------------------------------------------
    //                                       Nested Field
    //                                       ---------------
    public List<TypeDocMeta> getNestTypeDocMetaList() {
        return nestTypeDocMetaList;
    }

    public void setNestTypeDocMetaList(List<TypeDocMeta> nestTypeDocMetaList) {
        this.nestTypeDocMetaList = nestTypeDocMetaList;
    }
}
