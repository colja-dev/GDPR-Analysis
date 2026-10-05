package mdpa.gdpr.analysis.dfd;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import mdpa.gdpr.analysis.core.ContextDependentAttributeSource;
import mdpa.gdpr.analysis.resource.DataFlowDiagramAndDataDictionary;
import mdpa.gdpr.analysis.resource.GDPRResourceProvider;
import mdpa.laf.referencemodel.LAF.AssessmentFact;
import mdpa.gdpr.metamodel.GDPR.PersonalData;
import mdpa.gdpr.metamodel.GDPR.Processing;
import mdpa.gdpr.metamodel.GDPR.Role;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.core.FlowGraphCollection;
import org.dataflowanalysis.analysis.dfd.core.DFDTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.core.DFDTransposeFlowGraphFinder;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.analysis.resource.ResourceProvider;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Pin;

/**
 * Models a collection of {@link DFDGDPRTransposeFlowGraph}s for use with the
 * {@link mdpa.gdpr.analysis.GDPRLegalAssessmentAnalysis}
 */
public class DFDGDPRFlowGraphCollection extends FlowGraphCollection {
    private final Logger logger = Logger.getLogger(DFDGDPRFlowGraphCollection.class);
    private DataDictionary dataDictionary;

    /**
     * Creates a new {@link DFDGDPRFlowGraphCollection} using the provided resource provider
     * @param resourceProvider {@link ResourceProvider} that provides the necessary models
     */
    public DFDGDPRFlowGraphCollection(ResourceProvider resourceProvider) {
        super(resourceProvider);
    }

    /**
     * Creates a new {@link DFDGDPRFlowGraphCollection} with the given list of transpose flow graphs and a given resource
     * provider
     * @param transposeFlowGraphs List of {@link DFDGDPRTransposeFlowGraph}s that are stored in the flow graph collection
     * @param resourceProvider {@link ResourceProvider} that has the relevant model elements loaded
     */
    public DFDGDPRFlowGraphCollection(List<DFDGDPRTransposeFlowGraph> transposeFlowGraphs, ResourceProvider resourceProvider) {
        super(transposeFlowGraphs, resourceProvider);
    }

    @Override
    public List<? extends AbstractTransposeFlowGraph> findTransposeFlowGraphs() {
        if (!(this.resourceProvider instanceof GDPRResourceProvider gdprResourceProvider)) {
            this.logger.error("Resource provider is not a GDPR resource provider!");
            throw new IllegalArgumentException();
        }
        DataFlowDiagramAndDataDictionary dfd = gdprResourceProvider.getTransformationManager()
                .transform(gdprResourceProvider.getGDPRModel(), gdprResourceProvider.getScopeDependentAssessmentFacts());
        this.dataDictionary = dfd.dataDictionary();
        DFDTransposeFlowGraphFinder finder = new DFDTransposeFlowGraphFinder(dfd.dataDictionary(), dfd.dataFlowDiagram());
        List<DFDGDPRTransposeFlowGraph> completeFlowGraphs = finder.findTransposeFlowGraphs()
                .stream()
                .map(it -> this.transformFlowGraph((DFDTransposeFlowGraph) it, dfd.dataDictionary()))
                .toList();
        return new ArrayList<>(completeFlowGraphs);
    }

    /**
     * Finds the partial responsibility flow graphs for the contained flow graphs and creates a new
     * {@link DFDGDPRFlowGraphCollection} containing the previous {@link DFDGDPRTransposeFlowGraph} with the additional
     * partial responsibility flow graphs
     * @return Returns a new {@link DFDGDPRFlowGraphCollection} containing additional partial responsibility flow graphs
     */
    public DFDGDPRFlowGraphCollection findResponsibilityFlowGraphs() {
        List<DFDGDPRTransposeFlowGraph> completeFlowGraphs = this.getTransposeFlowGraphs()
                .stream()
                .filter(DFDGDPRTransposeFlowGraph.class::isInstance)
                .map(DFDGDPRTransposeFlowGraph.class::cast)
                .toList();
        List<DFDGDPRTransposeFlowGraph> flowGraphs = new ArrayList<>(completeFlowGraphs);
        flowGraphs.addAll(completeFlowGraphs.stream()
                .map(this::getPartialTransposeFlowGraphs)
                .flatMap(List::stream)
                .toList());
        return new DFDGDPRFlowGraphCollection(flowGraphs, this.resourceProvider);
    }

