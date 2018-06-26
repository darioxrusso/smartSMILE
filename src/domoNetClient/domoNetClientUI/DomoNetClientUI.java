/* This file is part of DOMONET.

Copyright (C) 2006-2007 ISTI-CNR (Dario Russo)

DOMONET is free software; you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation; either version 2 of the License, or
(at your option) any later version.

DOMONET is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with DOMONET; if not, write to the Free Software
Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301  USA */

package domoNetClient.domoNetClientUI;

import javax.swing.*;
import javax.swing.tree.*;
import java.io.*;
import java.util.*;
import java.util.Calendar;
import java.text.SimpleDateFormat;

import org.xml.sax.*;
import org.xml.sax.helpers.*;

import org.w3c.dom.*;

import domoNetClient.*;
import domoML.domoDevice.*;
import domoML.domoMessage.*;

// explicit declaration of java.awt components in order to don't specify
// every time that I declare a List that is a List
// (exists a java.awt.List too)
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Dimension;

import java.awt.event.*;

/** This class implements an graphical interface for the DomoNetClient. */
public class DomoNetClientUI {
    // private static JTree tree;
    /** The combo box containing the list of web services used. */
    private static JComboBox webServicesComboBox;

    /** The text field keeps the description of the web services used. */
    private static JTextField webServicesDescription;

    /** The tree rappresetation of the devices. */
    private static JScrollPane treeView;

    /** The debug text area. */
    private static JTextArea debugTextArea;

    /** The real client. */
    private static DomoNetClient domoNetClient = null;

    /** Take a collection of the default web services used. */
    private static HashMap<String, String> defaultWebServicesDescriptors = new HashMap<String, String>();

    /** The file used for taking the list of the default web services. */
    private static String defaultWebServicesDescriptorsFile;

