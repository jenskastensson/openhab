/**
 * Copyright (c) 2010-2014, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.proserv.internal;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import org.openhab.core.events.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * With the proServ connector the internal state of a proServ can be read.
 * 
 * @author JEKA
 * @since 1.0.0
 */
public class ProservConnector {

	static final Logger logger = LoggerFactory
			.getLogger(ProservConnector.class);

	private DataInputStream datain = null;
	private DataOutputStream dataout = null;
	private static String serverIp = "";
	private static int serverPort = 80;
	protected static MonitorThread monitorThread = null;

	private Socket sock;

	public ProservConnector(String serverIp, int serverPort) {
		ProservConnector.serverIp = serverIp;
		ProservConnector.serverPort = serverPort;
	}

	/**
	 * connects to the proServ via network
	 * 
	 * @throws UnknownHostException
	 *             indicate that the IP address of a host could not be
	 *             determined.
	 * @throws IOException
	 *             indicate that no data can be read from the proServ
	 */
	public void connect() throws UnknownHostException, IOException {
		try {
			sock = new Socket(serverIp, serverPort);
			sock.setSoTimeout(5000);
			InputStream in = sock.getInputStream();
			OutputStream out = sock.getOutputStream();
			datain = new DataInputStream(in);
			dataout = new DataOutputStream(out);
			logger.debug("ProservConnector connect() completed");
		} catch (IOException e) {
			logger.warn("ProservConnector connect() exception: {}",e);
			throw e;
		} finally {
		}	
	}

