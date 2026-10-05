package mdpa.gdpr.analysis.validation;

import mdpa.gdpr.analysis.core.TransformationManager;
import mdpa.gdpr.analysis.resource.GDPRResourceProvider;
import mdpa.gdpr.metamodel.GDPR.LegalAssessmentFacts;
import mdpa.gdpr.metamodel.contextproperties.ScopeDependentAssessmentFacts;

public class MemoryGDPRResourceProvider extends GDPRResourceProvider {
    private GDPRModelBuilder modelBuilder;
    private final TransformationManager transformationManager = new TransformationManager();

    public MemoryGDPRResourceProvider(GDPRModelBuilder modelBuilder) {
        this.modelBuilder = modelBuilder;
    }

    @Override
    public LegalAssessmentFacts getGDPRModel() {
        return modelBuilder.getGdprModel();
    }

    @Override
    public ScopeDependentAssessmentFacts getScopeDependentAssessmentFacts() {
        return modelBuilder.getContextDependentAttributes();
    }

    @Override
    public TransformationManager getTransformationManager() {
        return this.transformationManager;
    }

    @Override
    public void loadRequiredResources() {

    }

}
