package ch.swaechter.wix.jheat;

import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

import java.io.File;
import java.util.concurrent.Callable;

@Command(name = "wix-jheat", mixinStandardHelpOptions = true, version = "0.0.1", description = "A more convenient implementation of the WiX heat.exe tool")
public class HeatApplication implements Callable<Integer> {

    @Parameters(index = "0", description = "Directory to harvest files and subdirectories")
    public File inputDirectory;

    @Parameters(index = "1", description = "File to serve the generated fragment")
    public File outputFile;

    @Parameters(index = "2", description = "Name of the directory reference")
    public String directoryReferenceName;

    @Parameters(index = "3", description = "Name of the component group")
    public String componentGroupName;

    @Option(names = {"--log-files"}, description = "Log the traversed files")
    public boolean logFiles = true;

    @Override
    public Integer call() throws Exception {
        System.out.println("JHeat launched with input directory " + inputDirectory.getAbsolutePath() + " and output file " + outputFile.getAbsolutePath());
        HeatService heatService = new HeatService();
        heatService.buildOutputFile(this);
        return 0;
    }

    public static void main(String[] arguments) {
        try {
            int exitCode = new CommandLine(HeatApplication.class).setCaseInsensitiveEnumValuesAllowed(true).execute(arguments);
            System.exit(exitCode);
        } catch (Exception exception) {
            System.err.println("An error occurred: " + exception.getMessage());
            System.exit(1);
        }
    }
}
