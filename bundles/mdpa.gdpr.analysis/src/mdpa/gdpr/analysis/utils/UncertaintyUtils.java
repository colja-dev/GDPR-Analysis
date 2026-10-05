package mdpa.gdpr.analysis.utils;

import java.util.ArrayList;
import java.util.List;
import mdpa.gdpr.analysis.core.ContextDependentAttributeScenario;
import mdpa.gdpr.analysis.core.ContextDependentAttributeSource;
import mdpa.gdpr.analysis.dfd.DFDGDPRVertex;
import mdpa.gdpr.metamodel.GDPR.Data;
import mdpa.gdpr.metamodel.GDPR.NaturalPerson;
import mdpa.gdpr.metamodel.GDPR.PersonalData;
import mdpa.gdpr.metamodel.contextproperties.Expression;
import mdpa.gdpr.metamodel.contextproperties.LAFScopeElement;
import mdpa.gdpr.metamodel.contextproperties.Scope;
import org.apache.log4j.Logger;
import org.dataflowanalysis.analysis.utils.LoggerManager;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.datadictionary.Label;
import org.dataflowanalysis.dfd.datadictionary.LabelType;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;

public class UncertaintyUtils {
    private static final Logger logger = LoggerManager.getLogger(UncertaintyUtils.class);

    /**
     * Modify the behavior for an impacted element using the given CDA Source and Scenario on the given impacted data.
     * @param element Original DFD {@link Node} that is impacted
     * @param impactedElement Impacted {@link DFDGDPRVertex}
     * @param dataDictionary Data dictionary required to determine the labels that should be applied
     * @param source {@link ContextDependentAttributeSource} that is impacting the element
     * @param scenario {@link ContextDependentAttributeScenario} that is impacting the element
     * @param targetedPerson {@link NaturalPerson} element from the GDPR model that is targeted by the CDA
     */
    public static void impactBehavior(Node element, DFDGDPRVertex impactedElement, DataDictionary dataDictionary,
            ContextDependentAttributeSource source, ContextDependentAttributeScenario scenario, NaturalPerson targetedPerson) {
        logger.debug("Modifying behavior for impacted element " + element.getEntityName());

        List<PersonalData> targetedData = impactedElement.getOutgoingData()
                .stream()
                .filter(PersonalData.class::isInstance)
                .map(PersonalData.class::cast)
                .distinct()
                .filter(it -> it.getDataReferences()
                        .contains(targetedPerson))
                .toList();

        for (PersonalData targetData : targetedData) {
            UncertaintyUtils.impactBehavior(element, impactedElement, dataDictionary, source, scenario, targetData);
        }
    }

    /**
     * Modify the behavior for an impacted element using the given CDA Source and Scenario on the given impacted data.
     * @param element Original DFD {@link Node} that is impacted
     * @param impactedElement Impacted {@link DFDGDPRVertex}
     * @param dataDictionary Data dictionary required to determine the labels that should be applied
     * @param source {@link ContextDependentAttributeSource} that is impacting the element
     * @param scenario {@link ContextDependentAttributeScenario} that is impacting the element
     * @param targetedData {@link Data} element from the GDPR model that is targeted by the CDA
     */
    public static void impactBehavior(Node element, DFDGDPRVertex impactedElement, DataDictionary dataDictionary,
            ContextDependentAttributeSource source, ContextDependentAttributeScenario scenario, Data targetedData) {
        if (element.getBehavior()
                .getOutPin()
                .stream()
                .noneMatch(it -> it.getEntityName()
                        .equals(targetedData.getEntityName()))) {
            logger.info("Scenario" + scenario.getName() + " does not impact " + impactedElement.getName());
            return;
        }

        logger.debug("Modifying behavior for impacted element " + element.getEntityName());

        if (element.getBehavior()
                .getOutPin()
                .isEmpty()) {
            logger.debug("Behavior for the element will not be modified, as it does not output any data!");
            return;
        }

        Pin outputPin = element.getBehavior()
                .getOutPin()
                .stream()
                .filter(it -> it.getEntityName()
                        .equals(targetedData.getEntityName()))
                .findAny()
                .orElseThrow();
        element.getBehavior()
                .getAssignment()
                .add(UncertaintyUtils.createImpactAssignment(element, outputPin, scenario, source, dataDictionary));

        for (Data data : impactedElement.getOutgoingData()) {
            if (!(data instanceof PersonalData personalData)) {
                continue;
            }
            Pin dataOutputPin = element.getBehavior()
                    .getOutPin()
                    .stream()
                    .filter(pin -> pin.getEntityName()
                            .equals(personalData.getEntityName()))
                    .findAny()
                    .orElseThrow(() -> new IllegalArgumentException("Element does not have output pin named after outgoing personal data!"));
            element.getBehavior()
                    .getAssignment()
                    .add(UncertaintyUtils.createPersonAssignment(element, dataOutputPin, personalData, dataDictionary));
        }
    }

    /**
     * Creates an assignment that applies the labels of a given source and scenario to the given element at the given pin
     * @param element Element that the source and scenario is applied to
     * @param outputPin Output pin of the element that is impacted
     * @param scenario {@link ContextDependentAttributeScenario} that is applied
     * @param source {@link ContextDependentAttributeSource} that is applied
     * @param dataDictionary Data dictionary used to resolve the labels from the CDA source and scenario
     * @return Returns an assignment that set the labels that the CDA source and scenario should set
     */
    private static Assignment createImpactAssignment(Node element, Pin outputPin, ContextDependentAttributeScenario scenario,
            ContextDependentAttributeSource source, DataDictionary dataDictionary) {
        List<Label> values = UncertaintyUtils.getAppliedLabel(source, scenario, dataDictionary);
        Assignment attributeAssignment = datadictionaryFactory.eINSTANCE.createAssignment();
        attributeAssignment.setTerm(datadictionaryFactory.eINSTANCE.createTRUE());
        attributeAssignment.getInputPins()
                .addAll(element.getBehavior()
                        .getInPin());
        attributeAssignment.setOutputPin(outputPin);
        attributeAssignment.getOutputLabels()
                .addAll(values);
        return attributeAssignment;
    }

