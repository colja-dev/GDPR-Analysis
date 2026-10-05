package mdpa.gdpr.analysis;

import java.util.Optional;
import mdpa.gdpr.analysis.dfd.DFDGDPRFlowGraphCollection;
import mdpa.gdpr.analysis.resource.GDPRResourceProvider;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.DataFlowConfidentialityAnalysis;
import org.dataflowanalysis.analysis.core.FlowGraphCollection;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.emf.ecore.plugin.EcorePlugin;
import tools.mdsd.library.standalone.initialization.StandaloneInitializationException;
import tools.mdsd.library.standalone.initialization.StandaloneInitializerBuilder;

/**
 * Extension of the {@link DataFlowConfidentialityAnalysis} for usage with the GDPR metamodel that is able to resolve
 * uncertain context dependent attributes.
 * <p/>
 * Inputs to the analysis are a metamodel instance of the GDPR model and the context properties model
 * <p/>
 * Note: Do not create an instance of this class manually, use the {@link GDPRLegalAssessmentAnalysisBuilder} instead
 */
public class GDPRLegalAssessmentAnalysis extends DataFlowConfidentialityAnalysis {
    public static final String PLUGIN_PATH = "mdpa.gdpr.analysis";

    private final Logger logger = Logger.getLogger(GDPRLegalAssessmentAnalysis.class);

    private final GDPRResourceProvider resourceProvider;
    private final Optional<Class<? extends Plugin>> modelProjectActivator;
    private final String modelProjectName;

    /**
     * Create a new {@link GDPRLegalAssessmentAnalysis} with the given resource provider and optionally a modelling project
     * with a plugin activator
     * <p/>
     * Note: Do not create an instance of this class manually, use the {@link GDPRLegalAssessmentAnalysisBuilder} instead
     * @param resourceProvider {@link GDPRResourceProvider} providing a metamodel instance of the GDPR and Context Property
     * model
     * @param modelProjectActivator Optional model project activator
     * @param modelProjectName Optional model project name
     */
    public GDPRLegalAssessmentAnalysis(GDPRResourceProvider resourceProvider, Optional<Class<? extends Plugin>> modelProjectActivator,
            String modelProjectName) {
        this.resourceProvider = resourceProvider;
        this.modelProjectActivator = modelProjectActivator;
        this.modelProjectName = modelProjectName;
    }

    @Override
    public void initializeAnalysis() {
        this.resourceProvider.setupResources();

        EcorePlugin.ExtensionProcessor.process(null);

        try {
            super.setupLoggers();
            var initializationBuilder = StandaloneInitializerBuilder.builder()
                    .registerProjectURI(GDPRLegalAssessmentAnalysis.class, PLUGIN_PATH);

            this.modelProjectActivator
                    .ifPresent(projectActivator -> initializationBuilder.registerProjectURI(projectActivator, this.modelProjectName));

            initializationBuilder.build()
                    .init();

            logger.info("Successfully initialized standalone environment for the data flow analysis.");

        } catch (StandaloneInitializationException e) {
            logger.error("Could not initialize analysis", e);
            throw new IllegalStateException("Could not initialize analysis");
        }
        this.resourceProvider.loadRequiredResources();
        this.resourceProvider.validate();
        if (!this.resourceProvider.sufficientResourcesLoaded()) {
            logger.error("Insufficient amount of resources loaded");
            throw new IllegalStateException("Could not initialize analysis");
        }
    }

    @Override
    public FlowGraphCollection findFlowGraphs() {
        return new DFDGDPRFlowGraphCollection(this.resourceProvider);
    }

    @Override
    public void setLoggerLevel(Level level) {
        logger.setLevel(level);
    }
}
