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

import uchicago.src.sim.space.*;
import uchicago.src.sim.util.*;
import java.util.ArrayList;

/**
*   A class to generates areas of terrain on RePast Discrete2DSpace instances.
*   Generated Terrain is of determinable size but of random shape.
*/
public class TerrainGenerator {
	
	private int extent;
	private int x;
	private int y;
	private Discrete2DSpace space;
	private ArrayList generatedTerrain;
	
	/**
	*   Constructs a new TerrainGenerator set to proliferate terrain to a given extent
	*   with a central point in space at x, y
	*   @param extent the size of the generated block of terrain
	*   @param x the center of the generated block in the x-dimension
	*   @param y the center of the generated block in the y-dimension
	*   @param space the Discrete2DSpace to generate the terrain in to
	*/
	public TerrainGenerator(int extent, int x, int y, Discrete2DSpace space) {
		
		this.generatedTerrain = new ArrayList();
		this.extent = extent;
		this.x = x;
		this.y = y;
		this.space = space;
		Random.createUniform();
	}
	
	/**
	*   Constructs a new TerrainGenerator with a list of already generated terrain.
	*   This constructor makes recursive algorithms easy to implement with
	*   TerrainGenerators.
	*   @param extent the size of the generated block of terrain
	*   @param x the center of the generated block in the x-dimension
	*   @param y the center of the generated block in the y-dimension
	*   @param space the Discrete2DSpace to generate the terrain in to
	*   @param generatedTerrain a list of terrain already generated in to space
	*/
	public TerrainGenerator(int extent, int x, int y, Discrete2DSpace space, ArrayList generatedTerrain) {
		
		this.generatedTerrain = generatedTerrain;
		this.extent = extent;
		this.x = x;
		this.y = y;
		this.space = space;
		Random.createUniform();
	}
	
	/**
	*   Executes a proliferation algorithm upon this TerrainGenerator, returning
	*   a List of Terrain objects which have been generated in to the space
	*   specified in the constructor.
	*   @return a list of Terrain objects which have been placed in the space
	*   by the algorithm
	*/
	public ArrayList proliferate() {
		
		Terrain generated = new Terrain(this.x, this.y);
		space.putObjectAt(this.x, this.y, generated);
		generatedTerrain.add(generated);

		if (this.extent > 0) {
			int random = Random.uniform.nextIntFromTo(1, 4);
			int putX = this.x;
			int putY = this.y;
			switch (random) {
				case 1: if (this.y > 0) 
							putY = y - 1;
						else
							putY = y + 1;
						break;
				case 2: if (this.x < space.getSizeX() - 1)
							putX = x + 1;
						else
							putX = x - 1;
						break;
				case 3: if (this.y < space.getSizeY() - 1)
							putY = y + 1;
						else
							putY = y - 1;
						break;
				case 4: if (this.x > 0)
							putX = x - 1;
						else
							putX = x + 1;
						break;
			}
			TerrainGenerator newGenerator = new TerrainGenerator(extent - 1, putX, putY, space, generatedTerrain);
			return newGenerator.proliferate();
		}
		else
			return this.generatedTerrain;			
	}
}
