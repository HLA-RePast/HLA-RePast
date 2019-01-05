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

import java.util.*;

/**
*   Uses simple threshold values to filter tasks which look worthy
*   of further analysis from those which do not. The overall aim of this
*   being to save on computation time for short-term planning.
*/
public class TaskFilter {

	private TaskDeliberator deliberator;
	private int threshold;
	private IntentionStructure intStruct;
	
	/**
	*   Constructs a TaskFilter that will use the specified threshold
	*   on the score availiable from a Hole to decide whether to pass
	*   the Hole over to the deliberator for more complex analysis of
	*   its utility to an Agent
	*/
	public TaskFilter(TaskDeliberator deliberator, int threshold) {
		
		this.deliberator = deliberator;
		this.threshold = threshold;
	}
	
	/**
	*   Passes the specified Hole through the filter. The hole is 
	*   compared to the hole currently being pursued by an agent
	*   (this is accessed through an IntentionStructure, if one is
	*   not registered to this TaskFilter the Hole is immediately
	*   passed through), if the positive difference between the score
	*   of the current Hole and h is greater than the threshold 
	*   specified in the constructor the Hole will be passed down
	*   to the deliberator, if it is equal to or less than the 
	*   threshold it will not be passed down.
	*   @param h the Hole to be filtered.
	*/
	public void filterHole(Hole h) {
		
		if (intStruct == null || intStruct.getHole() == null) {
			deliberator.addHole(h);
			return;
		}
		if (h.getValue() - intStruct.getHole().getValue() > threshold)
			deliberator.addHole(h);
	}
	
	/**
	*   Filter a list of holes using the same filtering technique
	*   described in filterHole(Hole h) above
	*   @param holes a java.util.List of Holes
	*/
	public void filterHoles(List holes) {
		
		for (int i = 0; i < holes.size(); i++) {
			Hole nextHole = (Hole)holes.get(i);
			filterHole(nextHole);
		}
	}
	
	/**
	*   Registers an IntentionStructure to use for comparisons of 
	*   filtered Holes and current Holes
	*   @param intStruct an IntentionStructure. This should always
	*   be the same InentionSturcture that is registered to the 
	*   TaskDeliberator that this TaskFilter is connected to.
	*/
	public void registerIntentionStructure(IntentionStructure intStruct) {
		
		this.intStruct = intStruct;
	}
}
