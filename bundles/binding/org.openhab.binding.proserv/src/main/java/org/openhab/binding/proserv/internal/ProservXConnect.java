package org.openhab.binding.proserv.internal;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.Socket;
import java.net.SocketException;
import java.net.URL;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.net.ssl.HttpsURLConnection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProservXConnect {

	private static final Logger logger = LoggerFactory.getLogger(ProservBinding.class);
	
	private String findMacidFromIp(String localIp) throws SocketException {
		String MacID = "";
		for (NetworkInterface network : IterableEnumeration.make(NetworkInterface.getNetworkInterfaces())) {
			byte[] mac = network.getHardwareAddress();
			if (mac != null) {
				StringBuilder sb = new StringBuilder();
				for (int i = 0; i < mac.length; i++) {
					sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
				}
				logger.debug("Examine MAC address : {}", sb.toString());

				// Bound InetAddress for interface
				for (InetAddress address : IterableEnumeration.make(network.getInetAddresses())) {
					logger.debug("address.getHostAddress(): {} compare to localIp: {}", address.getHostAddress(), localIp);
					if (address.getHostAddress().equals(localIp)) {
						MacID = sb.toString();
						if (MacID.isEmpty()) {
							logger.debug("Found IP: {} match but MacID is empty", localIp);
						} else {
							logger.debug("Found MacID bound to local IP: {} MacID: {}", localIp, sb.toString());
						}
					}
				}
			}
		}
		return MacID;
	}

	private String getLocalIPAddress() {
		try {
			for (Enumeration<NetworkInterface> mEnumeration = NetworkInterface.getNetworkInterfaces(); mEnumeration.hasMoreElements();) {
				NetworkInterface intf = (NetworkInterface) mEnumeration.nextElement();
				for (Enumeration<InetAddress> enumIPAddr = intf.getInetAddresses(); enumIPAddr.hasMoreElements();) {
					InetAddress inetAddress = (InetAddress) enumIPAddr.nextElement();
					if (!inetAddress.isLoopbackAddress()) {
						return inetAddress.getHostAddress().toString();
					}
				}
			}
		} catch (SocketException ex) {
			logger.debug("Error", ex.toString());
		}
		return "";
	}

	private boolean bDoneProservConnectServer = false;
	private java.sql.Timestamp timestampProservConnectServer;

	public void handleProservConnectServer() {
		if (!bDoneProservConnectServer) {
			bDoneProservConnectServer = doProservConnectServer();
			if (bDoneProservConnectServer) {
				timestampProservConnectServer = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
			}
		} else {
			java.sql.Timestamp timestampNow = new java.sql.Timestamp(Calendar.getInstance().getTime().getTime());
			long diff = timestampNow.getTime() - timestampProservConnectServer.getTime();
			if(diff > 1000*60*60){ // re-send every ~hour
			//if (diff > 1000 * 60) {
				bDoneProservConnectServer = false; // will force a new call next time
			}
		}
	}

	private boolean doProservConnectServer() {
		boolean bRet = false;
		boolean bFoundGIP = false;
		boolean bFoundLIP = false;
		boolean bFoundMAC = false;
		String publicIp = "";
		String localIp = "";
		String MacID = "";
		String hostName = "";
		try {
			// Get public IP
			URL whatismyip = new URL("http://checkip.amazonaws.com");
			BufferedReader in = null;
			in = new BufferedReader(new InputStreamReader(whatismyip.openStream()));
			publicIp = in.readLine();
			if (validateIPv4(publicIp)) {
				bFoundGIP = true;
				logger.debug("Current public IP address : {}", publicIp);
			} else {
				logger.error("Invalid public IP address : {}", publicIp);
			}
			
			// Get the hostname
			hostName = InetAddress.getLocalHost().getHostName();

			// Get local IP and MacID Method 1
			// PC : OK
			// PC & VPN: OK
			// DS : nok (wrong local IP v6?)
			// DS & VPN: nok (wrong local ip vpn server plus wrong macid)
			localIp = getLocalIPAddress();
			logger.debug("Method1: getLocalIPAddress returns local IP : {}", localIp);
			if (validateIPv4(localIp)) {
				bFoundLIP = true;
				MacID = findMacidFromIp(localIp);
				if (!MacID.isEmpty()) {
					bFoundMAC = true;
					logger.debug("Method 1: found local IP : {}, MacID : {}", localIp, MacID);
				}
			} else {
				logger.debug("Invalid local IP address : {}", localIp);
			}
			logger.debug("Method 1 summary : MacID: {}, publicIp: {}, localIp: {}", MacID, publicIp, localIp);

			// Get local IP and MacID Method 2
			// PC : OK
			// PC & VPN: nok (wrong local ip vpn server & empty macid)
			// DS : OK
			// DS & VPN: nok (wrong local ip vpn server & invalid macid)
			if (MacID.isEmpty()) {
				// force a connection to get the real local IP
				Socket s = new Socket("www.google.com", 80);
				localIp = s.getLocalAddress().getHostAddress();
				s.close();
				logger.debug("Method 2: local IP address : {}", localIp);
				if (validateIPv4(localIp)) {
					bFoundLIP = true;
					logger.debug("Alternative method found local IP address : {}", localIp);
					MacID = findMacidFromIp(localIp);
					if (!MacID.isEmpty()) {
						bFoundMAC = true;
						logger.debug("Method 2: found local IP : {}, MacID : {}", localIp, MacID);
					}
				} else {
					logger.debug("Invalid local IP address : {}", localIp);
				}
				logger.debug("Method 2 summary : MacID: {}, publicIp: {}, localIp: {}", MacID, publicIp, localIp);
			}

			// Get local IP and MacID Method 3
			// PC : nok
			// PC & VPN: nok (wrong local v6?)
			// DS : OK
			// DS & VPN: OK
			if (MacID.isEmpty()) {
				InetAddress addrs[] = InetAddress.getAllByName(hostName);
				for (InetAddress a : addrs) {
					localIp = a.getHostAddress();
					logger.debug("Method 3: local IP address : {}", localIp);
					if (validateIPv4(localIp)) {
						MacID = findMacidFromIp(localIp);
						if (!MacID.isEmpty()) {
							bFoundMAC = true;
							logger.debug("Method 3: found local IP : {}, MacID : {}", localIp, MacID);
						}
					} else {
						logger.debug("Invalid local IP address : {}", localIp);
					}
				}
				logger.debug("Method 3 summary : MacID: {}, publicIp: {}, localIp: {}, hostName:{}", MacID, publicIp, localIp, hostName);
			}

		} catch (IOException e) {
			logger.debug("IOException when looking for IP and MacID: Error:{}", e.toString());
		}

		if (bFoundGIP && bFoundLIP && bFoundMAC) {
			logger.debug("Send HTTP to proSevConnectServer");
			try {
				logger.info("Sending to ProservConnectServer: MacID: {}, publicIp: {}, localIp: {}, hostName: {}", MacID, publicIp, localIp, hostName);
				bRet = sendToProservConnectServer(MacID, publicIp, localIp, hostName);
			} catch (Exception e) {
				logger.error("sendToProservConnectServer exception: {}", e);
			}

		} else {
			logger.error("Error: Did not find all of GlobalIP:{} LocalIP:{} MacID:{}", bFoundGIP, bFoundLIP, bFoundMAC);
		}
		return bRet;
	}

	private boolean sendToProservConnectServer(String MAC, String PIP, String LIP, String hostName) throws Exception {
		String url = "https://script.google.com/macros/s/AKfycbwz0tCIHC_d59jV0uiuXDABPX48e0lYGyDjifc7-9O6ATbEh8dB/exec?method=set";
		url += "&mac=" + MAC + "&pip=" + PIP + "&lip=" + LIP + "&host=" + hostName;
		// mac=01:23:45:67:89:ab&pip=33.44.55.66&lip=192.168.1.1
		URL obj;

		obj = new URL(url);
		HttpsURLConnection con = (HttpsURLConnection) obj.openConnection();
		con.setRequestMethod("GET");
		int responseCode = con.getResponseCode();

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine;
		StringBuffer response = new StringBuffer();

		while ((inputLine = in.readLine()) != null) {
			response.append(inputLine);
		}
		in.close();

		// Typical response: Method=set Mac=00-11-32-27-F8-9F
		// publicIP=88.182.160.155 localIP=192.168.1.22
		String r = response.toString();
		if (r.length() > 200) 
			r = r.substring(0, 200);
		logger.debug("Response (code:{}) from ProservConnectServer: {}", responseCode, r);
		if (response.length() > 0 && response.length() < 100 
				&& response.toString().toLowerCase().indexOf("method=set") != -1
				&& response.toString().toLowerCase().indexOf(MAC.toLowerCase()) != -1
				&& response.toString().toLowerCase().indexOf(PIP.toLowerCase()) != -1
				&& response.toString().toLowerCase().indexOf(LIP.toLowerCase()) != -1)
			return true;
		return false;
	}

	private static final String PATTERN_IPv4 = "^(([01]?\\d\\d?|2[0-4]\\d|25[0-5])\\.){3}([01]?\\d\\d?|2[0-4]\\d|25[0-5])$";

	Date dateProservConnectServer;

	public static boolean validateIPv4(final String ip) {

		Pattern pattern = Pattern.compile(PATTERN_IPv4);
		Matcher matcher = pattern.matcher(ip);
		return matcher.matches();
	}

}
