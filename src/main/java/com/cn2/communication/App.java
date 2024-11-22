package com.cn2.communication;

import java.io.*;
import java.net.*;

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.*;
import java.awt.event.*;
import java.awt.Color;
import java.lang.Thread;

public class App extends Frame implements WindowListener, ActionListener {

	// Definition of the app's fields
	static TextField inputTextField;		
	static JTextArea textArea;				 
	static JFrame frame;					
	static JButton sendButton;				
	static JTextField meesageTextField;		  
	public static Color gray;				
	final static String newline="\n";		
	static JButton callButton;
	
	// TODO: Please define and initialize your variables here...
	
	// Construct the app's frame and initialize important parameters
	public App(String title) {
		
		// 1. Defining the components of the GUI
		// Setting up the characteristics of the frame
		super(title);
		gray = new Color(226, 191, 228);
		setBackground(gray);
		setLayout(new FlowLayout());
		addWindowListener(this);
		
		// Setting up the TextField and the TextArea
		inputTextField = new TextField();
		inputTextField.setColumns(31);
		
		// Setting up the TextArea.
		textArea = new JTextArea(34,40);
		textArea.setLineWrap(true);
		textArea.setEditable(false);
		JScrollPane scrollPane = new JScrollPane(textArea);
		scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		
		//Setting up the buttons
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
	}
	
	// The main method of the application. It continuously listens for new messages.
	public static void main(String[] args){
	
		// 1. Create the app's window
		App app = new App("Voice and Chat App by Tzanetis Savvas and Zoidis Vasilis");																		  
		app.setSize(425,640);				  
		app.setVisible(true);				  

		// 2. 
		do{		
			// TODO: Your code goes here...
		}while(true);
	}
	
	// The method that corresponds to the Action Listener. Whenever an action is performed
	// (i.e., one of the buttons is clicked) this method is executed. 
	@Override
	public void actionPerformed(ActionEvent e) {
		// Check which button was clicked.
		if (e.getSource() == sendButton){	
			String message = inputTextField.getText();

			// If the message is not empty, append it to the textArea and clear the inputTextField.
			if (!message.trim().isEmpty()) {
				textArea.append("Me: " + message + newline);
				inputTextField.setText("");
			} 
		}else if(e.getSource() == callButton){
			// The "Call" button was clicked	
			// TODO: Your code goes here...
		}
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
