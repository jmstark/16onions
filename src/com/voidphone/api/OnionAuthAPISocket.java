package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;

public class OnionAuthAPISocket extends APISocket {
	
	public byte[] encryptData(byte[] data)
	{
		//TODO: actually encrypt with onionAuth
		return data;
	}
	
	public AUTHSESSIONHS1 AUTHSESSIONSTART(AUTHSESSIONSTART data) {
		return null;
	}

	public AUTHSESSIONHS2 AUTHSESSIONINCOMINGHS1(AUTHSESSIONINCOMINGHS1 data) {
		return new AUTHSESSIONHS2((short) 10, new byte[] { 42, 43, 44 });
	}

	public void AUTHSESSIONINCOMINGHS2(AUTHSESSIONINCOMINGHS2 data) {

	}

	public AUTHLAYERENCRYPTRESP AUTHLAYERENCRYPT(AUTHLAYERENCRYPT data) {
		return null;
	}

	public AUTHLAYERDECRYPTRESP AUTHLAYERDECRYPT(AUTHLAYERDECRYPT data) {
		return null;
	}

	public void AUTHSESSIONCLOSE(AUTHSESSIONCLOSE data) {

	}
	
	public OnionAuthAPISocket(InetSocketAddress addr) throws IOException {
		super(addr);
	}

	public static class AUTHSESSIONSTART {
		private byte hostkey[];

		public AUTHSESSIONSTART(byte[] hostkey) {
			this.hostkey = hostkey;
		}

		public byte[] getHostkey() {
			return hostkey;
		}
	}

	public static class AUTHSESSIONHS1 {
		private short session;
		private byte payload[];

		public AUTHSESSIONHS1(short session, byte[] payload) {
			this.session = session;
			this.payload = payload;
		}

		public short getSession() {
			return session;
		}

		public byte[] getPayload() {
			return payload;
		}
	}

	public static class AUTHSESSIONINCOMINGHS1 {
		private byte hostkey[];
		private byte payload[];

		public AUTHSESSIONINCOMINGHS1(byte[] hostkey, byte[] payload) {
			this.hostkey = hostkey;
			this.payload = payload;
		}

		public byte[] getHostkey() {
			return hostkey;
		}

		public byte[] getPayload() {
			return payload;
		}
	}

	public static class AUTHSESSIONHS2 {
		private short session;
		private byte payload[];

		public AUTHSESSIONHS2(short session, byte[] payload) {
			this.session = session;
			this.payload = payload;
		}

		public short getSession() {
			return session;
		}

		public byte[] getPayload() {
			return payload;
		}
	}

	public static class AUTHSESSIONINCOMINGHS2 {
		private short session;
		private byte payload[];

		public AUTHSESSIONINCOMINGHS2(short session, byte[] payload) {
			this.session = session;
			this.payload = payload;
		}

		public short getSession() {
			return session;
		}

		public byte[] getPayload() {
			return payload;
		}
	}

	public static class AUTHLAYERENCRYPT {
		private short sessions[];
		private byte payload[];

		public AUTHLAYERENCRYPT(short[] sessions, byte[] payload) {
			this.sessions = sessions;
			this.payload = payload;
		}

		public short[] getSessions() {
			return sessions;
		}

		public byte[] getPayload() {
			return payload;
		}
	}

	public static class AUTHLAYERENCRYPTRESP {
		private byte payload[];

		public AUTHLAYERENCRYPTRESP(byte[] payload) {
			this.payload = payload;
		}

		public byte[] getPayload() {
			return payload;
		}
	}

	public static class AUTHLAYERDECRYPT {
		private short sessions[];
		private byte payload[];

		public AUTHLAYERDECRYPT(short[] sessions, byte[] payload) {
			this.sessions = sessions;
			this.payload = payload;
		}

		public short[] getSessions() {
			return sessions;
		}

		public byte[] getPayload() {
			return payload;
		}
	}

	public static class AUTHLAYERDECRYPTRESP {
		private byte payload[];

		public AUTHLAYERDECRYPTRESP(byte[] payload) {
			this.payload = payload;
		}

		public byte[] getPayload() {
			return payload;
		}
	}

	public static class AUTHSESSIONCLOSE {
		private short session;

		public AUTHSESSIONCLOSE(short session) {
			this.session = session;
		}

		public short getSession() {
			return session;
		}
	}
}
