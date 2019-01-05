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
 * Created on 05-Jan-2004
 */
package models;

import java.awt.Color;
import java.awt.Font;
import java.util.Hashtable;

import exceptions.ExcludedException;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import hla.rti13.java1.RTIexception;
import object.PublicObject;
import object.RemoteObject;
import object.variables.CumulativeInteger;
import object.variables.ExclusiveInteger;
import object.variables.PublicVariable;
import object.variables.ViewableInteger;

/**
 * 
 * 
 * @author Rob Minson
 */
public class BasicTestObject extends PublicObject implements RemoteObject, Drawable {

	protected static final Font display_font = new Font("Sans Serif", Font.PLAIN, 12);
	private static String[] vars = new String[] {	"ExclusiveAttribute", 
													"CumulativeAttribute", 
													"ViewableAttribute"};
	
	
	/* the globally visible portions of the object */
	private ExclusiveInteger excAttr;
	private CumulativeInteger cumulAttr;
	private ViewableInteger viewAttr;
	private Hashtable<String, PublicVariable> varMap = 
									new Hashtable<String, PublicVariable>();
	
	/* the private portions of the object */
	private int x;
	private int y;
	
	public BasicTestObject() {		
		this(0, 0, 0, 0, 0);
	}
	
	public BasicTestObject(int excVal, int cumulVar, int viewVal, int x, int y) {		
		excAttr = new ExclusiveInteger(excVal);
			varMap.put(vars[0], excAttr);
		cumulAttr = new CumulativeInteger(cumulVar);
			varMap.put(vars[1], cumulAttr);
		viewAttr = new ViewableInteger(viewVal);
			varMap.put(vars[2], viewAttr);
		this.x = x;
		this.y = y;
		
	}
	
	public PublicVariable getVariable(String varName) {
		return varMap.get(varName);
	}
	
	public int getExclusive() {
		return excAttr.get();
	}
	
	public void setExclusive(int newPop) throws RTIexception, ExcludedException {
		excAttr.set(newPop);
	}
	
	public int getCumulative() {
		return cumulAttr.get();
	}
	
	public void setCumulative(int increment) throws RTIexception {
		cumulAttr.set(increment);
	}
	
	public int getViewable() {
		return viewAttr.get();
	}
	
	public void setViewable(int newValue) throws RTIexception {
		viewAttr.set(newValue);
	}
	
	public boolean isOwned() {
		return excAttr.isOwned();
	}
	
	public String[] getPublicVariables() {
		return vars;
	}
	
	public int getX() {		
		return x;
	}
	
	public int getY() {		
		return y;
	}
	
	public String toString() {
		return "exc: " + excAttr.get() + "/cum: " + cumulAttr.get() + "/view: " + viewAttr.get();
	}
	
	public void draw(SimGraphics g) {
		g.setFont(display_font);
		g.drawRoundRect(Color.WHITE);
		g.drawString(this.toString(), Color.BLACK);
	}
}
