/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package edu.kcc.java;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.time.LocalDate;
import java.util.StringTokenizer;

/**
 * Handles the requests from the web.
 * NOTE: This file contains many calls to System.out.println().  These are for 
 * illustration purposes only.  There should never be such calls in your actual
 * program!  Instead, you should use logging to record any requests and errors.
 *
 * @author Bob Trapp
 */
public class RequestHandler implements Runnable {

    /**
     * The socket will handle the communications with the client.
     */
    private final Socket socket;

    /**
     * The folder in which to search for requested files.
     */
    private final File documentRootDirectory;

    /**
     * The default file name to look for if the request does not contain one.
     */
    private final String defaultFileName;

    /**
     * The constructor requires a socket or the RequestHandler won't work. The
     * other parameters tell where to find things.
     *
     * @param socket
     * @param documentRootDirectory
     * @param defaultFileName
     */
    public RequestHandler(Socket socket, File documentRootDirectory, String defaultFileName) {
        this.socket = socket;
        this.defaultFileName = defaultFileName;
        this.documentRootDirectory = documentRootDirectory;
    }

    /**
     * The primary class logic is in this method. The run() method is required
     * for the Runnable interface.
     * 
     * In this method, we read the request from the socket, determine if we can
     * return the requested item, and then package everything to send back, with
     * an HTTP header appended to the front.  If we cannot give the requested 
     * item, we try to give the appropriate error message.
     * 
     */
    @Override
    public void run() {
        try {
            
            /**
             * Prepare for reading. This involves getting the stream and then
             * reading from it character by character.
             */
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            
            /**
             * Prepare to write to the socket
             */
            PrintWriter out = new PrintWriter(socket.getOutputStream());
            
            StringBuilder requestLine = new StringBuilder();
            String line = ".";
            while(!line.equals("")){
                line = in.readLine();
                requestLine.append(line);
            }
            
           
            /**
             * Convert the character buffer to a string and then tokenize.
             */
            String userRequest = requestLine.toString();
            StringTokenizer stringTokenizer = new StringTokenizer(userRequest);

            /**
             * For educational purposes, show the request.
             */
            System.out.println("\n*****************************************");
            System.out.println("USER REQUEST: " + userRequest);

            /**
             * Get the first token. It should be the request method.
             */
            String method = stringTokenizer.nextToken();
            System.out.println("REQUEST METHOD: " + method);
            String version = "";
            if (method.equals("GET")) {  // GET is the only supported method
                String fileName = stringTokenizer.nextToken();
                if (fileName.endsWith("/")) {
                    fileName = fileName + this.defaultFileName;
                } else if(fileName.endsWith("?")) {
                    fileName = fileName.substring(0,fileName.length()-1);
                }
                if (stringTokenizer.hasMoreElements()) {
                    version = stringTokenizer.nextToken();
                }
                
                /**
                 * Start looking for the file. Make sure the file is within the
                 * document root directory or the user will be able to request
                 * things elsewhere on the host machine!
                 */
                System.out.println("Looking for file  [" + documentRootDirectory +  fileName + "]");
                File theFile = new File(this.documentRootDirectory, fileName.substring(1, fileName.length()));
                
                if (theFile.canRead()
                        && theFile.getCanonicalPath().startsWith(
                                this.documentRootDirectory.getCanonicalPath())) {
                    System.out.println("\tCan read file " + this.documentRootDirectory.getCanonicalPath() + fileName);
                    DataInputStream fileInputStream = new DataInputStream(
                            new BufferedInputStream(
                                    new FileInputStream(theFile)));
                    byte[] theData = new byte[(int) theFile.length()];
                    fileInputStream.readFully(theData);
                    fileInputStream.close();

                    /**
                     * Prepare to send a response
                     * Note that we determine the Content-type (mime type) from
                     * the file name.
                     * Also note that the HTTP response code is 200 OK.
                     */
                    if (version.startsWith("HTTP")) { // Send a MIME header
                        System.out.println("\tSending HTTP/1.0 200 OK");
                        out.write("HTTP/1.0 200 OK\r\n");
                        LocalDate now = LocalDate.now();
                        out.write("Date: " + now + "\r\n");
                        out.write("Server: WebServer\r\n");
                        out.write("Content-length: " + theData.length + "\r\n");
                        out.write("Content-type:" + getMimeTypeFromName(fileName) + "\r\n\r\n");
                        out.flush();
                    } else {
                        System.out.println("Version did not start with HTTP ");
                    }
                    out.write(new String(theData));
                    out.flush();
                } else { // Can't find or access the file
                    System.out.println("\tCould not read, trying 404");
                    if (version.startsWith("HTTP")) { // Send a MIME header
                        out.write("HTTP/1.0 404 File Not Found\r\n");
                        LocalDate now = LocalDate.now();
                        out.write("Date: " + now + "\r\n");
                        out.write("Server: WebServer\r\n");
                        out.write("Content-type: text/html\r\n\r\n");
                    }
                    out.write("<html>\r\n");
                    out.write("<head>\r\n<title>File Not Found</title>\r\n</head>\r\n");
                    out.write("<body>\r\n");
                    out.write("<h1>Error 404: File Not Found</h1>\r\n");
                    out.write("</body>\r\n</html>\r\n");
                    out.flush();

                }
            } else { // the method is not GET
                System.out.println("\tMethod was not GET, trying 501");
                if (version.startsWith("HTTP")) { // send a MIME header
                    out.write("HTTP/1.0 501 Not Implemented\r\n");
                        LocalDate now = LocalDate.now();
                    out.write("Date: " + now + "\r\n");
                    out.write("Server: WebServer\r\n");
                    out.write("Content-type: text/html\r\n\r\n");
                }
                out.write("<html>\r\n");
                out.write("<head>\r\n<title>Not Implemented</title>\r\n</head>\r\n");
                out.write("<body>\r\n");
                out.write("<h1>Error 501: Not Implemented</h1>\r\n");
                out.write("</body>\r\n</html>\r\n");
                out.flush();
            }
            /**
             * Remember to close() or the browser will continue to show the 
             * page loading indicator!
             */
            out.close();
        } catch (IOException ioe) {
            /**
             * This would normally be a logging item
             */
            System.out.println("ERROR: " + ioe.getMessage());
        } 
        System.out.println("End of run()\n");
    } // end run()

    
    /**
     * The mime type tells the receiving program (web browser) what to do with 
     * the series of bits it gets from the server.  Usually, the file extension
     * will provide enough information to determine the mime type.  We use it
     * here to make a simplified guess.
     * By default, if no extension can be determined or if it is not one that 
     * we are set up to handle, we use "text/plain" as the mime type.
     * 
     * @param fileName The name of the file being returned
     * @return The text specifying the mime type of the file.
     */
    private String getMimeTypeFromName(String fileName){
        String mimeType = "text/plain";
        String extension = getExtensionFromFileName(fileName);
        if(null != extension){
            switch(extension){
                case "html":
                    mimeType = "text/html";
                    break;
                case "css":
                    mimeType = "text/css";
                    break;
                case "js":
                    mimeType = "text/javascript";
                    break;
                case "ico":
                    mimeType = "image/x-icon";
                    break;
                case "png":
                    mimeType = "image/png";
                    break;
                case "jpeg":
                    mimeType = "image/jpeg";
                    break;
                case "gif":
                    mimeType = "image/gif";
                    break;
                default:
                    mimeType = "text/plain";
            
            }
        }
        System.out.println("\t\tMime Type: " + mimeType);
        return mimeType;
    }
    
    /**
     * Extract the file extension from the file name.  We assume that the file
     * extension is the last bit of text following the last period in the 
     * file name.  If the file does not fit this patter, the returned value
     * will be null.
     * 
     * @param fileName The file name from which to extract the extension
     * @return The file extension text.
     */
    private String getExtensionFromFileName(String fileName) {
        System.out.println("\t\tGetting extension from: " + fileName);
        String extension = null;
        int periodLocation = fileName.lastIndexOf('.');
        System.out.println("\t\tPeriod Location: " + periodLocation);
        if(-1 <= periodLocation) {
            extension = fileName.substring(periodLocation + 1);
        }
        System.out.println("\t\tExtension: " + extension);
        return extension;
    }
   
}