	/**
	 * read the internal state of the proServ
	 * 
	 * @return a array with all internal data of the proServ
	 * @throws IOException
	 *             indicate that no data can be read from the proServ
	 */
	public byte[] getParameterBytes(short startByte, short numberOfBytes) { 

		byte[] parameterBytes = null;
		try {

			while (datain.available() > 0)
				datain.readByte();

			// QUICK FIX: send a "start byte" to avoid first real byte to be
			// transmitted alone, to be investigated!
			dataout.writeByte(0x00);
			dataout.flush();

			// Connection header
			byte structureLength = (byte) 0x04;
			byte reserved = (byte) 0x00;
			// KNXnet/IP header
			byte headerSize = (byte) 0x06;
			byte version = (byte) 0x20;
			short objectServerRequest = (short) 0xf080;
			short frameSize = (short) (headerSize + structureLength + 6); // 6 =length of object server message

			// KNXnet/IP header
			dataout.writeByte(headerSize);
			dataout.writeByte(version);
			dataout.writeShort(objectServerRequest);
			dataout.writeShort(frameSize);
			// Connection header
			dataout.writeByte(structureLength);
			dataout.writeByte(reserved);
			dataout.writeByte(reserved);
			dataout.writeByte(reserved);

			// Object Server message
			byte mainService = (byte) 0xF0;
			byte subService = (byte) 0x07;
			dataout.writeByte(mainService);
			dataout.writeByte(subService);
			dataout.writeShort(startByte);
			dataout.writeShort(numberOfBytes);

			dataout.flush();

			// read past the KNXnet/IP header and Connection header
			datain.skipBytes(10);

			byte mainServiceResp = datain.readByte();
			byte subServiceResp = datain.readByte();
			short startByteResp = datain.readShort();
			short numberOfBytesResp = datain.readShort();
			if (mainServiceResp != mainService) {
				logger.debug("wrong mainServiceResp");
				return null;
			}
			if (subServiceResp != (byte) 0x87) {
				logger.debug("wrong subService");
				return null;
			}
			if (startByteResp != startByte || numberOfBytesResp == 0) {
				Byte error = datain.readByte();
				logger.debug("startByteResp == 0 , error=" + error.toString());
				return null;
			}

			if (numberOfBytes != numberOfBytesResp)
				numberOfBytes = numberOfBytesResp;
			parameterBytes = new byte[numberOfBytes];
			for (int i = 0; i < numberOfBytes; i++) {
				parameterBytes[i] = datain.readByte();
			}
		} catch (IOException e) {
			logger.debug("IOException in getParameterBytes: " + e.toString());
			return null;
		}
		return parameterBytes;
	}

	
	public byte[] getDataPointValue(short startDataPoint, short numberOfDatapoints) {

		byte[] dataPointValue = new byte[255];
		try {

			while (datain.available() > 0)
				datain.readByte();

			// QUICK FIX: send a "start byte" to avoid first real byte to be
			// transmitted alone, to be investigated!
			dataout.writeByte(0x00);
			dataout.flush();

			// Connection header
			byte structureLength = (byte) 0x04;
			byte reserved = (byte) 0x00;
			// KNXnet/IP header
			byte headerSize = (byte) 0x06;
			byte version = (byte) 0x20;
			short objectServerRequest = (short) 0xf080;
			short frameSize = (short) (headerSize + structureLength + 7); // 7 = length of object server message

			// KNXnet/IP header
			dataout.writeByte(headerSize);
			dataout.writeByte(version);
			dataout.writeShort(objectServerRequest);
			dataout.writeShort(frameSize);
			// Connection header
			dataout.writeByte(structureLength);
			dataout.writeByte(reserved);
			dataout.writeByte(reserved);
			dataout.writeByte(reserved);

			// Object Server message
			byte mainService = (byte) 0xF0;
			byte subService = (byte) 0x05;
			byte filter = (byte) 0x00;
			dataout.writeByte(mainService);
			dataout.writeByte(subService);
			dataout.writeShort(startDataPoint);
			dataout.writeShort(numberOfDatapoints);
			dataout.writeByte(filter);

			dataout.flush();

			// read past the KNXnet/IP header and Connection header
			datain.skipBytes(10);

			byte mainServiceResp = datain.readByte();
			byte subServiceResp = datain.readByte();
			short startDataPointResp = datain.readShort();
			short numberOfDatapointsResp = datain.readShort();
			if (mainServiceResp != mainService) {
				logger.debug("wrong mainServiceResp");				
				return null;
			}
			if (subServiceResp != (byte) 0x85) {
				logger.debug("wrong subServiceResp");
				return null;
			}
			if (startDataPointResp != startDataPoint || numberOfDatapointsResp == 0) {
				Byte error = datain.readByte();
				if(error != 2) // 2->object doesn't exist
				{
					logger.debug("startByteResp == 0 , error=" + error.toString());
				}
				return null;
			}

			int pos = 0;
			for (int i = 0; i < numberOfDatapointsResp; i++) {
				@SuppressWarnings("unused")
				short dataPointID = datain.readShort();
				@SuppressWarnings("unused")
				byte dataPointState = datain.readByte();
				byte dataPointLength = datain.readByte();
				for (int n = 0; n < dataPointLength; n++) {
					dataPointValue[pos++] = datain.readByte();
				}
			}
		} catch (IOException e) {
			logger.debug("IOException in getDataPointValue: " + e.toString());
			return null;
		}
		return dataPointValue;
	}

	
	/**
	 * disconnect from proServ
	 */
	public void disconnect() {
		try {
			datain.close();
			dataout.close();
		} catch (IOException e) {
			logger.error("can't close datain/out error:{}", e);
		} catch (Exception e) {
			logger.error("exception in disconnect error:{}", e);
		}
	}


	public void startMonitor(EventPublisher eventPublisher, ProservData proservData, ProservBinding proservBinding) {
		monitorThread = new MonitorThread(eventPublisher, proservData, proservBinding);
		monitorThread.start();		
	}	
	
	public void stopMonitor() {
		if (monitorThread != null) {
			monitorThread.interrupt();
		}
		monitorThread = null;		
	}	
	
	private static class MonitorThread extends Thread {
	
	
		/** flag to notify the thread to terminate */
		private boolean interrupted = false;
	
		/** retry interval in ms, if connection fails */
		private long waitBeforeRetry = 60000L;
	
		private ProservData proservData;
		private ProservBinding proservBinding;
		
		public MonitorThread(EventPublisher eventPublisher, ProservData proservData,  ProservBinding proservBinding) {
			this.proservData = proservData;
			this.proservBinding = proservBinding;
		}

	
		/**
		 * Notifies the thread to terminate itself. The current connection will
		 * be closed.
		 */
		public void interrupt() {
			this.interrupted = true;
		}
	

