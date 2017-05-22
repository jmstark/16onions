package com.voidphone.onion;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

public class Main {

	/**
	 * Returns one.
	 * 
	 * @return always one.
	 */
	public static int one()
	{
		return 1;
	}
	
	/**
	 * @param args Command line arguments
	 */
	public static void main(String[] args) throws IOException {
		System.out.println("Hello " + one());
		
		//from https://www.cs.uic.edu/~troy/spring05/cs450/sockets/EchoServer.java
	    ServerSocket serverSocket = null; 

	    try { 
	         serverSocket = new ServerSocket(10007); 
	        } 
	    catch (IOException e) 
	        { 
	         System.err.println("Could not listen on port: 10007."); 
	         System.exit(1); 
	        } 

	    Socket clientSocket = null; 
	    System.out.println ("Waiting for connection.....");

	    try { 
	         clientSocket = serverSocket.accept(); 
	        } 
	    catch (IOException e) 
	        { 
	         System.err.println("Accept failed."); 
	         System.exit(1); 
	        } 

	    System.out.println ("Connection successful");
	    System.out.println ("Waiting for input.....");

	    PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), 
	                                      true); 
	    BufferedReader in = new BufferedReader( 
	            new InputStreamReader( clientSocket.getInputStream())); 

	    String inputLine; 

	    while ((inputLine = in.readLine()) != null) 
	        { 
	         System.out.println ("Server: " + inputLine); 
	         out.println(inputLine); 

	         if (inputLine.equals("Bye.")) 
	             break; 
	        } 

	    out.close(); 
	    in.close(); 
	    clientSocket.close(); 
	    serverSocket.close(); 
	}
}
