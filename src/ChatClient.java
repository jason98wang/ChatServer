/* [ChatClient.java]
 * The chat client
 * Use username Admin to access special commands
 * @author Mangat
 * @ version 1.0a
 */

import java.awt.*;
import javax.swing.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.awt.event.*;
import java.util.HashMap;

class ChatClient {

	//declaring variables
	private JTextField typeField;
	private JTextArea msgArea;
	private Socket mySocket; // socket for connection
	private BufferedReader input; // reader for network stream
	private PrintWriter output; // printwriter for network output
	private boolean running; // thread status via boolean
	private String username;
	JFrame window1;
	private ArrayList<String> blockedUsers;
	private HashMap<String, String> map;

	// private static ChatClient cc = new ChatClient();
	/**
	 * Main
	 * Runs the client interface
	 * @param args parameters from command line
	 */
	public static void main(String[] args) {
		new ChatClient().login();
	}

	public void login() {
		
		//getting user userName
		String userName = JOptionPane.showInputDialog("Plesae enter your userName (Without spaces)");
		if (userName == null) {
			running = false;
			return;		
		}
		userName = userName.trim();

		//making sure the userName entered is valid
		if (!userName.matches("[a-zA-Z0-9]+")) {
			JOptionPane.showMessageDialog(null, "Username must only contain alphanumeric characters!");
			login();
			return;
		}

		//getting the Ip address that the client want to connect to
		String ipAddress = JOptionPane.showInputDialog("Plesae enter your Ip Address");
		if (ipAddress == null) {
			running = false;
			return;
		}
		
		//if the Ip address is localhost convert it to 127.0.0.1 which is the same thing 
		if (ipAddress.equals("localhost")) {
			ipAddress = "127.0.0.1";
		}

		//making sure the ip address entered is valid
		if (!ipAddress.matches("[0-9.]+")) {
			JOptionPane.showMessageDialog(null,"IP must only contain digits and periods!");
			login();
			return;
		}

		//getting the port num that the user would like to connect to
		String portNum = JOptionPane.showInputDialog("Plesae enter a port number");
		if (portNum == null) {
			running = false;
			return;
		}
		
		//making sure the port number entered is valid
		if (!portNum.matches("[0-9]+")) {
			JOptionPane.showMessageDialog(null, "Port must be a number!");
			login();
			return;
		}

		go(userName, ipAddress, Integer.parseInt(portNum));
	}

	private JList<String> status;

