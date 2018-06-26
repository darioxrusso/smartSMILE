/*
 *  Copyright (C) 2004 Cidero, Inc.
 *
 *  Permission is hereby granted to any person obtaining a copy of 
 *  this software to use, copy, modify, merge, publish, and distribute
 *  the software for any non-commercial purpose, subject to the
 *  following conditions:
 *  
 *  The above copyright notice and this permission notice shall be included
 *  in all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS 
 *  OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL 
 *  THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY IN CONNECTION WITH THE SOFTWARE.
 * 
 *  File: $RCSfile: MediaDevice.java,v $
 *
 */

package domoNetWS.techManager.upnpManager;

import java.util.logging.Logger;
import java.util.Properties;
import java.net.URL;
import java.net.MalformedURLException;

import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Image;
import java.awt.Dimension;

import javax.swing.JButton;
import javax.swing.ImageIcon;
import javax.swing.SwingConstants;
import javax.swing.BorderFactory;
import javax.swing.border.EmptyBorder;

import org.cybergarage.upnp.Device;
import org.cybergarage.upnp.Service;
import org.cybergarage.upnp.ServiceList;
import org.cybergarage.upnp.Action;
import org.cybergarage.upnp.UPnPStatus;

import com.cidero.upnp.AbstractService;
import com.cidero.util.MrUtil;
// modified by Dario Russo
// import com.cidero.util.AppPreferences;

/**
 * Class containing common control application info for MediaServer's and
 * MediaRenderer's. MediaServer and MediaRenderer-specific subclasses extend
 * this class
 */
public abstract class MediaDevice implements ActionListener {
	private final static Logger logger = Logger.getLogger("com.cidero.control");

	public final static int MAX_DEVICES = 20;

	public final static int SERVER = 0;
	public final static int RENDERER = 1;

	MediaController parentController; // parent controller instance

	Device device; // Underlying clink UPNP device
	JButton button;
	boolean enabled = true;

	// Device icon - can come from device, or be overridden in property file
	ImageIcon icon = null;
	// Defaults to friendly name, but can be overridden in property file
	String iconName = null;

	AppPreferences pref;

	public MediaDevice(MediaController parentController, Device device) {
		this.parentController = parentController;
		this.device = device;
	}

	abstract public int getDeviceType(); // RENDER or SERVER

	abstract public String getDeviceTypeString(); // 'renderer' or 'server'

	abstract public void actionPerformed(ActionEvent e);

	abstract public void destroy();

