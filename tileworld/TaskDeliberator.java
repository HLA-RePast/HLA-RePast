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

import java.util.ArrayList;

public class TaskDeliberator {
	
	private ArrayList holes;
	private IntentionStructure intStruct;
	private SEUCalculator calc;
	
	public TaskDeliberator(SEUCalculator calc) {
		
		this.calc = calc;
		holes = new ArrayList();
	}
	
	/**
	*   Adds a Hole to be a part of a future deliberation
	*   @param h the Hole to be added
	*/
	public void addHole(Hole h) {
		
		holes.add(h);
	}
	
	/**
	*   Picks one Hole from the set of all Holes in its list + the 
	*   Hole currently being used by the IntentionStructure
	*/
	public synchronized void deliberate() {
		
		if (intStruct == null)
			return;
		if (holes.size() < 1)
			return;
		SEUProfile currentProfile = intStruct.getCurrentPlan();
		SEUProfile bestProfile = currentProfile;
		for (int i = 0; i < holes.size(); i++) {
			Hole nextHole = (Hole)holes.get(i);
			try {
				SEUProfile nextProfile = calc.getSEUProfile(nextHole);
				if (bestProfile == null) {
					if (nextProfile != null)
						bestProfile = nextProfile;
				}
				else {
					if (nextProfile != null) {
						if (nextProfile.getRating() > bestProfile.getRating())
							bestProfile = nextProfile;
					}
				}
			} catch (InsufficientTilesException e) {}
		}
		if (currentProfile != bestProfile && bestProfile != null)
			intStruct.preemptPlan(bestProfile);
		holes.clear();
	}	
	
	/**
	*   Requests that the TaskDeliberator assign new goals to the 
	*   IntentionStructure, this drops all boldness criteria and 
	*   just finds the best choice from its current options, passing
	*   it as quickly as possible to the registered IntentionStructure.
	*/
	public void requestNewGoals() {
		
		deliberate();
	}

	/**
	*   Registers an intention structure that should be used in deliberation comparisons
	*   and should be communicated with when the deliberator decides that a high-level
	*   goal should change
	*   @param intStruct the IntentionStructure to be communicated with by the deliberator
	*/
	public synchronized void registerIntentionStructure(IntentionStructure intStruct) {

		this.intStruct = intStruct;
	}		
}		
