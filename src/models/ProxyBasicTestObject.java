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
package models;

import hla.rti13.java1.RTIexception;

import java.awt.Color;

import uchicago.src.sim.gui.SimGraphics;

public class ProxyBasicTestObject extends BasicTestObject {
	public void draw(SimGraphics g) {
		g.setFont(display_font);
		g.drawRoundRect(Color.GRAY);
		g.drawString(this.toString(), Color.BLACK);
	}

	public void setViewable(int newValue) throws RTIexception {
		//noop
	}	
}