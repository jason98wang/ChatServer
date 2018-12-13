
/* [ChatClient.java]
 * A not-so-pretty implementation of a basic chat client
 * @author Mangat
 * @ version 1.0a
 */

import java.awt.*;
import javax.swing.*;

import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;
import java.util.HashMap;

class ChatClient {

	private JTextField typeField;
	private JTextArea msgArea;
	private Socket mySocket; // socket for connection
	private BufferedReader input; // reader for network stream
	private PrintWriter output; // printwriter for network output
	private boolean running = true; // thread status via boolean
	private String username;
	JFrame window;
	private ArrayList<String> blockedUsers = new ArrayList<String>();
	private HashMap<String,String> map = new HashMap<String,String>();
	private static ChatClient cc = new ChatClient();
	public static void main(String[] args) {
//		cc.login("","","");
		cc.go("eric", "127.0.0.1", 5000);
	}
	
	public void login(String username, String ip, String port) {
		JFrame frame = new JFrame("Login");
		JPanel contentPane = new JPanel();
		JButton ok = new JButton("OK");
		JLabel usernameLabel = new JLabel("Enter your username: ");
		JLabel ipLabel = new JLabel("Enter IP address: ");
		JLabel portLabel = new JLabel("Enter port: ");
		JTextField usernameField = new JTextField(username);
		JTextField ipField = new JTextField(ip);
		JTextField portField = new JTextField(port);
		ok.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				if (!usernameField.getText().matches("[a-zA-Z0-9]+")) {
					JOptionPane.showMessageDialog(null, "Username must only contain alphanumeric characters!");
					return;
				}
				if (usernameField.getText().equals("admin")) {
					JOptionPane.showMessageDialog(null, "Username cannot be admin!");
					return;
				}
				if (ipField.getText().equals("localhost")) {
					ipField.setText("127.0.0.1");
				}
				if (!ipField.getText().matches("[0-9.]+")) {
					JOptionPane.showMessageDialog(null, "IP must only contain digits and periods!");
					return;
				}
				if (!portField.getText().matches("[0-9]+")) {
					JOptionPane.showMessageDialog(null, "Port must be a number!");
					return;
				}
				frame.dispose();
				System.out.println(Integer.parseInt(portField.getText()));
				
				cc.go(usernameField.getText(), ipField.getText(), Integer.parseInt(portField.getText()));
				
				
			}
		});
		contentPane.add(usernameLabel);
		contentPane.add(usernameField);
		contentPane.add(ipLabel);
		contentPane.add(ipField);
		contentPane.add(portLabel);
		contentPane.add(portField);
		contentPane.add(ok);
		contentPane.setLayout(new BoxLayout(contentPane,BoxLayout.Y_AXIS));
		frame.setContentPane(contentPane);
		frame.setSize(400, 400);
		frame.setVisible(true);
		
	}
	private DefaultListModel<String> model = new DefaultListModel<String>();
	public void go(String username1, String ip, int port) {
		JFrame	window1 = new JFrame("Chat Client");
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

		JScrollPane scroll = new JScrollPane(msgArea, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		typeField.addActionListener(new EnterListener());

		southPanel.add(typeField);
		southPanel.add(sendButton);
		southPanel.add(errorLabel);
		southPanel.add(clearButton);

		JList<String> status = new JList<String>(model);
		JScrollPane scrollList = new JScrollPane(status,JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		
		
		window1.add(BorderLayout.WEST, scrollList);
		window1.add(BorderLayout.CENTER, scroll);
		window1.add(BorderLayout.SOUTH, southPanel);
		window1.setSize(600, 400);
		window1.setVisible(true);
		
		// call a method that connects to the server
		connect(ip, port);
		
		// after connecting loop and keep appending[.append()] to the JTextArea

		readMessagesFromServer();
		
		

	}

	// Attempts to connect to the server and creates the socket and streams
	public Socket connect(String ip, int port) {
		System.out.println("Attempting to make a connection..");

		try {
			mySocket = new Socket(ip, port); // attempt socket
														// connection (local
														// address). This will
														// wait until a
														// connection is made

			InputStreamReader stream1 = new InputStreamReader(mySocket.getInputStream()); // Stream
																							// for
																							// network
																							// input
			input = new BufferedReader(stream1);
			output = new PrintWriter(mySocket.getOutputStream()); // assign
																	// printwriter
																	// to
																	// network
																	// stream
			output.println(username);
			output.flush();
		} catch (IOException e) { // connection error occured
			System.out.println("Connection to Server Failed");
			//window.dispose();
			login(username, ip, Integer.toString(port));
			e.printStackTrace();
		}

		System.out.println("Connection made.");
		return mySocket;
	}

	// Starts a loop waiting for server input and then displays it on the
	// textArea
	public void readMessagesFromServer() {

		while (running) { // loop unit a message is received
			try {

				if (input.ready()) { // check for an incoming messge
					String msg, user;
					user = input.readLine();
					if (!blockedUsers.contains(user)) {
						msg = input.readLine(); // read the message
						if (msg.equals("")) {
							msgArea.append(user + " disconnected.");
						} else if (msg.startsWith("/status")){
							int status = Integer.parseInt(msg.split(" ")[1]);
							String statusStr = "";
							if (status == 1) {
								statusStr = "Active";
							} else if (status == 2) {
								statusStr = "Offline";
							} else if (status == 3) {
								statusStr = "Do not disturb";
							}
							if (!map.containsKey(user)) {
								model.addElement(user + " - " + statusStr);
								msgArea.append(user + " joined the chat.\n");
							} else {
								for (int i = 0; i < model.size(); i++) {
									if (model.getElementAt(i).startsWith(user + " ")) {
										model.setElementAt(user + " - " + statusStr, i);
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
						blockedUsers.add(block[i]);
					}
				} else if (msg.startsWith("/msg")) {
					output.println(username);
					output.println(msg);
				} else if (msg.startsWith("/status")) {
					String[] split = msg.split(" ");
					if (split.length == 2 && split[1].matches("[0-2]")) {
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
			output.println();
			output.flush();
			running = false;
			window.dispose();
		}
	}

	class EnterListener implements ActionListener {
		@Override
		public void actionPerformed(ActionEvent arg0) {
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
						blockedUsers.add(block[i]);
					}
				} else if (msg.startsWith("/msg")) {
					output.println(username);
					output.println(msg);
				} else if (msg.startsWith("/status")) {
					output.println(username);
					output.println(msg);
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