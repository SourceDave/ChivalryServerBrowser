package com.tranek.chivalryserverbrowser;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.JLabel;
import javax.swing.filechooser.FileSystemView;
import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;


@SuppressWarnings("serial")
public class GamepadSensitivityDialog extends JDialog {

	private final MainWindow mw;
	private JSpinner spLRSensitivity;
	private JSpinner spUDSensitivity;
	
	public GamepadSensitivityDialog(MainWindow MW, Frame frame) {
		super(frame, "Gamepad Sensitivity", true);
		mw = MW;
		setMinimumSize(new Dimension(450, 315));
		setResizable(false);
		setPreferredSize(new Dimension(450, 315));
		getContentPane().setLayout(null);
		
		double lrSensitivity = getSpeed("UDKGame.ini", Keybinds.GBA_TURN_LEFT_GAMEPAD);
		spLRSensitivity = new JSpinner();
		spLRSensitivity.setModel(new SpinnerNumberModel(1.0, 0.05, 10.0, 0.01));
		spLRSensitivity.setBounds(253, 37, 56, 22);
		if ( lrSensitivity >= 0 ) {
			spLRSensitivity.setValue(lrSensitivity);
		}
		getContentPane().add(spLRSensitivity);
		
		JLabel lblLeftrightSensitivity = new JLabel("Left/Right Sensitivity");
		lblLeftrightSensitivity.setBounds(51, 38, 156, 16);
		getContentPane().add(lblLeftrightSensitivity);
		
		JLabel lblUpdownSensitivity = new JLabel("Up/Down Sensitivity");
		lblUpdownSensitivity.setBounds(51, 120, 156, 16);
		getContentPane().add(lblUpdownSensitivity);
		
		double udSensitivity = getSpeed("UDKGame.ini", Keybinds.GBA_LOOK_GAMEPAD);
		spUDSensitivity = new JSpinner();
		spUDSensitivity.setModel(new SpinnerNumberModel(0.65, 0.05, 10.0, 0.01));
		spUDSensitivity.setBounds(253, 119, 56, 22);
		if ( udSensitivity >= 0 ) {
			spUDSensitivity.setValue(udSensitivity);
		}
		getContentPane().add(spUDSensitivity);
		
		JLabel lblNote = new JLabel("Note: For a point of reference, a value of 5 is VERY FAST.");
		lblNote.setBounds(31, 203, 388, 16);
		getContentPane().add(lblNote);
		
		JButton btnSetToDefaults = new JButton("Set to defaults");
		btnSetToDefaults.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				setDefaults();
			}
		});
		btnSetToDefaults.setBounds(253, 240, 136, 25);
		getContentPane().add(btnSetToDefaults);
		
		JButton btnSave = new JButton("Save");
		btnSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if ( writeSpeed("UDKGame.ini", Keybinds.GBA_TURN_LEFT_GAMEPAD, "" + spLRSensitivity.getValue()) &&
		    			writeSpeed("UDKGame.ini", Keybinds.GBA_LOOK_GAMEPAD, "" + spUDSensitivity.getValue()) ) {
		    		mw.printlnMC("Saved keybind changes.");
		    	}
			}
		});
		btnSave.setBounds(51, 240, 136, 25);
		getContentPane().add(btnSave);
		
		
		setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		this.addWindowListener(new WindowAdapter() {
		    @Override
		    public void windowClosing(WindowEvent e) {
		    	setVisible(false);
		    }
		});
		
		setLocationRelativeTo(frame);
	}
	
	public void showDialog() {
		setVisible(true);
	}
	
	public void cancel() {
		setDefaults();
		setVisible(false);
	}
	
	public void setDefaults() {
		spLRSensitivity.setValue(1.0);
		spUDSensitivity.setValue(0.65);
	}
	
	public String getBinding(String file, String button) {
		JFileChooser fr = new JFileChooser();
		FileSystemView fw = fr.getFileSystemView();		
		String configDirectory = fw.getDefaultDirectory() + "\\My Games\\Chivalry Medieval Warfare\\UDKGame\\Config\\";
		
		String binding = "";
		BufferedReader br;
		try {
			br = new BufferedReader(new FileReader(configDirectory + file));
			String line;
			boolean foundKeybind = false;
			//find [UTGame.UTPlayerInput]
			while ((line = br.readLine()) != null) {
				if ( line.contains("[UTGame.UTPlayerInput]") ) {
					foundKeybind = true;
					break;
				}
			}
			//find our button
			if ( foundKeybind ) {
				boolean foundButton = false;
				boolean endOfBinding = false;
				while ((line = br.readLine()) != null) {
					if ( !foundButton && !endOfBinding ) {
						if ( line.contains("Bindings=(Name=\"" + button) && !line.contains("Bindings=(Name=\"" + button + "Axis") ) {
							foundButton = true;
							binding += line;
						}
					} else if ( foundButton && !endOfBinding ){
						if ( line.contains("Bindings=(Name=\"") ) {
							endOfBinding = true;
						} else {
							binding += line;
						}
					}
				}
			}
			br.close();
			
			//we found button, parse out commands
			if ( !binding.equals("") ) {
				int startIndex = binding.indexOf("Command=");
				String commands = binding.substring(startIndex + 9).trim();
				int endIndex = commands.indexOf("Control=") - 2;
				commands = commands.substring(0, endIndex).trim();
				return commands;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public double getSpeed(String file, String button) {
		String commands = getBinding(file, button);
		int startIndex = commands.indexOf("Speed=") + 6;
		int endIndex = commands.indexOf("DeadZone=");
		if ( startIndex > -1 && endIndex > -1 ) {
			return Double.parseDouble(commands.substring(startIndex, endIndex));
		}
		return -1;
	}
	
	public boolean writeSpeed(String file, String button, String speed) {
		String com = getBinding(file, button);
		String commands = "";
		int startIndex = com.indexOf("Speed=") + 6;
		int endIndex = com.indexOf("DeadZone=");
		if ( startIndex > -1 && endIndex > -1 ) {
			commands += com.substring(0, startIndex);
			commands += speed + " ";
			commands += com.substring(endIndex, com.length());
		} else {
			return false;
		}
		
		if ( commands.equals("") ) {
			return false;
		}
		
		return writeBinding(file, button, commands);
	}
	
	public boolean writeBinding(String file, String button, String commands) {
		
		JFileChooser fr = new JFileChooser();
		FileSystemView fw = fr.getFileSystemView();		
		String configDirectory = fw.getDefaultDirectory() + "\\My Games\\Chivalry Medieval Warfare\\UDKGame\\Config\\";
		
		String result = "";
		String result2 = "";
		String binding = "";
		BufferedReader br;
		String newLine = System.getProperty("line.separator");
		try {
			br = new BufferedReader(new FileReader(configDirectory + file));
			String line;
			boolean foundKeybind = false;
			//find [UTGame.UTPlayerInput]
			while ((line = br.readLine()) != null) {
				result += line + newLine;
				if ( line.contains("[UTGame.UTPlayerInput]") ) {
					foundKeybind = true;
					break;
				}
			}
			//find our button
			if ( foundKeybind ) {
				boolean foundButton = false;
				boolean endOfBinding = false;
				while ((line = br.readLine()) != null) {
					if ( !foundButton && !endOfBinding ) {
						if ( line.contains("Bindings=(Name=\"" + button) && !line.contains("Bindings=(Name=\"" + button + "Axis") ) {
							foundButton = true;
							binding += line;
						} else {
							result += line + newLine;
						}
					} else if ( foundButton && !endOfBinding ){
						if ( line.contains("Bindings=(Name=\"") ) {
							endOfBinding = true;
							result2 += line + newLine;
						} else {
							binding += line;
						}
					} else {
						result2 += line + newLine;
					}
				}
			}
			br.close();
			
			//found button, remove and insert new commands
			if ( !binding.equals("") && !commands.equals("") ) {
				int startIndex = binding.indexOf("Command=");
				int endIndex = binding.indexOf("Control=") - 2;
				
				String newBinding = binding.substring(0, startIndex + 9);
				newBinding += commands;
				newBinding += binding.substring(endIndex, binding.length());
				
				BufferedWriter bw = new BufferedWriter(new FileWriter(configDirectory + file));
				String out = result + newBinding + newLine + result2;
				bw.write(out);
				bw.close();
				return true;
			}
			return false;
		} catch (IOException e) {
			e.printStackTrace();
		}
			
		return false;
	}
	
}