	public void setEnabled(boolean value) {
		enabled = value;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public Device getDevice() {
		return device;
	}

	public String getFriendlyName() {
		return device.getFriendlyName();
	}

	public String getPresentationURL() {
		return device.getPresentationURL();
	}

	public String getURLBase() {
		return device.getResolvedURLBase(); // HACK Alert - TODO - fix Clink
											// lib!
	}

	public String getModelName() {
		return device.getModelName();
	}

	public String getUUID() {
		return device.getUUID();
	}

	public String getHost() {
		return device.getHost();
	}

	public int getPort() {
		return device.getPort();
	}

	public ImageIcon getIcon() {
		return icon;
	}

	public MediaController getParentController() {
		return parentController;
	}

	public void setButton(JButton button) {
		this.button = button;
	}

	public JButton getButton() {
		return button;
	}

	/**
	 * Post a control action, saving action in debug list if debugging enabled
	 */
	public synchronized boolean postControlAction(Action action, AbstractService serviceHelper) {
		System.out.println("ENTRO IN POST CONTROL ACTION!");
		// logger.info("postControlAction: Entered");

		boolean status = action.postControlAction();

		// logger.info("postControlAction: got status back");

		UPnPStatus upnpStatus = null;

		if (status == false) {
			upnpStatus = action.getControlStatus();
			System.out.println("Action status for " + action.getName() + " = " + upnpStatus.getCode());
		}
		/*
		 * // If debug enabled, save action in debug object list if(
		 * DebugAction.getEnabled() ) { DebugAction debugAction = new
		 * DebugAction( action, serviceHelper ); if( status == false ) {
		 * debugAction.setStatus( DebugObj.STATUS_ERROR );
		 * debugAction.setErrorCode( upnpStatus.getCode() ); }
		 * parentController.addDebugObj( debugAction );
		 * 
		 * }
		 */
		if (status == true)
			logger.fine("postControlAction: " + action.getName() + " Success");
		else
			logger.warning("postControlAction: " + action.getName() + " Failure");

		return status;
	}

	/**
	 * Variant of above that doesn't add anything to the debug list (leaves it
	 * up to higher-level code). Useful when one wants to parse the action
	 * response and check for errors that are very action-specific
	 */
	public synchronized boolean postControlAction(Action action) {
		// logger.info("postControlAction: Entered");

		boolean status = action.postControlAction();

		// logger.info("postControlAction: got status back");

		UPnPStatus upnpStatus = null;

		if (status == false) {
			upnpStatus = action.getControlStatus();
			// System.out.println("Action status for " + action.getName() + " =
			// " +
			// upnpStatus.getCode() );
		}

		if (status == true)
			logger.fine("postControlAction: " + action.getName() + " Success");
		else
			logger.warning("postControlAction: " + action.getName() + " Failure");

		return status;
	}

	/**
	 * Utility routine to add object to controllers debug object list when a
	 * method is not supported by the device (no Action node exists)
	 */
	synchronized public void addUnsupportedActionDebugObj(String actionName) {
		/*
		 * DebugAction debugAction = new DebugAction( actionName );
		 * 
		 * debugAction.setStatus( UPnPStatus.INVALID_ACTION );
		 * 
		 * parentController.addDebugObj( debugAction );
		 */
	}

	/**
	 * Process MediaDevice preferences
	 */
	public void processPreferences() {
		pref = MediaController.getPreferences();

		String friendlyName = getFriendlyName().toLowerCase();
		String modelName = getModelName();
		if (modelName != null)
			modelName = modelName.toLowerCase();

		//
		// Check for match with 'known device' in property file. If there
		// is a match, use the icon info specified there instead of the
		// device's built-in UPnP icon (if one exists)
		//
		// The match first tries to use the 'modelName' property of the
		// device. Backup is 'friendlyName'
		//
		String iconFilename = null;
		iconName = null;

		// 'renderer' or 'server'
		String propFilePrefix = getDeviceTypeString().toLowerCase();

		for (int n = 0; n < MAX_DEVICES; n++) {
			String baseName = propFilePrefix + ".knownDevice" + n + ".";

			// First try using modelName
			String nameMatch = pref.get(baseName + "modelNameMatch");
			if (nameMatch != null)
				nameMatch = nameMatch.toLowerCase();

			if ((nameMatch != null) && (modelName != null) && modelName.indexOf(nameMatch) >= 0) {
				iconFilename = pref.get(baseName + "icon");
				iconName = pref.get(baseName + "iconName");
				break;
			}

			// then use friendlyNamea
			nameMatch = pref.get(baseName + "friendlyNameMatch");
			if (nameMatch == null)
				continue;

			nameMatch = nameMatch.toLowerCase();

			// TODO: may want to add device IPAddr matching logic here to handle
			// multiple-device case
			// String ipAddrMatch = props.getProperty( baseName + "ipAddrMatch"
			// );

			if (friendlyName.indexOf(nameMatch) >= 0) {
				iconFilename = pref.get(baseName + "icon");
				iconName = pref.get(baseName + "iconName");
				break;
			}
		}

		// If icon name was set in preferences file, still want to extract
		// host info if present and add as second line underneath label
		// Host info in most servers is inside parenthesis '(HOST)' somewhere
		// in the friendly name string. Windows Media Connect is an exception
		// in that it puts the host at the beginning of the friendly name,
		// followed by a ':'
		if (iconName != null) {
			String host = extractHostNameFromFriendlyName(friendlyName.trim());
			if (host != null)
				iconName = "<html><div style=\"text-align: center;\">" + iconName + "<br>" + host + "</div></html>";
			else
				iconName = "<html><div style=\"text-align: center;\">" + iconName + "<br><br>" + "</div></html>";
		}

		if (iconFilename == null) {
			// Try to retrieve device's UPnP icon, if it has one. Select
			// smallest
			// one
			org.cybergarage.upnp.Icon deviceIcon;
			org.cybergarage.upnp.Icon bestMatchDeviceIcon = null;
			org.cybergarage.upnp.IconList iconList = getDevice().getIconList();

			if (iconList.size() > 0)
				logger.fine("IconList for device:");

			int minHeight = 9999;
			for (int n = 0; n < iconList.size(); n++) {
				deviceIcon = iconList.getIcon(n);
				logger.fine(" Icon: " + deviceIcon.getURL());
				String heightString;
				if ((heightString = deviceIcon.getHeight()) != null) {
					int iconHeight = Integer.parseInt(heightString);
					if (iconHeight < minHeight) {
						bestMatchDeviceIcon = deviceIcon;
						minHeight = iconHeight;
					}
				}
			}
			if (bestMatchDeviceIcon != null) {
				// Found one - retrieve it
				try {
					String urlBase = getDevice().getResolvedURLBase();

					// If URLBase has a trailing '/', trim it off since
					// scpdURLStr
					// returned from above always has a leading '/' (TODO:
					// Rethink this)
					if (urlBase.endsWith("/"))
						urlBase = urlBase.substring(0, urlBase.length() - 1);

					String iconURLString = urlBase + bestMatchDeviceIcon.getURL();
					logger.fine("Loading icon " + iconURLString);
					URL iconURL = new URL(iconURLString);
					icon = new ImageIcon(iconURL);

					// If Icon height > 36, rescale it to smaller, 32-pixel
					// version
					if (icon.getIconHeight() > 36) {
						logger.fine("Rescaling icon of height " + icon.getIconHeight());
						float scaleFactor = (float) 32.0 / (float) icon.getIconHeight();
						int scaledHeight = (int) ((float) icon.getIconHeight() * scaleFactor);
						int scaledWidth = (int) ((float) icon.getIconWidth() * scaleFactor);

						ImageIcon scaledIcon = new ImageIcon(
								icon.getImage().getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_DEFAULT));
						icon = scaledIcon;
					}
				} catch (MalformedURLException e) {
					logger.warning("Device Icon URL not valid");
				}
			}
		} else {
			icon = MrUtil.createImageIcon(iconFilename);
		}

		if (icon == null) // still no icon - fall back to default
		{
			// TODO: Need default renderer icon
			icon = MrUtil.createImageIcon("avserver2.gif");
		}

		if (iconName == null) {
			// Default to use friendly name. If the text is longer
			// than 20 characters, it is truncated,
			iconName = getFriendlyName().trim();

			// Test - if host embedded in name as '(host)' it is stripped out
			// and put on a second line. Split to two lines at first space or
			// '(' symbol.
			String host = extractHostNameFromFriendlyName(iconName);
			if (host != null) {
				// System.out.println("iconName before strip = " + iconName );
				iconName = stripHostName(iconName);
				// System.out.println("iconName after strip = " + iconName );
			}

			if (iconName.length() > 20) {
				iconName = iconName.substring(0, 20);
			}

			if (host != null)
				iconName = "<html><div style=\"text-align: center;\">" + iconName + "<br>" + host + "</div></html>";
			else
				iconName = "<html><div style=\"text-align: center;\">" + iconName + "<br><br>" + "</div></html>";

		}
	}

