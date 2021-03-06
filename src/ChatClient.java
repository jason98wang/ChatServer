/* [ChatClient.java]
 * The chat client
 * Use userName Admin to access special commands
 * use command /help to see all available commands 
 * Author: Jason Wang, Eric Long
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
	private String userName;
	JFrame window1;
	private JLabel label;
	private ArrayList<JLabel> listData;
	private ArrayList<String> blockedUsers;
	private HashMap<String, String> map;

	/**
	 * Main
	 * Runs the client interface
	 * @param args parameters from command line
	 */
	public static void main(String[] args) {
		new ChatClient().login();
	}

	/**
	 * login
	 * Allows the user to choose userName, server IP and the port number
	 */
	public void login() {
		//getting user userName
		String userName = JOptionPane.showInputDialog("Please enter your username (Without spaces)");
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
		String ipAddress = JOptionPane.showInputDialog("Please enter your Ip Address");
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
		String portNum = JOptionPane.showInputDialog("Please enter a port number");
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

	private JPanel status;

	/**
	 * go
	 * Runs the chat client UI
	 * @param username1, the userName of the user
	 * @param ip, the server Ip address the the client wants to connect to
	 * @param port, the port that client wants to make a connection on
	 */
	public void go(String username1, String ip, int port) {
		//Creating the chat client UI
		blockedUsers = new ArrayList<String>();
		map = new HashMap<String,String>();
		listData = new ArrayList<JLabel>();
		window1 = new JFrame("Chat Client");
		JPanel southPanel = new JPanel();
		southPanel.setLayout(new GridLayout(2, 0));
		JButton sendButton = new JButton("SEND");
		sendButton.addActionListener(new SendButtonListener());
		JButton clearButton = new JButton("QUIT");
		clearButton.addActionListener(new QuitButtonListener());
		JLabel errorLabel = new JLabel("");
		userName = username1;
		typeField = new JTextField(10);

		//creating message area and setting it as not editable 
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
		status = new JPanel();
		status.setLayout(new BoxLayout(status,BoxLayout.Y_AXIS));
		//adding and setting size of scroll wheel
		JScrollPane scrollList = new JScrollPane(status, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scrollList.setPreferredSize(new Dimension(100,309));
		scrollList.setMaximumSize(scrollList.getPreferredSize());
		scrollList.setMinimumSize(scrollList.getMinimumSize());

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
			
			//send new userName to server to allow all clients to see new user joining the chat
			msgArea.append(userName + " has joined the chat.\n");
			output.println(userName);
			output.flush();
			
			//setting status of user to active
			label = new JLabel(userName + " - Active");
			listData.add(label);
			status.add(label);
			status.revalidate();
			status.repaint();
			map.put(userName, "Active");
			String S = input.readLine();
			
			// duplicate user
			if (!S.equals("")) {
				JOptionPane.showMessageDialog(null, "Username already exists");
				window1.dispose();
				login();
				running = false;
				return null;
			}
			
			// updates status of everyone else on the server
			while (true) {
				
				// get username
				String userName = input.readLine();
				
				if (userName == null || userName.equals("")) {
					break;
				}
				//get the status that the user would like to change to
				int statusNum = Integer.parseInt(input.readLine());
				String statusStr = "";
				//Changed user status based on the user's command
				if (statusNum == 1) {
					statusStr = "Active";
				} else if (statusNum == 2) {
					statusStr = "Offline";
				} else if (statusNum == 3) {
					statusStr = "Do not disturb";
				}
				// checks for valid status
				if (statusStr.equals("")) {
					continue;
				}
				//updating the user's new status
				label = new JLabel(userName + " - " + statusStr);
				listData.add(label);
				status.add(label);
				status.revalidate();
				status.repaint();
				map.put(userName, statusStr);
			}
		} catch (IOException e) { // connection error occurred
			//Show that connection to server failed 
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
					String msg, user, privateMessage = null;
					user = input.readLine();
					msg = input.readLine(); // read the message
					if (user.startsWith("FROM ")) {
						privateMessage = user.substring(5);
					}
					// cannot block admin
					if ((!blockedUsers.contains(user) && !blockedUsers.contains(privateMessage))|| user.equals("admin")) {
						
						// admin command
						if (user.equals("admin") && msg.startsWith("/")) {
							
							// user is banned
							if (msg.equals("/ban")) {
								
								// tell user they are banned
								for (int i = 0; i < 20; i++) {
									try {
										Thread.sleep(50);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									msgArea.append("YOU ARE BANNED\n");
								}
								
								// exit
								window1.dispose();
								JOptionPane.showMessageDialog(null, "You have been banned");
								running = false;
								System.exit(-1);
							
								// user is kicked
							} else if (msg.equals("/kick")) {
								
								// tells user they are kicked
								for (int i = 0; i < 20; i++) {
									try {
										Thread.sleep(50);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									msgArea.append("YOU ARE KICKED\n");
								}
								
								// exit window
								window1.dispose();
								JOptionPane.showMessageDialog(null, "You have been Kicked");
								running = false;
								System.exit(-1);
								//login();
								
								// server stop
							} else if (msg.startsWith("/stop")) {
								
								// tells user that the server has stopped
								for (int i = 0; i < 20; i++) {
									try {
										Thread.sleep(50);
									} catch (InterruptedException e) {
										e.printStackTrace();
									}
									msgArea.append("SERVER CLOSING\n");
								}
								
								// exit window
								window1.dispose();
								JOptionPane.showMessageDialog(null, "Server closed");
								running = false;
								System.exit(-1);
							}
						}
						
						// user disconnects
						if (msg.equals("")) {
							msgArea.append(user + " disconnected.\n");
							
							// update status
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
							
							// check for invalid status
							if (statusStr.equals("")) {
								break;
							}
							
							// new user joining
							if (!map.containsKey(user)) {
								label = new JLabel(user + " - " + statusStr);
								listData.add(label);
								status.add(label);
								status.revalidate();
								status.repaint();
								msgArea.append(user + " joined the chat.\n");
								map.put(user, statusStr);
							} else {
									
								// update status and not new
								for (int i = 0; i < listData.size(); i++) {
									if (listData.get(i).getText().startsWith(user + " ")) {
										listData.get(i).setText(user + " - " + statusStr);
										status.revalidate();
										status.repaint();
										map.put(user, statusStr);
									}
								}
							}
						} else {
							// regular user message
							msgArea.append(user + ": " + msg + "\n");
						}

					}
				}

				// failed
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
		@Override
		public void actionPerformed(ActionEvent event) {
			// Send a message to the client
			String msg = typeField.getText().trim();
			
			// is a command
			if (msg.startsWith("/")) {
				
				// displays all commands
				if (msg.startsWith("/help") || msg.startsWith("/?")) {
					msgArea.append("/help or /? - displays all available commands\n");
					msgArea.append("/stop - stops the server");
					msgArea.append("/ban userName - bans ip from server\n");
					msgArea.append("/kick userName - kicks user from server\n");
					msgArea.append("/block userName - ignores all message from user\n");
					msgArea.append("/msg userName message - sends a private message to user\n");
					msgArea.append("/status - sets your status");
					msgArea.append("1 for active, 2 for offline, 3 for do not disturb\n");
				
				// stops server
				} else if (msg.startsWith("/stop")) {
					
					
					// works only with admin
					if (userName.equals("admin")) {
						msgArea.append("Stopping server\n");
						output.println(userName);
						output.println(msg);
					} else {
						msgArea.append("Do not have the privileges for this\n");
					}
					
					// ban user(s)
				} else if (msg.startsWith("/ban")) {
					
					// only works with admin
					if (userName.equals("admin")) {
						output.println(userName);
						output.println(msg);
					} else {
						msgArea.append("Do not have the privileges for this\n");
					}
					
					// kick user(s)
				} else if (msg.startsWith("/kick")) {
					
					// only works with users
					if (userName.equals("admin")) {
						output.println(userName);
						output.println(msg);
					} else {
						msgArea.append("Do no have the privileges for this\n");
					}
					
					// block user(s)
				} else if (msg.startsWith("/block")) {
					String[] block = msg.trim().split(" ");
					for (int i = 1; i < block.length; i++) {
						
						// blocking twice unblocks
						if (blockedUsers.contains(block[i])) {
							blockedUsers.remove(block[i]);
							msgArea.append("Unblocked " + block[i] + "\n");
						} else {
							// block user
							blockedUsers.add(block[i]);
							msgArea.append("Blocked " + block[i] + "\n");
						}

					}
					
					// message another user
				} else if (msg.startsWith("/msg")) {
					output.println(userName);
					output.println(msg);
				} else if (msg.startsWith("/status")) {
					String[] split = msg.split(" ");
					if (split.length == 2 && split[1].matches("[1-3]")) {
						output.println(userName);
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
				// sends infomration to server
				output.println(userName);
				output.println(msg);
			}
			
			output.flush();
			typeField.setText("");
		}
	}

	// QuitButtonListener - Quit the program
	class QuitButtonListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			// reports user as offline
			output.println(userName);
			output.println("/status 2");
			output.flush();
			
			// tells everyone user has disconnected
			output.println(userName);
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
			
			// is a command
			if (msg.startsWith("/")) {
				
				// displays all commands
				if (msg.startsWith("/help") || msg.startsWith("/?")) {
					msgArea.append("/help or /? - displays all available commands\n");
					msgArea.append("/stop - stops the server");
					msgArea.append("/ban userName - bans ip from server\n");
					msgArea.append("/kick userName - kicks user from server\n");
					msgArea.append("/block userName - ignores all message from user\n");
					msgArea.append("/msg userName message - sends a private message to user\n");
					msgArea.append("/status - sets your status");
					msgArea.append("1 for active, 2 for offline, 3 for do not disturb\n");
				
				// stops server
				} else if (msg.startsWith("/stop")) {
					
					
					// works only with admin
					if (userName.equals("admin")) {
						msgArea.append("Stopping server\n");
						output.println(userName);
						output.println(msg);
					} else {
						msgArea.append("Do not have the privileges for this\n");
					}
					
					// ban user(s)
				} else if (msg.startsWith("/ban")) {
					
					// only works with admin
					if (userName.equals("admin")) {
						output.println(userName);
						output.println(msg);
					} else {
						msgArea.append("Do not have the privileges for this\n");
					}
					
					// kick user(s)
				} else if (msg.startsWith("/kick")) {
					
					// only works with users
					if (userName.equals("admin")) {
						output.println(userName);
						output.println(msg);
					} else {
						msgArea.append("Do no have the privileges for this\n");
					}
					
					// block user(s)
				} else if (msg.startsWith("/block")) {
					String[] block = msg.trim().split(" ");
					for (int i = 1; i < block.length; i++) {
						
						// blocking twice unblocks
						if (blockedUsers.contains(block[i])) {
							blockedUsers.remove(block[i]);
							msgArea.append("Unblocked " + block[i] + "\n");
						} else {
							// block user
							blockedUsers.add(block[i]);
							msgArea.append("Blocked " + block[i] + "\n");
						}

					}
					
					// message another user
				} else if (msg.startsWith("/msg")) {
					output.println(userName);
					output.println(msg);
				} else if (msg.startsWith("/status")) {
					String[] split = msg.split(" ");
					if (split.length == 2 && split[1].matches("[1-3]")) {
						output.println(userName);
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
				// sends infomration to server
				output.println(userName);
				output.println(msg);
			}
			
			output.flush();
			typeField.setText("");
		}
	}
}
