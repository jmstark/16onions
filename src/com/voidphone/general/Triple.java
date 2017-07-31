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

/**
 * Represents a triple of objects.
 * 
 * @param <A>
 *            type of the first object
 * @param <B>
 *            type of the second object
 * @param <C>
 *            type of the third object
 */
public class Triple<A, B, C> {
	/**
	 * the first object
	 */
	public final A a;
	/**
	 * the second object
	 */
	public final B b;
	/**
	 * the third object
	 */
	public final C c;

	/**
	 * Creates a new triple of objects.
	 * 
	 * @param a
	 *            the first object
	 * @param b
	 *            the second object
	 * @param c
	 *            the third object
	 */
	public Triple(A a, B b, C c) {
		this.a = a;
		this.b = b;
		this.c = c;
	}
}
