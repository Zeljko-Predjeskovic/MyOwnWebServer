package webServer;

import service.ContentReader;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Date;
import java.util.StringTokenizer;


public class HTTPServer implements Runnable{

    private static final File WEB_ROOT = new File("./");
    private static final String DEFAULT_FILE = "/src/main/resources/templates/index.html";
    private static final String FILE_NOT_FOUND = "/src/main/resources/templates/404.html";

    private final ContentReader contentReader = new ContentReader();

    private static final int PORT = 8080;

    private static final boolean verbose = true;

    private Socket connect;


    /**
     *
     * @param c Constructor with Socket for the HTTPServer
     */
    public HTTPServer(Socket c) {
        connect = c;
    }

    /**
     * main method is starting the Server and managing Client connections with help of
     *
     * startServerConnection() and clientConnection(ServerSocket socket)
     *
     * @param args not used
     */
    public static void main(String[] args) {

        HTTPServer httpServer = new HTTPServer(new Socket());
        ServerSocket serverConnect = httpServer.startServerConnection();
        httpServer.clientConnection(serverConnect);

    }

    /**
     *
     * @return Returns ServerSocket HTTPServer connection
     */
    private ServerSocket startServerConnection(){
        try {
            ServerSocket serverConnect = new ServerSocket(PORT);
            System.out.println("Server listening on port : " + PORT + " ...\n");
            return serverConnect;
        }
        catch (IOException e){
            throw new RuntimeException("Server Connection error: " ,e);
        }
    }

    /**
     * Starts thread with the Client connection
     *
     * @param serverConnect Current HTTPServer
     */
    private void clientConnection(ServerSocket serverConnect){
        try {
            while (true) {
                HTTPServer myServer = new HTTPServer(serverConnect.accept());

                if (verbose) {
                    System.out.println("Connecton opened. (" + new Date() + ")");
                }
                Thread thread = new Thread(myServer);
                thread.start();
            }
        }
        catch (IOException e){
            throw new RuntimeException("Client Connection error:",e);
        }
    }

    @Override
    public void run() {

        requests();

    }

    /**
     * managing get request
     */
    private void requests() {

        BufferedReader in = null;
        PrintWriter out = null;
        BufferedOutputStream dataOut = null;
        String fileRequested = null;

        try {
            // we read characters from the client via input stream on the socket
            in = new BufferedReader(new InputStreamReader(connect.getInputStream()));

            // client header
            out = new PrintWriter(connect.getOutputStream());

            // requested data
            dataOut = new BufferedOutputStream(connect.getOutputStream());

            // first line of the request from the client
            String input = in.readLine();

            // we parse the request with a string tokenizer
            StringTokenizer parse = new StringTokenizer(input);

            String method = parse.nextToken().toUpperCase(); // we get the HTTP method of the client

            // we get file requested
            fileRequested = parse.nextToken().toLowerCase();

            if (method.equals("GET")) { // GET method so we return content
                getRequest(fileRequested, method, dataOut, out);
            }



        } catch (FileNotFoundException fnfe) {
            try {
                fileNotFound(out, dataOut, fileRequested,new File(WEB_ROOT, FILE_NOT_FOUND));
            } catch (IOException ioe) {
                throw new RuntimeException("Error with file not found exception : " + ioe.getMessage());
            }

        } catch (IOException ioe) {
            throw new RuntimeException("Server error : " + ioe);
        } finally {
            try {
                in.close();
                out.close();
                dataOut.close();
                connect.close(); // we close socket connection
            } catch (Exception e) {
                throw new RuntimeException("Error closing stream : " + e.getMessage());
            }

            if (verbose) {
                System.out.println("Connection closed.\n");
            }
        }


    }

    private void getRequest(String fileRequested, String method, BufferedOutputStream dataOut, PrintWriter out) throws IOException {
        // GET
        if (fileRequested.endsWith("/")) {
            fileRequested += DEFAULT_FILE;
        }

        File file = new File(WEB_ROOT, fileRequested);
        int fileLength = (int) file.length();
        String content = contentReader.getContentType(fileRequested);


            byte[] fileData = contentReader.readFileData(file, fileLength);

            // send HTTP Headers
            out.println("HTTP/1.1 200 OK");
            out.println("Server: Java HTTP Server : 1.0");
            out.println("Date: " + new Date());
            out.println("Content-type: " + content);
            out.println("Content-length: " + fileLength);
            out.println(); // blank line between headers and content, very important !
            out.flush(); // flush character output stream buffer

            dataOut.write(fileData, 0, fileLength);
            dataOut.flush();

            if (verbose) {
                System.out.println("File " + fileRequested + " of type " + content + " returned");
            }
    }

    /**
     * Sending response with error page and header
     * @param out
     * @param dataOut
     * @param fileRequested
     * @param file
     * @throws IOException
     */
    public void fileNotFound(PrintWriter out, OutputStream dataOut, String fileRequested, File file) throws IOException {
        int fileLength = (int) file.length();
        String content = "text/html";
        byte[] fileData = contentReader.readFileData(file, fileLength);

        out.println("HTTP/1.1 404 File Not Found");
        out.println("Server: Java HTTP Server : 1.0");
        out.println("Date: " + new Date());
        out.println("Content-type: " + content);
        out.println("Content-length: " + fileLength);
        out.println(); // blank line between headers and content, very important !
        out.flush(); // flush character output stream buffer

        dataOut.write(fileData, 0, fileLength);
        dataOut.flush();

        if (verbose) {
            System.out.println("File " + fileRequested + " not found");
        }
    }


}