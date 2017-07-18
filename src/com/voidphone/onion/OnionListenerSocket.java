package com.voidphone.onion;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.channels.SocketChannel;

import com.voidphone.api.Config;
import com.voidphone.api.OnionAuthApiSocket;
import com.voidphone.general.General;

/**
 * Main application runs a TCP server socket. There, when it receives a new
 * connection, it then constructs an OnionServerSocket, passing the new TCP
 * socket to the constructor. Then it should send a TUNNEL_READY API message.
 * 
 */
public class OnionListenerSocket extends OnionBaseSocket implements Main.Attachable
{
	protected DataInputStream previousHopDis;
	protected DataOutputStream previousHopDos;
	protected DataInputStream nextHopDis;
	protected DataOutputStream nextHopDos;

	
	public OnionListenerSocket(Socket previousHopSocket, Config config) throws IOException
	{
		this.config = config;
		previousHopDis = new DataInputStream(previousHopSocket.getInputStream());
		previousHopDos = new DataOutputStream(previousHopSocket.getOutputStream());
		authSessionIds = new short[1];
		authSessionIds[0] = authenticate();
	}

	/**
	 * Encrypts payload for previous hop.
	 * 
	 * @return encrypted payload
	 * @throws Exception 
	 */
	protected byte[] encrypt(byte[] payload) throws Exception
	{
		return super.encrypt(payload, 1);
	}
	
	/**
	 * Decrypts payload of previous hop.
	 * 
	 * @return Decrypted payload
	 * @throws Exception 
	 */
	protected byte[] decrypt(byte[] payload) throws Exception
	{
		return super.decrypt(payload, 1);
	}
	
	/**
	 * Counterpart to authenticate() of OnionConnectingSocket.
	 * Since at the moment of authentication this node is the last one,
	 * we receive the authentication always unencrypted. The encryption
	 * was removed at the previous hop.
	 * 
	 * @return session ID
	 * 
	 * @throws IOException
	 */
	short authenticate() throws IOException
	{
		OnionAuthApiSocket.AUTHSESSIONHS2 hs2;
		
		buffer.clear();
		
		previousHopDis.readFully(buffer.array());
		
		if (buffer.getInt() != MAGIC_SEQ_CONNECTION_START | buffer.getInt() != VERSION)
			throw new IOException("Tried to connect with non-onion node or wrong version node");

		// read incoming hostkey
		byte[] previousHopHostkey = new byte[buffer.getShort()];
		buffer.get(previousHopHostkey);
		
		// read incoming hs1 from remote peer
		byte[] hs1Payload = new byte[buffer.getShort()];
		buffer.get(hs1Payload);

		buffer.clear();
		
		// get hs2 from onionAuth and send it back to remote peer
		hs2 = config.getOnionAuthAPISocket().AUTHSESSIONINCOMINGHS1(new AUTHSESSIONINCOMINGHS1(previousHopHostkey, hs1Payload));
		buffer.putShort((short)hs2.getPayload().length);
		buffer.put(hs2.getPayload());
		previousHopDos.write(buffer.array());
		
		buffer.clear();
		
		return hs2.getSession();
	}

	/**
	 * This function is used to build and destroy tunnels. Building/destroying
	 * is done iteratively. A control message only consists of message type,
	 * address length (4 or 6, depending on IP version), IP address and port
	 * number (same port number for TCP and UDP). The function receives the
	 * message, and if the next hop is already known the message gets forwarded
	 * there, if not, the next hop is constructed.
	 * 
	 * @throws IOException
	 */
	void processNextControlMessage() throws Exception
	{
		buffer.clear();
		
		// When invoking this method, the authentication has already
		// succeeded, i.e. the data is encrypted irrespective of whether
		// we need to forward it or it is for us.

		byte[] encryptedData = new byte[previousHopDis.readShort()];
		previousHopDis.readFully(encryptedData);
		byte[] data = decrypt(encryptedData);
		
		if(nextHopDos != null)
		{
			// forward the data, now that we peeled off one layer of encryption.
			nextHopDos.writeShort(data.length);
			nextHopDos.write(data);
			nextHopDos.flush();
		}
		
		else
		{
			// the data is for us, we are (at least until now) the end of the tunnel
			// and the data has no more encryption layers, i.e. it is now plaintext.
			buffer.put(data);
			short msgType = buffer.getShort();
			if (msgType == MSG_BUILD_TUNNEL)
			{
				//A request to add a new hop and forward all data to that.
				//Unpack the hop address and then open the connection.
				byte[] rawAddress = new byte[buffer.getShort()];
				buffer.get(rawAddress);
				InetAddress nextHopAddress = InetAddress.getByAddress(rawAddress);
				short nextHopPort = buffer.getShort();
				Socket nextHopSocket = new Socket(nextHopAddress, nextHopPort);
				nextHopDis = new DataInputStream(nextHopSocket.getInputStream());
				nextHopDos = new DataOutputStream(nextHopSocket.getOutputStream());
			}
			else if (msgType == MSG_DESTROY_TUNNEL)
			{
				// tear down the tunnel, i.e. connections to next and previous hop.
				if(nextHopDis != null)
					nextHopDis.close();
				nextHopDis = null;
				if(nextHopDos != null)
					nextHopDos.close();
				nextHopDos = null;
				if(previousHopDis != null)
					previousHopDis.close();
				previousHopDis = null;
				if(previousHopDos != null)
					previousHopDos.close();
				previousHopDos = null;
			}
		}		
	}	
	

	
	@Override
	public boolean handle() {
		try {
			System.out.println(previousHopDis.read());
			System.out.println(previousHopDis.read());
			System.out.println(previousHopDis.read());
		} catch (IOException e) {
			e.printStackTrace();
		}
		System.exit(1);
		return false;
	}
	
}
