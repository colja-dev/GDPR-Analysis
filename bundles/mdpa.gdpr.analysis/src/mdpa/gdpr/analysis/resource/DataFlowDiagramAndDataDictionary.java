package mdpa.gdpr.analysis.resource;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Map;
import org.dataflowanalysis.dfd.datadictionary.DataDictionary;
import org.dataflowanalysis.dfd.dataflowdiagram.DataFlowDiagram;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.xmi.XMLResource;
import org.eclipse.emf.ecore.xmi.impl.XMLResourceFactoryImpl;

/**
 * Contains a full metamodel required to run a DFD-based
 * {@link org.dataflowanalysis.analysis.DataFlowConfidentialityAnalysis}
 * @param dataFlowDiagram Data Flow Diagram of the model
 * @param dataDictionary Data Dictionary of the model
 */
public record DataFlowDiagramAndDataDictionary(DataFlowDiagram dataFlowDiagram, DataDictionary dataDictionary) {
    private Resource createResource(String outputFile, String[] fileExtensions, ResourceSet resourceSet) {
        for (String fileExtension : fileExtensions) {
            resourceSet.getResourceFactoryRegistry()
                    .getExtensionToFactoryMap()
                    .put(fileExtension, new XMLResourceFactoryImpl());
        }
        URI uri = URI.createFileURI(outputFile);
        return resourceSet.createResource(uri);
    }

    private void saveResource(Resource resource) {
        Map<Object, Object> saveOptions = ((XMLResource) resource).getDefaultSaveOptions();
        try {
            resource.save(saveOptions);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Saves the given {@link DataFlowDiagramAndDataDictionary} at the given file path with the given name
     * @param filePath File path the {@link DataFlowDiagramAndDataDictionary} should be saved at
     * @param fileName File name (without extension) that the files should have
     */
    public void save(String filePath, String fileName) {
        ResourceSet resourceSet = new ResourceSetImpl();
        Path basePath = Path.of(filePath, fileName)
                .toAbsolutePath()
                .normalize();

        Resource dfdResource = createResource(basePath + ".dataflowdiagram", new String[] {"dataflowdiagram"}, resourceSet);
        Resource ddResource = createResource(basePath + ".datadictionary", new String[] {"datadictionary"}, resourceSet);

        dfdResource.getContents()
                .add(dataFlowDiagram);
        ddResource.getContents()
                .add(dataDictionary);

        saveResource(dfdResource);
        saveResource(ddResource);
    }
}
