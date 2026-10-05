package mdpa.gdpr.analysis.dfd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;
import mdpa.gdpr.analysis.core.ContextAttributeState;
import mdpa.gdpr.analysis.core.ContextDependentAttributeScenario;
import mdpa.gdpr.analysis.core.ContextDependentAttributeSource;
import mdpa.gdpr.analysis.utils.DFDUtils;
import mdpa.gdpr.analysis.utils.UncertaintyUtils;
import mdpa.gdpr.metamodel.GDPR.Data;
import mdpa.gdpr.metamodel.GDPR.NaturalPerson;
import mdpa.gdpr.metamodel.GDPR.PersonalData;
import mdpa.gdpr.metamodel.contextproperties.Expression;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.core.AbstractTransposeFlowGraph;
import org.dataflowanalysis.analysis.core.AbstractVertex;
import org.dataflowanalysis.analysis.dfd.core.DFDTransposeFlowGraph;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class DFDGDPRTransposeFlowGraph extends DFDTransposeFlowGraph {
    private final Logger logger = Logger.getLogger(DFDGDPRTransposeFlowGraph.class);
    private final List<ContextDependentAttributeSource> relevantContextDependentAttributes;
    private final DataDictionary dataDictionary;

    private final Optional<ContextAttributeState> contextAttributeState;
    private final DFDUtils DFDUtils = new DFDUtils();

    /**
     * Creates a new dfd transpose flow graph with the given sink that induces the transpose flow graph
     * @param sink Sink vertex that induces the transpose flow graph
     */
    public DFDGDPRTransposeFlowGraph(AbstractVertex<?> sink, List<ContextDependentAttributeSource> contextDependentAttributes,
            DataDictionary dataDictionary) {
        super(sink);
        this.relevantContextDependentAttributes = contextDependentAttributes;
        this.contextAttributeState = Optional.empty();
        this.dataDictionary = dataDictionary;
    }

    /**
     * Creates a new dfd transpose flow graph with the given sink that induces the transpose flow graph
     * @param sink Sink vertex that induces the transpose flow graph
     */
    public DFDGDPRTransposeFlowGraph(AbstractVertex<?> sink, List<ContextDependentAttributeSource> contextDependentAttributes,
            ContextAttributeState contextAttributeState, DataDictionary dataDictionary) {
        super(sink);
        this.relevantContextDependentAttributes = contextDependentAttributes;
        this.contextAttributeState = Optional.of(contextAttributeState);
        this.dataDictionary = dataDictionary;
    }

    /**
     * Determine the alternate flow graphs that are caused due to the resolving of CDAs
     * @return Returns a list of transpose flow graphs that each handle different applications of CDAs for this TFG
     */
    public List<DFDGDPRTransposeFlowGraph> determineAlternateFlowGraphs() {
        List<DFDGDPRTransposeFlowGraph> result = new ArrayList<>();
        List<ContextAttributeState> states = new ArrayList<>(
                ContextAttributeState.createAllContextAttributeStates(this.relevantContextDependentAttributes));
        for (ContextAttributeState state : states) {
            result.addAll(this.determineAlternateFlowGraphForState(state));
        }
        return result;
    }

    /**
     * Recursively handles the alternate flow graphs for a given state
     * @param state Given state of context dependent attributes
     * @return Returns a list of all alternate flow graphs for the given state
     */
    public List<DFDGDPRTransposeFlowGraph> determineAlternateFlowGraphForState(ContextAttributeState state) {
        List<DFDGDPRTransposeFlowGraph> result = new ArrayList<>();
        if (state.selectedScenarios()
                .stream()
                .noneMatch(it -> it.applicable(this))) {
            logger.warn("State not applicable to transpose flow graph, skipping");
            return List.of();
        }
        DFDGDPRTransposeFlowGraph currentTransposeFlowGraph = (DFDGDPRTransposeFlowGraph) this.copy(new HashMap<>(), state);

        for (ContextDependentAttributeScenario scenario : state.selectedScenarios()) {
            var scenarioResult = this.handleScenario(scenario, currentTransposeFlowGraph, state);
            if (scenarioResult.states()
                    .isEmpty()) {
                currentTransposeFlowGraph = scenarioResult.transposeFlowGraph()
                        .orElse(currentTransposeFlowGraph);
            } else {
                return scenarioResult.states()
                        .stream()
                        .map(this::determineAlternateFlowGraphForState)
                        .flatMap(List::stream)
                        .toList();
            }
        }
        return List.of(currentTransposeFlowGraph);
    }

    /**
     * Handles the given {@link ContextDependentAttributeScenario} scenario using the given current transpose flow graph.
     * Additionally, each newly created transpose flow graph gets the given state.
     * @param scenario {@link ContextDependentAttributeScenario} that is applied to the given transpose flow graph
     * @param currentTransposeFlowGraph Transpose flow graph that is modified
     * @param state {@link ContextAttributeState} that the resulting transpose flow graphs should have
     * @return Returns a {@link ScenarioResult} either containing new states to be explored, or an alternate flow graph with
     * the applied scenario
     */
    private ScenarioResult handleScenario(ContextDependentAttributeScenario scenario, DFDGDPRTransposeFlowGraph currentTransposeFlowGraph,
            ContextAttributeState state) {
        ContextDependentAttributeSource source = scenario.getContextDependentAttributeSource();
        Optional<DFDGDPRVertex> matchingVertex = currentTransposeFlowGraph.getVertices()
                .stream()
                .filter(DFDGDPRVertex.class::isInstance)
                .map(DFDGDPRVertex.class::cast)
                .filter(source::applicable)
                .findFirst();
        if (matchingVertex.isEmpty()) {
            logger.warn("Could not find matching vertex for context dependent attribute");
            return new ScenarioResult(Optional.empty(), List.of());
        }
        if (!scenario.applicable(matchingVertex.get())) {
            logger.warn("Scenario not applicable to vertex!");
            return new ScenarioResult(Optional.empty(), List.of());
        }

        if (source.getAnnotatedElement() instanceof NaturalPerson person) {
            return this.handlePersonalDataCharacteristicScenario(currentTransposeFlowGraph, source, scenario, state, person);
        } else if (source.getAnnotatedElement() instanceof Data data) {
            return this.handleDataCharacteristicScenario(currentTransposeFlowGraph, source, scenario, state, data);
        } else {
            return this.handleNodeCharacteristicScenario(currentTransposeFlowGraph, source, scenario, state);
        }
    }

    /**
     * Handle context dependent attributes to a natural person element that cause a data characteristic to be applied at the
     * given node
     * @param currentTransposeFlowGraph Transpose flow graph that is modified
     * @param source {@link ContextDependentAttributeSource} that is applied to the given transpose flow graph
     * @param scenario {@link ContextDependentAttributeScenario} that is applied to the given transpose flow graph
     * @param state {@link ContextAttributeState} that the resulting transpose flow graphs should have
     * @param person {@link NaturalPerson} element the context dependent attribute is applied to
     * @return Returns a {@link ScenarioResult} either containing new states to be explored, or an alternate flow graph with
     * the applied scenario
     */
    private ScenarioResult handlePersonalDataCharacteristicScenario(DFDGDPRTransposeFlowGraph currentTransposeFlowGraph,
            ContextDependentAttributeSource source, ContextDependentAttributeScenario scenario, ContextAttributeState state, NaturalPerson person) {
        List<DFDGDPRVertex> targetedVertices = this.determineTargetedVertices(currentTransposeFlowGraph, scenario);

        for (DFDGDPRVertex targetVertex : targetedVertices) {
            if (!source.applicable(targetVertex) && state.doesNotHandle(targetVertex)) {
                List<ContextAttributeState> additionalStates = new ArrayList<>();
                ContextDependentAttributeSource additionalSource = new ContextDependentAttributeSource(source.getAnnotation(),
                        source.getScopeDependentAssessmentFact()
                                .getExpression(),
                        List.of(source));
                for (Expression expression : source.getScopeDependentAssessmentFact()
                        .getExpression()) {
                    ContextDependentAttributeScenario additionalScenario = new ContextDependentAttributeScenario(expression, additionalSource,
                            List.of(source));
                    ContextAttributeState additionalState = new ContextAttributeState(Stream.concat(state.selectedScenarios()
                            .stream(), Stream.of(additionalScenario))
                            .toList());
                    additionalStates.add(additionalState);
                }
                logger.warn("Explore with Uncertainty!");
                return new ScenarioResult(Optional.empty(), additionalStates);
            }
            DFDGDPRVertex currentTargetVertex = currentTransposeFlowGraph.getVertices()
                    .stream()
                    .filter(DFDGDPRVertex.class::isInstance)
                    .map(DFDGDPRVertex.class::cast)
                    .filter(it -> it.getReferencedElement()
                            .getId()
                            .equals(targetVertex.getReferencedElement()
                                    .getId()))
                    .filter(it -> it.getReferencedElement()
                            .getEntityName()
                            .equals(targetVertex.getReferencedElement()
                                    .getEntityName()))
                    .findAny()
                    .orElseThrow();
            DFDGDPRVertex impactedElement = currentTargetVertex.getPreviousElements()
                    .stream()
                    .filter(DFDGDPRVertex.class::isInstance)
                    .map(DFDGDPRVertex.class::cast)
                    .filter(it -> it.getOutgoingData()
                            .stream()
                            .filter(PersonalData.class::isInstance)
                            .map(PersonalData.class::cast)
                            .anyMatch(data -> data.getDataReferences()
                                    .contains(person)))
                    .findAny()
                    .orElse(currentTargetVertex);
            Node replacingNode = DFDUtils.copyNode(impactedElement.getReferencedElement());
            UncertaintyUtils.impactBehavior(replacingNode, impactedElement, dataDictionary, source, scenario, person);
            DFDGDPRVertex replacingVertex = this.copyVertex(impactedElement, replacingNode);
            List<ContextDependentAttributeScenario> scenarios = new ArrayList<>(impactedElement.getContextDependentAttributes());
            scenarios.add(scenario);
            replacingVertex.setContextDependentAttributes(scenarios);
            Map<DFDVertex, DFDVertex> mapping = new HashMap<>();
            mapping.put(impactedElement, replacingVertex);
            currentTransposeFlowGraph = (DFDGDPRTransposeFlowGraph) currentTransposeFlowGraph.copy(mapping, state);
        }
        return new ScenarioResult(Optional.of(currentTransposeFlowGraph), List.of());
    }

    /**
     * Handle context dependent attributes to a data element that cause a data characteristic to be applied at the given
     * node
     * @param currentTransposeFlowGraph Transpose flow graph that is modified
     * @param source {@link ContextDependentAttributeSource} that is applied to the given transpose flow graph
     * @param scenario {@link ContextDependentAttributeScenario} that is applied to the given transpose flow graph
     * @param state {@link ContextAttributeState} that the resulting transpose flow graphs should have
     * @param data {@link Data} data element the context dependent attribute is applied to
     * @return Returns a {@link ScenarioResult} either containing new states to be explored, or an alternate flow graph with
     * the applied scenario
     */
    private ScenarioResult handleDataCharacteristicScenario(DFDGDPRTransposeFlowGraph currentTransposeFlowGraph,
            ContextDependentAttributeSource source, ContextDependentAttributeScenario scenario, ContextAttributeState state, Data data) {
        List<DFDGDPRVertex> targetedVertices = this.determineTargetedVertices(currentTransposeFlowGraph, scenario);

        for (DFDGDPRVertex targetVertex : targetedVertices) {
            if (!source.applicable(targetVertex) && state.doesNotHandle(targetVertex)) {
                List<ContextAttributeState> additionalStates = new ArrayList<>();
                ContextDependentAttributeSource additionalSource = new ContextDependentAttributeSource(source.getAnnotation(),
                        source.getScopeDependentAssessmentFact()
                                .getExpression(),
                        List.of(source));
                for (Expression expression : source.getScopeDependentAssessmentFact()
                        .getExpression()) {
                    ContextDependentAttributeScenario additionalScenario = new ContextDependentAttributeScenario(expression, additionalSource,
                            List.of(source));
                    ContextAttributeState additionalState = new ContextAttributeState(Stream.concat(state.selectedScenarios()
                            .stream(), Stream.of(additionalScenario))
                            .toList());
                    additionalStates.add(additionalState);
                }
                logger.warn("Explore with Uncertainty!");
                return new ScenarioResult(Optional.empty(), additionalStates);
            }
            DFDGDPRVertex currentTargetVertex = currentTransposeFlowGraph.getVertices()
                    .stream()
                    .filter(DFDGDPRVertex.class::isInstance)
                    .map(DFDGDPRVertex.class::cast)
                    .filter(it -> it.getReferencedElement()
                            .getId()
                            .equals(targetVertex.getReferencedElement()
                                    .getId()))
                    .filter(it -> it.getReferencedElement()
                            .getEntityName()
                            .equals(targetVertex.getReferencedElement()
                                    .getEntityName()))
                    .findAny()
                    .orElseThrow();
            DFDGDPRVertex impactedElement = currentTargetVertex.getPreviousElements()
                    .stream()
                    .filter(DFDGDPRVertex.class::isInstance)
                    .map(DFDGDPRVertex.class::cast)
                    .filter(it -> it.getOutgoingData()
                            .contains(data))
                    .findAny()
                    .orElse(currentTargetVertex);
            Node replacingNode = DFDUtils.copyNode(impactedElement.getReferencedElement());
            UncertaintyUtils.impactBehavior(replacingNode, impactedElement, dataDictionary, source, scenario, data);
            DFDGDPRVertex replacingVertex = this.copyVertex(impactedElement, replacingNode);
            List<ContextDependentAttributeScenario> scenarios = new ArrayList<>(impactedElement.getContextDependentAttributes());
            scenarios.add(scenario);
            replacingVertex.setContextDependentAttributes(scenarios);
            Map<DFDVertex, DFDVertex> mapping = new HashMap<>();
            mapping.put(impactedElement, replacingVertex);
            currentTransposeFlowGraph = (DFDGDPRTransposeFlowGraph) currentTransposeFlowGraph.copy(mapping, state);
        }
        return new ScenarioResult(Optional.of(currentTransposeFlowGraph), List.of());
    }

    /**
     * Handle context dependent attributes that cause a node characteristic to be applied at the given node
     * @param currentTransposeFlowGraph Transpose flow graph that is modified
     * @param source {@link ContextDependentAttributeSource} that is applied to the given transpose flow graph
     * @param scenario {@link ContextDependentAttributeScenario} that is applied to the given transpose flow graph
     * @param state {@link ContextAttributeState} that the resulting transpose flow graphs should have
     * @return Returns a {@link ScenarioResult} either containing new states to be explored, or an alternate flow graph with
     * the applied scenario
     */
    private ScenarioResult handleNodeCharacteristicScenario(DFDGDPRTransposeFlowGraph currentTransposeFlowGraph,
            ContextDependentAttributeSource source, ContextDependentAttributeScenario scenario, ContextAttributeState state) {
        List<String> matchingVertices = currentTransposeFlowGraph.getVertices()
                .stream()
                .filter(DFDGDPRVertex.class::isInstance)
                .map(DFDGDPRVertex.class::cast)
                .filter(source::applicable)
                .filter(scenario::applicable)
                .map(it -> it.getReferencedElement()
                        .getId())
                .toList();
        for (String targetVertexID : matchingVertices) {
            DFDGDPRVertex targetVertex = currentTransposeFlowGraph.getVertices()
                    .stream()
                    .filter(DFDGDPRVertex.class::isInstance)
                    .map(DFDGDPRVertex.class::cast)
                    .filter(it -> it.getReferencedElement()
                            .getId()
                            .equals(targetVertexID))
                    .findFirst()
                    .orElseThrow();
            Node replacingNode = EcoreUtil.copy(targetVertex.getReferencedElement());

            List<Label> labels = UncertaintyUtils.getAppliedLabel(source, scenario, dataDictionary);
            replacingNode.getProperties()
                    .addAll(labels);

            DFDGDPRVertex replacingVertex = this.copyVertex(targetVertex, replacingNode);
            List<ContextDependentAttributeScenario> scenarios = new ArrayList<>(targetVertex.getContextDependentAttributes());
            scenarios.add(scenario);
            replacingVertex.setContextDependentAttributes(scenarios);
            Map<DFDVertex, DFDVertex> mapping = new HashMap<>();
            mapping.put(targetVertex, replacingVertex);
            currentTransposeFlowGraph = (DFDGDPRTransposeFlowGraph) currentTransposeFlowGraph.copy(mapping, state);
        }
        return new ScenarioResult(Optional.of(currentTransposeFlowGraph), List.of());
    }

    private List<DFDGDPRVertex> determineTargetedVertices(DFDGDPRTransposeFlowGraph currentTransposeFlowGraph,
            ContextDependentAttributeScenario scenario) {
        List<DFDGDPRVertex> targetedVertices = currentTransposeFlowGraph.getVertices()
                .stream()
                .filter(DFDGDPRVertex.class::isInstance)
                .map(DFDGDPRVertex.class::cast)
                .filter(scenario::applicable)
                .toList();
        List<DFDGDPRVertex> finalTargetedVertices = targetedVertices;
        targetedVertices = targetedVertices.stream()
                .filter(it -> UncertaintyUtils.shouldReapply(finalTargetedVertices, it))
                .toList();
        return targetedVertices;
    }

    @Override
    public AbstractTransposeFlowGraph evaluate() {
        if (!(this.sink instanceof DFDGDPRVertex dfdSink)) {
            logger.error("Stored sink of DFD Transpose flow graph is not a DFDVertex");
            throw new IllegalStateException("Stored sink of DFD Transpose flow graph is not a DFD Vertex");
        }
        if (this.contextAttributeState.isEmpty()) {
            logger.error("Before evaluating the data flow, alternative flow graphs need to be created!");
            throw new IllegalStateException();
        }
        DFDGDPRVertex newSink = dfdSink.copy(new IdentityHashMap<>());
        newSink.unify(new HashSet<>());
        newSink.evaluateDataFlow();
        return new DFDGDPRTransposeFlowGraph(newSink, this.relevantContextDependentAttributes, this.contextAttributeState.get(), this.dataDictionary);
    }

    public List<ContextDependentAttributeSource> getContextDependentAttributeSources() {
        return this.relevantContextDependentAttributes;
    }

    /**
     * Copies the dfd vertex with the given replacing node
     * @param vertex DFD vertex of which the pin to vertex and pin to flow map should be copied
     * @param replacingElement Referenced node by the new dfd vertex
     * @return Returns a new dfd vertex with the given node and maps of the given vertex
     */
    private DFDGDPRVertex copyVertex(DFDGDPRVertex vertex, Node replacingElement) {
        Map<Pin, DFDVertex> copiedPinDFDVertexMap = new HashMap<>();
        vertex.getPinDFDVertexMap()
                .keySet()
                .forEach(key -> copiedPinDFDVertexMap.put(key, vertex.getPinDFDVertexMap()
                        .get(key)
                        .copy(new HashMap<>())));
        return new DFDGDPRVertex(replacingElement, copiedPinDFDVertexMap, new HashMap<>(vertex.getPinFlowMap()), vertex.getProcessing(),
                new ArrayList<>(vertex.getRelatedElements()));
    }

    @Override
    public AbstractTransposeFlowGraph copy(Map<DFDVertex, DFDVertex> mapping) {
        DFDGDPRVertex copiedSink;
        if (mapping.containsKey((DFDVertex) this.sink)) {
            copiedSink = (DFDGDPRVertex) mapping.get(this.sink);
        } else {
            copiedSink = ((DFDGDPRVertex) sink).copy(mapping);
        }
        copiedSink.unify(new HashSet<>());
        return this.contextAttributeState
                .map(attributeState -> new DFDGDPRTransposeFlowGraph(copiedSink, this.relevantContextDependentAttributes, attributeState,
                        this.dataDictionary))
                .orElseGet(() -> new DFDGDPRTransposeFlowGraph(copiedSink, this.relevantContextDependentAttributes, this.dataDictionary));
    }

    public AbstractTransposeFlowGraph copy(Map<DFDVertex, DFDVertex> mapping, ContextAttributeState contextAttributeState) {
        DFDGDPRVertex copiedSink;
        if (mapping.containsKey((DFDVertex) this.sink)) {
            copiedSink = (DFDGDPRVertex) mapping.get(this.sink);
        } else {
            copiedSink = ((DFDGDPRVertex) sink).copy(mapping);
        }
        copiedSink.unify(new HashSet<>());
        return new DFDGDPRTransposeFlowGraph(copiedSink, this.relevantContextDependentAttributes, contextAttributeState, this.dataDictionary);
    }

    public ContextAttributeState getContextAttributeState() {
        return contextAttributeState.orElseThrow();
    }
}
