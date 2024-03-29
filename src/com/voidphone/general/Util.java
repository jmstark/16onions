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
package com.voidphone.general;

import java.security.InvalidKeyException;
import java.security.interfaces.RSAPublicKey;

import util.SecurityHelper;

public class Util {

	public static RSAPublicKey getHostkeyObject(byte[] hostkeyBytes) {
		try {
			return SecurityHelper.getRSAPublicKeyFromEncoding(hostkeyBytes);
		} catch (InvalidKeyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return null;
		}
	}

	public static byte[] getHostkeyBytes(RSAPublicKey hostkeyObject) {
		return SecurityHelper.encodeRSAPublicKey(hostkeyObject);
	}

}
