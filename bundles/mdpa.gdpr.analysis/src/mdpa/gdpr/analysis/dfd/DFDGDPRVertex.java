package mdpa.gdpr.analysis.dfd;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mdpa.gdpr.analysis.core.ContextDependentAttributeScenario;
import mdpa.gdpr.metamodel.GDPR.Data;
import mdpa.gdpr.metamodel.GDPR.LegalBasis;
import mdpa.gdpr.metamodel.GDPR.Processing;
import mdpa.gdpr.metamodel.GDPR.Purpose;
import mdpa.gdpr.metamodel.GDPR.Role;
import mdpa.laf.referencemodel.LAF.AssessmentFact;
import org.dataflowanalysis.analysis.dfd.core.DFDVertex;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.dataflowdiagram.Flow;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.eclipse.emf.ecore.util.EcoreUtil;

public class DFDGDPRVertex extends DFDVertex {
    private final Processing processing;
    private final List<AssessmentFact> relatedElements;
    private List<ContextDependentAttributeScenario> contextDependentAttributes;

    /**
     * Creates a new vertex with the given referenced node and pin mappings
     * @param node Node that is referenced by the vertex
     * @param pinDFDVertexMap Map containing relationships between the pins of the vertex and previous vertices
     * @param pinFlowMap Map containing relationships between the pins of the vertex and the flows connecting the node to
     * other vertices
     * @param processing {@link Processing} of the GDPR model that is represented by the vertex
     * @param relatedElements All elements of the GDPR model that are related to the processing of the vertex
     */
    public DFDGDPRVertex(Node node, Map<Pin, DFDVertex> pinDFDVertexMap, Map<Pin, Flow> pinFlowMap, Processing processing,
            List<AssessmentFact> relatedElements) {
        super(node, pinDFDVertexMap, pinFlowMap);
        this.processing = processing;
        this.relatedElements = relatedElements;
        this.contextDependentAttributes = new ArrayList<>();
    }

    /**
     * Creates a clone of the vertex without considering data characteristics nor vertex characteristics
     */
    public DFDGDPRVertex copy(Map<DFDVertex, DFDVertex> mapping) {
        Map<Pin, DFDVertex> copiedPinDFDVertexMap = this.copyPinDFDVertexMap(mapping);
        Map<Pin, Flow> copiedPinFlowMap = this.copyPinFlowMap(copiedPinDFDVertexMap);
        DFDGDPRVertex copy = new DFDGDPRVertex(this.referencedElement, copiedPinDFDVertexMap, copiedPinFlowMap, this.processing,
                new ArrayList<>(this.relatedElements));
        if (!this.contextDependentAttributes.isEmpty()) {
            copy.setContextDependentAttributes(this.contextDependentAttributes);
        }
        return copy;
    }

    /**
     * Returns the Map from a pin on the vertex to the given predecessor {@link DFDVertex}, while adhering to the given
     * mapping
     * @param mapping Mapping that should be applied to the mapping process
     * @return Returns a new copied pin to vertex map that adheres to the given mapping
     */
    private Map<Pin, DFDVertex> copyPinDFDVertexMap(Map<DFDVertex, DFDVertex> mapping) {
        Map<Pin, DFDVertex> copiedPinDFDVertexMap = new HashMap<>();
        this.pinDFDVertexMap.keySet()
                .forEach(key -> copiedPinDFDVertexMap.put(key, mapping.getOrDefault(this.pinDFDVertexMap.get(key), this.pinDFDVertexMap.get(key)
                        .copy(mapping))));
        return copiedPinDFDVertexMap;
    }

    /**
     * Returns the Map from pin to outgoing flow that references the new correct DFD Vertex that is given by the mapping
     * @param pinDFDVertexMap Given mapping from pin to dfd vertex that each entry should respect
     * @return Returns a new map from pin to flow that is correct in the context of the given pin to vertex map
     */
    private Map<Pin, Flow> copyPinFlowMap(Map<Pin, DFDVertex> pinDFDVertexMap) {
        Map<Pin, Flow> copiedPinFlowMap = new HashMap<>();
        this.pinFlowMap.keySet()
                .forEach(key -> {
                    Pin correspondingPin = pinDFDVertexMap.get(key)
                            .getReferencedElement()
                            .getBehavior()
                            .getOutPin()
                            .stream()
                            .filter(it -> it.getEntityName()
                                    .equals(key.getEntityName()))
                            .findAny()
                            .orElseThrow();
                    Flow flow = EcoreUtil.copy(this.pinFlowMap.get(key));
                    flow.setSourcePin(correspondingPin);
                    flow.setSourceNode(pinDFDVertexMap.get(key)
                            .getReferencedElement());
                    copiedPinFlowMap.put(key, flow);
                });
        return copiedPinFlowMap;
    }

    public void setContextDependentAttributes(List<ContextDependentAttributeScenario> contextDependentAttributes) {
        this.contextDependentAttributes = contextDependentAttributes;
    }

    public List<ContextDependentAttributeScenario> getContextDependentAttributes() {
        return this.contextDependentAttributes;
    }

    public List<AssessmentFact> getRelatedElements() {
        return this.relatedElements;
    }

    public Processing getProcessing() {
        return this.processing;
    }

    public List<Data> getIncomingData() {
        return this.processing.getInputData();
    }

    public List<Data> getOutgoingData() {
        return this.processing.getOutputData();
    }

    public List<Purpose> getPurpose() {
        return this.processing.getPurpose();
    }

    public List<LegalBasis> getLegalBasis() {
        return this.processing.getOnTheBasisOf();
    }

    public Role getResponsibilityRole() {
        return this.processing.getResponsible();
    }
}
