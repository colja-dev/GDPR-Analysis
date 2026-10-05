package mdpa.gdpr.analysis.validation.cases;

import java.util.ArrayList;
import java.util.List;
import mdpa.gdpr.analysis.validation.AnalysisExecutor;
import mdpa.gdpr.analysis.validation.GDPRModelBuilder;
import mdpa.gdpr.analysis.validation.ScalibilityParameter;
import mdpa.gdpr.metamodel.contextproperties.SAFAnnotation;
import mdpa.gdpr.metamodel.contextproperties.ScopeDependentAssessmentFact;
import mdpa.gdpr.metamodel.contextproperties.ScopeSet;

public class DataDefinedContextDependentAttributeScalibilityCase extends AbstractScalibilityCase {

    @Override
    public void runScalibilityCase(ScalibilityParameter parameter, AnalysisExecutor analysisExecutor) {
        // ------------ Model creation ---------------
        GDPRModelBuilder builder = new GDPRModelBuilder();
        builder.createStoringElement("Storing");

        // -------- Context Dependent Attribute -------------------
        List<String> values = new ArrayList<>(parameter.getModelSize());
        for (int i = 0; i < parameter.getModelSize(); i++) {
            values.add("Value" + i);
        }
        ScopeDependentAssessmentFact property = builder.createProperty("Type", values);
        SAFAnnotation propertyAnnotation = builder.createPropertyAnnotation(builder.getDefaultPersonalData(), property);
        ScopeSet contextAnnotation = builder.createContextAnnotation("Annotation", property.getExpression(), propertyAnnotation);
        builder.createContextDefinition("Definition", builder.getDefaultController(), contextAnnotation);

        // ------------ Analysis Execution ------------------
        analysisExecutor.executeAnalysis(parameter, builder);
    }

    @Override
    public String getTestName() {
        return "DataDefinedContextDependentAttribute";
    }

}
