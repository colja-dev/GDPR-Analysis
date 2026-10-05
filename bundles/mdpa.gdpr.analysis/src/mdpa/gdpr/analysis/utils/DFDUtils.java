package mdpa.gdpr.analysis.utils;

import java.util.Objects;
import org.dataflowanalysis.dfd.datadictionary.AbstractAssignment;
import org.dataflowanalysis.dfd.datadictionary.Assignment;
import org.dataflowanalysis.dfd.datadictionary.Behavior;
import org.dataflowanalysis.dfd.datadictionary.Pin;
import org.dataflowanalysis.dfd.datadictionary.datadictionaryFactory;
import org.dataflowanalysis.dfd.dataflowdiagram.External;
import org.dataflowanalysis.dfd.dataflowdiagram.Node;
import org.dataflowanalysis.dfd.dataflowdiagram.Store;
import org.dataflowanalysis.dfd.dataflowdiagram.dataflowdiagramFactory;

public class DFDUtils {
    public DFDUtils() {
    }

    /**
     * Deep copies the given Node
     * @param oldNode Given node that should be copied
     * @return Copy of the given node
     */
    public Node copyNode(Node oldNode) {
        Node node;
        if (oldNode instanceof External) {
            node = dataflowdiagramFactory.eINSTANCE.createExternal();
        } else if (oldNode instanceof Store) {
            node = dataflowdiagramFactory.eINSTANCE.createStore();
        } else {
            node = dataflowdiagramFactory.eINSTANCE.createProcess();
        }
        node.setId(oldNode.getId());
        node.setEntityName(oldNode.getEntityName());
        node.getProperties()
                .addAll(oldNode.getProperties());
        Behavior behavior = datadictionaryFactory.eINSTANCE.createBehavior();
        behavior.getInPin()
                .addAll(oldNode.getBehavior()
                        .getInPin()
                        .stream()
                        .map(pin -> {
                            Pin copy = datadictionaryFactory.eINSTANCE.createPin();
                            copy.setId(pin.getId());
                            copy.setEntityName(pin.getEntityName());
                            return copy;
                        })
                        .toList());
        behavior.getOutPin()
                .addAll(oldNode.getBehavior()
                        .getOutPin()
                        .stream()
                        .map(pin -> {
                            Pin copy = datadictionaryFactory.eINSTANCE.createPin();
                            copy.setId(pin.getId());
                            copy.setEntityName(pin.getEntityName());
                            return copy;
                        })
                        .toList());
        behavior.getAssignment()
                .forEach(it -> {
                    AbstractAssignment copy;
                    if (it instanceof Assignment oldAssignment) {
                        Assignment newAssignment = datadictionaryFactory.eINSTANCE.createAssignment();
                        for (Pin inputPin : oldAssignment.getInputPins()) {
                            newAssignment.getInputPins()
                                    .add(behavior.getInPin()
                                            .stream()
                                            .filter(pin -> Objects.equals(inputPin.getId(), pin.getId()))
                                            .findFirst()
                                            .orElseThrow());
                        }
                        copy = newAssignment;
                    } else {
                        copy = datadictionaryFactory.eINSTANCE.createForwardingAssignment();
                    }
                    copy.setId(it.getId());
                    copy.setEntityName(it.getEntityName());
                    copy.setOutputPin(behavior.getInPin()
                            .stream()
                            .filter(pin -> Objects.equals(it.getOutputPin()
                                    .getId(), pin.getId()))
                            .findFirst()
                            .orElseThrow());
                });
        node.setBehavior(behavior);
        return node;
    }
}