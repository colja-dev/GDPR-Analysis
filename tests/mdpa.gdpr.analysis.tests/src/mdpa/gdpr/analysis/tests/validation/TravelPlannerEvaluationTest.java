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

public class TravelPlannerEvaluationTest extends ValidationBase {
    private final Logger logger = LoggerManager.getLogger(TrainModelEvaluationTest.class);

    public TravelPlannerEvaluationTest() {
        super("default", "models/TravelPlanner");
    }

    @Test
    public void testFlowGraphAmount() {
        DFDGDPRFlowGraphCollection flowGraphs = (DFDGDPRFlowGraphCollection) this.analysis.findFlowGraphs();
        assertEquals(1, flowGraphs.getTransposeFlowGraphs()
                .size());

        var alternateFlowGraphs = flowGraphs.resolveContextDependentAttributes();
        logger.info("Number of TFGs: " + alternateFlowGraphs.getTransposeFlowGraphs()
                .size());
        assertEquals(2, alternateFlowGraphs.getTransposeFlowGraphs()
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
                    .filter(it -> !it.getName()
                            .equals("UserNecessity"))
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
        assertEquals(2, affectedFlowGraphCount);
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
                // C1: Not Necessary Data
                if (this.hasDataCharacteristic(element, "Necessary", "False")) {
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
            var sourcesString = flowGraph.getContextAttributeState()
                    .selectedScenarios()
                    .stream()
                    .map(it -> it.getName())
                    .collect(Collectors.joining(","));
            logger.info("Violation in state: " + sourcesString);
            logger.info("Violating vertices:" + violations);
            logger.info("------------------------");
        }
        assertEquals(1, violatingFlowGraphCount);
    }
}
