package mdpa.gdpr.analysis.validation;

import java.util.List;
import java.util.UUID;
import mdpa.gdpr.metamodel.GDPR.*;
import mdpa.gdpr.metamodel.contextproperties.ScopeSet;
import mdpa.gdpr.metamodel.contextproperties.Scope;
import mdpa.gdpr.metamodel.contextproperties.ScopeDependentAssessmentFacts;
import mdpa.gdpr.metamodel.contextproperties.ContextpropertiesFactory;
import mdpa.gdpr.metamodel.contextproperties.LAFScopeElement;
import mdpa.gdpr.metamodel.contextproperties.ScopeDependentAssessmentFact;
import mdpa.gdpr.metamodel.contextproperties.SAFAnnotation;
import mdpa.gdpr.metamodel.contextproperties.Expression;
import mdpa.laf.referencemodel.LAF.AssessmentFact;

public class GDPRModelBuilder {
    private final LegalAssessmentFacts gdprModel;
    private final ScopeDependentAssessmentFacts contextDependentAttributes;

    private Processing lastElement;
    private final Processing firstElement;
    private final Controller defaultController;
    private final Purpose defaultPurpose;
    private final LegalBasis defaultLegalBasis;
    private final PersonalData defaultPersonalData;
    private final NaturalPerson defaultNaturalPerson;

    public GDPRModelBuilder() {
        this.gdprModel = GDPRFactory.eINSTANCE.createLegalAssessmentFacts();
        this.contextDependentAttributes = ContextpropertiesFactory.eINSTANCE.createScopeDependentAssessmentFacts();
        this.defaultController = this.createController("Default Controller");
        this.defaultPurpose = this.createPurpose("Default Purpose");
        this.defaultNaturalPerson = this.createNaturalPerson("Default Natural Person");
        this.defaultLegalBasis = this.createConsentLegalBasis("Default Legal Basis", this.defaultPurpose, this.defaultNaturalPerson);
        this.defaultPersonalData = this.createPersonalData("Default Personal Data", this.defaultNaturalPerson);
        Collecting element = GDPRFactory.eINSTANCE.createCollecting();
        element.setEntityName("Collecting");
        element.setId(String.valueOf(UUID.randomUUID()));
        element.setResponsible(this.defaultController);
        element.getPurpose()
                .add(this.defaultPurpose);
        element.getOnTheBasisOf()
                .add(this.defaultLegalBasis);
        element.getOutputData()
                .add(this.defaultPersonalData);
        gdprModel.getActions()
                .add(element);
        this.firstElement = element;
        this.lastElement = element;
    }

    public Processing createProcessingElement(String name) {
        Processing element = GDPRFactory.eINSTANCE.createProcessing();
        this.updateFlowElement(element, name, this.defaultController);
        return element;
    }

    public Collecting createCollectingElement(String name) {
        Collecting element = GDPRFactory.eINSTANCE.createCollecting();
        this.updateFlowElement(element, name, this.defaultController);
        return element;
    }

    public Storing createStoringElement(String name) {
        Storing element = GDPRFactory.eINSTANCE.createStoring();
        this.updateFlowElement(element, name, this.defaultController);
        return element;
    }

    public Processing createProcessingElement(String name, Role role) {
        Processing element = GDPRFactory.eINSTANCE.createProcessing();
        this.updateFlowElement(element, name, role);
        return element;
    }

    public Collecting createCollectingElement(String name, Role role) {
        Collecting element = GDPRFactory.eINSTANCE.createCollecting();
        this.updateFlowElement(element, name, role);
        return element;
    }

    public Storing createStoringElement(String name, Role role) {
        Storing element = GDPRFactory.eINSTANCE.createStoring();
        this.updateFlowElement(element, name, role);
        return element;
    }

    private void updateFlowElement(Processing element, String name, Role role) {
        element.setEntityName(name);
        element.setId(String.valueOf(UUID.randomUUID()));
        element.setResponsible(role);
        element.getInputData()
                .add(this.defaultPersonalData);
        element.getPurpose()
                .add(this.defaultPurpose);
        element.getOnTheBasisOf()
                .add(this.defaultLegalBasis);
        this.lastElement.getFollowingProcessing()
                .add(element);
        this.gdprModel.getActions()
                .add(element);
        this.lastElement = element;
    }

    public Controller createController(String name) {
        Controller role = GDPRFactory.eINSTANCE.createController();
        role.setName(name);
        role.setEntityName(name);
        role.setId(String.valueOf(UUID.randomUUID()));
        this.gdprModel.getSubjects()
                .add(role);
        return role;
    }

    public Purpose createPurpose(String name) {
        Purpose purpose = GDPRFactory.eINSTANCE.createPurpose();
        purpose.setEntityName(name);
        purpose.setId(String.valueOf(UUID.randomUUID()));
        this.gdprModel.getContext()
                .add(purpose);
        return purpose;
    }

    public Consent createConsentLegalBasis(String name, Purpose purpose, NaturalPerson consentee) {
        Consent legalBasis = GDPRFactory.eINSTANCE.createConsent();
        legalBasis.setEntityName(name);
        legalBasis.setId(String.valueOf(UUID.randomUUID()));
        legalBasis.getForPurpose()
                .add(purpose);
        legalBasis.setConsentee(consentee);
        this.gdprModel.getContext()
                .add(legalBasis);
        return legalBasis;
    }

