package mdpa.gdpr.analysis.validation.cases;

import java.util.ArrayList;
import java.util.List;
import mdpa.gdpr.analysis.validation.AnalysisExecutor;
import mdpa.gdpr.analysis.validation.GDPRModelBuilder;
import mdpa.gdpr.analysis.validation.ScalibilityParameter;
import mdpa.gdpr.metamodel.GDPR.Purpose;
import mdpa.gdpr.metamodel.contextproperties.ScopeSet;
import mdpa.gdpr.metamodel.contextproperties.ScopeDependentAssessmentFact;
import mdpa.gdpr.metamodel.contextproperties.SAFAnnotation;

public class ContextDefinitionSizeScalibilityCase extends AbstractScalibilityCase {

    @Override
    public void runScalibilityCase(ScalibilityParameter parameter, AnalysisExecutor analysisExecutor) {
        // ------------ Model creation ---------------
        GDPRModelBuilder builder = new GDPRModelBuilder();
        List<Purpose> purposes = new ArrayList<>(parameter.getModelSize());
        purposes.add(builder.getDefaultPurpose());
        for (int i = 1; i < parameter.getModelSize(); i++) {
            purposes.add(builder.createPurpose("Purpose " + i));
        }
        builder.getFirstElement()
                .getPurpose()
                .clear();
        builder.getFirstElement()
                .getPurpose()
                .addAll(purposes);
        builder.createStoringElement("Storing");

        // -------- Context Dependent Attribute -------------------
        ScopeDependentAssessmentFact property = builder.createProperty("Type", List.of("True", "False"));
        SAFAnnotation propertyAnnotation = builder.createPropertyAnnotation(builder.getDefaultPersonalData(), property);
        ScopeSet contextAnnotation = builder.createContextAnnotation("Annotation", List.of(property.getExpression()
                .get(0)), propertyAnnotation);
        builder.createContextDefinition("Definition", purposes, contextAnnotation);

        // ------------ Analysis Execution ------------------
        analysisExecutor.executeAnalysis(parameter, builder);
    }

    @Override
    public String getTestName() {
        return "ContextDefinitionSize";
    }

}