    /**
     * Create the GUI and show it. For thread safety, this method should be
     * invoked from the event-dispatching thread.
     */
    private static void createAndShowGUI() {
	// Make sure have nice window decorations.
	JFrame.setDefaultLookAndFeelDecorated(true);

	// Create the main panel to contain the three sub panels.
	JPanel mainPanel = new JPanel();
	mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.PAGE_AXIS));
	mainPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	// Add the three panels to the main panel.
	mainPanel.add(buildManageWebServicesPanel());
	mainPanel.add(buildShowWebServicesPanel());
	mainPanel.add(buildDebugPanel());

	// Create and set up the window.
	JFrame frame = new JFrame("DomoNet Client");
	frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	frame.getContentPane().add(mainPanel);

	// Display the window.
	frame.pack();
	frame.setVisible(true);
    }

    /** Launch methods for creating the swing application. */
    public static final void main(final String[] args) {
	// init the real client
	try {
	    domoNetClient = new DomoNetClient();
	} catch (Exception e) {
	    e.printStackTrace();
	}
	// Schedule a job for the event-dispatching thread:
	// creating and showing this application's GUI.
	javax.swing.SwingUtilities.invokeLater(new Runnable() {
	    public final void run() {
		initValues();
		createAndShowGUI();
		printLicenceNote();
		// Some debug messages
		DomoNetClientUI.writeln("Connecting to "
			+ webServicesComboBox.getSelectedItem().toString()
			+ " (" + webServicesDescription.getText().toString()
			+ ")...");
		// Call connect method on the real client
		try {
		    domoNetClient.connectToWebServices(
			    webServicesComboBox.getSelectedItem().toString(),
			    webServicesDescription.getText().toString());
		    // update domo list viewer
		    updateTreeView(domoNetClient.domoDeviceList.values());
		    writeln("  device list updated");
		} catch (Exception e2) {
		    writeln("  Cannot connect. The site may be down. ");
		}
		writeln("done.");
	    }
	});
    }

    /**
     * Build the panel containing a combobox with buttons in order to manage web
     * services used.
     * 
     * @return The managing web services panel.
     */
    private static final JPanel buildManageWebServicesPanel() {
	JPanel panel = new JPanel();

	// Create a border for the panel
	panel.setBorder(BorderFactory.createCompoundBorder(
		BorderFactory.createTitledBorder("Manage Web Services"),
		BorderFactory.createEmptyBorder(5, 5, 5, 5)));

	// Flexible layout manager that aligns components vertically and
	// horizontally
	panel.setLayout(new GridBagLayout());

	// Specifies constraints for components that are laid out using the
	// GridBagLayout class.
	GridBagConstraints gridBagConstraints = new GridBagConstraints();

	// Add label for URL field
	gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
	gridBagConstraints.fill = GridBagConstraints.NONE;
	gridBagConstraints.weightx = 0.0;
	panel.add(new JLabel("URL: "), gridBagConstraints);

	// Create a combobox with web services choises and with line editor
	// feature initialized by an XML file.
	// loading values of combo box from the defaults web services
	// settings.
	Vector<String> defaultWebServicesURLs = new Vector<String>(
		defaultWebServicesDescriptors.keySet());
	webServicesComboBox = new JComboBox(defaultWebServicesURLs);
	webServicesComboBox.setEditable(true);

	// Add combobox for URL field
	gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
	gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
	gridBagConstraints.weightx = 1.0;
	panel.add(webServicesComboBox, gridBagConstraints);

	// Add label for Description field
	gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
	gridBagConstraints.fill = GridBagConstraints.NONE;
	gridBagConstraints.weightx = 0.0;
	panel.add(new JLabel("Description: "), gridBagConstraints);

	// Create a text field for description
	webServicesDescription = new JTextField(20);
	// Add combobox Description field
	gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
	gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
	gridBagConstraints.weightx = 1.0;
	panel.add(webServicesDescription, gridBagConstraints);

	// Assign the description for the selected item (if any) in the
	// webServicesComboBox
	if (defaultWebServicesDescriptors.containsKey(
		webServicesComboBox.getSelectedItem().toString())) {
	    webServicesDescription.setText(defaultWebServicesDescriptors
		    .get(webServicesComboBox.getSelectedItem().toString()));
	}

	// Add an action to comboBox when switching values.
	webServicesComboBox.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		// put in the webServicesDescription the text taken from
		// the hashmap containing URL as key and description as value
		webServicesDescription.setText(defaultWebServicesDescriptors
			.get(webServicesComboBox.getSelectedItem().toString()));
	    }
	});

	// panel.add(webServicesDescription, gridBagConstraints);
	JButton button = new JButton("Connect");
	button.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		// Some debug messages
		DomoNetClientUI.writeln("Connecting to "
			+ webServicesComboBox.getSelectedItem().toString()
			+ " (" + webServicesDescription.getText().toString()
			+ ")...");
		// Call connect method on the real client
		try {
		    domoNetClient.connectToWebServices(
			    webServicesComboBox.getSelectedItem().toString(),
			    webServicesDescription.getText().toString());
		    // update domo list viewer
		    updateTreeView(domoNetClient.domoDeviceList.values());
		    writeln("  device list updated");
		} catch (Exception e2) {
		    writeln("  Cannot connect. The site may be down. ");
		}
		writeln("done.");
	    }
	});
	panel.add(button);

	JButton disconnectButton = new JButton("Close update socket");
	// Some debug messages
	disconnectButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		DomoNetClientUI.writeln("Disconnecting from "
			+ webServicesComboBox.getSelectedItem().toString()
			+ " (" + webServicesDescription.getText().toString()
			+ ")...");
		// Call disconnect method on the real client
		try {
		    domoNetClient.disconnectUpdateSocket();
		} catch (Exception e2) {
		    writeln("  Cannot close update socket. ");
		}
		writeln("done.");
	    }
	});
	panel.add(disconnectButton);

	JButton shutdownButton = new JButton("Shutdown local server");
	// Some debug messages
	shutdownButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		DomoNetClientUI.writeln("Shutting down "
			+ webServicesComboBox.getSelectedItem().toString()
			+ " (" + webServicesDescription.getText().toString()
			+ ")...");
		// Call shutdown method on the real client
		try {
		    domoNetClient.shutdownWebServices(
			    webServicesComboBox.getSelectedItem().toString(),
			    webServicesDescription.getText().toString());
		} catch (Exception e2) {
		    writeln("  Cannot shutdown local server. ");
		}
		writeln("done.");
	    }
	});
	panel.add(shutdownButton);
	return panel;
    }

    /**
     * Build the panel containing a box showing a tree with the components of
     * each device of each web service.
     * 
     * @return The showing web services panel.
     */
    private static final JPanel buildShowWebServicesPanel() {
	JPanel panel = new JPanel();
	// Create a border for the panel.
	panel.setBorder(BorderFactory.createCompoundBorder(
		BorderFactory.createTitledBorder("Show Web Services"),
		BorderFactory.createEmptyBorder(5, 5, 5, 5)));

	// Flexible layout manager that aligns components vertically and
	// horizontally
	panel.setLayout(new GridBagLayout());

	// Specifies constraints for components that are laid out using the
	// GridBagLayout class.
	GridBagConstraints gridBagConstraints = new GridBagConstraints();

	// Create the nodes.
	DefaultMutableTreeNode top = new DefaultMutableTreeNode("Devices");
	// createNodes(top);

	// Create a tree that allows one selection at a time.
	JTree tree;
	tree = new JTree(top);
	tree.getSelectionModel()
		.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

	tree = new JTree(top);
	tree.getSelectionModel()
		.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

	// Listen for when the selection changes.
	// tree.addTreeSelectionListener(this);

	// Create the scroll pane and add the tree to it.
	treeView = new JScrollPane(tree);
	treeView.setPreferredSize(new Dimension(200, 170));
	treeView.setMinimumSize(new Dimension(100, 100));

	// Add the treeView to the panel
	gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
	gridBagConstraints.fill = GridBagConstraints.BOTH;
	gridBagConstraints.weightx = 1.0;
	panel.add(treeView, gridBagConstraints);

	return panel;
    }

    /**
     * Build the panel containing a box showing debug and state messages.
     * 
     * @return The debug web services panel.
     */
    private static final JPanel buildDebugPanel() {
	JPanel panel = new JPanel();
	// Create a border for the panel.
	panel.setBorder(BorderFactory.createCompoundBorder(
		BorderFactory.createTitledBorder("Messages"),
		BorderFactory.createEmptyBorder(5, 5, 5, 5)));

	// Flexible layout manager that aligns components vertically and
	// horizontally
	panel.setLayout(new GridBagLayout());

	// Specifies constraints for components that are laid out using the
	// GridBagLayout class.
	GridBagConstraints gridBagConstraints = new GridBagConstraints();

	// Create a text area to show messages. This area is not editable.
	debugTextArea = new JTextArea();
	debugTextArea.setEditable(false);
	debugTextArea.setFont(new java.awt.Font("Verdana", 0, 11));
	JScrollPane debugView = new JScrollPane(debugTextArea);
	debugView.setPreferredSize(new Dimension(200, 145));
	debugView.setMinimumSize(new Dimension(10, 10));

	// Add to the panel the text area.
	gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
	gridBagConstraints.fill = GridBagConstraints.BOTH;
	gridBagConstraints.weightx = 1.0;
	panel.add(debugView, gridBagConstraints);

	return panel;
    }

    /** Set the default web services descriptors file. */
    public static final void setDefaultWebServicesDescriptorsFile(
	    final String value) {
	defaultWebServicesDescriptorsFile = value;
    }

    /** Get the default web services descriptors file. */
    public static final String getDefaultWebServicesDescriptorsFile() {
	return defaultWebServicesDescriptorsFile;
    }

    /** Inits some statup values. */
    private static final void initValues() {
	setDefaultWebServicesDescriptorsFile(
		"xml/defaultWebServicesDescriptorsFile.xml");
	initDefaultWebServicesDescriptors(
		getDefaultWebServicesDescriptorsFile());
    }

    /**
     * Init the defaultWebServicesDescriptors parsing an xmlFile and return an
     * hashmap rappresenting the URL of the web services as key and the
     * description as value.
     * 
     * @param xmlFile
     *            The input file.
     * @return The hashmap.
     */
    private static final void initDefaultWebServicesDescriptors(
	    final String xmlFile) {
	// checking if a file is selected
	if (xmlFile.trim().equalsIgnoreCase("")) {
	    writeln("No default web services descriptors file found.");
	} else {
	    File configFile = new File(xmlFile);
	    // check if file exists and it's readable
	    if (!configFile.canRead()) {
		writeln(xmlFile + " can't be readed."
			+ " Check if file exists or the permission flags.");
	    } else {
		// preparing for parsing xml config file.
		try {
		    XMLReader parser = XMLReaderFactory.createXMLReader();
		    DefaultWebServicesDescriptorsSAXParser dwsdsp = new DefaultWebServicesDescriptorsSAXParser();
		    parser.setContentHandler(dwsdsp);
		    parser.setErrorHandler(dwsdsp);
		    FileReader fileReader = new FileReader(xmlFile);
		    parser.parse(new InputSource(fileReader));
		    // assign returned value to the property of this class.
		    defaultWebServicesDescriptors = dwsdsp
			    .getDefaultWebServicesDescriptors();
		} catch (Exception e) {
		    e.printStackTrace();
		}
	    }
	}
    }

    /**
     * Update the Treeview showing the current available devices.
     * 
     * @param domoDeviceList
     *            The hashmap containing the list of device.
     */
    private static final void updateTreeView(final Collection domoDeviceList) {
	// Create the main node
	DefaultMutableTreeNode top = new DefaultMutableTreeNode("Devices");
	// Than create the others
	// scann the domoDeviceList hashmap
	Iterator domoDeviceListIt = domoDeviceList.iterator();
	while (domoDeviceListIt.hasNext()) {
	    // get the device
	    DomoDevice currentDevice = (DomoDevice) domoDeviceListIt.next();
	    // build the tree for the device containing services
	    DefaultMutableTreeNode deviceNode = new DefaultMutableTreeNode(
		    currentDevice.getType() + " ("
			    + currentDevice.getDescription() + ")");
	    // add services to the deviceNode
	    Iterator deviceServiceIterator = currentDevice.getServices()
		    .iterator();
	    while (deviceServiceIterator.hasNext()) {
		DomoServiceDescriptor serviceDescriptor = new DomoServiceDescriptor(
			currentDevice,
			((DomoDeviceService) deviceServiceIterator.next()));
		deviceNode.add(new DefaultMutableTreeNode(serviceDescriptor));
	    }

	    String currentUrl = currentDevice.getUrl();
	    // searching for a top child containing the currentUrl
	    // and store it's position into j
	    boolean found = false;
	    int j = 0;
	    for (j = 0; j < top.getChildCount() && !found; j++) {
		if (top.getChildAt(j).toString().contains(currentUrl)) {
		    found = true;
		}
	    }
	    // The child was not found so must create one
	    if (!found) {
		// insert a new child for top in the childCount position
		// (the position of the last top child is childCount -1)
		j = top.getChildCount();
		top.add(new DefaultMutableTreeNode(
			defaultWebServicesDescriptors.get(currentUrl) + " ("
				+ currentUrl + ")"));
	    } else
		j--; // j was incremented into for loop when found was true
		     // than insert the device in the right place
	    ((DefaultMutableTreeNode) top.getChildAt(j)).add(deviceNode);
	}
	final JTree tree;
	tree = new JTree(top);
	tree.addMouseListener(new MouseAdapter() {
	    DefaultMutableTreeNode node;

	    // listen for a double click on leaf
	    public void mousePressed(MouseEvent e) {
		TreePath path = tree.getPathForLocation(e.getX(), e.getY());
		if (path != null && e.getClickCount() == 2) {
		    node = (DefaultMutableTreeNode) path.getLastPathComponent();
		    if (node.isLeaf()) {
			// double-Click detected on leaf node!
			serviceDisplayer(node);
		    } else {
			writeln(" ");
			if (tree.isCollapsed(path))
			    tree.expandPath(path);
			else
			    tree.collapsePath(path);
		    }
		}
	    }
	});
	treeView.setViewportView(tree);
    }

    /**
     * Display a detailed device inside a new JPanel. Using the new JPanel it's
     * possible to use the services of the device.
     * 
     * @param node
     *            The node that contains the DomoDeviceId the device.
     */
    private static final void serviceDisplayer(
	    final DefaultMutableTreeNode node) {

	writeln("Displaying: " + node.toString());
	JPanel servicePanel = new JPanel();
	servicePanel
		.setLayout(new BoxLayout(servicePanel, BoxLayout.PAGE_AXIS));
	servicePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));

	DomoServiceDescriptor serviceDescriptor = (DomoServiceDescriptor) node
		.getUserObject();

	// add service informations and fields
	servicePanel.add(
		buildDeviceDescriptorPanel(serviceDescriptor.getDomoDevice()));
	servicePanel
		.add(buildDeviceServicePanel(serviceDescriptor.getDomoDevice(),
			serviceDescriptor.getDomoService()));

	// Create and set up the window.
	JFrame frame = new JFrame("DomoNet Client");
	frame.getContentPane().add(servicePanel);

	// Display the window.
	frame.pack();
	frame.setVisible(true);
    }

    /**
     * Build the panel containing a box showing description of the device.
     * 
     * @return The JPanel containing the description of the device.
     */
    private static final JPanel buildDeviceDescriptorPanel(
	    final DomoDevice domoDevice) {
	JPanel panel = new JPanel();
	// Create a border for the panel
	panel.setBorder(BorderFactory.createCompoundBorder(
		BorderFactory.createTitledBorder("Device informations"),
		BorderFactory.createEmptyBorder(5, 5, 5, 5)));

	// Flexible layout manager that aligns components vertically and
	// horizontally
	panel.setLayout(new GridBagLayout());

	// Specifies constraints for components that are laid out using the
	// GridBagLayout class.
	GridBagConstraints gridBagConstraints = new GridBagConstraints();

	// Add labels for information about url
	gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
	gridBagConstraints.fill = GridBagConstraints.NONE;
	gridBagConstraints.weightx = 0.0;
	panel.add(new JLabel("URL: "), gridBagConstraints);

	gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
	gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
	gridBagConstraints.weightx = 1.0;
	panel.add(
		new JLabel(
			domoDevice.getUrl() + " [" + domoDevice.getId() + "]"),
		gridBagConstraints);

	// Add labels for informations
	gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
	gridBagConstraints.fill = GridBagConstraints.NONE;
	gridBagConstraints.weightx = 0.0;
	panel.add(new JLabel("Type: "), gridBagConstraints);

	// Add a text for type field
	gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
	gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
	gridBagConstraints.weightx = 1.0;
	panel.add(new JLabel(domoDevice.getType()), gridBagConstraints);

	// Add label for Description field
	gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;
	gridBagConstraints.fill = GridBagConstraints.NONE;
	gridBagConstraints.weightx = 0.0;
	panel.add(new JLabel("Description: "), gridBagConstraints);

	// Create a text field for description
	webServicesDescription = new JTextField(20);
	// Add combobox Description field
	gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
	gridBagConstraints.fill = GridBagConstraints.HORIZONTAL;
	gridBagConstraints.weightx = 1.0;
	panel.add(new JLabel(domoDevice.getDescription()), gridBagConstraints);

	return panel;
    }

    /**
     * Build the piece of panel that displays a single service of the device.
     * 
     * @param domoDevice
     *            The device.
     * @param serviceElement
     *            The service element containing the informations.
     * @return The panel.
     */
    private static final JPanel buildDeviceServicePanel(
	    final DomoDevice domoDevice,
	    final DomoDeviceService serviceElement) {
	List<StringJComponentObj> serviceInputs = new LinkedList<StringJComponentObj>();
	// Little hack: use an HashMap for store only one field with key
	// "output"
	// in order to have a reference to the value that rappresent the widget
	// that implements the
	// output for the service.
	// If the value is null (init value), no output is provided.
	HashMap<String, JComponent> serviceOutput = new HashMap<String, JComponent>();
	serviceOutput.put("output", null);
	// Maps as key the input name and as value this DomoDevice.DataType
	HashMap<String, DomoDevice.DataType> inputDataTypes = new HashMap<String, DomoDevice.DataType>();
	// the panel to be returned
	JPanel panel = new JPanel();
	// Flexible layout manager that aligns components vertically and
	// horizontally
	panel.setLayout(new GridBagLayout());
	// Specifies constraints for components that are laid out using the
	// GridBagLayout class.
	GridBagConstraints gridBagConstraints = new GridBagConstraints();

	// Create a border for the panel
	panel.setBorder(BorderFactory.createCompoundBorder(
		BorderFactory.createTitledBorder(
			serviceElement.getAttribute("description")),
		BorderFactory.createEmptyBorder(5, 5, 5, 5)));
	// add description for the service
	gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
	gridBagConstraints.gridwidth = GridBagConstraints.RELATIVE;

	// add the output field for the service if one exists
	panel.add(buildDeviceServiceOutputPanel(serviceElement, serviceOutput));
	// add the button to the panel
	panel.add(buildDeviceServiceButtonPanel(domoDevice, serviceElement,
		serviceOutput, serviceInputs, inputDataTypes));
	// add the input field / s to the panel if one or more exists
	panel.add(buildDeviceServiceInputsPanel(serviceElement, serviceInputs,
		inputDataTypes));
	return panel;
    }

    /**
     * Build the field for insert the output result (if one) of the service.
     * 
     * @param domoDevice
     *            The device that contains the service.
     * @param serviceElement
     *            The service to be added.
     * @param serviceOutput
     *            The reference to the output field.
     * @return The panel.
     */
    private static final JPanel buildDeviceServiceOutputPanel(
	    final DomoDeviceService serviceElement,
	    HashMap<String, JComponent> serviceOutput) {
	JPanel panel = new JPanel();
	// if serviceElement has an attribute named "output",
	// the service implements the output
	try {
	    // try to get the output attribute. if fail catch the
	    // NoElementFoundException else the output attribute exists
	    serviceElement.getOutput(); // do nothing but if fails throws the
	    // NoElementFoundException
	    JComponent outputField = null;
	    /*
	     * // TODO: support to show MEDIALIST datatype
	     * if(serviceElement.getOutput().equals(DomoDevice.DataType.
	     * MEDIALIST)) outputField = buildShowWebServicesPanel(); else
	     */outputField = new JTextField(10);
	    // store the output field with type
	    serviceOutput.put("output", outputField);
	    // then add it to the panel
	    panel.add(outputField);
	} catch (domoML.domoDevice.NoAttributeFoundException nafe) {
	    // no output required so insert into list a null value
	    serviceOutput = null;
	    panel.add(new JLabel(""));
	}
	return panel;
    }

    /**
     * Build the button to be pressed to execute the service to be lauched.
     * 
     * @param domoDevice
     *            The device.
     * @param serviceElement
     *            The service to be added.
     * @param serviceOutput
     *            The reference to the output field.
     * @param serviceInputs
     *            The reference to the HashMap that contains the input fields.
     * @param inputDataType
     *            The reference to the HashMap that contains the
     *            DomoDevice.dataType of the input fields.
     * @return The panel that contains the button field.
     */
    private static final JPanel buildDeviceServiceButtonPanel(
	    final DomoDevice domoDevice, final DomoDeviceService serviceElement,
	    final HashMap<String, JComponent> serviceOutput,
	    final List<StringJComponentObj> serviceInputs,
	    final HashMap<String, DomoDevice.DataType> inputDataTypes) {
	JPanel panel = new JPanel();
	// creating the button with the service to call
	JButton prettyNameButton = new JButton(
		serviceElement.getAttribute("prettyName"));
	// add to the button an action
	prettyNameButton.addActionListener(new ActionListener() {
	    public void actionPerformed(ActionEvent e) {
		// build a basic domoMessage
		DomoMessage domoMessage = new DomoMessage(domoDevice.getUrl(),
			domoDevice.getId(), domoDevice.getUrl(),
			domoDevice.getId(), serviceElement.getAttribute("name"),
			DomoMessage.MessageType.COMMAND);
		// build paramters
		// getting keys for inputs
		Iterator serviceInputEntriesIterator = serviceInputs.iterator();
		// scanning all inputs
		while (serviceInputEntriesIterator.hasNext()) {
		    StringJComponentObj imputEntry = (StringJComponentObj) serviceInputEntriesIterator
			    .next();
		    DomoDevice.DataType inputDataType = inputDataTypes
			    .get(imputEntry.getString());
		    if (imputEntry.getJcomponent().getClass().toString()
			    .matches("class javax.swing.JTextField")) {
			// searching for the type of the input
			domoMessage
				.addInput(imputEntry.getString(),
					((JTextField) imputEntry
						.getJcomponent()).getText(),
				inputDataType);
		    } else {
			// take the input parameter from a combo box
			domoMessage.addInput(imputEntry.getString(),
				((JComboBox) imputEntry.getJcomponent())
					.getSelectedItem().toString(),
				inputDataType);
		    }
		}
		write("Executing " + domoMessage.getMessage() + " on "
			+ domoMessage.getReceiverURL() + "... ");
		try {
		    if (serviceOutput.get("output") != null) {
			// an output field exists
			// get the output type and store it in the DomoMessage
			domoMessage.setOutput(serviceElement.getOutput());
			try {
			    // set the name of the output
			    domoMessage.setOutputName(
				    serviceElement.getOutputName());
			} catch (domoML.domoDevice.NoAttributeFoundException nafe) {
			}
			// getting the datatype of output
			if (serviceOutput.get("output").getClass().toString()
				.matches("class javax.swing.JTextField")) {
			    // set the text of the output field as result of the
			    // execute command
			    ((JTextField) serviceOutput.get("output")).setText(
				    domoNetClient.execute(domoMessage));
			} else if (serviceOutput.get("output").getClass()
				.toString()
				.matches("class javax.swing.JPanel")) {
			    domoNetClient.execute(domoMessage);
			}
		    } else {
			// no output is needed
			domoNetClient.execute(domoMessage);
		    }
		    writeln("done.");
		} catch (Exception e2) {
		    writeln("failure.");
		}
	    }
	});
	panel.add(prettyNameButton);
	return panel;
    }

    /**
     * Build the input fields for the service.
     * 
     * @param domoDevice
     *            The device.
     * @param serviceElement
     *            The service to be added.
     * @return The panel that contains the input fields.
     */
    private static final JPanel buildDeviceServiceInputsPanel(
	    final DomoDeviceService serviceElement,
	    List<StringJComponentObj> serviceInputs,
	    HashMap<String, DomoDevice.DataType> inputDataTypes) {
	JPanel panel = new JPanel();
	GridBagConstraints gridBagConstraints = new GridBagConstraints();

	// try to get inputElement. If it doesn't exists a
	// NoElementFound exception is raised
	List<DomoDeviceServiceInput> serviceInEl = new LinkedList<DomoDeviceServiceInput>();
	serviceInEl = (LinkedList<DomoDeviceServiceInput>) serviceElement
		.getInputs();
	// input elements found. It can be 0 if no inputs found.
	int serviceInputElementsSize = serviceInEl.size();
	// panel where to store input Elements
	JPanel inputElementsPanel = new JPanel();
	// layout for the new panel
	GridBagLayout gridbag = new GridBagLayout();
	GridBagConstraints inputElementsGBC = new GridBagConstraints();
	inputElementsPanel.setLayout(gridbag);
	// wants all inputs fields on vertical
	inputElementsGBC.gridwidth = GridBagConstraints.REMAINDER;
	inputElementsGBC.fill = GridBagConstraints.VERTICAL;
	for (int i = 0; i < serviceInputElementsSize; i++) {
	    DomoDeviceServiceInput serviceInputElement = serviceInEl.get(i);
	    inputElementsPanel
		    .add(new JLabel(serviceInputElement.getAttribute("type")
			    + " " + serviceInputElement.getAttribute("name")));
	    // create the input field. A JComboBox if there are
	    // "allow" tag for this input, JTextField otherwise

	    List<DomoDeviceServiceInputAllowed> allowedValues = (LinkedList<DomoDeviceServiceInputAllowed>) serviceInputElement
		    .getAllowed();
	    if (allowedValues.size() != 0) {
		// found allowed values so put a JComboBox
		JComboBox inputField = new JComboBox();
		inputField.setEditable(true);
		for (int j = 0; j < allowedValues.size(); j++)
		    inputField.addItem(((Element) allowedValues.get(j))
			    .getAttribute("value"));
		// add it at the end of the list
		serviceInputs.add(new StringJComponentObj(
			serviceInputElement.getAttribute("name"), inputField));
		inputDataTypes.put(serviceInputElement.getAttribute("name"),
			DomoDevice.DataType.valueOf((String) serviceInputElement
				.getAttribute("type")));
		// then add it to the panel
		inputElementsPanel.add(inputField, inputElementsGBC);
	    } else {
		// not found allowed values so put a JTextField
		JTextField inputField = new JTextField(10);
		// add it at the end of the list
		serviceInputs.add(new StringJComponentObj(
			serviceInputElement.getAttribute("name"), inputField));
		inputDataTypes.put(serviceInputElement.getAttribute("name"),
			DomoDevice.DataType.valueOf((String) serviceInputElement
				.getAttribute("type")));
		// then add it to the panel
		inputElementsPanel.add(inputField, inputElementsGBC);
	    }
	}

	// the input element panel is the last to be inserted in this row
	gridBagConstraints.gridwidth = GridBagConstraints.REMAINDER;
	panel.add(inputElementsPanel, gridBagConstraints);
	return panel;
    }

    public final static void printLicenceNote() {
	debugTextArea
		.append("************************************************\n");
	debugTextArea.append("** DomoNet Client\n");
	debugTextArea.append("**\n");
	debugTextArea.append("** Copyright(C) 2006 Dario Russo\n");
	debugTextArea.append("** DomoNet comes with ABSOLUTELY NO WARRANTY.\n");
	debugTextArea.append("** This is free software, and you are welcome\n");
	debugTextArea.append("** to redistribuite it under certain\n");
	debugTextArea.append("** conditions. For details see COPYING file.\n");
	debugTextArea
		.append("************************************************\n");
    }

    /*** Code for write to the debug window ***********************************/

    /**
     * The string format of the date and time to be displayed with the message
     */
    private static final String DATE_FORMAT_NOW = "yyyy-MM-dd HH:mm:ss";

    private static String now() {
	Calendar cal = Calendar.getInstance();
	SimpleDateFormat sdf = new SimpleDateFormat(DATE_FORMAT_NOW);
	return sdf.format(cal.getTime());
    }

    /** Say if the next write is on a new line */
    private static boolean writeToNewLine = true;

    /**
     * Writes a message on the debug window.
     * 
     * @param message
     *            The message to display.
     * @param retCarr
     *            true if new line is requested, false otherwise.
     */
    private static final void write(final String message,
	    final boolean retCarr) {
	if (writeToNewLine)
	    // places date and time only at the beginning of a line
	    debugTextArea.append("[" + now() + "] ");
	debugTextArea.append(message);
	if (retCarr) {
	    debugTextArea.append("\n");
	    writeToNewLine = true;
	} else
	    writeToNewLine = false;
    }

    /**
     * Writes a message on the debug window.
     * 
     * @param message
     *            The message to display followed by new line.
     */
    public static final void write(final String message) {
	write(message, false);
    }

    /**
     * Writes a message on the debug window.
     * 
     * @param message
     *            The message to display followed by new line.
     */
    public static final void writeln(final String message) {
	write(message, true);
    }
}

// Maps the input name and widget that
// implements the input for the service
class StringJComponentObj {

    String string;
    JComponent jcomponent;

    public StringJComponentObj(String string, JComponent jcomponent) {
	this.string = string;
	this.jcomponent = jcomponent;
    }

    public String getString() {
	return string;
    }

    public JComponent getJcomponent() {
	return jcomponent;
    }

}
