/*********************************************************************
 *
 * This file java file contains the class Server which represents a 
 * server for a chat application between user. It creates a server
 * socket, listens for client connections, handles multiple client
 * connections concurrently, maintains a database for credentials
 * and another one for all messages history. It also handles the 
 * message reception and broadcast to all users with instances of
 * the ClientHandler class that it creates for all client connection.
 *
 * file: Server.java
 * authors: Hamza Boukaftane, Mehdi El Harami and Omar Benzekri
 * date: 18 may 2023
 * modified: 21 may 2023
 *
 **********************************************************************/

import java.io.BufferedWriter;
import java.io.BufferedReader;
import java.io.FileWriter;
import java.io.FileReader;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

public class Server {
	
	private int serverPort;
	
	private static int LIMIT = 15;
	
	private static int FIRST_MESSAGE = 0;
	
	private String serverAddress;
	
	private Scanner scanner = new Scanner(System.in);
	
	private ServerSocket listener;
	
	private InetAddress serverIP;
	
	private String userCredentialsDBName;
	
	private String messagesDBName;
	
	private ConcurrentHashMap<String, String> usersCredentials = new ConcurrentHashMap<>();
	
	private ConcurrentHashMap<String, ArrayList<String>> messagesSync = new ConcurrentHashMap<>();
	
	private ConcurrentHashMap<String, ClientHandler> connectedClients = new ConcurrentHashMap<>();
	
	
	/**
	 * 
	 * The main entry point of the server application.
	 * 
	 * @param args The command line arguments
	 * @throws Exception If an error occurs during server set up or the client management.
	 * 
	 */
	public static void main(String[] args) throws Exception {
		System.out.println("TP1 - Application Server");
		System.out.println("****************************************\n");
		Server server = new Server();
		server.setUpServer();
		try {
			while (true) {
				new ClientHandler(
					server.listener.accept(),
					server.usersCredentials,
					server.userCredentialsDBName,
					server.messagesSync,
					server.messagesDBName,
					server.connectedClients).start();
			}
		} finally {
			server.listener.close();
		} 
	}
	
	/**
	 * 
	 * This method set up the server by configuring the IP address, the socket and
	 * establishing connection with clients. Moreover, it also set up the server's
	 * user credentials and messages databases.
	 * 
	 */
	private void setUpServer() {
		setUpIPAddress();
		setUpSocket();
		try {
			setUpConnexionWithClient();
			setUpCredentialsDB();
			setUpMessagesDB();
		} catch (IOException e) {
			System.out.println("An error occured during the server's configuration :");
			e.printStackTrace();
			System.out.println("Try again.");
			System.exit(1);
		}
	}
	
	/**
	 * 
	 * This method prompt an IP address from user. It validates the IP address 
	 * format. Then, if user input is valid, it assigns the IP address to 
	 * the severAddress private attribute. Else, the method is called
	 * recursively to get a the good IP address from user.
	 * 
	 */
	private void setUpIPAddress() {
		System.out.println("Please enter desired server IP adress in this format (ex: 127.0.0.1)");
		String userInput = scanner.nextLine();
		if (!(InputValidator.isValidIPAddress(userInput))) {
			System.out.println("The IP address you have written is not valid.");
			setUpIPAddress();
		} else {
			serverAddress = userInput;
		}
		
	}
	
	/**
	 *
	 * This method prompt a port number from user. It validates the 
	 * port number. Then, if user input is valid, it assigns the port
	 * number to the severPort private attribute. Else, the method is 
	 * called recursively to get a the good port number from user.
	 * 
	 */
	private void setUpSocket() {
		System.out.println("Please enter a port # between 5000 and 5050:");
		int userInput = scanner.nextInt();
		if (!(InputValidator.isValidPortNumber(userInput))) {
			System.out.println("The port number you have written is not valid");
			setUpSocket();
		} else {
			serverPort = userInput;
		}
	}
	
	/**
	 * 
	 * This method sets up the server's connection with clients by creating a 
	 * ServerSocket and binding it to the specified server IP address and port.
	 *
 	 * @throws IOException if an I/O error occurs during the setup of the 
 	 * server's connection with clients.
 	 * 
	 */
	private void setUpConnexionWithClient() throws IOException {
		serverIP = InetAddress.getByName(serverAddress);
		listener = new ServerSocket();
		listener.setReuseAddress(true);
		listener.bind(new InetSocketAddress(serverIP, serverPort));
		System.out.println("****************************************\n");
	}
	
