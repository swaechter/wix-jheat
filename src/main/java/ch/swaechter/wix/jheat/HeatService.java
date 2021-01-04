package ch.swaechter.wix.jheat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HeatService {

    private final MessageDigest md5MessageDigest;

    public HeatService() throws IllegalStateException {
        try {
            md5MessageDigest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("Unable to initialize the MD5 message digest: " + exception.getMessage(), exception);
        }
    }

    public void buildOutputFile(HeatApplication heatApplication) throws Exception {
        // Get the absolute path representation of the input parameters so we are always working with absolute paths
        final File absoluteInputDirectory = heatApplication.inputDirectory.getAbsoluteFile();
        final File absoluteOutputFile = heatApplication.outputFile.getAbsoluteFile();

        // Check the input path
        if (!absoluteInputDirectory.isDirectory()) {
            throw new IllegalStateException("The given input path " + absoluteInputDirectory.getAbsolutePath() + " is not an existing directory!");
        }

        // Check the output path
        if (absoluteOutputFile.isDirectory()) {
            throw new IllegalStateException("The given output path " + absoluteOutputFile.getAbsolutePath() + " already exists as directory and not as file/does not exist at all!");
        }

        // Create the Wix document
        WixFile wixFile = createWixFile(heatApplication, absoluteInputDirectory);

        // Traverse all directories and files
        Element parentElement = wixFile.getDirectoryRefElement();
        traverseFilesAndDirectories(wixFile, parentElement, absoluteInputDirectory);

        // Write the file
        writeWixFileToFile(wixFile, absoluteOutputFile);
    }

    private WixFile createWixFile(HeatApplication heatApplication, File inputDirectory) throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.newDocument();

        Element wixElement = document.createElementNS("http://schemas.microsoft.com/wix/2006/wi", "Wix");
        document.appendChild(wixElement);

        Element firstFragmentElement = document.createElement("Fragment");
        wixElement.appendChild(firstFragmentElement);

        Element directoryRefElement = document.createElement("DirectoryRef");
        directoryRefElement.setAttribute("Id", heatApplication.directoryReferenceName);
        firstFragmentElement.appendChild(directoryRefElement);

        Element secondFragmentElement = document.createElement("Fragment");
        wixElement.appendChild(secondFragmentElement);

        Element componentGroupElement = document.createElement("ComponentGroup");
        componentGroupElement.setAttribute("Id", heatApplication.componentGroupName);
        secondFragmentElement.appendChild(componentGroupElement);

        return new WixFile(document, inputDirectory, directoryRefElement, componentGroupElement);
    }

    private void traverseFilesAndDirectories(WixFile wixFile, Element parentDirectoryElement, File fileOrDirectory) {
        File[] items = fileOrDirectory.listFiles();
        if (items != null) {
            for (File item : items) {
                if (item.isDirectory()) {
                    Element directoryElement = harvestDirectory(wixFile, parentDirectoryElement, item);
                    traverseFilesAndDirectories(wixFile, directoryElement, item);
                } else if (item.isFile()) {
                    harvestFile(wixFile, parentDirectoryElement, item);
                }
            }
        }
    }

    private Element harvestDirectory(WixFile wixFile, Element parentDirectoryElement, File directory) {
        String relativeFilePath = generateRelativePath(directory);
        Document document = wixFile.getDocument();

        Element directoryElement = document.createElement("Directory");
        directoryElement.setAttribute("Id", generateGuid("dir", relativeFilePath));
        directoryElement.setAttribute("Name", directory.getName());
        parentDirectoryElement.appendChild(directoryElement);
        return directoryElement;
    }

    private void harvestFile(WixFile wixFile, Element parentDirectoryElement, File file) {
        String relativeFilePath = generateRelativePath(file);
        Element componentGroupElement = wixFile.getComponentGroupElement();
        Document document = wixFile.getDocument();

        Element componentElement = document.createElement("Component");
        componentElement.setAttribute("Id", generateGuid("cmp", relativeFilePath));
        componentElement.setAttribute("Guid", "*");
        parentDirectoryElement.appendChild(componentElement);

        Element fileElement = document.createElement("File");
        fileElement.setAttribute("Id", generateGuid("fil", relativeFilePath));
        fileElement.setAttribute("KeyPath", "yes");
        fileElement.setAttribute("Source", relativeFilePath);
        componentElement.appendChild(fileElement);

        Element componentRefElement = document.createElement("ComponentRef");
        componentRefElement.setAttribute("Id", generateGuid("cmp", relativeFilePath));
        componentGroupElement.appendChild(componentRefElement);
    }

    private void writeWixFileToFile(WixFile document, File outputFile) throws TransformerException {
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();

        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4");

        DOMSource source = new DOMSource(document.getDocument());
        StreamResult streamResult = new StreamResult(outputFile);

        transformer.transform(source, streamResult);
    }

    private String generateRelativePath(File subFileOrDirectory) {
        String currentWorkingDirectory = System.getProperty("user.dir");
        String subFileOrDirectoryPath = subFileOrDirectory.getAbsolutePath();
        return subFileOrDirectoryPath.substring(currentWorkingDirectory.length() + 1); // +1 to remove trailing backslash
    }

    private String generateGuid(String prefix, String relativePath) {
        return prefix + hashString(relativePath);
    }

    public String hashString(String value) {
        byte[] hash = md5MessageDigest.digest(value.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash).toUpperCase();
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString().toLowerCase();
    }
}
