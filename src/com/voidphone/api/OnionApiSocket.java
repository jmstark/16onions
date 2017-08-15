/*
 * Copyright (c) 2017, Charlie Groh and Josef Stark. All rights reserved.
 * 
 * This file is part of 16onions.
 *
 * 16onions is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * 16onions is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with 16onions.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousChannelGroup;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Random;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.voidphone.general.General;
import com.voidphone.general.IllegalIDException;
import com.voidphone.general.NoRpsPeerException;
import com.voidphone.general.SizeLimitExceededException;
import com.voidphone.general.TunnelCrashException;
import com.voidphone.onion.Main;
import com.voidphone.onion.OnionBaseSocket;
import com.voidphone.onion.OnionConnectingSocket;

import onion.api.OnionCoverMessage;
import onion.api.OnionErrorMessage;
import onion.api.OnionTunnelBuildMessage;
import onion.api.OnionTunnelDataMessage;
import onion.api.OnionTunnelDestroyMessage;
import onion.api.OnionTunnelIncomingMessage;
import onion.api.OnionTunnelReadyMessage;
import protocol.MessageParserException;
import protocol.MessageSizeExceededException;
import protocol.Protocol;
import protocol.ProtocolException;
import protocol.Protocol.MessageType;

public class OnionApiSocket extends ApiSocket {
	protected Config config;
	protected InetSocketAddress tunnelDestination;
	protected byte[] destinationHostkey;
	private HashMap<Integer, OnionBaseSocket> activeTunnels;
	private HashMap<Short, OnionConnectingSocket> detachedTunnels;
	private OnionConnectingSocket randomTunnel = null;
	private boolean roundHasTunnel = false;
	private final ReentrantReadWriteLock lock;
	private final HashMap<Integer, Void> map;
	private final Random random;

	/**
	 * Listens for a new Onion API connection.
	 * 
	 * @param port
	 *            the port to listen on
	 * @throws IOException
	 *             if there is an I/O-error
	 */
	public OnionApiSocket(final InetSocketAddress addr, AsynchronousChannelGroup group) throws IOException {
		super(addr, group, true);
		random = new Random();
		map = new HashMap<Integer, Void>();
		activeTunnels = new HashMap<Integer, OnionBaseSocket>();
		detachedTunnels = new HashMap<Short, OnionConnectingSocket>();
		lock = new ReentrantReadWriteLock(true);

		/*
		 * Main.getScheduler().scheduleInNextRound(new Runnable() { public void run() {
		 * try { General.info("New round commenced!"); if (randomTunnel != null) {
		 * randomTunnel.requestDestruction(); randomTunnel = null; } if
		 * (!roundHasTunnel) { General.info("building random tunnel");
		 * ONIONTUNNELBUILD(null); } } catch (Exception e) { General.
		 * warning("Error in random tunnel - new one at next round if neccessary");
		 * General.fatalException(e); }
		 * 
		 * } });
		 */
	}

	/**
	 * Adds a tunnel to the map of currently active tunnels
	 * 
	 * @param tun
	 */
	public void addActiveTunnel(OnionBaseSocket tun) {
		synchronized (activeTunnels) {
			activeTunnels.put(tun.getOnionApiId(), tun);
		}
	}

	/**
	 * Removes a tunnel from the map of currently active tunnels
	 * 
	 * @param tun
	 */
	public void removeActiveTunnel(OnionBaseSocket tun) {
		synchronized (activeTunnels) {
			activeTunnels.remove(tun.getOnionApiId());
		}
	}

	public OnionBaseSocket getActiveTunnelById(int id) {
		synchronized (activeTunnels) {
			return activeTunnels.get(id);
		}
	}

	public OnionBaseSocket getAndRemoveActiveTunnelById(int id) {
		synchronized (activeTunnels) {
			OnionBaseSocket result = activeTunnels.get(id);
			activeTunnels.remove(id);
			return result;
		}
	}

	/**
	 * Adds a tunnel to the map of currently detached tunnels
	 * 
	 * @param tun
	 */
	public void addDetachedTunnel(OnionConnectingSocket tun) {
		synchronized (detachedTunnels) {
			detachedTunnels.put(tun.getNextHopMId(), tun);
		}
	}

	/**
	 * Removes a tunnel from the map of currently detached tunnels
	 * 
	 * @param tun
	 */
	public void removeDetachedTunnel(OnionConnectingSocket tun) {
		synchronized (detachedTunnels) {
			detachedTunnels.remove(tun.getNextHopMId());
		}
	}

	public OnionConnectingSocket getDetachedTunnelById(short id) {
		synchronized (detachedTunnels) {
			return detachedTunnels.get(id);
		}
	}

	public synchronized OnionConnectingSocket getAndRemoveDetachedTunnelById(short id) {
		synchronized (detachedTunnels) {
			OnionConnectingSocket result = detachedTunnels.get(id);
			detachedTunnels.remove(id);
			return result;
		}
	}

	/**
	 * Returns a OnionTunnelReadyMessage.
	 * 
	 * @param id
	 *            the ID of the connection
	 * @param hostkey
	 *            the Hostkey
	 * @return the OnionTunnelReadyMessage
	 * @throws IllegalIDException
	 *             if the ID is not registered
	 */
	public OnionTunnelReadyMessage newOnionTunnelReadyMessage(int id, byte hostkey[]) throws IllegalIDException {
		isRegistered(id);
		try {
			return new OnionTunnelReadyMessage((long) id, hostkey);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	/**
	 * Returns a OnionTunnelIncomingMessage.
	 * 
	 * @param id
	 *            the ID of the connection
	 * @return the OnionTunnelIncomingMessage
	 * @throws IllegalIDException
	 *             if the ID is not registered
	 */
	public OnionTunnelIncomingMessage newOnionTunnelIncomingMessage(int id) throws IllegalIDException {
		isRegistered(id);
		return new OnionTunnelIncomingMessage((long) id);
	}

	/**
	 * Returns a OnionTunnelDataMessage.
	 * 
	 * @param id
	 *            the ID of the connection
	 * @param data
	 *            the data
	 * @return the OnionTunnelDataMessage
	 * @throws IllegalIDException
	 *             if the ID is not registered
	 */
	public OnionTunnelDataMessage newOnionTunnelDataMessage(int id, byte data[]) throws IllegalIDException {
		isRegistered(id);
		try {
			return new OnionTunnelDataMessage((long) id, data);
		} catch (MessageSizeExceededException e) {
			General.fatalException(e);
			return null;
		}
	}

	/**
	 * Returns a OnionErrorMessage.
	 * 
	 * @param id
	 *            the ID of the connection
	 * @param requestType
	 *            the requestType
	 * @return the OnionErrorMessage
	 * @throws IllegalIDException
	 *             if the ID is not registered
	 */
	public OnionErrorMessage newOnionErrorMessage(int id, Protocol.MessageType requestType) throws IllegalIDException {
		isRegistered(id);
		return new OnionErrorMessage(requestType, (long) id);
	}

	/**
	 * Sent from CM/UI to Onion to build an onion tunnel.
	 * 
	 * @param otbm
	 *            the OnionTunnelBuildMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONTUNNELBUILD(OnionTunnelBuildMessage otbm) {
		tunnelDestination = otbm.getAddress();
		destinationHostkey = otbm.getEncoding();
		final MessageType messageType = otbm.getType();

		Main.getScheduler().scheduleInNextRound(new Runnable() {
			public void run() {
				OnionConnectingSocket currentConnectingTunnel = null;
				int onionApiId;
				try {
					onionApiId = Main.getOas().register();
				} catch (SizeLimitExceededException e) {
					General.error(e.getMessage());
					return;
				}
				try {
					// build the tunnel
					currentConnectingTunnel = new OnionConnectingSocket(Main.getMultiplexer(), tunnelDestination,
							destinationHostkey, onionApiId);
					currentConnectingTunnel.beginAuthentication(null, 0);
					currentConnectingTunnel.detachId();
					addDetachedTunnel(currentConnectingTunnel);
					roundHasTunnel = true;
					return;
				} catch (SizeLimitExceededException e) {
					General.error(e.getMessage());
				} catch (InterruptedException e) {
					General.fatalException(e);
				} catch (TunnelCrashException | NoRpsPeerException e) {
					General.error("Tunnel establishing failed!");
				}
				try {
					Main.getOas().ONIONERROR(newOnionErrorMessage(onionApiId, messageType));
				} catch (IllegalIDException e) {
					General.fatalException(e);
				}
				try {
					Main.getOas().unregister(onionApiId);
				} catch (IllegalIDException e) {
					General.fatalException(e);
				}
			}

		});
	}

	/**
	 * Sent from Onion to CM/UI to confirm that the previously requested tunnel is
	 * ready.
	 * 
	 * @param otrm
	 *            the OnionTunnelReadyMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONTUNNELREADY(OnionTunnelReadyMessage otrm) {
		connection.sendMsg(otrm);
	}

	/**
	 * Sent from CM/UI to Onion to destroy an onion tunnel.
	 * 
	 * @param otdm
	 *            the OnionTunnelDestroyMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONTUNNELDESTROY(OnionTunnelDestroyMessage otdm) {

		OnionBaseSocket tun = getAndRemoveActiveTunnelById((int) otdm.getId());
		if (tun == null) {
			General.warning("Cannot destroy tunnel - ID not found");
		} else {
			tun.requestDestruction();
		}

	}

	/**
	 * Sent from Onion to CM/UI to signal an incoming onion tunnel.
	 * 
	 * @param otim
	 *            the OnionTunnelIncomingMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONTUNNELINCOMING(OnionTunnelIncomingMessage otim) {
		connection.sendMsg(otim);
	}

	/**
	 * Sent from CM/UI to Onion to send data through the tunnel.
	 * 
	 * @param otdm
	 *            the OnionTunnelDataMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONTUNNELDATAOUTGOING(OnionTunnelDataMessage otdm) {
		General.info("Try sending " + Arrays.toString(otdm.getData()));

		OnionBaseSocket targetTunnel = activeTunnels.get((int) otdm.getId());
		if (targetTunnel != null) {
			try {
				targetTunnel.sendRealData(otdm.getData());
				return;
			} catch (TunnelCrashException e) {
				General.error("Tunnel crashed!");
			}
			try {
				Main.getOas().ONIONERROR(
						newOnionErrorMessage(targetTunnel.getOnionApiId(), MessageType.API_ONION_TUNNEL_DATA));
			} catch (IllegalIDException e) {
				General.fatalException(e);
			}
			try {
				Main.getOas().unregister(targetTunnel.getOnionApiId());
			} catch (IllegalIDException e) {
				General.fatalException(e);
			}
		} else {
			General.error("No tunnel with given ID found: " + (int) otdm.getId());
		}
	}

	/**
	 * Sent from Onion to CM/UI to signal an incoming packet from the onion tunnel.
	 * 
	 * @param otdm
	 *            the OnionTunnelDataMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONTUNNELDATAINCOMING(OnionTunnelDataMessage otdm) {
		connection.sendMsg(otdm);
		General.info("Got data: " + Arrays.toString(otdm.getData()));
	}

	/**
	 * Sent from Onion to CM/UI to signal an error caused by an earlier request.
	 * 
	 * @param oem
	 *            the OnionErrorMessage
	 * @param connection
	 *            the connection to which the packet will be sent
	 */
	public void ONIONERROR(OnionErrorMessage oem) {
		connection.sendMsg(oem);
	}

	/**
	 * Sent from CM/UI to Onion to send cover data through the tunnel.
	 * 
	 * @param ocm
	 *            the OnionCoverMessage
	 * @param connection
	 *            the connection from which the packet was received
	 */
	private void ONIONCOVER(OnionCoverMessage ocm) {
		try {
			if (randomTunnel == null) {
				General.warning("There is no random tunnel available where we can send cover traffic");
			} else {
				randomTunnel.sendCoverData(ocm.getCoverSize());
			}
		} catch (Exception e) {
			General.fatalException(e);
		}
	}

	/**
	 * Parses the received message and calls the proper method.
	 * 
	 * @param buffer
	 *            the received message
	 * @param type
	 *            the type of the received message
	 * @throws MessageParserException
	 *             if there is an error while parsing the message
	 * @throws ProtocolException
	 *             if the message does not match the protocol
	 */
	@Override
	protected void receive(ByteBuffer buffer, MessageType type) throws MessageParserException, ProtocolException {
		switch (type) {
		case API_ONION_COVER:
			ONIONCOVER(OnionCoverMessage.parse(buffer));
			return;
		case API_ONION_TUNNEL_BUILD:
			ONIONTUNNELBUILD(OnionTunnelBuildMessage.parse(buffer));
			return;
		case API_ONION_TUNNEL_DATA: {
			OnionTunnelDataMessage msg = OnionTunnelDataMessage.parse(buffer);
			try {
				isRegistered((int) msg.getId());
			} catch (IllegalIDException e) {
				General.fatalException(e);
			}
			ONIONTUNNELDATAOUTGOING(msg);
			return;
		}
		case API_ONION_TUNNEL_DESTROY: {
			OnionTunnelDestroyMessage msg = OnionTunnelDestroyMessage.parse(buffer);
			try {
				isRegistered((int) msg.getId());
			} catch (IllegalIDException e) {
				General.fatalException(e);
			}
			ONIONTUNNELDESTROY(msg);
			return;
		}
		default:
			throw new ProtocolException("Unexpected message received");
		}
	}

	private void isRegistered(int id) throws IllegalIDException {
		lock.readLock().lock();
		if (!map.containsKey(id)) {
			lock.readLock().unlock();
			throw new IllegalIDException();
		}
		lock.readLock().unlock();
	}

	/**
	 * Registers a new logical connection to the CM/UI module.
	 * 
	 * @return ID of the new connection
	 * @throws SizeLimitExceededException
	 *             if too many connections are registered
	 */
	@Override
	public int register() throws SizeLimitExceededException {
		int id;
		lock.writeLock().lock();
		if (map.size() >= Integer.MAX_VALUE) {
			lock.writeLock().unlock();
			throw new SizeLimitExceededException("Too many connections registered!");
		}
		do {
			id = random.nextInt();
		} while (map.containsKey(id));
		map.put(id, null);
		lock.writeLock().unlock();
		return id;
	}

	/**
	 * Unregisters a logical connection.
	 * 
	 * @param id
	 *            ID of the connection
	 * @throws IllegalIDException
	 *             if the ID is not registered
	 */
	@Override
	public void unregister(int id) throws IllegalIDException {
		lock.writeLock().lock();
		if (!map.containsKey(id)) {
			lock.writeLock().unlock();
			throw new IllegalIDException();
		}
		map.remove(id);
		lock.writeLock().unlock();
	}
}
