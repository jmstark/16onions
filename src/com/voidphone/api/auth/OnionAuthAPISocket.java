package com.voidphone.api.auth;

import java.io.IOException;
import java.net.InetSocketAddress;

import com.voidphone.api.APISocket;

public class OnionAuthAPISocket extends APISocket {
	public OnionAuthSessionHS1 AUTHSESSIONSTART(
			OnionAuthSessionStartMessage data) {
		return super
				.<OnionAuthSessionStartMessage, OnionAuthSessionHS1> sendrecv(data);
	}

	public OnionAuthSessionHS2 AUTHSESSIONINCOMINGHS1(
			OnionAuthSessionIncomingHS1 data) {
		return super
				.<OnionAuthSessionIncomingHS1, OnionAuthSessionHS2> sendrecv(data);
	}

	public void AUTHSESSIONINCOMINGHS2(OnionAuthSessionIncomingHS2 data) {
		super.<OnionAuthSessionIncomingHS2> send(data);
	}

	public OnionAuthEncryptResp AUTHLAYERENCRYPT(OnionAuthEncrypt data) {
		return super.<OnionAuthEncrypt, OnionAuthEncryptResp> sendrecv(data);
	}

	public OnionAuthDecryptResp AUTHLAYERDECRYPT(OnionAuthDecrypt data) {
		return super.<OnionAuthDecrypt, OnionAuthDecryptResp> sendrecv(data);
	}

	public void AUTHSESSIONCLOSE(OnionAuthClose data) {
		super.<OnionAuthClose> send(data);
	}

	public OnionAuthAPISocket(InetSocketAddress addr) throws IOException {
		super(addr);
	}
}
