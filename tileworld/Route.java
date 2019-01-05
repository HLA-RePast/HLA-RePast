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
package tileworld;

import java.awt.Point;
import java.util.Stack;

/**
*   A stack-based route consisting of an ordered list of waypoints along a 
*   planned route through a 2D space.
*/
public class Route implements Comparable {
	
	private Stack pointStack;
	
	/**
	*   Creates a new Route, it is initially empty, with no waypoints set
	*/
	public Route() {
		
		pointStack = new Stack();
	}
	
	public Route(Route r, Point p) {
		
		this.pointStack = (Stack)(r.getStack()).clone();
		pointStack.push(p);
		//System.out.println("NEW");
		//this.printRoute();
	}
	
	Stack getStack() {
		
		return pointStack;
	}
	
	/**
	*   Adds a waypoint to the route, waypoints should be added in reverse order,
	*   starting with the destination and progressing backward to the origin.
	*   @param p the next, reverse order, waypoint.
	*/
	public void addWayPoint(Point p) {
		
		if (p != null)		
			pointStack.push(p);
	}
	
	/**
	*   Gets the next wayPoint, waypoints for a Route are returned in forward-order,
	*   starting with the origin and progressing forward to the destination.
	*   @return the next, forward order, waypoint.
	*   @throws EmptyStackException if this.hasMoreWayPoints() returns false before
	*   the call to getNextWayPoint is made.
	*/
	public Point getNextWayPoint() {
		
		return (Point)pointStack.pop();
	}
	
	/**
	*   Returns true if a call to getNextWayPoint will return the next waypoint,
	*   false if the call to getNextWayPoint will throw an EmptyStackException.
	*   @return whether this Route has more waypoints
	*/
	public boolean hasMoreWayPoints() {
		
		if (pointStack.empty())
			return false;
		else
			return true;
	}
	
	/**
	*   Return the length as an integer of the total distance of the currently proposed 
	*   route. This is calculated by the summation of the distances between all points 
	*	(pi, pi+1) where 'i' is from 0 to n, being the ith point added to the route, and 
	*   point n+1 (being the origin point specified at construction time).
	*   @return the total length of the route proposed so far.
	*/
	public int getLength() {
		
		/*
		int length = 0;
		if (pointStack.empty())
			return length;
		Stack tempStack = new Stack();
		Point pI = (Point)pointStack.pop();
		Point pII;
		if (pointStack.empty())
			tempStack.push(pI);
		//summation of all lengths from point n to point n+1
		while (this.hasMoreWayPoints()) {
			pII = (Point)pointStack.pop();
			length += dist(pI, pII);
			tempStack.push(pI);
			pI = pII;
		}
		//put all the points back in to the original stack
		while (!tempStack.empty()) {
			pointStack.push(tempStack.pop());
		}
		return length;
		*/
		return pointStack.size();
	}
	
	/**
	*   Returns the first point in this route, useful for judging the distance
	*   one must travel before using this route.
	*   @return the origin point of this route
	*/
	public Point getStart() {
		
		return (Point)pointStack.peek();
	}
			
	
	private int dist(Point p1, Point p2) {
		
		int xDist = (int)Math.abs(p1.getX() - p2.getX());
		int yDist = (int)Math.abs(p1.getY() - p2.getY());
		return xDist + yDist;
	}
	
	public void printRoute() {
		
		Stack tempStack = new Stack();
		while (!pointStack.empty()) {
			Point nextPoint = (Point)pointStack.pop();
			System.out.println(nextPoint);
			tempStack.push(nextPoint);
		}
		while (!tempStack.empty())
			pointStack.push(tempStack.pop());
	}

	public int compareTo(Object o) {
		
		int thislength = pointStack.size();
		int otherlength = ((Route)o).getLength();
		if (thislength > otherlength)
			return 1;
		if (thislength < otherlength)
			return -1;
		return 0;
	}
}
