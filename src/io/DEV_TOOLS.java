/*

Copyright 2008, Rob Minson (rzm@cs.bham.ac.uk)

School of Computer Science
University of Birmingham
Edgbaston
B152TT
United Kingdom

This file is part of HLA_RePast.

    HLA_RePast is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    HLA_RePast is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with HLA_RePast.  If not, see <http://www.gnu.org/licenses/>.

*/
/*
 * Created on 16-Dec-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package io;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import manager.Globals;


/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DEV_TOOLS {

	private static BufferedReader reader =
		new BufferedReader(new InputStreamReader(System.in));

	public static void stopRead(String msg) {
		try {
			System.out.print(msg);
			reader.readLine();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static void showException(Exception e) {
		showException("no pre-message", e);		
	}
	
	public static void showException(String pre, Exception e) {
		System.err.println("EXCEPTION >> " + pre);
		e.printStackTrace();
		System.err.println("End of Stack!");
	}
	
	public static void print(String message) {
		if (!Globals.VERBOSE)
			return;
		for (int i = 0; i < indent; i++)
			System.out.print("\t");
		System.out.println(message);
	}
	
	private static int indent = 0;
	public static void indent() {
		indent++;
	}
	public static void undent() {
		indent--;
	}
}
