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
package org.lastaflute.meta.document;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.dbflute.helper.message.ExceptionMessageBuilder;
import org.dbflute.optional.OptionalThing;
import org.lastaflute.core.util.ContainerUtil;
import org.lastaflute.di.core.exception.ComponentNotFoundException;
import org.lastaflute.job.JobManager;
import org.lastaflute.job.LaJob;
import org.lastaflute.job.LaScheduledJob;
import org.lastaflute.meta.document.docmeta.JobDocMeta;
import org.lastaflute.meta.document.docmeta.TypeDocMeta;
import org.lastaflute.meta.sourceparser.SourceParserReflector;

/**
 * @author p1us2er0
 * @author jflute
 * @since 0.6.9 (2075/03/05 Sunday)
 */
public class JobDocumentAnalyzer extends BaseDocumentAnalyzer {

    // ===================================================================================
    //                                                                           Attribute
    //                                                                           =========
    /** The list of source directory. (NotNull) */
    protected final List<String> srcDirList;

    /** depth of analyzed target, to avoid cyclic analyzing. */
    protected final int depth;

    /** The optional reflector of source parser, e.g. java parser. (NotNull, EmptyAllowed) */
    protected final OptionalThing<SourceParserReflector> sourceParserReflector;

    // ===================================================================================
    //                                                                         Constructor
    //                                                                         ===========
    public JobDocumentAnalyzer(List<String> srcDirList, int depth, OptionalThing<SourceParserReflector> sourceParserReflector) {
        this.srcDirList = srcDirList;
        this.depth = depth;
        this.sourceParserReflector = sourceParserReflector;
    }

    // ===================================================================================
    //                                                                             Analyze
    //                                                                             =======
    public List<JobDocMeta> analyzeJobDocMetaList() {
        final JobManager jobManager = getJobManager();
        final boolean rebooted = automaticallyRebootIfNeeds(jobManager);
        try {
            return prepareJobDocMetaList(jobManager);
        } finally {
            automaticallyDestroyIfNeeds(jobManager, rebooted);
        }
    }

    // -----------------------------------------------------
    //                                 JobManager Management
    //                                 ---------------------
    protected JobManager getJobManager() {
        try {
            return ContainerUtil.getComponent(org.lastaflute.job.JobManager.class);
        } catch (ComponentNotFoundException e) {
            final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
            br.addNotice("Not found the job manager as Lasta Di component.");
            br.addItem("Advice");
            br.addElement("lasta-meta needs JobManager to get job information.");
            br.addElement("So confirm your app.xml (or test_app.xml?)");
            br.addElement("whether the Di xml includes lasta_job.xml or not.");
            final String msg = br.buildExceptionMessage();
            throw new IllegalStateException(msg, e);
        }
    }

    protected boolean automaticallyRebootIfNeeds(org.lastaflute.job.JobManager jobManager) {
        if (!jobManager.isSchedulingDone()) { // basically no scheduling in UTFlute
            try {
                jobManager.reboot();
            } catch (RuntimeException e) {
                final ExceptionMessageBuilder br = new ExceptionMessageBuilder();
                br.addNotice("Cannot reboot job scheduling for lasta-meta.");
                br.addItem("Advice");
                br.addElement("Confirm nested exception message");
                br.addElement("and your job environment in unit test.");
                br.addItem("Job Manager");
                br.addElement(jobManager);
                final String msg = br.buildExceptionMessage();
                throw new IllegalStateException(msg, e);
            }
            return true;
        } else {
            return false;
        }
    }

    protected void automaticallyDestroyIfNeeds(org.lastaflute.job.JobManager jobManager, boolean rebooted) {
        if (rebooted) {
            jobManager.destroy();
        }
    }

    // -----------------------------------------------------
    //                                          Job Document
    //                                          ------------
    protected List<JobDocMeta> prepareJobDocMetaList(JobManager jobManager) {
        return jobManager.getJobList().stream().map(job -> {
            return createJobDocMeta(job);
        }).collect(Collectors.toList());
    }

    protected JobDocMeta createJobDocMeta(LaScheduledJob job) {
        // basically use getNoException() for simple implementation (and Job is extra!?) by jflute (2022/04/23)
        final JobDocMeta jobDocMeta = new JobDocMeta();

        jobDocMeta.setJobKey(getNoException(() -> job.getJobKey().value()));
        jobDocMeta.setJobUnique(getNoException(() -> job.getJobUnique().map(jobUnique -> jobUnique.value()).orElse(null)));
        jobDocMeta.setJobTitle(getNoException(() -> job.getJobNote().flatMap(jobNote -> jobNote.getTitle()).orElse(null)));
        jobDocMeta.setJobDescription(getNoException(() -> job.getJobNote().flatMap(jobNote -> jobNote.getDesc()).orElse(null)));
        jobDocMeta.setCronExp(getNoException(() -> job.getCronExp().orElse(null)));

        final Class<? extends LaJob> jobClass = getNoException(() -> job.getJobType());
        if (jobClass != null) {
            jobDocMeta.setTypeName(jobClass.getName());
            jobDocMeta.setSimpleTypeName(jobClass.getSimpleName());
            jobDocMeta.setFieldTypeDocMetaList(Arrays.stream(jobClass.getDeclaredFields()).map(field -> {
                final TypeDocMeta typeDocMeta = new TypeDocMeta();
                typeDocMeta.setName(field.getName());
                typeDocMeta.setType(field.getType());
                typeDocMeta.setTypeName(adjustTypeName(field.getGenericType()));
                typeDocMeta.setSimpleTypeName(adjustSimpleTypeName((field.getGenericType())));
                typeDocMeta.setAnnotationTypeList(Arrays.asList(field.getAnnotations()));
                typeDocMeta.setAnnotationList(arrangeAnnotationList(typeDocMeta.getAnnotationTypeList()));
                sourceParserReflector.ifPresent(sourceParserReflector -> {
                    sourceParserReflector.reflect(typeDocMeta, field.getType());
                });
                return typeDocMeta;
            }).collect(Collectors.toList()));
            jobDocMeta.setMethodName("run"); // fixedly
            sourceParserReflector.ifPresent(sourceParserReflector -> {
                sourceParserReflector.reflect(jobDocMeta, jobClass);
            });
        }

        jobDocMeta.setParams(getNoException(() -> {
            return job.getParamsSupplier().map(paramsSupplier -> paramsSupplier.supply()).orElse(null);
        }));
        jobDocMeta.setNoticeLogLevel(getNoException(() -> job.getNoticeLogLevel().name()));
        jobDocMeta.setConcurrentExec(getNoException(() -> job.getConcurrentExec().name()));
        jobDocMeta.setTriggeredJobKeyList(getNoException(() -> {
            return job.getTriggeredJobKeySet().stream().map(triggeredJobKey -> triggeredJobKey.value()).collect(Collectors.toList());
        }));

        return jobDocMeta;
    }

    protected <T extends Object> T getNoException(Supplier<T> supplier) {
        try {
            return supplier.get();
        } catch (Throwable t) {
            return null;
        }
    }
}