		@Override
		public void run() {
			boolean connectionEstablished = false;
			while (!interrupted) {
				while (serverIp == null) {
					// if we don't have an IP, let's wait
					try {
						sleep(1000L);
					} catch (InterruptedException e) {
						interrupted = true;
						break;
					}
				}
				if (serverIp != null) {
					DataInputStream datain = null;
					try {
						Socket connectionForTest = new Socket(serverIp, serverPort);
						connectionEstablished = true;
						connectionForTest.close();
						// reset the retry interval
						waitBeforeRetry = 20000L;
					} catch (Exception e) {
						logger.error("----Monitor Could not connect to Proserv on {}: {}",
								serverIp + ":" + serverPort, e.toString());
						logger.info("----Monitor Retrying connection to Proserv in {} s.",
								waitBeforeRetry / 1000L);
						try {
							Thread.sleep(waitBeforeRetry);
						} catch (InterruptedException ex) {
							interrupted = true;
						}
						// wait more 
						waitBeforeRetry += 20000L;
					}
					if (connectionEstablished == true) {
						
						while (!interrupted) {
							try {
								Socket connection = new Socket(serverIp, serverPort);
								connection.setSoTimeout(5000);
								InputStream in = connection.getInputStream();
								datain = new DataInputStream(in);								
								int nofBytesToRead = 0;
								try {
									// read past the KNXnet/IP header and Connection header
									//logger.debug("----Monitor before skipBytes ");
									nofBytesToRead = datain.skipBytes(10);
									//logger.debug("----Monitor after skipBytes ");
								} catch (IOException e) {
									while (datain.available() > 0)
										datain.readByte();									
								}								
								if(nofBytesToRead > 0)
								{
									byte mainService = datain.readByte();
									byte subService = datain.readByte();
									@SuppressWarnings("unused")
									short startDataPoint = datain.readShort();
									short numberOfDatapoints = datain.readShort();
									if (mainService == (byte) 0xF0 &&  subService == (byte) 0xC1) {
										short[] dataPointIDs = new short[16];
										@SuppressWarnings("unused")
										short nextDataPointID = 0;
										byte[] dataPointValue = new byte[255];
										int pos = 0;
										for (int i = 0; i < numberOfDatapoints; i++) {
											dataPointIDs[i] = datain.readShort();
											@SuppressWarnings("unused")
											byte dataPointState = datain.readByte();
											byte dataPointLength = datain.readByte();
											if(i>0){
												pos += (dataPointIDs[i] - dataPointIDs[i-1] - 1 ) * dataPointLength;
											}
											for (int n = 0; n < dataPointLength; n++) {
												dataPointValue[pos++] = datain.readByte();
											}
										}
										boolean bFound = false;
										for (int x = 0; x < 18; x++) {
											for (int y = 0; y < 16; y++) {
												for (int z = 0; z < 2; z++) {
													if (proservData.getFunctionDataPoint(x, y, z) == dataPointIDs[0]) {
														proservBinding.postUpdateFunction(x, y, dataPointValue);
														bFound = true;
														//logger.debug("----Monitor New value DP:{} x:{} y:{} z:{} value:{}", dataPointIDs[0], x, y, z, dataPointValue.toString());															
													}
												}
											}
										}
										if(!bFound){
											for (int x = 0; x < 18; x++) {
												for (int z = 0; z < 2; z++) {
													if (proservData.getHeatingDataPoint(x, z) == dataPointIDs[0]) {
														proservBinding.postUpdateHeating(x, dataPointValue);
														bFound = true;
														//logger.debug("----Monitor New value DP:{} x:{} y:{} z:{} value:{}", dataPointIDs[0], x, y, z, dataPointValue.toString());															
													}
												}
											}											
										}
									}
									else
									{
										while (datain.available() > 0)
											datain.readByte();									
									}
								}
								connection.close();

							} catch (IOException e) {
								logger.warn(
										"----Monitor Lost connection to Proserv on {}: {}",
										serverIp + ":" + serverPort, e.getMessage());
								break;
							}
						}
					}
				}
			}
		}
	}
}