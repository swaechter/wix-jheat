package ch.swaechter.wix.jheat;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.File;

public class WixFile {

    private final Document document;

    private final File inputDirectory;

    private final Element directoryRefElement;

    private final Element componentGroupElement;

    public WixFile(Document document, File inputDirectory,  Element directoryRefElement, Element componentGroupElement) {
        this.document = document;
        this.inputDirectory = inputDirectory;
        this.directoryRefElement = directoryRefElement;
        this.componentGroupElement = componentGroupElement;
    }

    public Document getDocument() {
        return document;
    }

    public File getInputDirectory() {
        return inputDirectory;
    }

    public Element getDirectoryRefElement() {
        return directoryRefElement;
    }

    public Element getComponentGroupElement() {
        return componentGroupElement;
    }
}