    public NaturalPerson createNaturalPerson(String name) {
        NaturalPerson naturalPerson = GDPRFactory.eINSTANCE.createNaturalPerson();
        naturalPerson.setName(name);
        naturalPerson.setEntityName(name);
        naturalPerson.setId(String.valueOf(UUID.randomUUID()));
        this.gdprModel.getContext()
                .add(naturalPerson);
        return naturalPerson;
    }

    public PersonalData createPersonalData(String name, NaturalPerson naturalPerson) {
        PersonalData personalData = GDPRFactory.eINSTANCE.createPersonalData();
        personalData.setEntityName(name);
        personalData.setId(String.valueOf(UUID.randomUUID()));
        personalData.getDataReferences()
                .add(naturalPerson);
        this.gdprModel.getObjects()
                .add(personalData);
        return personalData;
    }

    public ScopeDependentAssessmentFact createProperty(String name, List<String> values) {
        ScopeDependentAssessmentFact property = ContextpropertiesFactory.eINSTANCE.createScopeDependentAssessmentFact();
        property.setEntityName(name);
        property.setId(String.valueOf(UUID.randomUUID()));
        for (String value : values) {
            Expression propertyValue = ContextpropertiesFactory.eINSTANCE.createExpression();
            propertyValue.setParentAssessmentFact(property);
            propertyValue.setEntityName(value);
            propertyValue.setId(String.valueOf(UUID.randomUUID()));
        }
        this.contextDependentAttributes.getScopeDependentAssessmentFact()
                .add(property);
        return property;
    }

    public SAFAnnotation createPropertyAnnotation(AssessmentFact annotatedElement, ScopeDependentAssessmentFact property) {
        SAFAnnotation propertyAnnotation = ContextpropertiesFactory.eINSTANCE.createSAFAnnotation();
        propertyAnnotation.setAnnotatedElement(annotatedElement);
        propertyAnnotation.setScopeDependentAssessmentFact(property);
        this.contextDependentAttributes.getSafAnnotation()
                .add(propertyAnnotation);
        return propertyAnnotation;
    }

    public ScopeSet createContextAnnotation(String name, List<Expression> propertyValues, SAFAnnotation propertyAnnotation) {
        ScopeSet contextAnnotation = ContextpropertiesFactory.eINSTANCE.createScopeSet();
        contextAnnotation.setEntityName(name);
        contextAnnotation.setId(String.valueOf(UUID.randomUUID()));
        contextAnnotation.getExpression()
                .addAll(propertyValues);
        propertyAnnotation.getScopeSet()
                .add(contextAnnotation);
        return contextAnnotation;
    }

    public Scope createContextDefinition(String name, AssessmentFact requiredElement, ScopeSet contextAnnotation) {
        Scope contextDefinition = ContextpropertiesFactory.eINSTANCE.createScope();
        contextDefinition.setEntityName(name);
        contextDefinition.setId(String.valueOf(UUID.randomUUID()));

        LAFScopeElement gdprContextElement = ContextpropertiesFactory.eINSTANCE.createLAFScopeElement();
        gdprContextElement.setLafElement(requiredElement);
        contextDefinition.getLafScopeElements()
                .add(gdprContextElement);

        contextAnnotation.getScope()
                .add(contextDefinition);
        this.contextDependentAttributes.getScope()
                .add(contextDefinition);
        return contextDefinition;
    }

    public Scope createContextDefinition(String name, List<? extends AssessmentFact> requiredElements,
            ScopeSet contextAnnotation) {
        Scope contextDefinition = ContextpropertiesFactory.eINSTANCE.createScope();
        contextDefinition.setEntityName(name);
        contextDefinition.setId(String.valueOf(UUID.randomUUID()));

        for (AssessmentFact requiredElement : requiredElements) {
            LAFScopeElement gdprContextElement = ContextpropertiesFactory.eINSTANCE.createLAFScopeElement();
            gdprContextElement.setLafElement(requiredElement);
            contextDefinition.getLafScopeElements()
                    .add(gdprContextElement);
        }

        contextAnnotation.getScope()
                .add(contextDefinition);
        this.contextDependentAttributes.getScope()
                .add(contextDefinition);
        return contextDefinition;
    }

    public Processing getFirstElement() {
        return firstElement;
    }

    public Controller getDefaultController() {
        return defaultController;
    }

    public LegalBasis getDefaultLegalBasis() {
        return defaultLegalBasis;
    }

    public NaturalPerson getDefaultNaturalPerson() {
        return defaultNaturalPerson;
    }

    public PersonalData getDefaultPersonalData() {
        return defaultPersonalData;
    }

    public Purpose getDefaultPurpose() {
        return defaultPurpose;
    }

    public ScopeDependentAssessmentFacts getContextDependentAttributes() {
        return contextDependentAttributes;
    }

    public LegalAssessmentFacts getGdprModel() {
        return gdprModel;
    }
}
