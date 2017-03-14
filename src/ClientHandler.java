import java.io.*;
import java.net.Socket;
import java.util.Set;

/**
 * ClientHandler: responsible for the correct routing of client on connect and request.
 */

public class ClientHandler implements Runnable {
    private final Socket client;
    private PrintWriter clientOutput;
    private static String directory = "root";
    private Set<File> fileSet = sFile.getFileSet();

    ClientHandler(Socket client) {
        this.client = client;
    }

    /* Returns the extension of a file based on a path (such as requested URL). */
    public static String getExtension(String filePath) {
        String extension = "";
        int i = filePath.lastIndexOf('.');
        if (i > 0) {
            extension = filePath.substring(i + 1);
        }
        return extension;
    }

    /**
     * Implemented from Runnable, called on Thread creation by unique user connection.
     */
    @Override
    public void run() {
        try (
                BufferedReader clientInput = new BufferedReader(new InputStreamReader(client.getInputStream()));
                PrintWriter clientOutput = new PrintWriter(client.getOutputStream())
        ) {
            this.clientOutput = clientOutput;
            requestHandler(clientInput);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void requestHandler(BufferedReader clientInput) throws IOException {
        String inp;
        while ((inp = clientInput.readLine()) != null) {
            if ("".equals(inp)) {
                break;
            } else if (inp.contains("GET")) {
                routeFilter(inp.split(" "));
            } else if (inp.contains("POST")) {
                badRequestHeader();
                return;
            }
        }
    }

    private void routeFilter(String[] request) {
        String url = request[1];
        url = url.replaceAll("%20", " ");
        /* Catch all external file calls */
        if (url.contains(".")) {
            loadExternalFile(url);
            return;
        }
        if(url.equals("/")){
            directory = "root";
        } else {
            directory = "root / " + url.substring(1);
        }
        for (File s : fileSet) {
                if (s.isDirectory()) {
                    if (url.substring(1).equals(s.getName())) {
                        if(!directory.equals("root")){
                        }
                        servePage(s);
                        return;
                    }
                }
            }
            try {
                loadPage(url);
            } catch(NullPointerException e){
                loadNotFound();
            }
    }

    private void loadNotFound() {
        /* Send a 404 and load the not found page for the client. */
        servePage("404");
    }

    private void loadPage(String pageRequest) {
        textHeader("html");
        servePage(pageRequest);
    }

    private void applicationHeader() {
        clientOutput.println(
                "HTTP/1.0 200 OK\r\n" +
                        "Content-Type: application/octet-stream"+"\r\n" +
                        "Connection: sFile\r\n"
        );
        clientOutput.flush();
    }

    private void textHeader(String type) {
        clientOutput.println(
                "HTTP/1.0 200 OK\r\n" +
                        "Content-Type: text/" + type + "\r\n" +
                        "Connection: sFile\r\n"
        );
        clientOutput.flush();
    }

    private void badRequestHeader() {
        clientOutput.println(
                "HTTP/1.0 400 Bad Request\r\n" +
                        "Connection: close\r\n"
        );
        clientOutput.flush();
    }

    private void errorHeader() {
        clientOutput.println(
                "HTTP/1.0 404 Not Found\r\n" +
                        "Connection: sFile\r\n"
        );
        clientOutput.flush();
    }

    private void imageHeader(String type) {
        clientOutput.println(
                "HTTP/1.0 200 OK\r\n" +
                        "Content-Type: image/" + type + "\r\n" +
                        "Content-Length:\r\n"
        );
        clientOutput.flush();
    }

    /* Handles the routing for external files.
    Sends proper headers and identifies file extensions. */
    private void loadExternalFile(String fileRequested) {
        if (fileRequested.contains("favicon") || fileRequested.contains("sFile.css")) {
            try {
                if (fileRequested.contains("favicon")) {
                    imageHeader("ico");
                } else {
                    textHeader("css");
                }
                InputStream in = getClass().getClassLoader().getResourceAsStream(fileRequested.substring(1));
                ByteArrayOutputStream buffer = new ByteArrayOutputStream();
                int read;
                byte[] data = new byte[17833];
                while ((read = in.read(data, 0, data.length)) != -1) {
                    buffer.write(data, 0, read);
                }
                buffer.flush();
                client.getOutputStream().write(buffer.toByteArray(), 0, buffer.toByteArray().length);
                buffer.close();
                return;
            } catch (IOException e) {
                loadNotFound();
            }
        }
        String extension = getExtension(fileRequested);
        extension = extension.toLowerCase();

                applicationHeader();
        

        fileRequested = fileRequested.substring(1);
        OutputStream out;
        try {
            out = client.getOutputStream();
            FileInputStream in = new FileInputStream(new File(sFile.rootPath + "/" + fileRequested));
            byte[] buffer = new byte[4096];
            int length;
            while ((length = in.read(buffer)) > 0) {
                out.write(buffer, 0, length);
            }
            in.close();
            out.flush();
        } catch (IOException e) {
            loadNotFound();
        }
    }

    private synchronized void servePage(String pageRequest) {
        if (pageRequest.equals("/")) {
            pageRequest = "index";
        }
        if (pageRequest.contains("404")) {
            pageRequest = "404";
        }
        try
                (
                        InputStream in = getClass().getClassLoader().getResourceAsStream(pageRequest + ".html");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        PrintWriter pw = new PrintWriter(client.getOutputStream());
                ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("<%DIRECTORY%>")) {
                    line = line.replace("<%DIRECTORY%>", "<div class=\"container\">"+directory+"</div>");
                }
                if (line.contains("<%FILELIST%>")) {
                    line = line.replaceAll("<%FILELIST%>", "");
                    for (File file : sFile.getFileSet()) {
                        String parentDir = file.getParent();
                        String rootDir = sFile.rootPath;
                        parentDir = parentDir.substring(parentDir.lastIndexOf("\\"));
                        parentDir = parentDir.substring(1);
                        rootDir = rootDir.substring(rootDir.lastIndexOf("\\"));
                        rootDir = rootDir.substring(1);
                        String fileLink = file.getName();
                        if (rootDir.equals(parentDir)) {
                        } else {
                            fileLink = parentDir + "/" + file.getName();
                        }
                        String extension = getExtension(file.getPath());
                        String fileType = getFileType(extension);
                        if (fileType.equals("")) {
                            fileType = file.isDirectory() ? "folder" : "document";
                        }
                        line += "<a href=\"" + fileLink + "\" class=\"list-group-item list-group-item-action\">" +
                                "<i class=\"icon ion-android-" + fileType + "\"></i>" + file.getName() + "</a>";
                    }

                }
                pw.println(line);
            }
            pw.println();
        } catch (IOException e) {
            loadNotFound();
        }
    }