	/**
	 * go
	 * Runs the chat client UI
	 * @param username1, the usernameof the user
	 * @param ip, the server Ip address the the client wants to connect to
	 * @param port, the port that client wants to make a connection on
	 */
	public void go(String username1, String ip, int port) {
		//Creating the chat client UI
		blockedUsers = new ArrayList<String>();
		map = new HashMap<String,String>();
		window1 = new JFrame("Chat Client");
		JPanel southPanel = new JPanel();
		southPanel.setLayout(new GridLayout(2, 0));
		JButton sendButton = new JButton("SEND");
		sendButton.addActionListener(new SendButtonListener());
		JButton clearButton = new JButton("QUIT");
		clearButton.addActionListener(new QuitButtonListener());
		JLabel errorLabel = new JLabel("");
		username = username1;
		typeField = new JTextField(10);

		msgArea = new JTextArea();
		msgArea.setEditable(false);

		//adding scroll to the message area
		JScrollPane scroll = new JScrollPane(msgArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		typeField.addActionListener(new EnterListener());

		//adding all panel to the main screen
		southPanel.add(typeField);
		southPanel.add(sendButton);
		southPanel.add(errorLabel);
		southPanel.add(clearButton);
		DefaultListModel<String> model = new DefaultListModel<String>();
		status = new JList<String>(model);

		JScrollPane scrollList = new JScrollPane(status, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

		//set window to be visible
		window1.add(BorderLayout.WEST, scrollList);
		window1.add(BorderLayout.CENTER, scroll);
		window1.add(BorderLayout.SOUTH, southPanel);
		window1.setSize(600, 400);
		window1.setVisible(true);
		// call a method that connects to the server
		connect(ip, port);

		running = true;
		// after connecting loop and keep appending[.append()] to the JTextArea
		window1.revalidate();
		readMessagesFromServer();

	}

	/**
	 * connect
	 * Attempts to connect to the server and creates the socket and streams
	 * @param ip
	 * @param port
	 * @return
	 */
	public Socket connect(String ip, int port) {
		System.out.println("Attempting to make a connection..");

		try {
			//attempt socket connection, wait until a connection is made
			mySocket = new Socket(ip, port); 

			//steam for network input
			InputStreamReader stream1 = new InputStreamReader(mySocket.getInputStream()); 
			input = new BufferedReader(stream1);
			//assign printwriter to network stream
			output = new PrintWriter(mySocket.getOutputStream()); 
			
			msgArea.append(username + " has joined the chat.\n");
			output.println(username);
			output.flush();
			((DefaultListModel<String>) status.getModel()).addElement(username + " - Active");
			map.put(username, "Active");
			System.out.println(map);
			while (true) {
				String username = input.readLine();
				if (username.equals("")) {
					break;
				}
				int statusNum = Integer.parseInt(input.readLine());
				String statusStr = "";
				if (statusNum == 1) {
					statusStr = "Active";
				} else if (statusNum == 2) {
					statusStr = "Offline";
				} else if (statusNum == 3) {
					statusStr = "Do not disturb";
				}
				if (statusStr.equals("")) {
					continue;
				}
				((DefaultListModel<String>) status.getModel()).addElement(username + " - " + statusStr);
				map.put(username, statusStr);
			}
		} catch (IOException e) { // connection error occured
			System.out.println("Connection to Server Failed");
			window1.dispose();
			JOptionPane.showMessageDialog(null, "Connection to Server Failed");
			login();
		}

		System.out.println("Connection made.");
		return mySocket;
	}


	/**
	 * readMessagesFromServer
	 * This method waits for server input and then displays it on the UI
	 */
	public void readMessagesFromServer() {

		while (running) { // loop unit a message is received
			try {

				if (input.ready()) { // check for an incoming messge
					String msg, user;
					user = input.readLine();
					msg = input.readLine(); // read the message
					if (!blockedUsers.contains(user) || user.equals("admin")) {
						if (user.equals("admin") && msg.startsWith("/")) {
							if (msg.equals("/ban")) {
								for (int i = 0; i < 20; i++) {
									try {
										Thread.sleep(50);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									msgArea.append("YOU ARE BANNED");
								}
								window1.dispose();
								JOptionPane.showMessageDialog(null, "You have been banned");
								running = false;
								//login();
							} else if (msg.equals("/kick")) {
								for (int i = 0; i < 20; i++) {
									try {
										Thread.sleep(50);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									msgArea.append("YOU ARE KICKED\n");
								}
								window1.dispose();
								JOptionPane.showMessageDialog(null, "You have been Kicked");
								running = false;
								//login();
							}
						}
						if (msg.equals("")) {
							msgArea.append(user + " disconnected.");
						} else if (msg.startsWith("/status")) {
							int statusNum = Integer.parseInt(msg.split(" ")[1]);
							String statusStr = "";
							if (statusNum == 1) {
								statusStr = "Active";
							} else if (statusNum == 2) {
								statusStr = "Offline";
							} else if (statusNum == 3) {
								statusStr = "Do not disturb";
							}
							if (statusStr.equals("")) {
								break;
							}
							System.out.println(map);
							if (!map.containsKey(user)) {
								System.out.println(user);
								((DefaultListModel<String>) status.getModel()).addElement(user + " - " + statusStr);
								msgArea.append(user + " joined the chat.\n");
								map.put(user, statusStr);
							} else {
								for (int i = 0; i < status.getModel().getSize(); i++) {
									if (((DefaultListModel<String>) status.getModel()).getElementAt(i)
											.startsWith(user + " ")) {
										((DefaultListModel<String>) status.getModel())
												.setElementAt(user + " - " + statusStr, i);
										map.put(user, statusStr);
									}
								}
							}
						} else {
							msgArea.append(user + ": " + msg + "\n");
						}

					}
				}

			} catch (IOException e) {
				System.out.println("Failed to receive msg from the server");
				e.printStackTrace();
			}
		}
		try { // after leaving the main loop we need to close all the sockets
			input.close();
			output.close();
			mySocket.close();
		} catch (Exception e) {
			System.out.println("Failed to close socket");
		}

	}
	// ****** Inner Classes for Action Listeners ****

	// send - send msg to server (also flush), then clear the JTextField
	class SendButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			// Send a message to the client
			String msg = typeField.getText().trim();
			if (msg.startsWith("/")) {
				if (msg.startsWith("/help") || msg.startsWith("/?")) {
					msgArea.append("/help or /? - displays all available commands\n");
					msgArea.append("/ban username - bans ip from server\n");
					msgArea.append("/kick username - kicks user from server\n");
					msgArea.append("/block username - ignores all message from user\n");
					msgArea.append("/msg username message - sends a private message to user\n");
					msgArea.append("/status - sets your status");
					msgArea.append("1 for active, 2 for offline, 3 for do not disturb\n");
				} else if (msg.startsWith("/ban")) {
					if (username.equals("admin")) {
						output.println(username);
						output.println(msg);
					} else {
						msgArea.append("Do not have the privileges for this\n");
					}
				} else if (msg.startsWith("/kick")) {
					if (username.equals("admin")) {
						output.println(username);
						output.println(msg);
					} else {
						msgArea.append("Do no have the privileges for this\n");
					}
				} else if (msg.startsWith("/block")) {
					String[] block = msg.trim().split(" ");
					for (int i = 1; i < block.length; i++) {
						if (blockedUsers.contains(block[i])) {
							blockedUsers.remove(block[i]);
							msgArea.append("Unblocked " + block[i] + "\n");
						} else {
							blockedUsers.add(block[i]);
							msgArea.append("Blocked " + block[i] + "\n");
						}

					}
				} else if (msg.startsWith("/msg")) {
					output.println(username);
					output.println(msg);
				} else if (msg.startsWith("/status")) {
					String[] split = msg.split(" ");
					if (split.length == 2 && split[1].matches("[1-3]")) {
						output.println(username);
						output.println(msg);
					} else {
						msgArea.append("Invalid status!\n");
					}
				} else {
					msgArea.append("Invalid command!\n");
				}
			} else {
				// Message must be entered
				if (msg.equals("")) {
					return;
				}
				output.println(username);
				output.println(msg);
			}
			output.flush();
			typeField.setText("");
		}
	}

	// QuitButtonListener - Quit the program
	class QuitButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			output.println(username);
			output.println("/status 2");
			output.println(username);
			output.println();
			output.flush();
			running = false;
			window1.dispose();
			//login();
		}
	}

