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
 * Created on 11-Dec-2003
 */
package testrti;

import io.DEV_TOOLS;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import exceptions.ExcludedException;

import manager.DistributedSchedule;
import manager.LocalManager;
import models.BasicTestObject;
import models.ProxyBasicTestObject;
import object.InstanceListener;
import object.ProxyList;
import object.RemoteObject;
import uchicago.src.sim.engine.BasicAction;
import uchicago.src.sim.engine.Schedule;
import uchicago.src.sim.engine.SimInit;
import uchicago.src.sim.engine.SimModelImpl;
import uchicago.src.sim.gui.DisplaySurface;
import uchicago.src.sim.gui.Object2DDisplay;
import uchicago.src.sim.space.Object2DGrid;

/**
 * @author Rob Minson
 */
public class BasicTest extends SimModelImpl {

	private static final int COUNTRIES_PER_FEDERATE = 1;

	//hla_repast objects
	LocalManager man;
	Schedule scedge;
	int iterations;	

	//simulation objects
	List<BasicTestObject> localCountries = new ArrayList<BasicTestObject>();
	ProxyList remoteCountries;
	Object2DGrid grid;
	int nextFreeRow = 0;
	Object2DDisplay gridDisplay;
	DisplaySurface dsurf;
	
	
	//utilities
	Random gen = new Random();
	long timer;
		
	public BasicTest(LocalManager man, int iterations, int countryCount) {	
		this.man = man;
		LocalManager.setLazyDeletion(true);
		this.addSimEventListener(man);
		this.iterations = iterations;			
		
		try {	
			grid = new Object2DGrid(1, countryCount * 2);		
			for (; nextFreeRow < countryCount; nextFreeRow++) {
				/* create, register and prepare to display local objects */
				BasicTestObject newCountry = new BasicTestObject(200, 1, 2, 0, nextFreeRow);
				localCountries.add(newCountry);
				grid.putObjectAt(newCountry.getX(), newCountry.getY(), newCountry);					
				man.registerPublicObject(newCountry);
				
				remoteCountries = man.getProxies(	BasicTestObject.class, 
													ProxyBasicTestObject.class);
				remoteCountries.registerInstanceListener(new InstanceListener() {
					public void instanceAdded(RemoteObject obj) {
						System.out.println("<BasicTest::$1InstanceListener> instance added, putting in grid at " + nextFreeRow);
						BasicTestObject newob = (BasicTestObject)obj;
						BasicTest.this.grid.putObjectAt(0, nextFreeRow++, newob);
					}

					public void instanceRemoved(RemoteObject obj) {
						for (int row = 0; row < grid.getSizeY(); row++) {
							if (grid.getObjectAt(0, row) == obj)
								grid.putObjectAt(0, row, null);
						}
					}					
				});
			}
		}
		catch (Exception e) {
			DEV_TOOLS.showException(e);
		}
	}
	
	public BasicTest(LocalManager man, int countryCount) {		
		this(man, 1000, countryCount);
	}

	public String[] getInitParam() {	
		return new String[] {};
	}

	public void begin() {	
		System.out.println("begin()");
		gridDisplay = new Object2DDisplay(grid);
		dsurf = new DisplaySurface(this, man.getFederateID());
		dsurf.addDisplayable(gridDisplay, "countries");
		dsurf.display();
	}

	public static int RACE_LOST = 0;
	
