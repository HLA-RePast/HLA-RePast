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
 * Created on 21-Jan-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tileworld;

import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.RTIexception;
import hla_past.io.Bytes;
import hla_past.object.ExcludedException;
import hla_past.object.InstanceListener;
import hla_past.object.PublicVariable;
import hla_past.object.ReflectedList;
import hla_past.object.RemoteObject;
import hla_past.object.VariableListener;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import testrti.DEV_TOOLS;
import uchicago.src.sim.space.Cell;
import uchicago.src.sim.space.Multi2DGrid;
import uchicago.src.sim.util.Random;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TileWorld extends Multi2DGrid {

	private ArrayList tiles;
	private ArrayList holes;
	private ArrayList terrain;
	
	private ArrayList allTiles;
	private ArrayList allHoles;
	private ArrayList allTerrain;
	
	private ReflectedList r_tiles;
	private ReflectedList r_holes;
	private ReflectedList r_terrain;
	
	private boolean tileAgeing;
	private boolean tileGeneration;
	private boolean holeAgeing;
	private boolean holeGeneration;
	
	private int ageingRate;
	private int maxLifeSpan;
	private int generationRate;
	
	private TileWorldModel model;
	
	public TileWorld(	int x,
									int y,
									int terrainDensity,
									int terrainSize,
									int numHoles,
									int numTiles,
									boolean tileAgeing,
									boolean holeAgeing,
									boolean tileGen,
									boolean holeGen,
									int ageRate,
									int genRate,
									TileWorldModel m,
									ReflectedList r_tiles,
									ReflectedList r_holes,
									ReflectedList r_terrain) {
		
		super(x, y, false);
		Random.createUniform();
		
		this.ageingRate = ageRate;
		this.generationRate = genRate;
		
		//we are going to populate these now
		this.tiles = new ArrayList();
		this.holes = new ArrayList();
		this.terrain = new ArrayList();
		
		this.allTiles = new ArrayList();
		this.allHoles = new ArrayList();
		this.allTerrain = new ArrayList();
		
		//assign these now, but they will only get populated at some point later
		this.r_tiles = r_tiles; 
		this.r_holes = r_holes;
		this.r_terrain = r_terrain; 
		//this method adds the relevant listeners for doing this population...
		initListeners();
		
		//generate the terrain and populate our own list with the results
		for (int i = 0; i < terrainDensity; i++) {
			int nextX = Random.uniform.nextIntFromTo(0, this.getSizeX() - 1);
			int nextY = Random.uniform.nextIntFromTo(0, this.getSizeY() - 1);
			int extent = Random.uniform.nextIntFromTo(1, terrainSize);
			TerrainGenerator nextGenerator = new TerrainGenerator(extent, nextX, nextY, this);
			this.terrain.addAll(nextGenerator.proliferate());
		}
		for (int i = 0; i < terrain.size(); i++) {
			Terrain t = (Terrain)terrain.get(i);
			this.putObjectAt(t.getX(), t.getY(), t);
		}
		
		//generate the tiles...
		this.maxLifeSpan = 1000 / Math.max(Math.max(ageingRate, 1), Math.min(ageingRate,10));
		System.out.println("maxLifeSpan = " + maxLifeSpan);
		for (int i = 0; i < numTiles; i++) {
			int lifespan = Random.uniform.nextIntFromTo(99, maxLifeSpan);
			boolean tilePlaced = false;
			do {				
				int nextX = Random.uniform.nextIntFromTo(0, this.getSizeX() - 1);
				int nextY = Random.uniform.nextIntFromTo(0, this.getSizeY() - 1);
				if (this.getCellAt(nextX, nextY) == null || this.getCellAt(nextX, nextY).size() == 0) {
					Tile nextTile = new Tile(nextX, nextY, lifespan, tileAgeing);
					initTile(nextTile);
					this.putObjectAt(nextX, nextY, nextTile);
					tiles.add(nextTile);
					tilePlaced = true;
				}
			}
			while (!tilePlaced);
		}
		
		//generate the holes...
		for (int i = 0; i < numHoles; i++) {			
			int depth = Random.uniform.nextIntFromTo(1, 10);
			int lifespan = Random.uniform.nextIntFromTo(99, maxLifeSpan);
			boolean holeDug = false;
			do {				
				int nextX = Random.uniform.nextIntFromTo(0, this.getSizeX() - 1);
				int nextY = Random.uniform.nextIntFromTo(0, this.getSizeY() - 1);
				if (this.getCellAt(nextX, nextY) == null || this.getCellAt(nextX, nextY).size() == 0) {
					Hole nextHole = new Hole(nextX, nextY, depth, lifespan, holeAgeing);
					initHole(nextHole);
					this.putObjectAt(nextX, nextY, nextHole);
					holes.add(nextHole);
					holeDug = true;
				}
			}
			while (!holeDug);
		}
		
		refreshCollections();
		
		this.model = m;
		this.tileAgeing = tileAgeing;
		this.tileGeneration = tileGen;
		this.holeAgeing = holeAgeing;
		this.holeGeneration = holeGen;
		this.ageingRate = ageRate;
		this.generationRate = genRate;	
	}
	
	public List getLocalTiles() {
		
		return tiles;
	}
	
	public List getLocalHoles() {
		
		return holes;
	}
	
	public List getLocalTerrain() {
		
		return terrain;
	}
	
	public List getTiles() {
		
		return allTiles;
	}
	
	public List getHoles() {
		
		return allHoles;
	}
	
	public List getTerrain() {
		
		return allTerrain;
	}
	
	private void refreshCollections() {
		
		allTiles.addAll(tiles);
		allTiles.addAll(r_tiles.getInstances());
		
		allHoles.addAll(holes);
		allHoles.addAll(r_holes.getInstances());
		
		allTerrain.addAll(terrain);
		allTerrain.addAll(r_terrain.getInstances());
	}
	
	private void initListeners() {
		
		//do registrations for objects already in the lists...
		List tileInsts = r_tiles.getInstances();
		for (int i = 0; i < tileInsts.size(); i++) {
			Tile t = (Tile)tileInsts.get(i);
			initTile(t);
			allTiles.add(t);		
		}
		
		List holeInsts = r_holes.getInstances();
		for (int i = 0; i < holeInsts.size(); i++) {
			Hole h = (Hole)holeInsts.get(i);
			initHole((Hole)holeInsts.get(i));
			allHoles.add(h);
		}
		
		//tiles listeners...
		r_tiles.registerInstanceListener(new InstanceListener() {			
			public void instanceAdded(RemoteObject obj) {
				Tile t = (Tile)obj;
				if (isTerrain(t.getX(), t.getY()))
					return;
				initTile(t);
				putObjectAt(t.getX(), t.getY(), t);
				allTiles.add(t);
			}			
			public void instanceRemoved(RemoteObject obj) {
				Tile t = (Tile)obj;
				removeObjectAt(t.getX(), t.getY(), t);
				allTiles.remove(t);
			}		
		});
		
		//holes listeners...
		r_holes.registerInstanceListener(new InstanceListener() {			
			public void instanceAdded(RemoteObject obj) {
				Hole h = (Hole)obj;
				if (isTerrain(h.getX(), h.getY()))
					return;
				initHole(h);
				putObjectAt(h.getX(), h.getY(), h);
				allHoles.add(h);
			}			
			public void instanceRemoved(RemoteObject obj) {
				Hole h = (Hole)obj;
				removeObjectAt(h.getX(), h.getY(), h);
				allHoles.remove(h);
			}		
		});
		
		//terrain listeners...
		r_terrain.registerInstanceListener(new InstanceListener() {			
			public void instanceAdded(RemoteObject obj) {
				Terrain t = (Terrain)obj;
				putObjectAt(t.getX(), t.getY(), t);
				allTerrain.add(t);
				sortCell(t.getX(), t.getY());
			}			
			public void instanceRemoved(RemoteObject obj) {
				Terrain t = (Terrain)obj;
				removeObjectAt(t.getX(), t.getY(), t);
				allTerrain.remove(t);
			}		
		});
	}
	
	private void initTile(Tile t) {
		
		class TileHeldListener implements VariableListener {
			Tile t;
			TileHeldListener(Tile t) {
				this.t = t;
			}
			public void variableChanged(byte[] oldValue, byte[] newValue) 
				throws IOException, FederateInternalError {
				boolean nv = Bytes.booleanValue(newValue);
				if (nv) {
					removeObjectAt(t.getX(), t.getY(), t);
					allTiles.remove(t);
					tiles.remove(t);
					fireTileRemoved(t);
				}
			}
		}
		VariableListener v = new TileHeldListener(t);
		PublicVariable isHeld = t.getVariable("isHeld");
		isHeld.addVariableListener(v);
		
		class AgeListener implements VariableListener {
			Tile t;
			AgeListener(Tile t) {
				this.t = t;
			}
			public void variableChanged(byte[] oldValue, byte[] newValue)
				throws IOException, FederateInternalError {
				int nv = Bytes.intValue(newValue);
				if (nv <= 0) {
					removeObjectAt(t.getX(), t.getY(), t);
					allTiles.remove(t);
					tiles.remove(t);
					fireTileRemoved(t);
				}
			}
		}
		VariableListener va = new AgeListener(t);
		PublicVariable age = t.getVariable("leftToLive");
		age.addVariableListener(va);
	}
	
	private void initHole(Hole h) {
		
		class HoleFilledListener implements VariableListener {
			Hole h;
			HoleFilledListener(Hole h) {
				this.h = h;
			}
			public void variableChanged(byte[] oldValue, byte[] newValue) 
				throws IOException, FederateInternalError {
				int newDepth = Bytes.intValue(newValue);
				if (newDepth < 1) {
					removeObjectAt(h.getX(), h.getY(), h);
					allHoles.remove(h);
					holes.remove(h);
					fireHoleRemoved(h);
				}
			}
		}
		VariableListener v = new HoleFilledListener(h);
		PublicVariable depth = h.getVariable("depth");
		depth.addVariableListener(v);
		
		class AgeListener implements VariableListener {
			Hole h;
			AgeListener(Hole h) {
				this.h = h;
			}
			public void variableChanged(byte[] oldValue, byte[] newValue)
				throws IOException, FederateInternalError {
				int nv = Bytes.intValue(newValue);
				if (nv <= 0) {
					removeObjectAt(h.getX(), h.getY(), h);
					allHoles.remove(h);
					holes.remove(h);
					fireHoleRemoved(h);
				}
			}
		}
		VariableListener va = new AgeListener(h);
		PublicVariable age = h.getVariable("leftToLive");
		age.addVariableListener(va);
	}
	
	/**
	*   Steps the environment forward one timestep. Removing
	*   from the board any Tiles or Holes that have died.
	*/
	public void stepEnv() {
		
		if (tileAgeing) {
			for (int i = 0; i < tiles.size(); i++) {
				Tile nextTile = (Tile)tiles.get(i);
				boolean deadOrAlive = nextTile.getOlder();
				if (deadOrAlive == Tile.JUST_DIED) {
					this.removeObjectAt(nextTile.getX(), nextTile.getY(), nextTile);
					tiles.remove(tiles.indexOf(nextTile));
					allTiles.remove(nextTile);
					fireTileRemoved(nextTile);
					i--;
				}
			}
		}
		if (holeAgeing) {
			for (int i = 0; i < holes.size(); i++) {
				Hole nextHole = (Hole)holes.get(i);
				boolean deadOrAlive = nextHole.getOlder();
				if (deadOrAlive == Hole.JUST_DIED) {
					this.removeObjectAt(nextHole.getX(), nextHole.getY(), nextHole);
					holes.remove(holes.indexOf(nextHole));
					allHoles.remove(nextHole);
					fireHoleRemoved(nextHole);
					i--;
				}
			}
		}
		
		ArrayList newTiles = new ArrayList();
		ArrayList newHoles = new ArrayList();
		
		if (tileGeneration) {
			for (int i = 0; i < generationRate; i++) {
				int chance = Random.uniform.nextIntFromTo(0, 100);
				if (chance <= generationRate) {
					int lifespan = Random.uniform.nextIntFromTo(99, maxLifeSpan);
					boolean tilePlaced = false;
					do {				
						int nextX = Random.uniform.nextIntFromTo(0, this.getSizeX() - 1);
						int nextY = Random.uniform.nextIntFromTo(0, this.getSizeY() - 1);
						if (cellEmpty(nextX, nextY)) {
							Tile nextTile = new Tile(nextX, nextY, lifespan, tileAgeing);
							this.putObjectAt(nextX, nextY, nextTile);
							tiles.add(nextTile);
							newTiles.add(nextTile);
							initTile(nextTile);
							tilePlaced = true;
						}
					}
					while (!tilePlaced);
				}
			}
		}	
		
		if (holeGeneration) {
			for (int i = 0; i < generationRate; i++) {
				int chance = Random.uniform.nextIntFromTo(0, 500);
				if (chance <= generationRate) {
					int depth = Random.uniform.nextIntFromTo(1, 10);
					int lifespan = Random.uniform.nextIntFromTo(99, maxLifeSpan);
					boolean holeDug = false;
					do {				
						int nextX = Random.uniform.nextIntFromTo(0, this.getSizeX() - 1);
						int nextY = Random.uniform.nextIntFromTo(0, this.getSizeY() - 1);
						if (cellEmpty(nextX, nextY)) {
							Hole nextHole = 
								new Hole(nextX, nextY, depth, lifespan, holeAgeing);
							this.putObjectAt(nextX, nextY, nextHole);
							holes.add(nextHole);
							newHoles.add(nextHole);
							initHole(nextHole);
							holeDug = true;
						}
					}
					while (!holeDug);
				}
			}
		}
		
		fireAdditions(new TileWorldObjects(newTiles, newHoles));
	}
	
	private ArrayList listeners = new ArrayList();
	
	private void fireAdditions(TileWorldObjects obs) {
		
		for (int i = 0; i < listeners.size(); i++) {
			TileWorldListener listener = (TileWorldListener)listeners.get(i);
			listener.updateEnvironment(obs);
		}
	}
	
	private void fireHoleRemoved(Hole h) {
		
		for (int i = 0; i < listeners.size(); i++)
			((TileWorldListener)listeners.get(i)).holeRemoved(h);
	}
	
	private void fireTileRemoved(Tile t) {
		
		for (int i = 0; i < listeners.size(); i++)
			((TileWorldListener)listeners.get(i)).tileRemoved(t);
	}
	
	public void addTileWorldListener(TileWorldListener t) {
		
		listeners.add(t);
	}
	
	public boolean cellEmpty(int x, int y) {
		
		return (this.getCellAt(x, y) == null || this.getCellAt(x, y).size() == 0);	
	}

	public boolean isTerrain(int x, int y) {
		
		try {
			if (cellEmpty(x, y))
				return false;
		}
		catch (IndexOutOfBoundsException e) {
			System.err.println("out of bounds on cellEmpty test: (" + x + ", " + y + ")");
			throw e;
		}
		Cell c = this.getCellAt(x, y);
		List l = c.getList();
		for (int i = 0; i < l.size(); i++) {
			Object o = l.get(i);
			if (o instanceof Terrain)
				return true;
		}
		return false;
	}


	public void pickupTile(int x, int y, Tile targetTile, Agent agent) 
		throws BadTileCoordsException, NotOnBoardException, TileOwnershipException {
		
		if (!allTiles.contains(targetTile))
			throw new NotOnBoardException();
		if (agent.getTile() != null)
			throw new TileOwnershipException("double pickup attempted");
		if (targetTile.getX() != x || targetTile.getY() != y)
			throw new BadTileCoordsException(	targetTile.getX(), 
																			targetTile.getY(), 
																			targetTile);
		try {
			targetTile.pickup();
			agent.setTile(targetTile);
			allTiles.remove(targetTile);
			tiles.remove(targetTile);
			this.removeObjectAt(x, y, targetTile);
			fireTileRemoved(targetTile);
		}
		catch (ExcludedException e) {
			throw new NotOnBoardException();
		}
	}

	public void fillHole(int x, int y, Tile tile, Hole h, Agent agent) 
		throws BadHoleCoordsException, NotOnBoardException, TileOwnershipException {
		
		if (!allHoles.contains(h))
			throw new NotOnBoardException();
		if (agent.getTile() == null)
			throw new TileOwnershipException("agent is not holding a tile");
		if (agent.getTile() != tile)
			throw new TileOwnershipException("agent is not holding the reported Tile");
		if (h.getX() != x || h.getY() != y || agent.getX() != x || agent.getY() != y)
			throw new BadHoleCoordsException(x, y, h);
		try {
			int fillScore = h.fill(tile);
			if (fillScore > -1) {
				agent.score(h, fillScore);
				this.removeObjectAt(x, y, h);
				allHoles.remove(h);
				holes.remove(h);
			}
			agent.setTile(null);
		}
		catch (RTIexception e) {
			DEV_TOOLS.showException(e);
		}
	}
	
	private void sortCell(int x, int y) {
		
		if (cellEmpty(x, y))
			return;
		List l = this.getCellAt(x, y).getList();
		List n = new ArrayList();
		Class c = null;
		int highestPrio = -1;
		for (int i = 0; i < l.size(); i++) {
			int next = priorityOf(l.get(i));
			if (next >= highestPrio) {
				n.add(l.get(i));
				highestPrio = next;
				c = l.get(i).getClass();
			}
		}
		for (int i = 0; i < l.size(); i++) {
			Object next = l.get(i);
			if (next.getClass() != c) {
				this.removeObjectAt(x, y, next);
				allTiles.remove(next);
				tiles.remove(next);
				allHoles.remove(next);
				holes.remove(next);
				if (next instanceof Agent && model.ownsAgent((Agent)next))
					replaceAgent((Agent)next);
			}
		}
	}
	
	private int priorityOf(Object o)	{
		
		if (o instanceof Terrain)
			return 3;
		if (o instanceof Hole)
			return 2;
		if (o instanceof Tile)
			return 1;
		return 0;
	}
	
	private void replaceAgent(Agent agent) {
		
		boolean placed = false;
		while (!placed) {
			int x = Random.uniform.nextIntFromTo(0, getSizeX() - 1);
			int y = Random.uniform.nextIntFromTo(0, getSizeY() - 1);
			if (!isTerrain(x, y)) {
				this.putObjectAt(x, y, agent);
				agent.setX(x);
				agent.setY(y);
				placed = true;
			}
		}		
	}
}