	/**
	 * 
	 * This method sets up a user credentials database. If the file already exist,
	 * the credential file is read and all data is put in the concurrent HashMap
	 * of the instance which is stored within the usersCredentials private
	 * attribute. Else, it creates a user credential file.
	 * 
	 */
	private void setUpCredentialsDB() {
		userCredentialsDBName = "user_credentials_" + serverIP.getHostAddress() + "_" + serverPort + ".txt";
	    Path credentialsFilePath = Paths.get(userCredentialsDBName);
	    if (Files.exists(credentialsFilePath)) {
	    	fillUserCredentialsHashMap();
	    } else {
	    	try {
	    		BufferedWriter writer = new BufferedWriter(new FileWriter(userCredentialsDBName));
	            writer.close();
	            System.out.println("Creating new credentials database: " + userCredentialsDBName);
	            System.out.println("Please wait, while creating credentials database ...");
	            System.out.println("Finished creating credentials database. \n");
	        } catch (IOException e) {
	        	System.out.println("Error creating credentials database : " + userCredentialsDBName);
	        	e.printStackTrace();
	        	System.out.println("Try again.");
				System.exit(1);
	        }
	    }
	}
	
	/**
	 * 
	 * This method adds all elements contained in the user credentials file to
	 * the concurrent HashMap of the instance which is stored within the 
	 * usersCredentials private attribute. It adds the user name as keys
	 * and password as values.
	 * 
	 */
	private void fillUserCredentialsHashMap() {
		try {
            BufferedReader fileReader = new BufferedReader(new FileReader(userCredentialsDBName));
            String line;
            while ((line = fileReader.readLine()) != null) {
                String[] credentials = line.split(":");
                if (credentials.length == 2) {
                    String username = credentials[0];
                    String password = credentials[1];
                    usersCredentials.put(username, password);
                }
            }
            fileReader.close();
            System.out.println("Loading existing user credentials from : " + userCredentialsDBName);
            System.out.println("Please wait, while loading credentials ...");
            System.out.println("Finished reading credentials database. \n");
        } catch (IOException e) {
            System.out.println("Error reading user credentials database : " + userCredentialsDBName);
            e.printStackTrace();
            System.out.println("Try again.");
			System.exit(1);
        }
	}
	
	/**
	 * 
	 * This method sets up a message database. If the file already exist,
	 * the message file is read and the 15 most recent data is put in 
	 * the concurrent HashMap of the instance which is stored within 
	 * the messagesSync private attribute. Else, it creates a message file.
	 * 
	 */
	private void setUpMessagesDB() {
		messagesSync.put("message", new ArrayList<String>());
		messagesDBName = "messages_" + serverIP.getHostAddress() + "_" + serverPort + ".txt";
	    Path messagesFilePath = Paths.get(messagesDBName);
	    if (Files.exists(messagesFilePath)) {
	    	fillMessageArray();
	    } else {
	    	try {
	    		BufferedWriter writer = new BufferedWriter(new FileWriter(messagesDBName));
	            writer.close();
	            System.out.println("Creating new messages database: " + messagesDBName);
	            System.out.println("Please wait, while creating messages database ...");
	            System.out.println("Finished creating messages database. \n");
	            System.out.println("The server is running on " + serverAddress + " : " + serverPort);
	    		System.out.println("**************************************** \n");
	    		System.out.println("There is 0 old messages in this chat room.");
	        } catch (IOException e) {
	        	System.out.println("Error creating messages database : " + messagesDBName);
	        	e.printStackTrace();
	        	System.out.println("Try again.");
				System.exit(1);
	        }
	    }
	}
	
	/**
	 * 
	 * This method adds the 15 most recent messages contained in the 
	 * message file to the concurrent HashMap of the instance which
	 * is stored within the messageSync private attribute. It adds 
	 * the string "message" as keys and an ArrayList containing
	 * all fifteen message from oldest to newest.
	 * 
	 */
	private void fillMessageArray() {
		try {
            BufferedReader fileReader = new BufferedReader(new FileReader(messagesDBName));
            String message;
            while ((message = fileReader.readLine()) != null) {
            	ArrayList<String> messages = getOldMessages();
            	if (messages.size() == LIMIT) {
            		messages.remove(FIRST_MESSAGE);
            	}
            	messages.add(message);
            } 
            fileReader.close();
            System.out.println("Loading existing messages from : " + messagesDBName);
            System.out.println("Please wait, while loading messages ...");
            System.out.println("Finished reading messages database. \n");
            System.out.println("The server is running on " + serverAddress + " : " + serverPort);
    		System.out.println("*************************************** \n");
    		printOldMessages();
        } catch (IOException e) {
            System.out.println("Error reading user credentials database : " + messagesDBName);
            e.printStackTrace();
            System.out.println("Try again.");
			System.exit(1);
        }
	}
	
	/**
	 * 
	 * @Getter Retrieve the list of 15 most recent messages 
	 * from the message synchronization object.
	 * 
	 * @return ArrayList<String> The list 15 most recent messages.
	 * 
	 */
	private ArrayList<String> getOldMessages(){
		return messagesSync.get("message");
	}
	
	/**
	 * 
	 * This method prints all old messages stored in the 
	 * messageSync private attribute of the instance to
	 * the server's instance console. 
	 * 
	 */
	private void printOldMessages() {
		System.out.println("There is " + getOldMessages().size() + " old messages in this chat room.");
		getOldMessages().forEach(message -> System.out.println(message));
		System.out.println("\nWaiting for a client to join the room...");
		System.out.println("**************************************** \n");
	}
}