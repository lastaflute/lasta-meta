package org.lastaflute.meta.document.zone.properties;

import java.util.Collections;

import org.dbflute.optional.OptionalThing;
import org.dbflute.utflute.core.PlainTestCase;
import org.lastaflute.meta.document.parts.action.FormFieldNameAdjuster;
import org.lastaflute.meta.document.parts.annotation.MetaAnnotationArranger;
import org.lastaflute.meta.document.parts.type.MetaTypeNameAdjuster;
import org.lastaflute.meta.infra.json.MetauseJsonEngineProvider;
import org.lastaflute.meta.unit.mock.dbflute.MockCDef;
import org.lastaflute.meta.unit.mock.web.SeaForm;

/**
 * @author jflute
 * @since 0.5.1 moved from ActionDocumentAnalyzerTest (2021/06/26 Saturday at ikspiari)
 */
public class ActionPropertyFieldAnalyzerTest extends PlainTestCase {

    // ===================================================================================
    //                                                              Analyze Property Field
    //                                                              ======================
    // -----------------------------------------------------
    //                                         Target Suffix
    //                                         -------------
    public void test_isTargetSuffixResolvedClass_basic() {
        // ## Arrange ##
        ActionPropertyFieldAnalyzer analyzer = createAnalyzerAsNonRecursive();

        // ## Act ##
        // ## Assert ##
        assertTrue(analyzer.isTargetSuffixResolvedClass(SeaForm.class));
        assertTrue(analyzer.isTargetSuffixResolvedClass(SeaForm.HangarPart.class));

        assertFalse(analyzer.isTargetSuffixResolvedClass(String.class));
        assertFalse(analyzer.isTargetSuffixResolvedClass(MockCDef.MemberStatus.class));
        assertFalse(analyzer.isTargetSuffixResolvedClass(MockCDef.WhiteConfusingFormatBodying.class));
    }

    private ActionPropertyFieldAnalyzer createAnalyzerAsNonRecursive() {
        MetaTypeNameAdjuster metaTypeNameAdjuster = new MetaTypeNameAdjuster();
        MetauseJsonEngineProvider metauseJsonEngineProvider = new MetauseJsonEngineProvider();
        FormFieldNameAdjuster formFieldNameAdjuster = new FormFieldNameAdjuster(metauseJsonEngineProvider);
        MetaAnnotationArranger metaAnnotationArranger = new MetaAnnotationArranger(metaTypeNameAdjuster);
        return new ActionPropertyFieldAnalyzer(OptionalThing.empty(), Collections.emptyMap(), metaAnnotationArranger, metaTypeNameAdjuster,
                formFieldNameAdjuster, /*actionPropertiesAnalyzer*/null);
    }
}