    /**
     * Transforms the given DFD-based {@link DFDTransposeFlowGraph} to an {@link DFDGDPRTransposeFlowGraph}
     * @param transposeFlowGraph Given {@link DFDTransposeFlowGraph}
     * @param dataDictionary Data dictionary containing the labels for CDAs
     * @return Returns the corresponding {@link DFDGDPRTransposeFlowGraph}
     */
    private DFDGDPRTransposeFlowGraph transformFlowGraph(DFDTransposeFlowGraph transposeFlowGraph, DataDictionary dataDictionary) {
        Map<DFDVertex, DFDGDPRVertex> mapping = new IdentityHashMap<>();
        transposeFlowGraph.getVertices()
                .stream()
                .map(DFDVertex.class::cast)
                .forEach(vertex -> mapping.put(vertex, this.getDFDGDPRVertex(vertex, new IdentityHashMap<>())));
        return new DFDGDPRTransposeFlowGraph(mapping.get((DFDVertex) transposeFlowGraph.getSink()),
                this.determineContextDependentAttributes(mapping.values()), dataDictionary);
    }

    /**
     * Determines the list of partial responsibility flow graphs for the given {@link DFDGDPRTransposeFlowGraph}
     * @param transposeFlowGraph Given {@link DFDGDPRTransposeFlowGraph}
     * @return Returns the list of responsibility transpose flow graphs for the input {@link DFDGDPRTransposeFlowGraph}
     */
    private List<DFDGDPRTransposeFlowGraph> getPartialTransposeFlowGraphs(DFDGDPRTransposeFlowGraph transposeFlowGraph) {
        List<DFDGDPRTransposeFlowGraph> result = new ArrayList<>();
        Map<Role, List<DFDGDPRVertex>> roleMap = new HashMap<>();
        transposeFlowGraph.getVertices()
                .stream()
                .filter(DFDGDPRVertex.class::isInstance)
                .map(DFDGDPRVertex.class::cast)
                .forEach(vertex -> {
                    Role role = vertex.getResponsibilityRole();
                    List<DFDGDPRVertex> roleVertices = roleMap.getOrDefault(role, new ArrayList<>());
                    roleVertices.add(vertex);
                    roleMap.put(role, roleVertices);
                });
        if (roleMap.size() < 2) {
            return List.of();
        }

        for (Map.Entry<Role, List<DFDGDPRVertex>> entry : roleMap.entrySet()) {
            List<DFDGDPRVertex> roleVertices = entry.getValue();
            List<DFDGDPRVertex> previousElements = roleVertices.stream()
                    .map(DFDGDPRVertex::getPreviousElements)
                    .flatMap(List::stream)
                    .filter(DFDGDPRVertex.class::isInstance)
                    .map(DFDGDPRVertex.class::cast)
                    .toList();
            List<DFDGDPRVertex> sinks = roleVertices.stream()
                    .filter(it -> !previousElements.contains(it))
                    .toList();
            for (DFDGDPRVertex sink : sinks) {
                result.add(new DFDGDPRTransposeFlowGraph(this.getMappingForSink(sink, roleVertices),
                        new ArrayList<>(transposeFlowGraph.getContextDependentAttributeSources()), this.dataDictionary));
            }
        }
        return result;
    }

    /**
     * Determines the {@link DFDGDPRVertex} that denotes the sink of the partial responsibility transpose flow graph
     * @param sink {@link DFDGDPRVertex} that is the current sink
     * @param roleVertices List of {@link DFDGDPRVertex} that denote the beginnings of each responsibility segment
     * @return Returns the {@link DFDGDPRVertex} sink of the partial responsibility TFG
     */
    private DFDGDPRVertex getMappingForSink(DFDGDPRVertex sink, List<DFDGDPRVertex> roleVertices) {
        Map<Pin, DFDVertex> pinVertexMap = new HashMap<>();
        sink.getPinDFDVertexMap()
                .entrySet()
                .stream()
                .filter(it -> it.getValue() instanceof DFDGDPRVertex)
                .filter(it -> roleVertices.contains(it.getValue()))
                .forEach(it -> pinVertexMap.put(it.getKey(), this.getMappingForSink((DFDGDPRVertex) it.getValue(), roleVertices)));
        return new DFDGDPRVertex(sink.getReferencedElement(), pinVertexMap, new HashMap<>(sink.getPinFlowMap()), sink.getProcessing(),
                new ArrayList<>(sink.getRelatedElements()));
    }

