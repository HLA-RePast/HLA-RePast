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

import java.awt.*;

/**
*   The interface required for any object which will be registered with an 
*   Agent and communicate with an ExecutionEngine to enable the motivation and
*   animation of a TileWorld agent. To create an Agent with a fully functioning
*   Think-Act cycle something similar to the following code will be used:<p>
*   <code>
*   Agent agent = new Agent(xValue, yValue, myTileWorld);<br>
*   AgentBrain brain = new AgentBrainImpl(agent, myTileWorld);<br>
*   ExecutionEngine engine = new ExecutionEngineImpl(agent, myTileWorld);<br>
*   brain.registerExecutionEngine(engine);<br>
*   agent.registerAgentBrain(brain);<br>
*   agent.registerExecutionEngine(engine);<br>
*   </code><p>
*   It is largely a matter of good-practice to register the brain with the engine
*   before they are individually registered with the agent, this is a means of
*   preventing an agent-without-a-brain or an agent-without-a-body.
*   It is, of course perfectly concievable that subclasses will hide registration
*   calls in their constructors (for example the constructor for an AgentBrain
*   could take and ExecutionEngine as its argument and hide the call to<p>
*   <code>
*   registerExecutionEngine(this);
*   </code>
*/
public interface AgentBrain extends TileWorldListener {
	
	/**
	*   Updates the information held by the brain about objects
	*   in the TileWorld for which this brain is registered. This is largely
	*   a labour-saving device which means that implementations of AgentBrain
	*   can be sure they are holding up-to-date information about Holes and Tiles
	*   provided that informationis updated upon receipt of this invocation. If
	*   this structure is not used then it will be necessary for the Brain to 
	*   check the state of the TileWorld every time it needs to make a decision.
	*   @param obs a pair of lists of the new TileWorld objects (tiles and holes)
	*   that have appeared in the TileWorld for which this brain is registered
	*/
	public void updateEnvironment(TileWorldObjects obs);
	
	/**
	*   Registers an ExecutionEngine, controlling an Agent which will perform
	*   intentions produced by this Brain
	*   @param engine the execution engine which will control the agent for
	*   which this Brain is thinking
	*/	
	public void registerExecutionEngine(ExecutionEngine engine);
		
	/**
	*   Invokes the production of the next command by the Brain.
	*   This will cause the registered execution engine to be issued
	*   the next command. This command will usually be invoked by
	*   the executionEngine itself in a call-back issued after the 
	*   current command is completed.
	*/
	public void produceNextIntention();
	
	/**
	 * Provides the brain with a thread with which to do some amount of
	 * deliberation/cognition etc... Clearly this is only required by brains which
	 * do not run on their own thread, those that do can simply provide an
	 * empty implementation of this method.
	 */
	public void think();
	
	/**
	*   Notifies the Brain that a hole has been filled by the agent to which
	*   it is registered.
	*   @param h the Hole that was filled
	*/
	public void notifyHoleFill(Hole h);
	
	
	/**
	*   Produces a valid Route between two points which can then
	*   be used by an ExecutionEngine. This method will usually
	*   be invoked as a call-back by the ExecutionEngine if a 
	*   Route previously provided by the Brain is found to be incorrect
	*   @param p1 the origin point of the Route
	*   @param p2 the destination point of the Route.
	*/
	public Route getRouteFromTo(Point p1, Point p2);
}
