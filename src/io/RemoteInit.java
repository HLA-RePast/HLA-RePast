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
 * Created on 27-Jan-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package io;

import uchicago.src.sim.engine.ParameterReader;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModel;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class RemoteInit extends SimInit {

	@Override
	public void loadModel(SimModel model, String fileName, boolean isBatch) {
		
		ParameterReader reader = null;
		try {
		  reader = new ParameterReader(fileName);
		} 
		catch (Exception ex) {
		  ex.printStackTrace();
		  DEV_TOOLS.showException(ex);
		  System.exit(0);
		}

		RemoteController control = new RemoteController(reader.getParameters());
		model.setController(control);
		control.setModel(model);
		model.addSimEventListener(control);
		control.begin();
	}
}