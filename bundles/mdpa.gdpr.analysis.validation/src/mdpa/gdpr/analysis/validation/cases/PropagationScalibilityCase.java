package mdpa.gdpr.analysis.validation.cases;

import java.util.List;
import mdpa.gdpr.analysis.validation.AnalysisExecutor;
import mdpa.gdpr.analysis.validation.GDPRModelBuilder;
import mdpa.gdpr.analysis.validation.ScalibilityParameter;
import mdpa.gdpr.metamodel.contextproperties.ScopeSet;
import mdpa.gdpr.metamodel.contextproperties.ScopeDependentAssessmentFact;
import mdpa.gdpr.metamodel.contextproperties.SAFAnnotation;

public class PropagationScalibilityCase extends AbstractScalibilityCase {

    @Override
    public void runScalibilityCase(ScalibilityParameter parameter, AnalysisExecutor analysisExecutor) {
        // ------------ Model creation ---------------
        GDPRModelBuilder builder = new GDPRModelBuilder();
        for (int i = 0; i < parameter.getModelSize() - 1; i++) {
            builder.createProcessingElement("Processing " + i);
        }
        builder.createStoringElement("Storing");
        ScopeDependentAssessmentFact property = builder.createProperty("Type", List.of("True", "False"));
        SAFAnnotation propertyAnnotation = builder.createPropertyAnnotation(builder.getDefaultPersonalData(), property);
        ScopeSet contextAnnotation = builder.createContextAnnotation("Annotation", List.of(property.getExpression()
                .get(0)), propertyAnnotation);
        builder.createContextDefinition("Definition", builder.getDefaultController(), contextAnnotation);

        // ------------ Analysis Execution ------------------
        analysisExecutor.executeAnalysis(parameter, builder);
    }

    @Override
    public String getTestName() {
        return "PropagationSequential";
    }

}
