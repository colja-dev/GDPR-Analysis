package mdpa.gdpr.analysis.tests.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.List;
import java.util.stream.Collectors;
import mdpa.gdpr.analysis.core.ContextDependentAttributeScenario;
import mdpa.gdpr.analysis.dfd.DFDGDPRFlowGraphCollection;
import mdpa.gdpr.analysis.dfd.DFDGDPRTransposeFlowGraph;
import mdpa.gdpr.analysis.dfd.DFDGDPRVertex;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.utils.LoggerManager;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.junit.jupiter.api.Test;

public class TrainModelEvaluationTest extends ValidationBase {
    private Logger logger = LoggerManager.getLogger(TrainModelEvaluationTest.class);

    public TrainModelEvaluationTest() {
        super("default", "models/TrainModel");
    }

    @Test
    public void testFlowGraphAmount() {
        DFDGDPRFlowGraphCollection flowGraphs = (DFDGDPRFlowGraphCollection) this.analysis.findFlowGraphs();
        assertEquals(3, flowGraphs.getTransposeFlowGraphs()
                .size());
        var alternateFlowGraphs = flowGraphs.resolveContextDependentAttributes();

        logger.info("Number of TFGs: " + alternateFlowGraphs.getTransposeFlowGraphs()
                .size());
        assertEquals(6, alternateFlowGraphs.getTransposeFlowGraphs()
                .size());
        for (var tfg : alternateFlowGraphs.getTransposeFlowGraphs()) {
            var gdprTFG = (DFDGDPRTransposeFlowGraph) tfg;
            System.out.println("---- State: " + gdprTFG.getContextAttributeState() + " -----------");
            for (var vertex : tfg.getVertices()) {
                var referencedElement = (Node) vertex.getReferencedElement();
                System.out.println(referencedElement.getEntityName() + ", " + referencedElement.getId());
            }
            System.out.println();
        }
    }

    @Test
    public void testImpactAmount() {
        DFDGDPRFlowGraphCollection flowGraphs = (DFDGDPRFlowGraphCollection) this.analysis.findFlowGraphs();
        var alternateFlowGraphs = flowGraphs.resolveContextDependentAttributes();

        int affectedFlowGraphCount = 0;
        for (DFDGDPRTransposeFlowGraph transposeFlowGraph : alternateFlowGraphs.getTransposeFlowGraphs()
                .stream()
                .filter(DFDGDPRTransposeFlowGraph.class::isInstance)
                .map(DFDGDPRTransposeFlowGraph.class::cast)
                .toList()) {
            List<ContextDependentAttributeScenario> impactScenarios = transposeFlowGraph.getContextAttributeState()
                    .selectedScenarios()
                    .stream()
                    .toList();
            var impactedElements = this.getImpactedElements(transposeFlowGraph, impactScenarios);
            System.out.println("---- State: " + transposeFlowGraph.getContextAttributeState() + " -----------");
            System.out.println("---- Impacted Elements: -----");
            if (!impactedElements.isEmpty()) {
                affectedFlowGraphCount++;
            }
            for (var vertex : impactedElements) {
                var referencedElement = (Node) vertex.getReferencedElement();
                System.out.println(referencedElement.getEntityName() + ", " + referencedElement.getId());
            }
            System.out.println();
        }
        assertEquals(6, affectedFlowGraphCount);
    }

    @Test
    public void testViolations() {
        DFDGDPRFlowGraphCollection flowGraphs = (DFDGDPRFlowGraphCollection) this.analysis.findFlowGraphs();
        var alternateFlowGraphs = flowGraphs.resolveContextDependentAttributes();
        alternateFlowGraphs.evaluate();

        int violatingFlowGraphCount = 0;
        for (DFDGDPRTransposeFlowGraph flowGraph : alternateFlowGraphs.getTransposeFlowGraphs()
                .stream()
                .filter(DFDGDPRTransposeFlowGraph.class::isInstance)
                .map(DFDGDPRTransposeFlowGraph.class::cast)
                .toList()) {
            logger.debug("Starting new TFG with state" + flowGraph.getContextAttributeState()
                    .toString());
            var violations = this.analysis.queryDataFlow(flowGraph, it -> {
                var element = (DFDGDPRVertex) it;
                logger.debug("Element: " + ((DFDGDPRVertex) it).getName());
                for (var dc : element.getAllDataCharacteristics()) {
                    String result = dc.getAllCharacteristics()
                            .stream()
                            .map(ch -> ch.getTypeName() + "." + ch.getValueName())
                            .collect(Collectors.joining(","));
                    logger.debug("DC: " + dc.getVariableName() + " with values [" + result + "]");
                }
                String result = element.getAllVertexCharacteristics()
                        .stream()
                        .map(ch -> ch.getTypeName() + "." + ch.getValueName())
                        .collect(Collectors.joining(","));
                logger.debug("VCs: [" + result + "]");
                logger.debug("Responsibility: " + element.getResponsibilityRole()
                        .getEntityName());
                // C1: Identifiable to MarketingProvider
                if (this.hasVertexCharacteristic(element, "ThirdParty", "Marketing")
                        && this.hasDataCharacteristic(element, "Identifiability", "True")) {
                    return true;
                }

                // C2: Any Transparency at false
                if (this.hasVertexCharacteristic(element, "Transparency", "False")) {
                    return true;
                }
                return false;
            });
            if (violations.isEmpty()) {
                logger.debug("No violations found!");
                logger.debug("------------------------");
                continue;
            }
            violatingFlowGraphCount++;
            System.out.println("---- State: " + flowGraph.getContextAttributeState() + " -----------");
            System.out.println("---- Impacted Elements: -----");
            for (var vertex : flowGraph.getVertices()) {
                var referencedElement = (Node) vertex.getReferencedElement();
                System.out.println(referencedElement.getEntityName() + ", " + referencedElement.getId());
            }
            System.out.println("---- Violations: " + violations);
            System.out.println();
        }
        assertEquals(3, violatingFlowGraphCount);
    }
}
