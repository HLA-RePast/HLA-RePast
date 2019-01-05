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
 * Created on 23-Jan-2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tileworld;

import java.awt.Color;
import java.awt.Point;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import hla.rti13.java1.FederateInternalError;
import hla_past.io.Bytes;
import hla_past.io.RemoteInit;
import hla_past.logging.DBLogger;
import hla_past.logging.StopWatch;
import hla_past.manager.DistributedSchedule;
import hla_past.manager.LocalManager;
import hla_past.object.InstanceListener;
import hla_past.object.PublicVariable;
import hla_past.object.ReflectedList;
import hla_past.object.RemoteObject;
import hla_past.object.VariableListener;
import testrti.DEV_TOOLS;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.ScheduleBase;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.MultiObject2DDisplay;
import uchicago.src.sim.util.Random;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TileWorldModel extends SimModelImpl {

	private TileWorld world;
	private List agents;
	private ReflectedList r_agents;
	
	private int worldSize;
		
	private int numObstacles;
	private int obstacleSize;
	
	private int tileCount = 15;
	private int holeCount = 5;
	private int numAgents;
	private int totalAgents;
	
	private boolean tileAgeing;
	private boolean tileGeneration;
	private boolean holeAgeing;
	private boolean holeGeneration;
	
	private int ageingRate;
	private int generationRate;
		
	private String[] initParam = new String[] { 
														"WorldSize",
														"ObstacleDensity", 
														"ObstacleSize", 
														"TileAgeing",
														"TileGeneration",
														"HoleAgeing",
														"HoleGeneration",
														"AgeingRate",
														"GenerationRate"};
	
	private LocalManager manager;
	private Schedule scedge;
	
	private DisplaySurface dsurf;
	private MultiObject2DDisplay displayGrid;
	
	private long startTime;
	private DBLogger logger = new DBLogger();
	
	public TileWorldModel(LocalManager man, Map options) {
		
		this.manager = man;
		this.addSimEventListener(manager);
		processInput(options);
		System.err.println("after processing input, numAgents = " + numAgents);
	}
	
	public String[] getInitParam() {
		
		return initParam;
	}

	public void begin() {
		
		try {
			buildModel();
		}
		catch (Exception e) {
			DEV_TOOLS.showException(e);
			getController().stopSim();
			getController().exitSim();
		}
		if (xEnv) {
			buildDisplay();
			dsurf.display();
		}
	}
	
	private void buildDisplay() {
		
		displayGrid = new MultiObject2DDisplay(world);
		dsurf.addDisplayableProbeable(displayGrid, "TileWorld");
	}
	
	private void buildModel() throws Exception {
				
		ReflectedList r_tiles = manager.getRemoteObjects(Tile.class, Tile.class);
		ReflectedList r_holes = manager.getRemoteObjects(Hole.class, Hole.class);
		ReflectedList r_terrain = manager.getRemoteObjects(Terrain.class, Terrain.class);
		r_agents = manager.getRemoteObjects(Agent.class, Agent.class);
		initAgentList(r_agents);
		
		if (environment) {
			world =
				new TileWorld(	worldSize, 
											worldSize, 
											numObstacles, 
											obstacleSize, 
											holeCount, 
											tileCount, 
											tileAgeing, 
											holeAgeing, 
											tileGeneration, 
											holeGeneration, 
											ageingRate, 
											generationRate, 
											this, 
											r_tiles,
											r_holes, 
											r_terrain);
		}
		else {
			world =
				new TileWorld(	worldSize, 
											worldSize, 
											0, 
											0, 
											0, 
											0, 
											false,
											false, 
											false, 
											false, 
											0, 
											0, 
											this, 
											r_tiles, 
											r_holes, 
											r_terrain);
		}
		
		List l_tiles = world.getLocalTiles();
		for (int i = 0; i < l_tiles.size(); i++)
			manager.registerPublicObject((Tile)l_tiles.get(i));
		
		List l_holes = world.getLocalHoles();
		for (int i = 0; i < l_holes.size(); i++)
			manager.registerPublicObject((Hole)l_holes.get(i));
		
		List l_terrain = world.getLocalTerrain();
		for (int i = 0; i < l_terrain.size(); i++)
			manager.registerPublicObject((Terrain)l_terrain.get(i));
			
		world.addTileWorldListener(new TileWorldListener() {
			public void updateEnvironment(TileWorldObjects obs) {
				List holes = obs.getNewHoles();
				for (int i = 0; i < holes.size(); i++) {
					try {
						Hole h = (Hole)holes.get(i);
						manager.registerPublicObject(h);
						h.refresh();
					}
					catch (Exception e) {
						DEV_TOOLS.showException(e);
					}
				}
				List tiles = obs.getNewTiles();
				for (int i = 0; i < tiles.size(); i++) {
					try {
						Tile t = (Tile)tiles.get(i);
						manager.registerPublicObject(t);
						t.refresh();
					}
					catch (Exception e) {
						DEV_TOOLS.showException(e);
					}
				}
			}
			public void tileRemoved(Tile t) {}
			public void holeRemoved(Hole h) {}			
		});
		
		Random.createUniform();
		
		System.out.println("just before populating agents, numAgents = " + numAgents);
		agents = new ArrayList();
		for (int i = 0; i < numAgents; i++) {
			System.out.println("trying to place agent");
			boolean agentPlaced = false;
			do {
				int xPos = Random.uniform.nextIntFromTo(0, world.getSizeX() - 1);
				int yPos = Random.uniform.nextIntFromTo(0, world.getSizeY() - 1);
				if (world.cellEmpty(xPos, yPos)) {
					int boldness = (int)((Math.random() - 0.5) * 10);
					Agent a = 
						new Agent(xPos, yPos, world, boldness * 2, boldness, Color.RED);
					world.putObjectAt(xPos, yPos, a);
					this.agents.add(a);
					manager.registerPublicObject(a);
					agentPlaced = true;
				}
			} while (!agentPlaced);
		}
	}
	
	private void initAgentList(ReflectedList agents) {
		
		agents.registerInstanceListener(new InstanceListener() {

			public void instanceAdded(RemoteObject obj) {
				initAgent((Agent)obj);
			}

			public void instanceRemoved(RemoteObject obj) {
				Agent a = (Agent)obj;
				world.removeObjectAt(a.getX(), a.getY(), a);
			}			
		});
		
		for (int i = 0; i < agents.getInstances().size(); i++)
			initAgent((Agent)agents.getInstances().get(i));
	}
	
	private void initAgent(Agent a) {
		
		class PosListener implements VariableListener {
			Agent a;
			PosListener(Agent a) {
				this.a = a;				
			}
			public void variableChanged(byte[] oldValue, byte[] newValue) 
				throws IOException, FederateInternalError {
				Point old = (Point)Bytes.objectValue(oldValue);
				Point newP = (Point)Bytes.objectValue(newValue);
				world.removeObjectAt((int)old.getX(), (int)old.getY(), a);
				world.putObjectAt((int)newP.getX(), (int)newP.getY(), a);
			}			
		}
		PublicVariable var = a.getVariable("position");	
		var.addVariableListener(new PosListener(a));	
		world.putObjectAt(a.getX(), a.getY(), a); 
	}
	
	public void setup() {
		
		try {
			scedge = populateSchedule(manager.getSchedule());
		}		
		catch (Exception e) {
			DEV_TOOLS.showException(e);
		}	
		if (dsurf != null)
			dsurf.dispose();
		if (xEnv)
			dsurf = new DisplaySurface(this, "TileWorld");		
	}
	
	private Schedule populateSchedule(DistributedSchedule ds) {
		
		ds.scheduleActionAt(1, new BasicAction() {
			public void execute() {
				startTime = System.currentTimeMillis();
			}
		});
		
		ds.scheduleActionAt(endTick, new BasicAction() {
			public void execute() {
				double stopTime = (System.currentTimeMillis() - startTime) / 1000;
				System.out.println("Total execution time <parallel> = " + stopTime);
				
				StopWatch hlaTime = DistributedSchedule.hlaTimer;
				StopWatch repastTime = DistributedSchedule.repastTimer;
				
				if (master) {
					try {
						logger.open();
						logger.logRun((int)stopTime, federationSize, worldSize, generationRate, ageingRate);
						logger.close();
					}	
					catch (Exception e) {
						e.printStackTrace();			
					}
				}
				else {
					try {
						logger.open();
						logger.logRelativeTimes(repastTime, hlaTime, numAgents, federationSize, generationRate, ageingRate);
						logger.close();
					}
					catch (Exception e) {
						e.printStackTrace();
					}
				}
				manager.endParticipation(master);
				TileWorldModel.this.removeSimEventListener(manager);
				getController().stopSim();
				getController().exitSim();
				//normally you can just do getController.stopSim() but for SGE runs
				//we need to ensure that a specific federate (the master) is the 'last
				//out the door' as it must explicitly delete the RTI from the grid
			}
		}, ScheduleBase.LAST);
		
		if (xEnv) {
			ds.addDisplayAction(new BasicAction() {
				public void execute() {
					dsurf.updateDisplay();
				}
			});
		}
		
		//one-time refresh...
		ds.scheduleActionAt(0, new BasicAction() {
			public void execute() {
				List tiles = world.getLocalTiles();
				List holes = world.getLocalHoles();
				List terrain = world.getLocalTerrain();
				for (int i = 0; i < tiles.size(); i++) {
					try {
						((Tile)tiles.get(i)).refresh();
					}
					catch (Exception e) {
						DEV_TOOLS.showException(e);
					}
				}
				for (int i = 0; i < holes.size(); i++) {
					try {
						((Hole)holes.get(i)).refresh();					
					}
					catch (Exception e) {
						DEV_TOOLS.showException(e);
					}
				}
				for (int i = 0; i < terrain.size(); i++) {
					try {
						((Terrain)terrain.get(i)).refresh();
					}
					catch (Exception e) {
						DEV_TOOLS.showException(e);
					}
				}
			}
		});
		
		//agent moves
		ds.scheduleActionAtInterval(1.0, new BasicAction() {
			public void execute() {
				for (int i = 0; i < agents.size(); i++)
					((Agent)agents.get(i)).step();
			}
		});
		
		//object ageing...
		ds.scheduleActionAtInterval(1.0, new BasicAction() {
			public void execute() {
				world.stepEnv();
			}
		});
				
		return ds;
	}


	public Schedule getSchedule() {
		
		return scedge;
	}


	public String getName() {
		
		return "TileWorld";
	}

	public static void main(String[] args) throws Exception {
		
		checkInput(args);
		Map optionsMap = packageInput(args);
		testMap(optionsMap);
		
		//these are the only two options we are concerned about in main()
		//the rest can be processed by the constructor
		int federation_size = ((Integer)optionsMap.get("-fed_size")).intValue();
		boolean xEnv = false;
		if (optionsMap.containsKey("-x"))
			xEnv = true;
			
		SimInit init = new SimInit();
		Class[] classes = new Class[] {Tile.class, Hole.class, Terrain.class, Agent.class};
		
		LocalManager man = 
			new LocalManager(classes, federation_size, "TileWorld", "TileWorld.fed");
		TileWorldModel mod = new TileWorldModel(man, optionsMap);
		
		String filename = null;
		if (!xEnv) {
			filename = 
				"./params/agents.params";
			init = new RemoteInit();
		}
		else
			filename = "./params/env.params";
		
		init.loadModel(mod, filename, true);
	}
	
	boolean xEnv = false;
	int federationSize = 2;
	int endTick = 0;
	boolean environment = false;
	boolean master = false;
	
	private static Map packageInput(String[] args) {
		
		Map m = new HashMap();
		
		for (int i = 0; i < args.length; i++) {
			
			if (args[i].equals("-fed_size")) {
				m.put(args[i], new Integer(Integer.parseInt(args[i + 1])));
				i++;				
			}
			else if (args[i].equals("-ticks")) {
				m.put(args[i], new Integer(Integer.parseInt(args[i + 1])));
				i++;				
			}
			else if (args[i].equals("-x") || args[i].equals("-env") || args[i].equals("-master")) {
				m.put(args[i], new Object());
			}
			else if (args[i].equals("-agents")) {
				m.put(args[i], new Integer(Integer.parseInt(args[i + 1])));
				i++;
			}
		}
		
		return m;
	}
	
	private static void testMap(Map m) {
		
		if (m.containsKey("-x"))
			System.err.println("using graphical environment");
		else
			System.err.println("no graphical environment");
		if (m.containsKey("-env"))
			System.err.println("environment federate");
		else
			System.err.println("agent federate");
		if (m.containsKey("-master"))
			System.err.println("master federate");
		else
			System.err.println("slave federate");
		if (m.containsKey("-fed_size")) 
			System.err.println("federation size: " + ((Integer)m.get("-fed_size")).intValue());
		else
			System.err.println("no federation size found");		
		if (m.containsKey("-ticks"))
			System.err.println("ticks: " + ((Integer)m.get("-ticks")).intValue());
		else
			System.err.println("no tick-count found");
		if (m.containsKey("-agents"))
			System.err.println("agents: " + ((Integer)m.get("-agents")).intValue());
		else
			System.err.println("no agent-count found");
	}
	
	private void processInput(Map m) {
		
		if (m.containsKey("-x"))
			xEnv = true;
		if (m.containsKey("-env"))
			environment = true;
		if (m.containsKey("-master"))
			master = true;
		if (m.containsKey("-fed_size")) {
			federationSize = ((Integer)m.get("-fed_size")).intValue();
		}
		if (m.containsKey("-ticks")) {
			endTick = ((Integer)m.get("-ticks")).intValue();
		}
		if (m.containsKey("-agents")) {
			this.numAgents = ((Integer)m.get("-agents")).intValue();
		}
	}
	
	private static void checkInput(String[] args) {
		
		for (int i = 0; i < args.length; i++) {			
			try {
				if (args[i].equals("-fed_size")) {
					i++;
					Integer.parseInt(args[i]);
				}
				else if (args[i].equals("-ticks")) {
					i++;
					Integer.parseInt(args[i]);
				}
				else if (args[i].equals("-agents")) {
					i++;
					Integer.parseInt(args[i]);
				}
				else if (!(args[i].equals("-x") || args[i].equals("-env") || args[i].equals("-master"))) {
					System.err.println("unexpected token " + args[i] + " inbetween " + args[i-1] + " and " + args[i+1]);
					printUsage();
					System.exit(1);
				}
			}
			catch (NumberFormatException e) {
				System.err.println("Found character '" + args[i] + "' expected integer");
				printUsage();
			}
		}
		

	}
	
	private static void printUsage() {
		
		System.err.println(	"Usage: \n" +
										"-x: graphical environment available \n" +
										"-fed_size <num>: number of federates participating \n" +
										"-agents <num>: number of agents to be modelled \n" +
										"-ticks <num>: number of ticks in this execution \n" +
										"-env: designate this federate to simulate tileworld environment \n" +
										"-master: designate this federate as the federation master"
										);
	}

	public boolean ownsAgent(Agent a) {
		
		return this.agents.contains(a);
	}
	
	public int getNumAgents() {
		
		return numAgents;
	}
	
	public void setNumAgents(int agents) {
		
		this.numAgents = agents;
	}
	
	public int getObstacleDensity() {
		
		return this.numObstacles;
	}
	
	public void setObstacleDensity(int density) {
		
		this.numObstacles = density;
	}
	
	public int getObstacleSize() {
		
		return this.obstacleSize;
	}
	
	public void setObstacleSize(int size) {
		
		this.obstacleSize = size;
	}	
	
	public boolean getTileAgeing() {
		
		return this.tileAgeing;
	}
	
	public void setTileAgeing(boolean tileAgeing) {
		
		this.tileAgeing = tileAgeing;
	}
	
	public boolean getTileGeneration() {
		
		return this.tileGeneration;
	}
	
	public void setTileGeneration(boolean tileGeneration) {
		
		this.tileGeneration = tileGeneration;
	}
	
	public boolean getHoleAgeing() {
		
		return this.holeAgeing;
	}
	
	public void setHoleAgeing(boolean holeAgeing) {
		
		this.holeAgeing = holeAgeing;
	}
	
	public boolean getHoleGeneration() {
		
		return this.holeGeneration;
	}
	
	public void setHoleGeneration(boolean holeGeneration) {
		
		this.holeGeneration = holeGeneration;
	}
	
	public void setAgeingRate(int rate) {
		
		this.ageingRate = rate;
	}
	
	public int getAgeingRate() {
		
		return this.ageingRate;
	}
	
	public void setGenerationRate(int rate) {
		
		this.generationRate = rate;
	}
	
	public int getGenerationRate() {
		
		return this.generationRate;
	}
	
	public int getWorldSize() {
		
		return worldSize;
	}
	
	public void setWorldSize(int size) {
		
		this.worldSize = size;
	}	

}
