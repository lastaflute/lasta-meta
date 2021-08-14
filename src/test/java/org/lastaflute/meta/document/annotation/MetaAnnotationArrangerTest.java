package org.lastaflute.meta.document.annotation;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.dbflute.utflute.core.PlainTestCase;
import org.hibernate.validator.constraints.Length;
import org.lastaflute.meta.document.parts.annotation.MetaAnnotationArranger;
import org.lastaflute.meta.document.parts.type.MetaTypeNameAdjuster;
import org.lastaflute.meta.unit.mock.web.SeaForm;
import org.lastaflute.web.validation.Required;

/**
 * @author jflute
 */
public class MetaAnnotationArrangerTest extends PlainTestCase {

    public void test_analyzeAnnotationList_basic() {
        // ## Arrange ##
        MetaAnnotationArranger analyzer = createMetaAnnotationAnalyzer();
        Field stageField;
        try {
            stageField = SeaForm.class.getDeclaredField("stageName");
        } catch (NoSuchFieldException | SecurityException e) {
            throw new IllegalStateException("Failed to get fields.", e);
        }
        List<Annotation> annotationList = Arrays.asList(stageField.getAnnotations()); // basically random
        annotationList.sort(Comparator.comparing(anno -> { // for fixed expectation
            return anno.annotationType().getSimpleName();
        }));

        // ## Act ##
        List<String> analyzedList = analyzer.arrangeAnnotationList(annotationList);

        // ## Assert ##
        log(analyzedList);
        assertHasAnyElement(analyzedList);
        assertTrue(analyzedList.size() >= 2);

        String first = analyzedList.get(0);
        assertEquals(Length.class.getSimpleName() + "{max=32, min=2}", first); // should be ordered

        String second = analyzedList.get(1);
        assertEquals(Required.class.getSimpleName(), second); // non-specified attribute
    }

    public void test_analyzeAnnotationList_empty() {
        // ## Arrange ##
        MetaAnnotationArranger analyzer = createMetaAnnotationAnalyzer();
        List<Annotation> annotationList = Collections.emptyList();

        // ## Act ##
        List<String> analyzedList = analyzer.arrangeAnnotationList(annotationList);

        // ## Assert ##
        assertHasZeroElement(analyzedList);
    }

    protected MetaAnnotationArranger createMetaAnnotationAnalyzer() {
        return new MetaAnnotationArranger(new MetaTypeNameAdjuster());
    }
}
