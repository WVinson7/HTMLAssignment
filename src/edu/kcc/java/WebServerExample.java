/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kcc.java;


import java.io.File;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 *
 * @author Bob
 */
public class WebServerExample {


    /**
     * The starting point to look for serve-able files.
     */
    private static File documentRootDirectory;

    /**
     * The port number for the server.
     */
    private static int port;

    /**
     * The port to use when one is not supplied.
     */
    private static final int DEFAULT_PORT = 8080;

    /**
     * The file to look for if one is not specified.
     */
    private static String indexFileName;

    /**
     * The indexFileName to use if one is not supplied.
     */
    private static final String DEFAULT_INDEX_FILE_NAME = "index.html";

    /**
     * A ServerSocket for listening for requests
     */
    private static ServerSocket serverSocket;
    
    /**
     * 
     */
    private static final String DEFAULT_DOCUMENT_ROOT_DIRECTORY 
         = java.nio.file.Paths.get(".").toAbsolutePath().normalize().toString();

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        /**
         * Make sure there is a document root directory
         */
        if (1 <= args.length) {
            try {
                documentRootDirectory = new File(args[0]);
                if (!documentRootDirectory.isDirectory()) {
                    showError("The document root directory must be a directory.");
                }
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        } else {
            documentRootDirectory = new File(DEFAULT_DOCUMENT_ROOT_DIRECTORY);
        }

        /**
         * Attempt to get the supplied port number
         */
        if (2 <= args.length) {
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException nfe) {
                port = DEFAULT_PORT;
            }
        } else {
            port = DEFAULT_PORT;
        }

        /**
         * Attempt to get the default file to return.
         */
        if (3 <= args.length) {
            indexFileName = args[2];
        } else {
            indexFileName = DEFAULT_INDEX_FILE_NAME;
        }

        // Get an ExecutorService to manage the Thread Pool
        // In this case, we are getting 100 threads to start
        ExecutorService executorService = Executors.newFixedThreadPool(100);

        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Instantiated ServerSocket listening on port "
                    + port);

            /**
             * This while loop uses the boolean value true instead of something
             * that can change.  This is bad practice.  
             * In a real web server, there would be separate control software 
             * that would be responsible for changing the value of the while
             * condition.  We are skipping that here to keep the example simple.
             */
            while (true) {
                // wait for the connection
                Socket socket = serverSocket.accept();

                // Tell the socket to timeout after three seconds if there is no 
                // activity from the client
                socket.setSoTimeout(3000);

                // hand the connected socket to a new thread and let it run its 
                // course
                executorService.execute(new RequestHandler(socket
                                       , documentRootDirectory, indexFileName));

            } // end while
        } catch (Exception ex) {
            showError(ex.getMessage());
        }

    } // end main()

    /**
     * Prints the correct usage for the program.
     */
    private static void showUsage() {
        System.out.println("Usage: java WebServer documentRoot port indexfile");
    }

    /**
     * Prints an error message and the correct usage for the program.
     *
     * @param message
     */
    private static void showError(String message) {
        System.out.println("ERROR: " + message);
        showUsage();
        System.exit(-1);
    }
    
}
