package mdpa.gdpr.analysis.validation.cases;

import java.util.List;
import mdpa.gdpr.analysis.validation.AnalysisExecutor;
import mdpa.gdpr.analysis.validation.GDPRModelBuilder;
import mdpa.gdpr.analysis.validation.ScalibilityParameter;
import mdpa.gdpr.metamodel.GDPR.Storing;
import mdpa.gdpr.metamodel.contextproperties.ScopeSet;
import mdpa.gdpr.metamodel.contextproperties.Scope;
import mdpa.gdpr.metamodel.contextproperties.ScopeDependentAssessmentFact;
import mdpa.gdpr.metamodel.contextproperties.SAFAnnotation;

public class BaseScalibilityCase extends AbstractScalibilityCase {

    @Override
    public void runScalibilityCase(ScalibilityParameter parameter, AnalysisExecutor analysisExecutor) {
        // ------------ Model creation ---------------
        GDPRModelBuilder builder = new GDPRModelBuilder();
        Storing storing = builder.createStoringElement("Storing");

        // -------- Context Dependent Attribute -------------------
        ScopeDependentAssessmentFact property = builder.createProperty("Type", List.of("True", "False"));
        SAFAnnotation propertyAnnotation = builder.createPropertyAnnotation(storing, property);
        ScopeSet contextAnnotation = builder.createContextAnnotation("Annotation", List.of(property.getExpression()
                .get(0)), propertyAnnotation);
        Scope contextDefinition = builder.createContextDefinition("Definition", builder.getDefaultController(), contextAnnotation);

        // ------------ Analysis Execution ------------------
        analysisExecutor.executeAnalysis(parameter, builder);
    }

    @Override
    public String getTestName() {
        return "Base";
    }

}
