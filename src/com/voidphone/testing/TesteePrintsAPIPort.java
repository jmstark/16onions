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
package com.voidphone.testing;

import java.io.BufferedReader;

import com.voidphone.general.TestProcess;

public class TesteePrintsAPIPort {
	public static void main(String args[]) throws Exception {
		TestProcess p = new TestProcess(com.voidphone.onion.Main.class, new String[]{"-c", "bla"});
		BufferedReader out = p.getOut();
		Stub.getAPIPort(out);
		p.terminate();
	}
}
