import java.io.File;
import java.nio.file.*;
import java.util.List;

public class WatchThread extends Thread {

    Path myDir;
    WatchService watcher;
    String path;

    WatchThread(String path) {
        try {
            this.path = path;
            myDir = Paths.get(path);
            watcher = myDir.getFileSystem().newWatchService();
            myDir.register(watcher, StandardWatchEventKinds.ENTRY_CREATE);
            myDir.register(watcher, StandardWatchEventKinds.ENTRY_DELETE);
            myDir.register(watcher, StandardWatchEventKinds.ENTRY_MODIFY);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public void run() {
        while (true) {
            try {
                WatchKey watchKey = watcher.take();
                List<WatchEvent<?>> events = watchKey.pollEvents();
                for (WatchEvent<?> event : events) {
                    sFile.scanDirectory(new File(path));
                }
                watchKey.reset();
            } catch (Exception e) {
                System.out.println("Error: " + e.toString());
            }
        }
    }
}