    private synchronized void servePage(File directory) {
        String pageRequest = "index";
        try
                (
                        InputStream in = getClass().getClassLoader().getResourceAsStream(pageRequest + ".html");
                        BufferedReader reader = new BufferedReader(new InputStreamReader(in));
                        PrintWriter pw = new PrintWriter(client.getOutputStream())
                ) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("<%DIRECTORY%>")) {
                    line = line.replace("<%DIRECTORY%>", "<div class=\"container\">"+ClientHandler.directory+"</div>");
                }
                if (line.contains("<%FILELIST%>")) {
                    line = line.replace("<%FILELIST%>", "");
                    line += "<a href=\"/\"><button type=\"button\" class=\"btn btn-outline-secondary btn-sm backButton\"><i class=\"ion-android-arrow-up\"></i> Directory Up</button></a>\n";
                    for (File file : directory.listFiles()) {
                        String parentDir = file.getParent();
                        String rootDir = sFile.rootPath;
                        parentDir = parentDir.substring(parentDir.lastIndexOf("\\"));
                        parentDir = parentDir.substring(1);
                        rootDir = rootDir.substring(1);
                        String fileLink = file.getName();
                        if (rootDir.equals(parentDir)) {} else {
                            fileLink = parentDir + "/" + file.getName();
                        }
                        String extension = getExtension(file.getPath());
                        String fileType = getFileType(extension);
                        if (fileType.equals("")) {
                            fileType = file.isDirectory() ? "folder" : "document";
                        }
                        line += "<a href=\"" + fileLink + "\" class=\"list-group-item list-group-item-action\">" +
                                "<i class=\"icon ion-android-" + fileType + "\"></i>" + file.getName() + "</a>";
                    }

                }
                pw.println(line);
            }
            pw.println();
        } catch (Exception e) {
e.printStackTrace();        }
    }

    private String getFileType(String extension){
        String fileType = "";
        switch (extension) {
            case "jpg":
            case "png":
            case "gif":
            case "jpeg":
                fileType = "image";
                break;
            case "mov":
            case "mp4":
            case "avi":
            case "wmv":
            case "flv":
                fileType = "film";
                break;
            case "mp3":
            case "flac":
            case "wav":
                fileType = "volume-up";
                break;
            default:
                break;
        }
        return fileType;
    }
}
