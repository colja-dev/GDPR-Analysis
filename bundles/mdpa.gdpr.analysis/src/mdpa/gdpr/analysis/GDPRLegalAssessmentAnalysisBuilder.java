package mdpa.gdpr.analysis;

import java.util.Optional;
import mdpa.gdpr.analysis.resource.GDPRResourceProvider;
import mdpa.gdpr.analysis.resource.GDPRURIResourceProvider;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.DataFlowAnalysisBuilder;
import org.dataflowanalysis.analysis.utils.ResourceUtils;
import org.eclipse.core.runtime.Plugin;

/**
 * Extension of the {@link DataFlowAnalysisBuilder} responsible for creating a valid {@link GDPRLegalAssessmentAnalysis}
 * from the following: - A valid path to a .gdpr metamodel instance - A valid path to a .contextproperties metamodel
 * instance
 */
public class GDPRLegalAssessmentAnalysisBuilder extends DataFlowAnalysisBuilder {
    private final Logger logger = Logger.getLogger(GDPRLegalAssessmentAnalysisBuilder.class);

    protected String gdprModelPath;
    protected String attributesPath;
    protected Optional<GDPRResourceProvider> customResourceProvider;

    /**
     * Constructs a dfd analysis builder with empty values
     */
    public GDPRLegalAssessmentAnalysisBuilder() {
        this.customResourceProvider = Optional.empty();
    }

    /**
     * Sets standalone mode of the analysis
     * @return Builder of the analysis
     */
    public GDPRLegalAssessmentAnalysisBuilder standalone() {
        super.standalone();
        return this;
    }

    /**
     * Sets the modelling project name of the analysis
     * @return Builder of the analysis
     */
    public GDPRLegalAssessmentAnalysisBuilder modelProjectName(String modelProjectName) {
        super.modelProjectName(modelProjectName);
        return this;
    }

    /**
     * Uses a plugin activator class for the given project
     * @param pluginActivator Plugin activator class of the modeling project
     * @return Returns builder object of the analysis
     */
    public GDPRLegalAssessmentAnalysisBuilder usePluginActivator(Class<? extends Plugin> pluginActivator) {
        super.usePluginActivator(pluginActivator);
        return this;
    }

    /**
     * Sets the data dictionary used by the analysis
     * @return Builder of the analysis
     */
    public GDPRLegalAssessmentAnalysisBuilder useGDPRModel(String gdprModelPath) {
        this.gdprModelPath = gdprModelPath;
        return this;
    }

    /**
     * Sets the data dictionary used by the analysis
     * @return Builder of the analysis
     */
    public GDPRLegalAssessmentAnalysisBuilder useProperties(String attributesPath) {
        this.attributesPath = attributesPath;
        return this;
    }

    /**
     * Registers a custom resource provider for the analysis
     * @param resourceProvider Custom resource provider of the analysis
     */
    public GDPRLegalAssessmentAnalysisBuilder useCustomResourceProvider(GDPRResourceProvider resourceProvider) {
        this.customResourceProvider = Optional.of(resourceProvider);
        return this;
    }

    /**
     * Determines the effective resource provider that should be used by the analysis
     */
    private GDPRResourceProvider getEffectiveResourceProvider() {
        if (this.customResourceProvider.isPresent()) {
            return this.customResourceProvider.get();
        }
        return new GDPRURIResourceProvider(ResourceUtils.createRelativePluginURI(this.gdprModelPath, this.modelProjectName),
                ResourceUtils.createRelativePluginURI(this.attributesPath, this.modelProjectName));
    }

    /**
     * Validates the stored data
     */
    protected void validate() {
        super.validate();
        if (this.customResourceProvider.isEmpty() && (this.gdprModelPath == null || this.gdprModelPath.isEmpty())) {
            logger.error("A GDPR model is required to run the data flow analysis",
                    new IllegalStateException("The GDPR analysis requires a gdpr model"));
        }
        if (this.customResourceProvider.isEmpty() && (this.attributesPath == null || this.attributesPath.isEmpty())) {
            logger.error("A file with context dependent attributes is required to run the data flow analysis",
                    new IllegalStateException("The GDPR analysis requires a file with context dependent attributes"));
        }
    }

    /**
     * Builds a new analysis from the given data
     */
    public GDPRLegalAssessmentAnalysis build() {
        this.validate();
        return new GDPRLegalAssessmentAnalysis(this.getEffectiveResourceProvider(), this.pluginActivator, this.modelProjectName);
    }
}