    /**
     * Creates an assignment for a data pin that set the labels of the personal data and its corresponding natural person
     * @param element Element that the assignment should be applied to
     * @param dataOutputPin Output pin that the assignment should be applied to
     * @param personalData Personal data that references natural persons
     * @param dataDictionary Data dictionary used to resolve labels
     * @return Returns an assignment that sets label for the natural persons for a given piece of personal data
     */
    private static Assignment createPersonAssignment(Node element, Pin dataOutputPin, PersonalData personalData, DataDictionary dataDictionary) {
        Assignment assignment = datadictionaryFactory.eINSTANCE.createAssignment();
        assignment.getInputPins()
                .addAll(element.getBehavior()
                        .getInPin());
        assignment.setOutputPin(dataOutputPin);
        assignment.setTerm(datadictionaryFactory.eINSTANCE.createTRUE());
        personalData.getDataReferences()
                .forEach(person -> {
                    assignment.getOutputLabels()
                            .add(UncertaintyUtils.getLabelForNaturalPerson(dataDictionary, person));
                });
        assignment.setEntityName("Send " + personalData.getEntityName());
        return assignment;
    }

    /**
     * Determines the label for a given natural person
     * @param dataDictionary Data dictionary used to resolve the labels
     * @param person Natural person that the label should correspond to
     * @return Returns the label that corresponds to the given natural person
     */
    private static Label getLabelForNaturalPerson(DataDictionary dataDictionary, NaturalPerson person) {
        LabelType labelType = dataDictionary.getLabelTypes()
                .stream()
                .filter(it -> it.getEntityName()
                        .equals("NaturalPerson"))
                .findAny()
                .orElseThrow();
        return labelType.getLabel()
                .stream()
                .filter(it -> it.getEntityName()
                        .equals(person.getEntityName()))
                .findAny()
                .orElseThrow();
    }

    /**
     * Determines whether the given scope is matched by the given vertex element
     * @param vertex Vertex element that the scope is matched against
     * @param scope Scope that is checked
     * @return Returns true, if the scope is applicable at the given vertex. Otherwis,e the method returns false.
     */
    public static boolean scopeApplicable(DFDGDPRVertex vertex, Scope scope) {
        return scope.getLafScopeElements()
                .stream()
                .allMatch(it -> UncertaintyUtils.scopeElementApplicable(vertex, it));
    }

    /**
     * Determines whether a scope element is matched by the given vertex
     * @param vertex Given vertex element that the scope element is matched against
     * @param scopeElement Scope element that is checked
     * @return Returns true, if the given scope element matches the vertex. Otherwise, the method returns false.
     */
    public static boolean scopeElementApplicable(DFDGDPRVertex vertex, LAFScopeElement scopeElement) {
        boolean matches = vertex.getRelatedElements()
                .contains(scopeElement.getLafElement());
        if (scopeElement.isNegated()) {
            return !matches;
        } else {
            return matches;
        }
    }

    /**
     * Determines when a CDA should be reapplied at a vertex. This happens in the following cases: - Initial application of
     * the CDA - Context between vertices has changed
     * @param matchingVertices Total list of vertices that have been matched
     * @param vertex Vertex that checked
     * @return Returns true, if the CDA should be reapplied at the given vertex. Otherwise, the method returns false.
     */
    public static boolean shouldReapply(List<DFDGDPRVertex> matchingVertices, DFDGDPRVertex vertex) {
        if (vertex.getPreviousElements()
                .stream()
                .filter(DFDGDPRVertex.class::isInstance)
                .map(DFDGDPRVertex.class::cast)
                .noneMatch(matchingVertices::contains)) {
            return true;
        }
        return vertex.getPreviousElements()
                .stream()
                .filter(DFDGDPRVertex.class::isInstance)
                .map(DFDGDPRVertex.class::cast)
                .noneMatch(it -> it.getResponsibilityRole()
                        .equals(vertex.getResponsibilityRole()));
    }

    /**
     * Returns a list of Labels that should be applied for the given CDA source and scenario
     * @param source {@link ContextDependentAttributeSource} that is applied
     * @param scenario {@link ContextDependentAttributeScenario} that is applied
     * @param dataDictionary Data dictionary used to resolve the labels
     * @return Returns a list of labels that are applied by the given CDA source and scenario
     */
    public static List<Label> getAppliedLabel(ContextDependentAttributeSource source, ContextDependentAttributeScenario scenario,
            DataDictionary dataDictionary) {
        LabelType labelType = dataDictionary.getLabelTypes()
                .stream()
                .filter(it -> it.getEntityName()
                        .equals(source.getScopeDependentAssessmentFact()
                                .getEntityName()))
                .findAny()
                .orElseThrow();
        List<Label> labels = new ArrayList<>();
        for (Expression expression : scenario.getExpressions()) {
            Label label = labelType.getLabel()
                    .stream()
                    .filter(it -> it.getEntityName()
                            .equals(expression.getEntityName()))
                    .findAny()
                    .orElseThrow();
            labels.add(label);
        }
        return labels;
    }
}