	public void setup() {	
		
		System.out.println("setup()");		
		try {
			DistributedSchedule disScedge = man.getSchedule();			
			this.scedge = disScedge;
			
//			/*
//			 * 1) 	Discover the remote objects which have been registered by the other
//			 * 		nodes. In order to ensure each object we registered is discoverable, 
//			 * 		we have to make sure it has a set of initial values, so first we 
//			 * 		issue an initial update to of the values of each object. Then we ask
//			 * 		for the list of RemoteObjects (note this is a pure race condition as
//			 * 		there is no checkpointing between initial update and discovery steps,
//			 * 		but we don't really need this level of coordination for a simple test). 
//			 */
//			scedge.scheduleActionAt(1.0, new BasicAction() {
//				public void execute() {
//					try {
//						for (int i = 0; i < localCountries.size(); i++) {
//							((BasicTestObject)localCountries.get(i)).refresh();
//						}
//						
////						remoteCountries = man.getRemoteObjects(Country.class, Country.class);
//						remoteCountries = man.getProxies(BasicTestObject.class, BasicTestObject.class);
//					}
//					catch (Exception e) {
//						e.printStackTrace();
//					}
//				}
//			});
			
			/*
			 * 2) 	Cycle through the three variables you could update, trying to 
			 * 		modify the appropriate variable of *all* objects (locals and 
			 * 		proxies) each tick.
			 */
			scedge.scheduleActionAtInterval(1, new BasicAction() {
				/* a labour-saving class */
				abstract class Call { public abstract void invoke(BasicTestObject o); }
				public void execute() {
					System.out.println("\n***** TICK " + man.getTick() + " ******\n");
					Call c = null;
					switch ((int)man.getTick() % 3) {
					case 0:
						/* try to update exclusive variable (race condition) */
						c = new Call() {
							public void invoke(BasicTestObject o) {
								try {
									System.out.println("setting exclusive @ tick " + man.getTick());
									o.setExclusive(o.getCumulative() + 10);	
								}
								catch (ExcludedException e) {
//									System.out.println("\tEXCLUDED @ " + man.getTick());
								}
								catch (Exception e) { e.printStackTrace(); }								
							}
						};
						break;
					case 1:
						/* update cumulative variable (always succeeds eventually) */
						c = new Call() {
							public void invoke(BasicTestObject o) {
								try {
									System.out.println("<BasicTest::execute> setting cumulative @ tick " + man.getTick());
									o.setCumulative(10);
								} catch (Exception e) { e.printStackTrace(); }
							}
						};
						break;
					case 2:
						/* try to update viewable variable (succeeds for locals, not for proxies) */
						c = new Call() {
							public void invoke(BasicTestObject o) {
								try {
									System.out.println("setting viewable @ tick " + man.getTick());
									o.setViewable(o.getViewable() + 10);
								} catch (Exception e) { e.printStackTrace(); }
							}
						};
						break;
					default:
						break;
					}
					
					assert (c != null) : "c is null at tick " + man.getTick();
					for (BasicTestObject ob : localCountries) {
						c.invoke(ob);
					}
					for (RemoteObject ob : remoteCountries.getProxies()) {
						c.invoke((BasicTestObject)ob);
					}
				}
			});
			
			
			/*
			 * 3) 	The event which displays the state of local and remote objects when
			 * 		called. By using addDisplayAction instead of the usual 
			 * 		scheduleActionAt style call, this event is ensured to execute at the
			 * 		'end' of every tick (in practice this is actually once the local 
			 * 		federate has been permitted to advance to the next tick, but before 
			 * 		any remote updates for the next tick have been applied to the state).
			 */
			disScedge.addDisplayAction(new BasicAction() {
				public void execute() {
					if (remoteCountries == null)
						return;
					System.out.println("********** DISPLAY STEP **********");
										
//					for (int i = 0; i < remoteCountries.getInstances().size(); i++) {
//						Country nextCountry = (Country)remoteCountries.getInstances().get(i);
					for (RemoteObject ro : remoteCountries.getProxies()) {
						BasicTestObject nextCountry = (BasicTestObject)ro;
						System.out.println("remote country pop = " + nextCountry.getExclusive() + " @ step " + scedge.getCurrentTime() + " (isOwned = " + nextCountry.isOwned() + ")");
					}
					for (int i = 0; i < localCountries.size(); i++) {
						BasicTestObject nextCountry = (BasicTestObject)localCountries.get(i);
						System.out.println("local country " + i + " pop " + nextCountry.getExclusive() + " @ step " + scedge.getCurrentTime() + " (isOwned = " + nextCountry.isOwned() + ")");
					}
					System.out.println("*******************");
					dsurf.updateDisplay();
				}
			});
			
			
			/*
			 * 4)	End the simulation at a specified number of ticks. The exitSim event
			 * 		on a hla_repast controller will gracefully resign the federation execution.
			 */
			scedge.scheduleActionAt((double)iterations, new BasicAction() {
				public void execute() {
					timer = (System.currentTimeMillis() - timer) / 1000;
					System.out.println(iterations + " steps complete in " + timer + " seconds");
					stop();
					DEV_TOOLS.stopRead(RACE_LOST + " RACES LOST!    Enter to destroy --> *");
					getController().exitSim();
				}
			});
			
			scedge.scheduleActionAt(0.0, new BasicAction() {
				public void execute() {
					timer = System.currentTimeMillis();
				}
			});
			
			/*
			scedge.scheduleActionAtInterval(gen.nextInt(19) + 1, new BasicAction() {
				public void execute() {
					try {
						Country newCountry = new Country((int)(Math.random() * 1000));
						man.registerPublicObject(newCountry);
						countries.add(newCountry);
					}
					catch (Exception e) {
						DEV_TOOLS_TOOLS.showException(e);
					}
				}
			});
			*/						
		}
		catch (Exception e) {
			DEV_TOOLS.showException(e);
		}
	}

	public Schedule getSchedule() {
	
		return scedge;
	}

	public String getName() {
	
		return "Bootstrap model";
	}
	
	public static void main(String[] args) throws Exception {
		
		try {
			SimInit init = new SimInit();		
			LocalManager manager = null;
			int numFederates = Integer.parseInt(args[0]);
			try {
				manager = LocalManager.createManager(new Class[] {BasicTestObject.class}, numFederates, args[1], "BasicTest", "BasicTest.fed"); 
			}
			catch (IndexOutOfBoundsException e) {
				manager = LocalManager.createManager(new Class[] {BasicTestObject.class}, numFederates, "BasicTest", "BasicTest.fed");
			}
			
			System.out.println("LocalManager created");
			BasicTest mod = null;
			try {
				mod = new BasicTest(manager, Integer.parseInt(args[2]), COUNTRIES_PER_FEDERATE);
				//mod = new BootstrapModel(manager, Integer.parseInt(args[2]), 2);
			}
			catch (Exception e) {
				mod = new BasicTest(manager, COUNTRIES_PER_FEDERATE);
				//mod = new BootstrapModel(manager, 1);
			}
			init.loadModel(mod, null, false);
		}
		catch (IndexOutOfBoundsException e) {
			System.out.println("Usage: \n[numFederates / federateName / iterations] \nOR \n[numFederates / federateName / <50 iterations>] \nOR \n[numFederates / <hostname> / <50 iterations>] ");
		}
	}
}