import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.URL;
import java.net.URLDecoder;
import java.security.CodeSource;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.concurrent.TimeUnit;


/*
     ______ _ _
    |  ____(_) |
 ___| |__   _| | ___
/ __|  __| | | |/ _ \
\__ \ |    | | |  __/
|___/_|    |_|_|\___|
=======================
*
*
*
*
* */
public class sFile {
    static String rootPath;
    private static Set<File> fileSet;
    private static int port = 80;
    private static Server server;
    private static Thread serverThread;
    private RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();

    public static void main(String[] args) {
        try {
            rootPath = getJarContainingFolder(sFile.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        sFile sFile = new sFile();
        String argString = Arrays.toString(args);
        if (argString.contains("-h") || argString.contains("/h") || argString.contains("help")) {
            sFile.printHelp();
        }
        switch (args.length) {
            case 1:
                try {
                    port = Integer.parseInt(args[0]);
                    System.out.println(port);
                } catch (NumberFormatException e) {
                    sFile.printHelp();
                }
                break;
        }
        new WatchThread(rootPath).start();
        fileSet = scanDirectory(new File(rootPath));
        serverThread = new Thread(() -> server = new Server(port));
        serverThread.start();
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.print("     ______ _ _      \n    |  ____(_) |     \n ___| |__   _| | ___ \n/ __|  __| | | |/ _ \\\n\\__ \\ |    | | |  __/\n|___/_|    |_|_|\\___|\n");
        System.out.println("======================");
        sFile.printStatus();
        sFile.serverInput();
    }

    static Set<File> scanDirectory(File dir) {
        Set<File> fileTree = new HashSet<>();
        if (dir == null || dir.listFiles() == null) {
            return fileTree;
        }
        for (File entry : dir.listFiles()) {
            String thisJar = sFile.class.getProtectionDomain().getCodeSource().getLocation().getPath();
            thisJar = thisJar.substring(1).replace("/", "\\");
            if (thisJar.equals(entry.getAbsolutePath())) {} else {
                if (entry.isFile() || entry.isDirectory()) fileTree.add(entry);
                else fileTree.addAll(scanDirectory(entry));
            }
        }
        fileSet = fileTree;
        return fileTree;
    }

    static Set<File> getFileSet() {
        return fileSet;
    }

    private static String getJarContainingFolder(Class curentClass) throws Exception {
        CodeSource codeSource = curentClass.getProtectionDomain().getCodeSource();
        File jarFile;
        if (codeSource.getLocation() != null) {
            jarFile = new File(codeSource.getLocation().toURI());
        } else {
            String path = curentClass.getResource(curentClass.getSimpleName() + ".class").getPath();
            String jarFilePath = path.substring(path.indexOf(":") + 1, path.indexOf("!"));
            jarFilePath = URLDecoder.decode(jarFilePath, "UTF-8");
            jarFile = new File(jarFilePath);
        }
        return jarFile.getParentFile().getAbsolutePath();
    }

    private void printStatus() {
        Long millis = rb.getUptime();
        long days = TimeUnit.MILLISECONDS.toDays(millis);
        millis -= TimeUnit.DAYS.toMillis(days);
        long hours = TimeUnit.MILLISECONDS.toHours(millis);
        millis -= TimeUnit.HOURS.toMillis(hours);
        long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
        millis -= TimeUnit.MINUTES.toMillis(minutes);
        long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
        System.out.println("sFile Uptime is: " + days + " days, " + hours + " hours, " + minutes + " minutes, " + seconds + " seconds");
        try {
            System.out.println("sFile is running on IP: " + getIp());
        } catch (Exception e) {
            e.printStackTrace();
        }
        System.out.println("sFile is running on port: " + Server.port);
    }

    private void printHelp() {
        System.out.print("     ______ _ _      \n    |  ____(_) |     \n ___| |__   _| | ___ \n/ __|  __| | | |/ _ \\\n\\__ \\ |    | | |  __/\n|___/_|    |_|_|\\___|\n");
        System.out.println("======================");
        System.out.println("sFile is an instant file sharing server that can be launched from any directory.");
        System.out.println("Args: <port>");
        System.out.println("Command options: ");
        System.out.println("Status - Print out sFile status ");
        System.out.println("Exit - exit sFile and terminate server ");
    }

    private void serverInput() {
        System.out.println("Server input: ");
        System.out.print("\r> ");
        Scanner scanner = new Scanner(System.in);
        while (scanner.hasNext()) {
            String command = scanner.nextLine().toLowerCase();
            if (command.contains("help")) {
                printHelp();
            }
            if (command.contains("status")) {
                printStatus();
            }
            if (command.contains("exit")) {
                System.exit(0);
            }
            System.out.print("\r> ");
        }
    }

    public static String getIp() throws Exception {
        URL whatismyip = new URL("http://checkip.amazonaws.com");
        BufferedReader in = null;
        try {
            in = new BufferedReader(new InputStreamReader(
                    whatismyip.openStream()));
            String ip = in.readLine();
            return ip;
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public static int getPort(){
        return port;
    }

}
