import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;





public class clientInterface {
    public static void main(String args[]) throws IOException {
        int numberOfusers = 5;

        // Creating a thread pool
        ExecutorService executorService = Executors.newFixedThreadPool(numberOfusers);

        for (int i = 0; i < numberOfusers; i++) {
            Runnable runnableTask = new sendQuery();
            executorService.submit(runnableTask);
        }
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow(); // Cancel currently executing tasks
                if (!executorService.awaitTermination(60, TimeUnit.SECONDS))
                    System.err.println("Pool did not terminate");
            }
        } catch (InterruptedException ex) {
            executorService.shutdownNow();
            Thread.currentThread().interrupt();
        }

    }
}
