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

import hla_past.object.PublicObject;
import hla_past.object.PublicVariable;
import hla_past.object.RemoteObject;
import hla_past.object.ViewableInteger;
import hla_past.object.ViewablePoint;

import java.awt.*;
import java.util.ArrayList;
import java.util.Hashtable;
import uchicago.src.sim.gui.*;
import uchicago.src.sim.space.*;
import uchicago.src.reflector.*;

/**
*   A partial implementation of a TileWorld Agent. The thinking and acting
*   modules of the Agent are un-implemented and can house different implementations
*   of the AgentBrain and ExecutionEngine classes which can be experimentally
*   registered with the agent through its lifetime. The basic form, without using
*   the registration methods is nothing more than a simple agent which does nothing
*   without being explicitly told to do so.
*/
public class Agent extends PublicObject
	implements 	TileWorldListener, 
							Drawable, 
							SpatialObject, 
							DescriptorContainer,
							RemoteObject {
	
	//private int x;
	//private int y;
	private ViewablePoint position;
	private String[] vars = new String[] {"position", "currentScore"};
	
	private TileWorld world;
	private Tile heldTile;
	private ExecutionEngine engine;
	private AgentBrain brain;
	//private int currentScore;
	private ViewableInteger currentScore;
	private Color color;
	
	private Hashtable descriptors;
	
	protected static int DEFAULT_CALC_TOLERANCE = 10;
	protected static int DEFAULT_FILTER_THRESHOLD = 3;
	protected static Color DEFAULT_AGENT_COLOR = Color.RED;
	
	public Agent() {
		
		this.color = Color.RED;
		this.position = new ViewablePoint(null);
		currentScore = new ViewableInteger(0);
	}
	
	/**
	*   Creates a new Agent with the initial x and y positions in the given TileWorld.
	*   Note that the Agent is not placed in the TileWorld with this constructor, 
	*   this must be performed manually. Using this constructor the implementations of
	*   AgentBrain and ExecutionEngine used are the ThreadedBrainImpl and ExecutionEngineImpl
	*   respectively.
	*   @param x the initial x-position of this Agent
	*   @param y the initial y-position of this Agent
	*   @param world the TileWorld in which this Agent exists
	*   @param calculationTolerance the tolerance with which the brain used in this agent
	*   will embark on lengthy calculations rather than settling for the status quo. A high
	*   tolerance will mean an agent that rarely searches for better options than the current
	*   one but as a result can often act more quickly.
	*   @param filterThreshold the threshold that the brain in this agent will use to determine
	*   whether a newly proposed goal is worth analysing or not. A high threshold will result in
	*   an agent that rarely changes its current goal, even when better ones are available, 
	*   whilst a low threshold will result in one which is constantly flitting from one goal to 
	*   another, often leaving achievable goals incomplete.
	*/
	public Agent(int x, int y, TileWorld world, int calculationTolerance, int filterThreshold) {
		
		//this.x = x;
		//this.y = y;
		position = new ViewablePoint(x, y);
		this.world = world;
		heldTile = null;
		//currentScore = 0;
		currentScore = new ViewableInteger(0);
		this.brain = 
			//new ThreadedBrainImpl(this, world, calculationTolerance, filterThreshold);
			new DeliberativeBrain(this, world, calculationTolerance, filterThreshold);
		this.engine = 
			new ExecutionEngineImpl(this, world, brain);
		this.color = DEFAULT_AGENT_COLOR;
		
		this.descriptors = new Hashtable();
		BooleanPropertyDescriptor holdingTile = 
			new BooleanPropertyDescriptor("HoldingTile", false);
		//descriptors.put("HoldingTile", holdingTile);
	}
	
	/**
	*   Creates a new Agent with the initial x and y positions in the given TileWorld.
	*   Note that the Agent is not placed in the TileWorld with this constructor, 
	*   this must be performed manually. Using this constructor the implementations of
	*   AgentBrain and ExecutionEngine used are the ThreadedBrainImpl and ExecutionEngineImpl
	*   respectively.
	*   @param x the initial x-position of this Agent
	*   @param y the initial y-position of this Agent
	*   @param world the TileWorld in which this Agent exists
	*   @param calculationTolerance the tolerance with which the brain used in this agent
	*   will embark on lengthy calculations rather than settling for the status quo. A high
	*   tolerance will mean an agent that rarely searches for better options than the current
	*   one but as a result can often act more quickly.
	*   @param filterThreshold the threshold that the brain in this agent will use to determine
	*   whether a newly proposed goal is worth analysing or not. A high threshold will result in
	*   an agent that rarely changes its current goal, even when better ones are available, 
	*   whilst a low threshold will result in one which is constantly flitting from one goal to 
	*   another, often leaving achievable goals incomplete.
	*   @param color the java.awt.Color used to paint the agent to the screen (useful for experimenting
	*   with various boldness settings)
	*/
	public Agent(int x, int y, TileWorld world, int calculationTolerance, int filterThreshold, Color color) {
		
		this(x, y, world, calculationTolerance, filterThreshold);
		this.color = color;
	}
	
	/**
	*   Creates a new Agent with the initial x and y positions in the given TileWorld.
	*   Note that the Agent is not placed in the TileWorld with this constructor,
	*   this must be performed manually. Using this constructor the implementations of 
	*   AgentBrain and ExecutionEngine used aree the ThreadedBrainImpl and ExecutionEngineImpl
	*   repsectively. Sensible defaults are used for calculationTolerance and filterThreshold.
	*   @param x the initial x-position of this Agent
	*   @param y the initial y-position of this Agent
	*   @param world the TileWorld in which this Agent exists
	*/	
	public Agent(int x, int y, TileWorld world) {
		
		this(x, y, world, DEFAULT_CALC_TOLERANCE, DEFAULT_FILTER_THRESHOLD);
	}
	
	/**
	*   Creates a new Agent with the initial x and y positions in the given TileWorld.
	*   Note that the Agent is not placed in the TileWorld with this constructor,
	*   this must be performed manually. The Agent thinks using the specified brain and
	*   acts according to the commands of the specified engine.
	*   @param x the initial x-position of this Agent
	*   @param y the initial y-position of this Agent
	*   @param world the TileWorld in which this Agent exists
	*   @param brain the AgentBrain to be used for the agent to think
	*   @param engine the Engine to be used for the agent act.
	*/
	public Agent(int x, int y, TileWorld world, AgentBrain brain, ExecutionEngine engine) {
		
		//this.x = x;
		//this.y = y;
		this.position = new ViewablePoint(x, y);
		this.world = world;
		heldTile = null;
		//currentScore = 0;
		currentScore = new ViewableInteger(0);
		this.brain = brain;
		this.engine = engine;
		this.color = DEFAULT_AGENT_COLOR;
		
		this.descriptors = new Hashtable();		
		BooleanPropertyDescriptor holdingTile = new BooleanPropertyDescriptor("HoldingTile", false);
		//descriptors.put("Holding Tile", holdingTile);
	}
	
	/**
	*   Registers the AgentBrain which will be used to think for this agent.
	*   It is not vital for an Agent to have a registered AgentBrain, the thinking
	*   can be done by some purpose-written algorithm outside of the Agent code
	*   however, for TileWorld-style Agent the Brain-Engine system is the most
	*   appropriate and provides a simple interface.
	*   @param brain the AgentBrain to be used
	*/
	public void registerAgentBrain(AgentBrain brain) {
		
		this.brain = brain;
		brain.updateEnvironment(new TileWorldObjects(world.getTiles(), world.getHoles()));
		if (engine != null)
			brain.registerExecutionEngine(engine);
	}
	
	/**
	*   Registers the ExecutionEngine which will be used to control the 
	*   actions of this Agent. It is not vital for an Agent to have a registered
	*   ExecutionEngine, the moving can be done using the get- and set- methods
	*   provided however, for a TileWorld-style Agents the Brain-Engine system is
	*   the most appropriate and the API provides a simple interface for its use.
	*   @param engine the ExecutionEngine to be used. This should be 
	*   registered with a given AgentBrain before it is registered to the 
	*   Agent.	
	*/
	public void registerExecutionEngine(ExecutionEngine engine) {
		
		this.engine = engine;
		if (brain != null)
			brain.registerExecutionEngine(engine);
	}
	
	//TileWorldListener interface
	
	/**
	*   Issues an update of the environment meant for perception by the 
	*   AgentBrain owned by this agent. This method should be overridden
	*   by certain implementations of the AgentBrain as it currently 
	*   does nothing more than call the same method in the brain with obs
	*   as the argument.
	*   @param obs A TileWorldObjects struct containing a list of tiles and
	*   a list of holes. The lists should only contain <i>new</i> holes and 
	*   <i>new</i> tiles, not already existing ones.
	*/
	public void updateEnvironment(TileWorldObjects obs) {
		
		if (brain != null)
			brain.updateEnvironment(obs);
	}
	
	public void holeRemoved(Hole h) {
		
		if (brain != null)
			brain.holeRemoved(h);
	}

	public void tileRemoved(Tile t) {

		if (brain != null)
			brain.tileRemoved(t);
	}
	
	/**
	*   Asks the agent to perform a step. The call does nothing more than 
	*   grants permission for the ExecutionEngine provided with the constructor 
	*   to perform the next alotted action. In multi-threaded implementations
	*   of AgentBrain (such as the one supplied with ThreadedBrainImpl) this
	*   may result in the agent doing nothing, this will happen in the situation
	*   in which the ExecutionEngine has completed the last command it recieved 
	*   from the Brain but, because the brain is performing a long computation to
	*   find the correct next intention, the engine has no action ready to 
	*   perform. In a single-threaded implementation of brain and engine this
	*   method call will always block until the brain has delivered the next 
	*   intention and it has been performed by the engine.
	*/
	public void step() {
		
		if (engine != null)
			engine.act();
	}	
	
	//utility methods and interface implementations
	
	/**
	*   The current x-position of this Agent
	*   @return the x-position
	*/
	public int getX() {
		
		//return this.x;
		return (int)position.get().getX();
	}
	
	/**
	*   Sets the current x-position of this agent
	*   @param x the new x-position of the agent
	*/
	public void setX(int x) {
		
		//this.x = x;
		try {
			position.set(new Point(x, (int)position.get().getY()));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}	
	
	/**
	*   The current y-position of this agent
	*   @return the y-position
	*/
	public int getY() {
		
		//return this.y;
		return (int)position.get().getY();
	}
	
	/**
	*   Sets the current y-position of this agent
	*   @param y the new y-position of the agent
	*/
	public void setY(int y) {
		
		//this.y = y;
		try {
			position.set(new Point((int)position.get().getX(), y));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	*   Draws the agent on a repast Displayable
	*   @param g the SimGraphics which will perform the draw operation
	*/
	public void draw(SimGraphics g) {
				
		g.drawFastRect(this.color);
		g.setFont(new Font("Monospaced", Font.BOLD, 12));
		g.drawString("" + currentScore.get(), Color.BLACK);
		if (heldTile != null)
			g.drawHollowFastRect(Color.GREEN);
	}	
	
	/**
	 * Sets the current position of the agent, the agents actual position in any space 
	 * must also be set as this is not atomic
	 * @param x the new x position of the agent
	 * @param y the new y position of the agent
	 */
	public void setPosition(int x, int y) {
		
		//this.x = x;
		//this.y = y;
		try {
			position.set(new Point(x, y));
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public void setPosition(Point p) {
		
		//this.setPosition((int)p.getX(), (int)p.getY());
		try {
			position.set(p);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	*   Sets the tile currently held by the agent
	*   @param the Tile just picked up
	*/
	public void setTile(Tile t) {
		
		this.heldTile = t;		
	}
	
	/**
	*   Gets the tile currently held by the agent
	*   @return the Tile held by the agent (can be null)
	*/
	public Tile getTile() {
		
		return this.heldTile;
	}
	
	/**
	*   Adds the given score to the current score for the agent
	*   @param the amount to add to the score
	*/
	public void score(Hole h, int amount) {
		
		//this.currentScore += amount;
		try {
			currentScore.set(currentScore.get() + amount);
			brain.notifyHoleFill(h);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public boolean isHoldingTile() {
		
		if (heldTile != null)
			return true;
		else
			return false;
	}
	
	public Hashtable getParameterDescriptors() {
		
		return this.descriptors;
	}

	public String[] getPublicVariables() {
		
		return vars;
	}

	public PublicVariable getVariable(String varName) {
		if (varName.equals("position"))
			return position;
		else
			return currentScore;
	}


}