	/**
	 * Strip out a host name with syntax '(host)' from a friendlyName
	 */
	public String stripHostName(String friendlyName) {
		int leftParenIndex = friendlyName.indexOf("(");
		int rightParenIndex = friendlyName.indexOf(")");

		if ((leftParenIndex < 0) || (rightParenIndex < 0)) {
			leftParenIndex = friendlyName.indexOf("[");
			rightParenIndex = friendlyName.indexOf("]");
		}

		if ((leftParenIndex == 0) && (rightParenIndex > leftParenIndex)) {
			return friendlyName.substring(rightParenIndex + 1);
		} else if ((leftParenIndex >= 0) && (rightParenIndex > leftParenIndex)
				&& ((rightParenIndex + 1) < friendlyName.length())) {
			return friendlyName.substring(0, leftParenIndex) + friendlyName.substring(rightParenIndex + 1);
		} else if ((leftParenIndex >= 0) && (rightParenIndex > leftParenIndex)
				&& ((rightParenIndex + 1) == friendlyName.length())) {
			return friendlyName.substring(0, leftParenIndex);
		} else {
			return friendlyName;
		}
	}

	public String extractHostNameFromFriendlyName(String friendlyName) {
		int index;
		String host = null;

		// System.out.println("Extracting host from '" + friendlyName + "'" );

		// WMC uses 'HOST:Windows Media Connect"
		if (friendlyName.toLowerCase().indexOf("windows media connect") >= 0) {
			if ((index = friendlyName.indexOf(":")) > 0)
				return "(" + friendlyName.substring(0, index) + ")";
		}

		// Many devices use hostname in regular paren as part of friendly name
		int leftParenIndex = friendlyName.indexOf("(");
		int rightParenIndex = friendlyName.indexOf(")");
		if ((leftParenIndex >= 0) && (rightParenIndex > leftParenIndex))
			return friendlyName.substring(leftParenIndex, rightParenIndex + 1);

		// Twonkyvision mediaserver uses brackets - convert it to paren
		leftParenIndex = friendlyName.indexOf("[");
		rightParenIndex = friendlyName.indexOf("]");
		if ((leftParenIndex >= 0) && (rightParenIndex > leftParenIndex))
			return "(" + friendlyName.substring(leftParenIndex + 1, rightParenIndex) + ")";

		// System.out.println("Host is '" + host + "'" );

		return null;
	}

	public JButton createButton() {
		JButton button = createButton(iconName, icon);
		button.addActionListener(this);
		return button;
	}

	public JButton createButton(String iconName, ImageIcon icon) {
		button = new JButton(iconName, icon);
		button.setVerticalAlignment(SwingConstants.BOTTOM);
		button.setVerticalTextPosition(SwingConstants.BOTTOM);
		button.setHorizontalTextPosition(SwingConstants.CENTER);
		button.setBorderPainted(false);
		button.setFocusPainted(false);
		button.setContentAreaFilled(false);

		// Set button tooltip to full friendly name
		button.setToolTipText(getFriendlyName().trim());

		// Dimension prefSize = button.getPreferredSize();
		// button.setPreferredSize( new Dimension( (int)prefSize.getWidth(),100)
		// );
		// button.setMinimumSize( new Dimension(32,80) );

		EmptyBorder emptyBorder = (EmptyBorder) BorderFactory.createEmptyBorder(2, 4, 0, 4);
		// BorderFactory.createEmptyBorder(2,4,2,4);
		button.setBorder(emptyBorder);
		this.icon = icon;

		return button;
	}

}