	class EnterListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent event) {
			// Send a message to the client
			String msg = typeField.getText().trim();
			if (msg.startsWith("/")) {
				if (msg.startsWith("/help") || msg.startsWith("/?")) {
					msgArea.append("/help or /? - displays all available commands\n");
					msgArea.append("/ban username - bans ip from server\n");
					msgArea.append("/kick username - kicks user from server\n");
					msgArea.append("/block username - ignores all message from user\n");
					msgArea.append("/msg username message - sends a private message to user\n");
					msgArea.append("/status - sets your status");
					msgArea.append("1 for active, 2 for offline, 3 for do not disturb\n");
				} else if (msg.startsWith("/ban")) {
					if (username.equals("admin")) {
						output.println(username);
						output.println(msg);
					} else {
						msgArea.append("Do not have the privileges for this\n");
					}
				} else if (msg.startsWith("/kick")) {
					if (username.equals("admin")) {
						output.println(username);
						output.println(msg);
					} else {
						msgArea.append("Do no have the privileges for this\n");
					}
				} else if (msg.startsWith("/block")) {
					String[] block = msg.trim().split(" ");
					for (int i = 1; i < block.length; i++) {
						if (blockedUsers.contains(block[i])) {
							blockedUsers.remove(block[i]);
							msgArea.append("Unblocked " + block[i] + "\n");
						} else {
							blockedUsers.add(block[i]);
							msgArea.append("Blocked " + block[i] + "\n");
						}

					}
				} else if (msg.startsWith("/msg")) {
					output.println(username);
					output.println(msg);
				} else if (msg.startsWith("/status")) {
					String[] split = msg.split(" ");
					if (split.length == 2 && split[1].matches("[1-3]")) {
						output.println(username);
						output.println(msg);
					} else {
						msgArea.append("Invalid status!\n");
					}
				} else {
					msgArea.append("Invalid command!\n");
				}
			} else {
				// Message must be entered
				if (msg.equals("")) {
					return;
				}
				output.println(username);
				output.println(msg);
			}
			output.flush();
			typeField.setText("");
		}

	}
}