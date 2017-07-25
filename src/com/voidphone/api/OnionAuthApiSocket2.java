package com.voidphone.api;

import auth.api.OnionAuthSessionHS1;
import auth.api.OnionAuthSessionHS2;
import auth.api.OnionAuthSessionIncomingHS1;
import auth.api.OnionAuthSessionIncomingHS2;
import auth.api.OnionAuthSessionStartMessage;

public class OnionAuthApiSocket2 {
	public void AUTHSESSIONSTART(OnionAuthSessionStartMessage oassm) {

	}

	private void AUTHSESSIONHS1(OnionAuthSessionHS1 oashs1) {

	}

	public void AUTHSESSIONINCOMINGHS1(OnionAuthSessionIncomingHS1 oasihs1) {

	}

	private void AUTHSESSIONHS2(OnionAuthSessionHS2 oashs2) {

	}

	public void AUTHSESSIONINCOMINGHS2(OnionAuthSessionIncomingHS2 oasihs2) {

	}

	public void AUTHLAYERENCRYPT(OnionAuthLayerEncryptMessage oalem) {

	}

	private void AUTHLAYERENCRYPTRESP(OnionAuthLayerEncryptRespMessage oalerm) {

	}

	public void AUTHLAYERDECRYPT(OnionAuthLayerDecryptMessage oaldm) {

	}

	private void AUTHLAYERDECRYPTRESP(OnionAuthLayerDecryptRespMessage oaldrm) {

	}

	public void AUTHCIPHERENCRYPT(OnionAuthCipherEncryptMessage oacem) {

	}

	private void AUTHCIPHERENCRYPTRESP(OnionAuthCipherEncryptRespMessage oacerm) {

	}

	public void AUTHCIPHERDECRYPT(OnionAuthCipherDecryptMessage oacdm) {

	}

	private void AUTHCIPHERDECRYPTRESP(OnionAuthCipherDecryptRespMessage oacdrm) {

	}

	public void AUTHSESSIONCLOSE(OnionAuthSessionCloseMessage oascm) {

	}

	private void AUTHERROR(OnionAuthErrorMessage oaem) {

	}
}
