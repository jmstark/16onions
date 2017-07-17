package com.voidphone.api;

import java.io.IOException;
import java.net.InetSocketAddress;

import protocol.MessageParserException;

import auth.api.OnionAuthClose;
import auth.api.OnionAuthDecrypt;
import auth.api.OnionAuthDecryptResp;
import auth.api.OnionAuthEncrypt;
import auth.api.OnionAuthEncryptResp;
import auth.api.OnionAuthSessionHS1;
import auth.api.OnionAuthSessionHS2;
import auth.api.OnionAuthSessionIncomingHS1;
import auth.api.OnionAuthSessionIncomingHS2;
import auth.api.OnionAuthSessionStartMessage;

public class OnionAuthApiSocket extends ApiSocket {
	public OnionAuthSessionHS1 AUTHSESSIONSTART(
			OnionAuthSessionStartMessage oassm) throws IOException,
			MessageParserException {
		oassm.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
		channel.read(readBuffer);
		return OnionAuthSessionHS1.parse(readBuffer);
	}

	public OnionAuthSessionHS2 AUTHSESSIONINCOMINGHS1(
			OnionAuthSessionIncomingHS1 oasihs1) throws IOException,
			MessageParserException {
		oasihs1.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
		channel.read(readBuffer);
		return OnionAuthSessionHS2.parse(readBuffer);
	}

	public void AUTHSESSIONINCOMINGHS2(OnionAuthSessionIncomingHS2 oasihs2)
			throws IOException {
		oasihs2.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
	}

	public OnionAuthEncryptResp AUTHLAYERENCRYPT(OnionAuthEncrypt oae)
			throws IOException, MessageParserException {
		oae.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
		channel.read(readBuffer);
		return OnionAuthEncryptResp.parse(readBuffer);
	}

	public OnionAuthDecryptResp AUTHLAYERDECRYPT(OnionAuthDecrypt oad)
			throws IOException, MessageParserException {
		oad.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
		channel.read(readBuffer);
		return OnionAuthDecryptResp.parse(readBuffer);
	}

	public void AUTHSESSIONCLOSE(OnionAuthClose oac) throws IOException {
		oac.send(writeBuffer);
		writeBuffer.flip();
		channel.write(writeBuffer);
	}

	public OnionAuthApiSocket(InetSocketAddress addr) throws IOException {
		super(addr);
	}
}
