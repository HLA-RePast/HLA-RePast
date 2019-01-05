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
 * Created on 20-Jan-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tileworld;

import java.awt.Color;
import java.awt.Font;
import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import hla.rti13.java1.RTIexception;
import hla_past.object.ExcludedException;
import hla_past.object.ExclusiveBoolean;
import hla_past.object.PublicObject;
import hla_past.object.PublicVariable;
import hla_past.object.RemoteObject;
import hla_past.object.VariableException;
import hla_past.object.ViewableBoolean;
import hla_past.object.ViewableInteger;
import hla_past.object.ViewablePoint;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Tile 
	extends PublicObject 
		implements RemoteObject, Drawable, AgeingObject, SpatialObject {

	private ViewablePoint position;
	private ExclusiveBoolean isHeld;
	
	private String[] vars = new String[] {"position", "isHeld", "leftToLive", "ageingOn"};

	public Tile() {
		
		this(0, 0, 0, false);
		
	}
	
	public Tile(int x, int y, int lifespan, boolean ageingOn) {
		
		position = new ViewablePoint(x, y);
		isHeld = new ExclusiveBoolean(false);
		leftToLive = new ViewableInteger(lifespan);
		this.ageingOn = new ViewableBoolean(ageingOn);
	}
	
	public void pickup() throws ExcludedException, NotOnBoardException {
		
		if (isHeld())
			throw new NotOnBoardException();
		try {
			isHeld.set(true);
		}
		catch (RTIexception e) {
			throw new VariableException("comms error", e);
		}
	}
	
	public boolean isHeld() {
		
		return isHeld.get();
	}

	public PublicVariable getVariable(String varName) {
		
		if (varName.equals("position"))
			return position;
		else if (varName.equals("isHeld"))
			return isHeld;
		else if (varName.equals("leftToLive"))
			return leftToLive;
		else
			return ageingOn;
	}

	public String[] getPublicVariables() {
		
		return vars;
	}

	public void draw(SimGraphics g) {
		
		g.drawFastRect(Color.GREEN);
		g.setFont(new Font("Monospaced", Font.BOLD, 18));
		g.drawString("T", Color.BLACK);
	}

	public int getX() {
		
		return (int)position.get().getX();
	}

	public int getY() {
		
		return (int)position.get().getY();
	}

	private ViewableInteger leftToLive;
	private ViewableBoolean ageingOn;

	public int getAge() {
		return leftToLive.get();
	}

	public boolean getOlder() {
		
		if (ageingOn.get()) {
			try {
				leftToLive.set(leftToLive.get() - 1);
			}
			catch (Exception e) {
				throw new VariableException("serialisation problem", e);
			}
			if (leftToLive.get() <= 0)
				return AgeingObject.JUST_DIED;
			else
				return AgeingObject.STILL_ALIVE;
		}
		else
			return AgeingObject.STILL_ALIVE;			
	}

	public boolean isAgeing() {
		
		return ageingOn.get();
	}

	public void setAgeingOn(boolean ageing) {
		
		try {
			ageingOn.set(ageing);
			
		}
		catch (Exception e) {
			throw new VariableException("problem...", e);
		}
	}
}
