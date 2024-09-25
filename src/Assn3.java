import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Assn3 {
    private static final List<String> history = new ArrayList<>();
    private static long totalExecutionTime = 0;

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        String homeDir = System.getProperty("user.home");
        String currentDir = System.getProperty("user.dir");

        while (true) {
            System.out.print("[" + currentDir + "]: ");
            String input = scanner.nextLine().trim();

            if (input.isEmpty()) {
                continue;
            }

            // Check for pipe ("|") and handle commands separately
            if (input.contains("|")) {
                String[] parts = input.split("\\|");  // "\\|" is used because "|" is a special character in regex

                // Trim the parts to remove any leading or trailing spaces
                String leftArgument = parts[0].trim();
                String rightArgument = parts[1].trim();

                // Execute both left and right commands with pipe handling
                executePipeCommand(leftArgument, rightArgument, currentDir);
            } else if (input.startsWith("^")) {
                // Handle history command

                System.out.println("input: " + input);
                executeHistoryCommand(input, currentDir);
            } else {
                // Regular commands
                history.add(input);
                String[] tokens = splitCommand(input);
                if (tokens.length == 0) continue;

                String command = tokens[0];

                switch (command) {
                    case "exit":
                        return;
                    case "ptime":
                        System.out.printf("Total time in child processes: %.4f seconds\n", totalExecutionTime / 1000.0);
                        break;
                    case "history":
                        printHistory();
                        break;
                    case "list":
                        listFiles(currentDir);
                        break;
                    case "cd":
                        currentDir = changeDirectory(tokens, currentDir, homeDir);
                        break;
                    case "mdir":
                        if (tokens.length > 1) {
                            createDirectory(tokens[1], currentDir);
                        } else {
                            System.out.println("mdir: missing directory name.");
                        }
                        break;
                    case "rdir":
                        if (tokens.length > 1) {
                            removeDirectory(tokens[1], currentDir);
                        } else {
                            System.out.println("rdir: missing directory name.");
                        }
                        break;
                    default:
                        executeCommand(input, currentDir);
                        break;
                }
            }
        }
    }


    private static void executePipeCommand(String leftCommand, String rightCommand, String currentDir) {
        try {
            // First command
            ProcessBuilder firstProcessBuilder = new ProcessBuilder(splitCommand(leftCommand));
            firstProcessBuilder.directory(new File(currentDir));
            Process firstProcess = firstProcessBuilder.start();

            // Capture output of the first command
            BufferedReader firstOutputReader = new BufferedReader(new InputStreamReader(firstProcess.getInputStream()));

            // Second command
            ProcessBuilder secondProcessBuilder = new ProcessBuilder(splitCommand(rightCommand));
            secondProcessBuilder.directory(new File(currentDir));
            Process secondProcess = secondProcessBuilder.start();

            // Send output of the first command as input to the second command
            BufferedWriter secondInputWriter = new BufferedWriter(new OutputStreamWriter(secondProcess.getOutputStream()));
            String line;
            while ((line = firstOutputReader.readLine()) != null) {
                secondInputWriter.write(line);
                secondInputWriter.newLine();
            }
            secondInputWriter.flush();
            secondInputWriter.close();

            // Measure execution time for both processes
            long startTime = System.currentTimeMillis();
            firstProcess.waitFor();
            long endTime = System.currentTimeMillis();
            totalExecutionTime += (endTime - startTime);

            startTime = System.currentTimeMillis();
            secondProcess.waitFor();
            endTime = System.currentTimeMillis();
            totalExecutionTime += (endTime - startTime);

            // Display the output of the second command
            BufferedReader secondOutputReader = new BufferedReader(new InputStreamReader(secondProcess.getInputStream()));
            while ((line = secondOutputReader.readLine()) != null) {
                System.out.println(line);
            }

        } catch (IOException | InterruptedException e) {
            System.out.println("Error handling pipe between commands: " + e.getMessage());
        }
    }

    private static void executeHistoryCommand(String input, String currentDir) {
//        String[] tokens = input.trim().split("\\s+");
        String[] tokens = splitCommand(input);
        String homeDir = System.getProperty("user.home");

        if (tokens.length > 1) {
            try {
                if (tokens.length == 0) {
                    System.out.println("Index Not Valid");
                };

                String command = tokens[0];
                System.out.println("Command " + command);
                switch (command) {
                    case "exit":
                        return;
                    case "ptime":
                        System.out.printf("Total time in child processes: %.4f seconds\n", totalExecutionTime / 1000.0);
                        break;
                    case "history":
                        printHistory();
                        break;
                    case "list":
                        listFiles(currentDir);
                        break;
                    case "cd":
                        currentDir = changeDirectory(tokens, currentDir, homeDir);
                        break;
                    case "mdir":
                        if (tokens.length > 1) {
                            createDirectory(tokens[1], currentDir);
                        } else {
                            System.out.println("mdir: missing directory name.");
                        }
                        break;
                    case "rdir":
                        if (tokens.length > 1) {
                            removeDirectory(tokens[1], currentDir);
                        } else {
                            System.out.println("rdir: missing directory name.");
                        }
                        break;
                    default:
                        executeCommand(input, currentDir);
                        break;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid history index.");
            }
        } else {
            System.out.println("Invalid history command format.");
        }
    }


    private static void executeCommand(String input, String currentDir) {
        String[] tokens = splitCommand(input);
        System.out.println("Execute input "+ input);
        if (tokens.length == 0) return;

        boolean runInBackground = input.endsWith("&");
        if (runInBackground) {
            input = input.substring(0, input.length() - 1).trim();
        }

        ProcessBuilder processBuilder = new ProcessBuilder(input);
        processBuilder.directory(new File(currentDir));
        System.out.println("process " + processBuilder);
        try {
            long startTime = System.currentTimeMillis();
            Process process = processBuilder.start();
            if (!runInBackground) {
                process.waitFor();
                long endTime = System.currentTimeMillis();
                totalExecutionTime += (endTime - startTime);
            }

            BufferedReader processReader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line;
            while ((line = processReader.readLine()) != null) {
                System.out.println(line);
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Invalid command: " + input);
        }
    }


    private static String[] splitCommand(String command) {
        List<String> matchList = new ArrayList<>();
        Pattern regex = Pattern.compile("[^\\s\"']+|\"([^\"]*)\"|'([^']*)'");
        Matcher regexMatcher = regex.matcher(command);
        while (regexMatcher.find()) {
            if (regexMatcher.group(1) != null) {
                matchList.add(regexMatcher.group(1));
            } else if (regexMatcher.group(2) != null) {
                matchList.add(regexMatcher.group(2));
            } else {
                matchList.add(regexMatcher.group());
            }
        }
        return matchList.toArray(new String[0]);
    }

    private static void printHistory() {
        System.out.println("-- Command History --");
        for (int i = 0; i < history.size(); i++) {
            System.out.println((i + 1) + ": " + history.get(i));
        }
    }

    private static void listFiles(String currentDir) {
        File dir = new File(currentDir);
        File[] files = dir.listFiles();
        if (files == null) return;

        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy HH:mm");
        for (File file : files) {
            String permissions = (file.isDirectory() ? "d" : "-")
                    + (file.canRead() ? "r" : "-")
                    + (file.canWrite() ? "w" : "-")
                    + (file.canExecute() ? "x" : "-");

            String size = String.format("%10d", file.length());
            String lastModified = sdf.format(file.lastModified());
            System.out.printf("%s %s %s %s\n", permissions, size, lastModified, file.getName());
        }
    }

    private static String changeDirectory(String[] tokens, String currentDir, String homeDir) {
        if (tokens.length == 1) {
            // If only "cd" is typed, go to the home directory
            return homeDir;
        } else {
            // Handle special cases like "cd .." and "cd ."
            String targetDir = tokens[1];
            File newDir;

            if (targetDir.equals("..")) {
                // Move one directory up
                newDir = new File(currentDir).getParentFile();
            } else if (targetDir.equals(".")) {
                // Stay in the current directory
                newDir = new File(currentDir);
            } else {
                // Handle normal cases like "cd folder"
                newDir = new File(currentDir, targetDir);
            }

            if (newDir != null && newDir.isDirectory()) {
                return newDir.getAbsolutePath();
            } else {
                System.out.println("cd: no such file or directory: " + targetDir);
                return currentDir;
            }
        }
    }

    private static void createDirectory(String dirName, String currentDir) {
        File dir = new File(currentDir, dirName);
        if (!dir.exists()) {
            if (dir.mkdir()) {
                System.out.println("Directory created: " + dirName);
            } else {
                System.out.println("mdir: unable to create directory: " + dirName);
            }
        } else {
            System.out.println("mdir: directory already exists: " + dirName);
        }
    }

    private static void removeDirectory(String dirName, String currentDir) {
        File dir = new File(currentDir, dirName);
        if (dir.exists() && dir.isDirectory()) {
            if (dir.delete()) {
                System.out.println("Directory removed: " + dirName);
            } else {
                System.out.println("rdir: unable to remove directory: " + dirName);
            }
        } else {
            System.out.println("rdir: no such directory: " + dirName);
        }
    }
}
