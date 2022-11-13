public class invokeWorkers implements Runnable
{
    /*************************/
     int secondLevelThreads = 0;
    /**************************/
    public invokeWorkers()            // Constructor to get arguments from the main thread
    {
       // Send args from main thread
    }

//    ExecutorService executorService = Executors.newFixedThreadPool(secondLevelThreads) ;

    public void run()
    {
//        for(int i=0; i < secondLevelThreads ; i++)
//        {
//            Runnable runnableTask = new sendQuery()  ;    //  Pass arg, if any to constructor sendQuery(arg)
//            executorService.submit(runnableTask) ;
//        }

        sendQuery s = new sendQuery();      // Send queries from current thread
        s.run();

        // Stop further requests to executor service
//        executorService.shutdown()  ;
//        try
//        {
//            // Wait for 8 sec and then exit the executor service
//            if (!executorService.awaitTermination(8, TimeUnit.SECONDS))
//            {
//                executorService.shutdownNow();
//            }
//        }
//        catch (InterruptedException e)
//        {
//            executorService.shutdownNow();
//        }
    }
}

