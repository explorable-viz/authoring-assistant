package authoringassistant;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

public class FluidCLI {

    public final Logger logger = Logger.getLogger(this.getClass().getName());

    public FluidCLI() {
    }

    private String buildCommand(String fluidFileName) {
        StringBuilder command = new StringBuilder();

        command.append(STR."yarn fluid evaluate -l -p \"")
                .append(Settings.FLUID_TEMP_FOLDER)
                .append("/\" -f ")
                .append(fluidFileName);

        return command.toString();
    }

    private String executeCommand(String command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder;
        if (isWindows()) {
            processBuilder = new ProcessBuilder("cmd.exe", "/c", command);
        } else {
            processBuilder = new ProcessBuilder("/bin/bash", "-c", command);
        }
        processBuilder.redirectErrorStream(true);
        Process process = processBuilder.start();
        String output = new String(process.getInputStream().readAllBytes());
        FileUtils.deleteDirectory(new File(Settings.FLUID_TEMP_FOLDER));
        return output;
    }

    public String evaluate(String fluidFileName) {
        try {
            return executeCommand(buildCommand(fluidFileName));
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException("Error during the execution of the fluid evaluate command", e);
        }
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("win");
    }
}
