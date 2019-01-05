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
package rtidep;

import hla.rti13.java1.RTIambassador;
import hla.rti13.java1.RTIexception;
import manager.LocalManager;

public class Threads {

	public static void RTI_SHORT_TICK() throws RTIexception {
		RTI_SHORT_TICK(LocalManager.getRTI());
	}

	public static void RTI_LONG_TICK() throws RTIexception {
		RTI_LONG_TICK(LocalManager.getRTI());
	}

	private static void RTI_SHORT_TICK(RTIambassador amb) throws RTIexception {
		RTI_TICK(amb, 0.1, 0.5);
	}

	private static void RTI_LONG_TICK(RTIambassador amb) throws RTIexception {
		RTI_TICK(amb, -1, -1);
	}
	
	private static void RTI_TICK(RTIambassador amb, double min, double max) 
		throws RTIexception {
		if (min <= 0 || max <= 0 || min >= max)
			amb.tick();
		amb.tick(min, max);
		LocalManager.getManager().flushCallbackBuffer(); 
			/* commits any buffered callbacks */
	}
}
