package mdpa.gdpr.analysis.core;

import java.util.Collection;
import java.util.List;

import mdpa.gdpr.analysis.utils.UncertaintyUtils;
import mdpa.gdpr.analysis.dfd.DFDGDPRVertex;
import mdpa.laf.referencemodel.LAF.AssessmentFact;
import mdpa.gdpr.metamodel.contextproperties.Expression;
import mdpa.gdpr.metamodel.contextproperties.SAFAnnotation;
import mdpa.gdpr.metamodel.contextproperties.Scope;
import mdpa.gdpr.metamodel.contextproperties.ScopeDependentAssessmentFact;
import mdpa.gdpr.metamodel.contextproperties.ScopeSet;

/**
 * This class models an application of a context dependent attribute on an element in the GDPR model. The different
 * values it can take are saved in one or multiple child {@link ContextDependentAttributeScenario}.
 */
public class ContextDependentAttributeSource {
    private final String name;
    private final AssessmentFact annotatedElement;
    private final ScopeDependentAssessmentFact scopeDependentAssessmentFact;
    private final List<ContextDependentAttributeScenario> contextDependentAttributeScenarios;

    private final SAFAnnotation annotation;
    private final List<Scope> scopes;
    private final List<ContextDependentAttributeSource> sources;
    private final boolean resolvedUncertainty;

    /**
     * Creates a new {@link ContextDependentAttributeSource} with the given property annotation containing the information
     * about the annotated element and value and the context annotation describing the context of the
     * {@link ContextDependentAttributeSource}
     * @param safAnnotation {@link SAFAnnotation} describing where the CDA is applied
     * @param scopeSet {@link ScopeSet} describing which scenarios the source has
     */
    public ContextDependentAttributeSource(SAFAnnotation safAnnotation, ScopeSet scopeSet) {
        this.name = scopeSet.getEntityName() + "@" + safAnnotation.getEntityName();
        this.annotation = safAnnotation;
        this.annotatedElement = safAnnotation.getAnnotatedElement();
        this.scopeDependentAssessmentFact = safAnnotation.getScopeDependentAssessmentFact();
        this.contextDependentAttributeScenarios = List.of(new ContextDependentAttributeScenario(scopeSet, this));
        this.scopes = scopeSet.getScope();
        this.sources = List.of();
        this.resolvedUncertainty = false;
    }

    /**
     * Creates a new {@link ContextDependentAttributeSource} that needs to be resolved with uncertain CDAs. Resolves an
     * uncertainty regarding the value of an {@link ContextDependentAttributeSource} by creating a scenario for each passed
     * {@link Expression}. Additionally, the given list of other {@link ContextDependentAttributeSource} denotes where
     * this source cannot apply
     * @param safAnnotation {@link SAFAnnotation} containing information about the annotated element and value
     * @param expressions Different {@link Expression} that are resolved by the uncertainty
     * @param sources List of {@link ContextDependentAttributeSource} that cannot be applied at the same time
     */
    public ContextDependentAttributeSource(SAFAnnotation safAnnotation, List<Expression> expressions,
            List<ContextDependentAttributeSource> sources) {
        this.name = "Unknown@" + safAnnotation.getEntityName();
        this.annotation = safAnnotation;
        this.annotatedElement = safAnnotation.getAnnotatedElement();
        this.scopeDependentAssessmentFact = safAnnotation.getScopeDependentAssessmentFact();
        this.contextDependentAttributeScenarios = expressions.stream()
                .map(it -> new ContextDependentAttributeScenario(it, this, sources))
                .toList();
        this.scopes = List.of();
        this.sources = sources;
        this.resolvedUncertainty = true;
    }

    /**
     * Determines whether this {@link ContextDependentAttributeSource} is applicable to the given list of vertices
     * @param vertices Given list of vertices
     * @return Returns true, if this {@link ContextDependentAttributeSource} is applicable at least one of the vertices
     * Otherwise, the method returns false.
     */
    public boolean applicable(Collection<DFDGDPRVertex> vertices) {
        if (!vertices.stream()
                .map(DFDGDPRVertex::getRelatedElements)
                .flatMap(List::stream)
                .toList()
                .contains(this.annotatedElement)) {
            return false;
        }
        return vertices.stream()
                .anyMatch(this::applicable);
    }

    /**
     * Determines whether the {@link ContextDependentAttributeSource} is applicable at the given vertex.
     * <p/>
     * This is the case, it the vertex has the annotated element in its context. If this
     * {@link ContextDependentAttributeSource} is resolving an uncertainty, the other saved sources must not match. If this
     * source is not resolving an uncertainty, it must match at least one context definition
     * @param vertex Given {@link DFDGDPRVertex} that is checked
     * @return Returns true, if the source is applicable to the vertex. Otherwise, the method returns false
     */
    public boolean applicable(DFDGDPRVertex vertex) {
        if (!vertex.getRelatedElements()
                .contains(this.annotatedElement)) {
            return false;
        }
        if (this.resolvedUncertainty) {
            return this.sources.stream()
                    .noneMatch(it -> it.applicable(vertex));
        }
        return this.scopes.stream()
                .anyMatch(it -> UncertaintyUtils.scopeApplicable(vertex, it));
    }

    /**
     * Returns the property type that will be applied if the source is applicable
     * @return Returns the applied {@link ScopeDependentAssessmentFact}
     */
    public ScopeDependentAssessmentFact getScopeDependentAssessmentFact() {
        return scopeDependentAssessmentFact;
    }

    /**
     * Returns the different possible {@link ContextDependentAttributeScenario} of this source
     * @return List of possible {@link ContextDependentAttributeScenario}
     */
    public List<ContextDependentAttributeScenario> getContextDependentAttributeScenarios() {
        return contextDependentAttributeScenarios;
    }

    /**
     * Returns the {@link AssessmentFact} the source is annotated to
     * @return Annotated {@link AssessmentFact}
     */
    public AssessmentFact getAnnotatedElement() {
        return annotatedElement;
    }

    /**
     * Returns the name of the {@link ContextDependentAttributeSource}
     * @return Returns the name of the source
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the {@link SAFAnnotation} that the {@link ContextDependentAttributeSource} corresponds to
     * @return Corresponding {@link SAFAnnotation}
     */
    public SAFAnnotation getAnnotation() {
        return annotation;
    }

    @Override
    public String toString() {
        return this.getName();
    }
}
