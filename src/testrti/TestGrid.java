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
 * Created on 27-Mar-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package testrti;

import java.net.InetAddress;

import hla.rti13.java1.FederatesCurrentlyJoined;
import hla.rti13.java1.NullFederateAmbassador;
import hla.rti13.java1.RTIambassador;
import hla.rti13.java1.ResignAction;
import logging.DBLogger;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TestGrid {

	public static String FED_NAME = "lockFed";

	public static void main(String[] args) throws Exception {
		
		boolean master = false;
		String master_slave = "Slave";
		if (args.length > 0 && args[0].equals("-m")) {
			master_slave = "Master";
			master = true;
		}
		System.out.println(master_slave + " executing on host: " + InetAddress.getLocalHost());
		System.out.println("Attempting to contact RTI");
		RTIambassador amb = new RTIambassador();
		if (master) {
			amb.createFederationExecution(FED_NAME, "HelloWorld.fed");
		}
		amb.joinFederationExecution(InetAddress.getLocalHost().toString(), FED_NAME, new NullFederateAmbassador());
		DBLogger log = new DBLogger(true);
		amb.resignFederationExecution(ResignAction.DELETE_OBJECTS_AND_RELEASE_ATTRIBUTES);
		if (master) {
			boolean success = false;
			while (!success) {
				try {
					amb.destroyFederationExecution(FED_NAME);
					success = true;
				}
				catch (FederatesCurrentlyJoined e) {}
			}
		}
	}
}
