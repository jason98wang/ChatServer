/* [ChatServer.java]
 * Chat server that broadcasts messages to all clients and handles various commands
 * Author: Jason Wang, Eric Long 
 * December 10, 2018
 */

//imports for network communication
import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.JOptionPane;

class ChatServer {

	//Declaring variables
	ServerSocket serverSock;// server socket for connection
	static Boolean running = true; // controls if the server is accepting clients
	public static ArrayList<Client> clientList = new ArrayList<Client>();
	static ArrayList<InetAddress> bannedIps = new ArrayList<InetAddress>();
	static HashMap<String, Client> map = new HashMap<String, Client>();

	/**
	 * Main
	 * Runs the server
	 * @param args parameters from command line
	 */
	public static void main(String[] args) {
		new ChatServer().go(); // start the server
	}

	/**
	 * Go 
	 * Starts the server
	 */
	public void go() {

		//allowing the user to choose the port to connect to 
		String portNum = "";
		while (!portNum.matches("[0-9]+")) {
			portNum = JOptionPane.showInputDialog("Please enter the port number");
		}

		//displaying the server Ip for clients to connect to
		Thread t1 = new Thread(new displayServer());
		t1.start();

		System.out.println("Waiting for a client connection..");
		// hold the client connection
		Socket client = null;

		try {
			// assigns an port to the server
			serverSock = new ServerSocket(Integer.parseInt(portNum));
			//serverSock.setSoTimeout(30000); // 15 second timeout

			while (running) { // this loops to accept multiple clients
				client = serverSock.accept(); // wait for connection

				//if the user is on the banned list, refuse the connection
				System.out.println("Client connected");
				if (bannedIps.contains(client.getInetAddress())) {
					JOptionPane.showMessageDialog(null, "Banned ip tried to connect");
					client.close();
				}

				//establishing input streams 
				BufferedReader br;
				InputStreamReader stream = new InputStreamReader(client.getInputStream());
				br = new BufferedReader(stream);
				String userName = br.readLine();

				//if the userName is already in use close client 
				if (map.containsKey(userName)) {
					client.close();
				}

				PrintWriter pw = new PrintWriter(client.getOutputStream());

				//add the client to the client list and set as active
				for (Client c : clientList) {
					pw.println(c.user);
					pw.println(c.status);
					c.output.println(userName);
					c.output.println("/status 1");
					c.output.flush();

				}

				//add the client to the client list
				clientList.add(new Client(client, userName));
				pw.println("");
				pw.flush();

				Thread t = new Thread(new ConnectionHandler(client)); // create a thread for the new client and pass in
																		// the socket
				t.start(); // start the new thread
			}
		} catch (Exception e) {
			// System.out.println("Error accepting connection");
			// close all and quit
			try {
				client.close();
			} catch (Exception e1) {
				System.out.println("Failed to close socket");
			}
			System.exit(-1);
		}
	}

	// ***** Inner class - thread for client connection
	class ConnectionHandler implements Runnable {
		private PrintWriter output; // assign printwriter to network stream
		private BufferedReader input; // Stream for network input
		private Socket client; // keeps track of the client socket
		private boolean running;

		/*
		 * ConnectionHandler Constructor
		 * 
		 * @param the socket belonging to this client connection
		 */
		ConnectionHandler(Socket s) {
			this.client = s; // constructor assigns client to this
			try { // assign all connections to client
				this.output = new PrintWriter(client.getOutputStream());
				InputStreamReader stream = new InputStreamReader(client.getInputStream());
				this.input = new BufferedReader(stream);
			} catch (IOException e) {
				e.printStackTrace();
			}
			running = true;
		} // end of constructor

		/*
		 * run 
		 * executed on start of thread
		 */
		public void run() {
			// Get a message from the client
			String msg, username;
			// Get a message from the client
			while (running) {
				// loop unit a message is received
				try {
					if (input.ready()) { // check for an incoming messge
						username = input.readLine(); //get userName from client
						msg = input.readLine(); // get a message from the client
						//check if the message is a command 
						if (msg.startsWith("/")) {
							if (msg.startsWith("/ban")) { //ban the user
								String[] bannedClients = msg.trim().split(" ");
								for (int i = 1; i < bannedClients.length; i++) {
									Client banned = map.get(bannedClients[i]);
									bannedIps.add(banned.client.getInetAddress());
									banned.output.println("admin");
									banned.output.println("/ban");
									banned.output.flush();
									banned.client.close();
								}
							} else if (msg.startsWith("/kick")) { //kick the user
								String[] kickedClients = msg.trim().split(" ");
								for (int i = 1; i < kickedClients.length; i++) {
									Client kicked = map.get(kickedClients[i]);
									kicked.output.println("admin");
									kicked.output.println("/kick");
									kicked.output.flush();
									kicked.client.close();
								}
							} else if (msg.startsWith("/msg")) { //send private message to a user
								String tmp = msg;
								if (tmp.indexOf(" ") >= 0) {
									tmp = tmp.substring(tmp.indexOf(" ") + 1);
								} else {
									return;
								}
								String user = "";
								if (tmp.indexOf(" ") >= 0) {
									user = tmp.substring(0, tmp.indexOf(" "));
									tmp = tmp.substring(tmp.indexOf(" ") + 1);
								}
								if (!tmp.equals("")) {
									Client messaged = map.get(user);
									Client sent = map.get(username);
									messaged.output.println("FROM: " + username);
									messaged.output.println(tmp);
									messaged.output.flush();
									sent.output.println("TO: " + user);
									sent.output.println(tmp);
									sent.output.flush();
								}
							} else if (msg.startsWith("/status")) { //change user status
								String[] read = msg.split(" ");
								Client current = map.get(username);
								current.status = Integer.parseInt(read[1]);
								for (Client c : clientList) {
									c.output.println(username);
									c.output.println(msg);
									c.output.flush();
								}
							}
						} else {
							//if not special command send message to everyone 
							for (Client c : clientList) {
								//output to be received by the client
								c.output.println(username);
								c.output.println(msg);
								c.output.flush();
							}
						}
					}
				} catch (IOException e) {
					System.out.println("Failed to receive msg from the client");
					e.printStackTrace();
				}
			}

			// Send a message to the client
			output.println("We got your message! Goodbye.");
			output.flush();

			// close the socket
			try {
				input.close();
				output.close();
				client.close();
			} catch (Exception e) {
				System.out.println("Failed to close socket");
			}
		} // end of run()
	} // end of inner class

	public class Client {
		Socket client;
		private PrintWriter output;
		private BufferedReader input;
		String user;
		int status;
		// 1 active
		// 2 offline
		// 3 do not disturb

		/**
		 * ConnectionHandler Constructor
		 * 
		 * @param the socket belonging to this client connection
		 */
		Client(Socket s, String userName) {
			user = userName;
			map.put(user, this);
			status = 1;
			client = s; // constructor assigns client to this
			try { // assign all connections to client
				output = new PrintWriter(client.getOutputStream());
				InputStreamReader stream = new InputStreamReader(client.getInputStream());
				input = new BufferedReader(stream);
			} catch (IOException e) {
				e.printStackTrace();
			}
			running = true;
		} // end of constructor

	}

	public class displayServer implements Runnable {
		public void run() {
			try {
				InetAddress ip = InetAddress.getLocalHost();
				JOptionPane.showMessageDialog(null, "Server IP: " + ip);
			} catch (UnknownHostException e2) {
				JOptionPane.showMessageDialog(null, "Error receiving Ip Address");
			}
			return;
		}
	}
} // end of Class