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
import java.awt.Point;

import uchicago.src.sim.gui.Drawable;
import uchicago.src.sim.gui.SimGraphics;
import hla_past.object.PublicObject;
import hla_past.object.PublicVariable;
import hla_past.object.RemoteObject;
import hla_past.object.ViewablePoint;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Terrain 
	extends PublicObject 
		implements RemoteObject, Drawable {
	
	private ViewablePoint position;
	private String[] vars = new String[] {"position"};
	
	public Terrain(Point position) {
		
		this.position = new ViewablePoint(position);
	}
	
	public Terrain(int x, int y) {
		
		this(new Point(x, y));
	}
	
	public Terrain() {
		
		this.position = new ViewablePoint(null);
	}
	
	public int getX() {
		
		return (int)position.get().getX();
	}
	
	public int getY() {
		
		return (int)position.get().getY();
	}
	
	public void draw(SimGraphics g) {
		
		g.drawFastRect(Color.WHITE);
		g.setFont(new Font("Monospaced", Font.BOLD, 18));
		g.drawString("#", Color.BLACK);
	}
	
	public PublicVariable getVariable(String varName) {
		
		return position;
	}

	public String[] getPublicVariables() {
		
		return vars;
	}
}
