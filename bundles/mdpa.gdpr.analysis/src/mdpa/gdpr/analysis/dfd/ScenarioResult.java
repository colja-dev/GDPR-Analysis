package mdpa.gdpr.analysis.dfd;

import java.util.List;
import java.util.Optional;
import mdpa.gdpr.analysis.core.ContextAttributeState;

/**
 * Represents the result from evaluating a {@link mdpa.gdpr.analysis.core.ContextDependentAttributeScenario} for a TFG.
 * It can either return a transpose flow graph that is the result of applying the scenario to the TFG, or return a list
 * of states that need to be explored due to failing to find a fitting
 * {@link mdpa.gdpr.analysis.core.ContextDependentAttributeScenario} for a
 * {@link mdpa.gdpr.analysis.core.ContextDependentAttributeSource}
 * @param transposeFlowGraph Optional transpose flow graph that is produced from the evaluation
 * @param states List of states that need to be explored in order to evaluate the scenario correctly
 */
public record ScenarioResult(Optional<DFDGDPRTransposeFlowGraph> transposeFlowGraph, List<ContextAttributeState> states) {

}