    /**
     * Creates the {@link DFDGDPRVertex} for a corresponding {@link DFDVertex} and a given mapping
     * @param vertex Given {@link DFDVertex}
     * @param mapping Mapping between {@link DFDVertex}
     * @return Returns the corresponding {@link DFDGDPRVertex} of the {@link DFDVertex}
     */
    private DFDGDPRVertex getDFDGDPRVertex(DFDVertex vertex, Map<DFDVertex, DFDVertex> mapping) {
        if (!(this.resourceProvider instanceof GDPRResourceProvider gdprResourceProvider)) {
            this.logger.error("Resource provider is not a GDPR resource provider!");
            throw new IllegalArgumentException();
        }
        Processing processing = gdprResourceProvider.getTransformationManager()
                .getElement(vertex.getReferencedElement())
                .orElseThrow();
        Map<Pin, DFDVertex> copiedPinDFDVertexMap = new HashMap<>();
        vertex.getPinDFDVertexMap()
                .keySet()
                .forEach(key -> copiedPinDFDVertexMap.put(key, mapping.getOrDefault(vertex.getPinDFDVertexMap()
                        .get(key),
                        this.getDFDGDPRVertex(vertex.getPinDFDVertexMap()
                                .get(key), mapping))));
        List<AssessmentFact> relatedElements = this.determineRelatedElements(processing);
        return new DFDGDPRVertex(vertex.getReferencedElement(), copiedPinDFDVertexMap, new HashMap<>(vertex.getPinFlowMap()), processing,
                relatedElements);
    }

    /**
     * Determines the relevant elements for the given {@link AssessmentFact}
     * @param gdprElement Given {@link AssessmentFact}
     * @return Related elements for the given {@link AssessmentFact}
     */
    private List<AssessmentFact> determineRelatedElements(AssessmentFact gdprElement) {
        List<AssessmentFact> result = new ArrayList<>();
        result.add(gdprElement);
        if (gdprElement instanceof Processing processing) {
            result.addAll(processing.getInputData());
            result.addAll(processing.getInputData()
                    .stream()
                    .filter(PersonalData.class::isInstance)
                    .map(PersonalData.class::cast)
                    .map(PersonalData::getDataReferences)
                    .flatMap(List::stream)
                    .toList());
            result.addAll(processing.getOutputData());
            result.addAll(processing.getOutputData()
                    .stream()
                    .filter(PersonalData.class::isInstance)
                    .map(PersonalData.class::cast)
                    .map(PersonalData::getDataReferences)
                    .flatMap(List::stream)
                    .toList());
            result.addAll(processing.getPurpose());
            result.addAll(processing.getOnTheBasisOf());
            result.add(processing.getResponsible());
        }
        return result;
    }

    /**
     * Determines the context dependent attribute sources for the given collection of vertices using the
     * {@link mdpa.gdpr.analysis.core.TransformationManager}
     * @param vertices List of vertices of which the {@link ContextDependentAttributeSource}s should be determined
     * @return List of {@link ContextDependentAttributeSource} that are applicable to the given list of vertices
     */
    private List<ContextDependentAttributeSource> determineContextDependentAttributes(Collection<DFDGDPRVertex> vertices) {
        if (!(this.resourceProvider instanceof GDPRResourceProvider gdprResourceProvider)) {
            this.logger.error("Resource provider is not a GDPR resource provider!");
            throw new IllegalArgumentException();
        }
        return gdprResourceProvider.getTransformationManager()
                .getContextDependentAttributes()
                .stream()
                .filter(it -> it.applicable(vertices))
                .toList();
    }

    /**
     * Returns the flow graph collection that contains the final flow graph with resolved context dependent attributes.
     * <p/>
     * Note: This will create a new instance of the {@link DFDGDPRFlowGraphCollection}
     * @return Returns a new {@link DFDGDPRFlowGraphCollection} containing the resolved {@link DFDGDPRTransposeFlowGraph}s
     */
    public DFDGDPRFlowGraphCollection resolveContextDependentAttributes() {
        List<DFDGDPRTransposeFlowGraph> resultingTransposeFlowGraphs = this.getTransposeFlowGraphs()
                .stream()
                .filter(DFDGDPRTransposeFlowGraph.class::isInstance)
                .map(DFDGDPRTransposeFlowGraph.class::cast)
                .map(DFDGDPRTransposeFlowGraph::determineAlternateFlowGraphs)
                .flatMap(Collection::stream)
                .toList();
        return new DFDGDPRFlowGraphCollection(resultingTransposeFlowGraphs, this.resourceProvider);
    }
}
