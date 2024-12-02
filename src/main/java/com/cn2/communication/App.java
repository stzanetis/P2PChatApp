package com.cn2.communication;

import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JOptionPane;

import java.awt.*;
import java.awt.event.*;
import java.lang.Thread;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.DataLine;
import javax.sound.sampled.LineUnavailableException;
import javax.sound.sampled.SourceDataLine;
import javax.sound.sampled.TargetDataLine;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.util.Base64;

public class App extends Frame implements WindowListener, ActionListener {

	// Definition of the app's fields
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;
	static JButton callButton;			
	static JTextField meesageTextField;
	public static Color darkGray;
	public static Color lightGray;			
	final static String newline="\n";		
	
	public static String destIp;
	public static String chatPort;
	public static String voicePort;
	public static boolean callInProgress = false;
	private TargetDataLine microphone;
	private SourceDataLine audioSocket;
	private static SecretKey secretKey;

	// Intialize the secret key for encryption and decryption
	static {
		try {
			KeyGenerator keyGen = KeyGenerator.getInstance("AES");
			keyGen.init(128);
			secretKey = keyGen.generateKey();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	// Construct the app's frame and initialize important parameters
	public App(String title) {
		// 1. Defining the components of the GUI
		super(title);
		darkGray = new Color(26, 26, 26);
		lightGray = new Color(211, 211, 211);
		setBackground(darkGray);
		setLayout(new FlowLayout());
		addWindowListener(this);
		setResizable(false);
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation((screenSize.width - getWidth() - 425) / 2, (screenSize.height - getHeight() - 640) / 2);

		// Setting up the TextField
		inputTextField = new TextField();
		inputTextField.setBackground(lightGray);
		inputTextField.setColumns(31);
		
		// Setting up the TextArea.
		textArea = new JTextArea(34,36);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		textArea.setBackground(lightGray);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		// Setting up the buttons
		sendButton = new JButton("Send");
		callButton = new JButton("Call");
						
		// 2. Adding the components to the GUI
		add(scrollPane);
		add(inputTextField);
		add(sendButton);
		add(callButton);
		
		// 3. Linking the buttons to the ActionListener
		sendButton.addActionListener(this);
		callButton.addActionListener(this);

		// 4. Addint a KeyListener to the TextField to send the message when the Enter key is pressed
		inputTextField.addKeyListener(new KeyAdapter() {
			@Override
			public void keyPressed(KeyEvent e) {
				if (e.getKeyCode() == KeyEvent.VK_ENTER) {
					sendButton.doClick();
				}
			}
		});
	}
	
	// The main method of the application. It continuously listens for new messages.
	public static void main(String[] args){
		// 1. Create the app's window
		App app = new App("Voice and Chat App by Tzanetis Savvas and Zoidis Vasilis");																		  
		app.setSize(425,640);			  
		app.setVisible(true);

		// Ask the user for the destination IP address and port number
		destIp = JOptionPane.showInputDialog(null, "Enter the IP address to send messages to:", "Destination IP", JOptionPane.QUESTION_MESSAGE);
		chatPort = "12345";  // Replace with the target port number
		voicePort = "12346"; // Replace with the target port number

		// Shows the message destination IP address
		textArea.append("Sending to " + destIp + newline);
		inputTextField.setText("");

		// Create a thread to receive messages
		new Thread(() -> {
			byte[] buffer = new byte[1024];
			try (DatagramSocket textSocket = new DatagramSocket(Integer.parseInt(chatPort))) {
				while (true) {
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
					textSocket.receive(packet);
					if (packet.getAddress().getHostAddress().equals(destIp)) {
						String message = new String(packet.getData(), 0, packet.getLength());
						textArea.append("Received: " + decrypt(message) + newline);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}).start();

		// Create a thread to receive audio data
		new Thread(() -> {
			byte[] buffer = new byte[1024];
			try (DatagramSocket audioSocket = new DatagramSocket(Integer.parseInt(voicePort))) {
				// Set up the audio format and line for playback
				AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
				DataLine.Info speakerInfo = new DataLine.Info(SourceDataLine.class, format);
				SourceDataLine speakers = (SourceDataLine) AudioSystem.getLine(speakerInfo);
				speakers.open(format);
				speakers.start();
				
				while (true) {
					DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
					audioSocket.receive(requestPacket);
					String requestMessage = new String(requestPacket.getData(), 0, requestPacket.getLength());
					if (requestMessage.equals("CALL_REQUEST") && !callInProgress) {
						int response = JOptionPane.showConfirmDialog(null, "Incoming call. Accept?", "Incoming Call", JOptionPane.YES_NO_OPTION);
						if (response == JOptionPane.NO_OPTION) {
							// Client did not accept the call

							// Send a response to the caller
							DatagramSocket responseSocket = new DatagramSocket();
							InetAddress address = InetAddress.getByName(destIp);
							int port = Integer.parseInt(voicePort);
							byte[] responseBuffer = "CALL_REJECTED".getBytes();
							DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, address, port);
							responseSocket.send(responsePacket);
							responseSocket.close();
							continue;
						} else {
							// Client accepted the call
							callInProgress = true;
							textArea.append("Call accepted." + newline);

							// Send a response to the caller
							DatagramSocket responseSocket = new DatagramSocket();
							InetAddress address = InetAddress.getByName(destIp);
							int port = Integer.parseInt(voicePort);
							byte[] responseBuffer = "CALL_ACCEPTED".getBytes();
							DatagramPacket responsePacket = new DatagramPacket(responseBuffer, responseBuffer.length, address, port);
							responseSocket.send(responsePacket);
							responseSocket.close();

							while(callInProgress) {
								DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
								audioSocket.receive(packet);
								if (packet.getAddress().getHostAddress().equals(destIp)) {
									String message = new String(packet.getData(), 0, packet.getLength());
									if (message.equals("CALL_END")) {
										callInProgress = false;
										speakers.close();
										textArea.append("Call ended." + newline);
										break;
									}
									speakers.write(packet.getData(), 0, packet.getLength());
								}
							}
						}
					}
				}
			} catch (LineUnavailableException | IOException e) {
				e.printStackTrace();
			}
		}).start();
	}
	
	// The method that corresponds to the Action Listener. Whenever an action is performed
	// (i.e., one of the buttons is clicked) this method is executed. 
	@Override
	public void actionPerformed(ActionEvent e) {
		// Check which button was clicked.
		if (e.getSource() == sendButton){
			// The "Send" button was clicked
			String message = inputTextField.getText();
			if (!message.trim().isEmpty()) {
				try {
					DatagramSocket textSocket = new DatagramSocket();
					InetAddress address = InetAddress.getByName(destIp); // Replace with the target IP address
					int port = Integer.parseInt(chatPort); // Replace with the target port number
					byte[] buffer = message.getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);

					// Print the message locally
					textArea.append("Me: " + message + newline);
					inputTextField.setText("");

					//Send the message to the target
					textSocket.send(packet);
					textSocket.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			}
		} else if (e.getSource() == callButton) {
			// The "Call" button was clicked
			if (callInProgress) {
				// End the call
				callInProgress = false;
				textArea.append("Call ended." + newline);
				audioSocket.close();
				microphone.close();
				callButton.setText("Call");

				// Send a message to the other client to end the call
				try {
					DatagramSocket audioSocket = new DatagramSocket();
					InetAddress address = InetAddress.getByName(destIp);
					int port = Integer.parseInt(voicePort);
					byte[] buffer = "CALL_END".getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
					audioSocket.send(packet);
					audioSocket.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}
			} else {
				// Send a call request to target
				try {
					DatagramSocket audioSocket = new DatagramSocket();
					InetAddress address = InetAddress.getByName(destIp);
					int port = Integer.parseInt(voicePort);
					byte[] buffer = "CALL_REQUEST".getBytes();
					DatagramPacket packet = new DatagramPacket(buffer, buffer.length, address, port);
					audioSocket.send(packet);
					audioSocket.close();
				} catch (IOException ex) {
					ex.printStackTrace();
				}

				// Wait for a response from the target
				try {
					DatagramSocket audioSocket = new DatagramSocket(Integer.parseInt(voicePort));
					byte[] buffer = new byte[1024];
					DatagramPacket responsePacket = new DatagramPacket(buffer, buffer.length);
					audioSocket.receive(responsePacket);
					String response = new String(responsePacket.getData(), 0, responsePacket.getLength());
					if (response.equals("CALL_ACCEPTED")) {
						// Target accepted the call
						callInProgress = true;
						textArea.append("Call accepted." + newline);
						textArea.append("");
						callButton.setText("End");

						// Capture audio from the microphone
						AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
						DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
						TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
						microphone.open(format);
						microphone.start();

						// Create a thread to send audio data
						new Thread(() -> {
							byte[] audioBuffer = new byte[1024];
							try {
								while (callInProgress) {
									int bytesRead = microphone.read(audioBuffer, 0, audioBuffer.length);
									DatagramPacket packet = new DatagramPacket(audioBuffer, bytesRead, responsePacket.getAddress(), responsePacket.getPort());
									audioSocket.send(packet);
								}
							} catch (IOException ex) {
								ex.printStackTrace();
							} finally {
								microphone.close();
								audioSocket.close();
							}
						}).start();
					} else {
						// Target rejected the call
						textArea.append("Call rejected." + newline);
						textArea.append("");
					}
					audioSocket.close();
				} catch (IOException | LineUnavailableException ex) {
					ex.printStackTrace();
				}
			}

			try {
				// Create a socket to send and receive audio data
				DatagramSocket audioSocket = new DatagramSocket();
				InetAddress address = InetAddress.getByName(destIp);
				int port = Integer.parseInt(voicePort);

				// Capture audio from the microphone
				AudioFormat format = new AudioFormat(44100, 16, 2, true, true);
				DataLine.Info info = new DataLine.Info(TargetDataLine.class, format);
				TargetDataLine microphone = (TargetDataLine) AudioSystem.getLine(info);
				microphone.open(format);
				microphone.start();

				// Create a thread to send audio data
				new Thread(() -> {
					byte[] buffer = new byte[1024];
					try {
						while (true) {
							int bytesRead = microphone.read(buffer, 0, buffer.length);
							DatagramPacket packet = new DatagramPacket(buffer, bytesRead, address, port);
							audioSocket.send(packet);
						}
					} catch (IOException ex) {
						ex.printStackTrace();
					} finally {
						if (audioSocket != null && !audioSocket.isClosed()) {
							audioSocket.close();
						}
					}
				}).start();
			} catch (LineUnavailableException | UnknownHostException | SocketException ex) {
				ex.printStackTrace();
			}
		}
	}

	// Method to encrypt messages
	public static String encrypt(String message) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.ENCRYPT_MODE, secretKey);
			byte[] encrypted = cipher.doFinal(message.getBytes("UTF-8"));
			return Base64.getEncoder().encodeToString(encrypted);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	// Method to decrypt messages
	public static String decrypt(String message) {
		try {
			Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, secretKey);
			byte[] decrypted = cipher.doFinal(Base64.getDecoder().decode(message));
			return new String(decrypted, "UTF-8");
		} catch (Exception e) {
			e.printStackTrace();
		}
		return message;
	}

	// These methods have to do with the GUI. You can use them if you wish to define what the program should do in specific scenarios
	// (e.g., when closing the window).
	@Override
	public void windowActivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosed(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowClosing(WindowEvent e) {
		// TODO Auto-generated method stub
		dispose();
        	System.exit(0);
	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowIconified(WindowEvent e) {
		// TODO Auto-generated method stub	
	}

	@Override
	public void windowOpened(WindowEvent e) {
		// TODO Auto-generated method stub	
	}
}
