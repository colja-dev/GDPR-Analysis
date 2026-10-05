package mdpa.gdpr.analysis.core;

import java.util.List;
import mdpa.gdpr.analysis.dfd.DFDGDPRTransposeFlowGraph;
import mdpa.gdpr.analysis.dfd.DFDGDPRVertex;
import mdpa.gdpr.analysis.utils.UncertaintyUtils;
import mdpa.gdpr.metamodel.contextproperties.Expression;
import mdpa.gdpr.metamodel.contextproperties.Scope;
import mdpa.gdpr.metamodel.contextproperties.ScopeSet;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.utils.LoggerManager;

/**
 * Models a Context Dependent Attribute Scenario that applies the given list of property values.
 * <p/>
 * As a Context Dependent Attribute Scenario can occur in two scenarios we differentiate:
 */
public class ContextDependentAttributeScenario {
    private final Logger logger = LoggerManager.getLogger(ContextDependentAttributeScenario.class);

    private final String name;

    private final List<Expression> expressions;
    private final List<Scope> scopes;
    private final List<ContextDependentAttributeSource> sources;
    private final ContextDependentAttributeSource contextDependentAttributeSource;
    private final boolean resolvedUncertainty;

    /**
     * Creates a new context dependent attribute scenario that matches a specific context. Therefore, it does not resolve an
     * uncertain CDA
     * @param scopeSet {@link ScopeSet} the Scenario requires
     * @param contextDependentAttributeSource Corresponding {@link ContextDependentAttributeSource}
     */
    public ContextDependentAttributeScenario(ScopeSet scopeSet, ContextDependentAttributeSource contextDependentAttributeSource) {
        this.name = scopeSet.getEntityName();
        this.expressions = scopeSet.getExpression();
        this.scopes = scopeSet.getScope();
        this.sources = List.of();
        this.contextDependentAttributeSource = contextDependentAttributeSource;
        this.resolvedUncertainty = false;
    }

    /**
     * Creates a new {@link ContextDependentAttributeScenario} that is resolving an uncertainty. Therefore, it requires a
     * expression that is applied, the corresponding {@link ContextDependentAttributeSource} and a list of other
     * {@link ContextDependentAttributeSource} that contradict the uncertain CDA
     * @param expression Expression that is applied, when this scenario is applied
     * @param contextDependentAttributeSource Corresponding {@link ContextDependentAttributeSource}
     * @param sources Other {@link ContextDependentAttributeSource} that must not be true
     */
    public ContextDependentAttributeScenario(Expression expression, ContextDependentAttributeSource contextDependentAttributeSource,
            List<ContextDependentAttributeSource> sources) {
        this.name = expression.getEntityName() + "@" + contextDependentAttributeSource.getName();
        this.expressions = List.of(expression);
        this.scopes = List.of();
        this.sources = sources;
        this.contextDependentAttributeSource = contextDependentAttributeSource;
        this.resolvedUncertainty = true;
    }

    /**
     * Returns whether the {@link ContextDependentAttributeScenario} is applicable to the given vertex
     * @param vertex {@link DFDGDPRVertex} that is checked
     * @return Returns true, if the scenario should be applied to the vertex. Otherwise, the method returns false
     */
    public boolean applicable(DFDGDPRVertex vertex) {
        logger.trace("Determining whether " + this.name + " can be applied to " + vertex);
        if (!vertex.getRelatedElements()
                .contains(this.contextDependentAttributeSource.getAnnotation()
                        .getAnnotatedElement())) {
            logger.trace("Cannot apply " + this.name + " to vertex, as it does not have the needed elements in context!");
            return false;
        }
        if (this.resolvedUncertainty) {
            if (!this.contextDependentAttributeSource.applicable(vertex)) {
                return false;
            }
            logger.trace("Context Dependent Attribute Scenario is resolved with uncertainties!");
            return this.sources.stream()
                    .map(it -> it.getContextDependentAttributeScenarios()
                            .get(0))
                    .noneMatch(it -> it.applicable(vertex));
        }
        return this.scopes.stream()
                .anyMatch(it -> UncertaintyUtils.scopeApplicable(vertex, it));
    }

    /**
     * Determines whether the {@link ContextDependentAttributeScenario} is applicable to any of the nodes in the given
     * transpose flow graph
     * @param transposeFlowGraph {@link DFDGDPRTransposeFlowGraph} that is checked
     * @return Returns true, if the {@link ContextDependentAttributeScenario} can be applied to any of the vertices in the
     * TFG. Otherwise, the method returns false
     */
    public boolean applicable(DFDGDPRTransposeFlowGraph transposeFlowGraph) {
        if (this.resolvedUncertainty) {
            return this.sources.stream()
                    .noneMatch(it -> {
                        var scenario = it.getContextDependentAttributeScenarios()
                                .get(0);
                        return scenario.applicable(transposeFlowGraph);
                    });
        }
        return transposeFlowGraph.getVertices()
                .stream()
                .filter(DFDGDPRVertex.class::isInstance)
                .map(DFDGDPRVertex.class::cast)
                .anyMatch(this::applicable);
    }

    /**
     * Returns the property values that are applied, when this scenario is fulfilled
     * @return Returns a list of {@link Expression} that are applied in the case the scenario is true
     */
    public List<Expression> getExpressions() {
        return this.expressions;
    }

    /**
     * Retrieves the {@link ContextDependentAttributeSource} that this scenario is a part of
     * @return Returns the parent {@link ContextDependentAttributeSource}
     */
    public ContextDependentAttributeSource getContextDependentAttributeSource() {
        return contextDependentAttributeSource;
    }

    /**
     * Returns the name of the {@link ContextDependentAttributeScenario}
     * @return The name of the {@link ContextDependentAttributeScenario}
     */
    public String getName() {
        return name;
    }
}
