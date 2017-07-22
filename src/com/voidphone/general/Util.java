package com.voidphone.general;

import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

public class Util {
	
	public static RSAPublicKey getHostkeyObject(byte[] hostkeyBytes)
	{
		try {
			//TODO: is this the right format? can it also be e.g. PKCS1?
			return (RSAPublicKey) KeyFactory.getInstance("RSA").generatePublic(new PKCS8EncodedKeySpec(hostkeyBytes));
		} catch (InvalidKeySpecException | NoSuchAlgorithmException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}
	
	public static byte[] getHostkeyBytes(RSAPublicKey hostkeyObject)
	{
		return hostkeyObject.getEncoded();
	}

}
