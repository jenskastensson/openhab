package org.openhab.binding.proserv.internal;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ProservDiscovery {
	
    private final static String INET_ADDR = "224.0.23.12";
    private final static int PORT = 3671;
	static final Logger logger = LoggerFactory
			.getLogger(ProservConnector.class);

    private static String proservIP = "";
    
	public static String getProservIP() {
		return proservIP;
	}
	public static boolean search() {
			      	
    	boolean bRet = false;
    	proservIP = "";
    	
		try {
			if (ProservXConnect.getValidMacIdAndIP4Address()) {
				InetAddress ipLocal = InetAddress.getByName(ProservXConnect.getLocalIp());
				byte[] bytesIP = ipLocal.getAddress();		
		    	byte[] outBuf = new byte[14];
		    	outBuf[0] = 0x06; // header
		    	outBuf[1] = 0x10; // version
		    	outBuf[2] = 0x02; // search request
		    	outBuf[3] = 0x01; //  -- " --
		    	outBuf[4] = 0x00; // packet length
		    	outBuf[5] = 0x0e; //  -- " --
		    	outBuf[6] = 0x08; // structure length
		    	outBuf[7] = 0x01; // protocol code
		    	outBuf[8] = (byte) (bytesIP[0] & 0xFF); // our IP X.0.0.0
		    	outBuf[9] = (byte) (bytesIP[1] & 0xFF); // our IP 0.X.0.0
		    	outBuf[10] = (byte) (bytesIP[2] & 0xFF); // our IP 0.0.X.0
		    	outBuf[11] = (byte) (bytesIP[3] & 0xFF); // our IP 0.0.0.X
//outBuf[8] = (byte) (0xC0); // our IP X.0.0.0
//outBuf[9] = (byte) (0xA8); // our IP 0.X.0.0
//outBuf[10] = (byte) (0x01); // our IP 0.0.X.0
//outBuf[11] = (byte) (0x0D); // our IP 0.0.0.X
		    	outBuf[12] = (byte) 0x0e; // port 3671 byte+0
		    	outBuf[13] = (byte) 0x57; // port 3671 byte+1
	    	
		        byte[] inBuf = new byte[1024];	        
				InetAddress group = InetAddress.getByName(INET_ADDR);		        
		        MulticastSocket socketIn = new MulticastSocket(PORT);
		        socketIn.setSoTimeout(3000);
		        
		        DatagramSocket socketOut = new DatagramSocket();
	        	DatagramPacket dgramOut = new DatagramPacket(outBuf, outBuf.length, group, PORT);
	        	socketOut.send(dgramOut);
	        	socketOut.close();
	        	
		        socketIn.joinGroup(group);
	        	
	        	int dgramInLength = 0;
	            while (true) {
	                DatagramPacket dgramIn = new DatagramPacket(inBuf, inBuf.length);
	                socketIn.receive(dgramIn);
	                dgramInLength = dgramIn.getLength();
	                break;
	            }
	            socketIn.leaveGroup(group);
	            socketIn.close();
	            
	            byte headerSize = inBuf[0];
	            byte  version = inBuf[1];
	            short searchResp = byte2short(inBuf[2],inBuf[3]);
	            short packetLength = byte2short(inBuf[4],inBuf[5]);
	            byte  hpaiLength = inBuf[6];
	            short port = byte2short(inBuf[12],inBuf[13]);
	            //byte devDibLen = inBuf[14];
	            // and more ...
            	logger.debug("proServ discovery response: dgramInLength: {} headerSize:{} version:{}  searchResp:{}  packetLength:{}  hpaiLength:{}  port:{}",
            			dgramInLength, headerSize, version, searchResp, packetLength, hpaiLength, port);
	            
	            if( headerSize==0x6 && version==0x10 && searchResp==0x0202 /*&& packetLength==0x0054*/ && hpaiLength==0x08 ){	            	
		            byte[] ipRemote = {inBuf[8], inBuf[9], inBuf[10], inBuf[11]};
		            InetAddress addrRemote = InetAddress.getByAddress(ipRemote);
		            if (ProservXConnect.validateIPv4(addrRemote.getHostAddress())){
		            	proservIP = addrRemote.getHostAddress();
		            	logger.info("proServ discovery found: {}",proservIP);
		            	bRet = true;	            	
		            }
	            }
			}
	        } catch (IOException ex) {
	        	logger.warn("proServ discovery search Exception: {}", ex.toString());
		}
        return bRet;
    }
    private static short byte2short(final byte left, final byte right) {
    	return (short) ((left & 0xff) << 8 | right & 0xff);
    }    
}
