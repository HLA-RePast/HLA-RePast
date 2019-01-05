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
 * Created on 23-Oct-2003
 */
package manager;

import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.AttributeHandleSetFactory;
import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.FederateNotExecutionMember;
import hla.rti13.java1.FederatesCurrentlyJoined;
import hla.rti13.java1.FederationExecutionAlreadyExists;
import hla.rti13.java1.FederationExecutionDoesNotExist;
import hla.rti13.java1.HandleValuePairMaximumExceeded;
import hla.rti13.java1.MemoryExhausted;
import hla.rti13.java1.RTIambassador;
import hla.rti13.java1.RTIexception;
import hla.rti13.java1.RTIinternalError;
import hla.rti13.java1.ResignAction;
import hla.rti13.java1.SuppliedAttributes;
import hla.rti13.java1.SuppliedAttributesFactory;
import hla.rti13.java1.ValueCountExceeded;
import io.DEV_TOOLS;
import io.ReflectedAttributes13;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import exceptions.BadClassException;
import exceptions.BadClassTreeException;
import exceptions.ObjectDeletedException;
import exceptions.ObjectNotFoundException;

import logging.DBLogger;
import logging.Logger;
import object.ObjectLookup;
import object.ProxyList;
import object.PublicObject;
import uchicago.src.sim.engine.SimEvent;
import uchicago.src.sim.engine.SimEventListener;
import uchicago.src.sim.engine.SimModel;

/**
 * This is the primary interface for the distributed executive which
 * is exported to the local model. From a modeller's perspective, the
 * most important function the LocalManager serves are the registration
 * of local objects and the discovery of remote objects registered by
 * other HLA_RePast instances.
 * 
 * In addition to these visible functions, the LocalManager coordinates
 * the creation and joining of the federation as well as constrained
 * time-advance and conflict resolution (via ownership-management).
 * 
 * Because the LocalManager performs reference tracking by interfacing
 * with the local JVM (to automate object deletion) it is implemented 
 * as a singleton class and should only be accessed statically.
 * 
 * @author Rob Minson
 */
public class LocalManager implements SimEventListener {
	
	/**
	 * Accesses the RTIambassador object itself. 
	 * 
	 * <b>(Should not be needed by modeller code.)</b>
	 * @return the raw RTIambassador
	 */
	public static RTIambassador getRTI() throws IllegalStateException {
		if (singletonManager == null)
			throw new IllegalStateException("LocalManager class not yet initialised");
		return singletonManager.rtiAmb;
	}

	/**
	 * The IPv4 address of the host machine.
	 */
	protected static InetAddress localhost;
	
	/**
	 * Returns the hostname of the local machine
	 * @return the local, fully qualified host name
	 * @throws UnknownHostException 
	 */
	public static String getHost() {
		return localhost.getHostName();
	}
	
	/**
	 * A singleton instance (one LocalManager per JVM)
	 */
	protected static LocalManager singletonManager;
	/**
	 * Get the LocalManager for this JVM.
	 * @return
	 */
	public static LocalManager getManager() {
		if (singletonManager == null)
			throw new IllegalStateException("LocalManager not initialised in this JVM yet");
		return singletonManager;
	}
	
	static boolean LAZY_DELETION = false;
	public static void setLazyDeletion(boolean lazy) {
		LAZY_DELETION = lazy;
	}
	
	private static String PUBLIC_OBJECT_USAGE_VIOLATION = "Federates cannot publish instances of PublicObject itself, "
			+ "only subclasses thereof, PublicObject must be removed from the "
			+ "class tree";

	private static String NON_PUBLIC_OBJECT_VIOLATION = "Federates cannot publish objects which are not subclasses of "
			+ "PublicObject, such classes must be removed from the class tree.";

	/**
	 * The id for this federate
	 */
	protected String federateID;

	/**
	 * The name of this federate execution
	 */
	protected String federationName;

	/**
	 * The full path of the .fed file to use in this federation
	 */
	protected String fedFile;
	
	/**
	 * Indicates whether this federate was elected leader during the
	 * federation coupling process. This is an HLA_RePast-specific
	 * piece of information and currently is simply used to ensure
	 * the simulation does not start until all federates are joined.
	 * 
	 * It can be re-used for any other algorithms which require
	 * leader-election however as there is a guarantee that only one
	 * federate will have this variable set to 'true'.
	 */
	protected boolean isLeader;
	
	/**
	 * Indicates whether this federate tried (or should try) to create the
	 * federation execution. At least one of the federates should have this 
	 * variable set to true.
	 * @see LocalManager#LocalManager(Class[], int, String, String, String, boolean)
	 */
	protected boolean create;
	
	/**
	 * The number of nodes in the federation in total if the coupling process
	 * completed succesfully.
	 */
	protected int numNodes;
	
	/**
	 * The set of classes this federate will be using to represent objects
	 * in the simulation (this includes both local and proxy objects).
	 */
	protected Class[] classes;
	
	/**
	 * The session_id for this execution, used purely for logging
	 */
	protected Timestamp sessionID;

	/**
	 * The RePast model which this Manager is hosting
	 */
	protected SimModel model;

	/**
	 * The RTIambassador which this Manager is communicating with
	 */
	protected RTIambassador rtiAmb;

	/**
	 * The Coupler used to constrain the initialisation of this manager
	 */
	protected Coupler coupler;

	/**
	 * The Advancer13 used to manage the time-advance of this manager
	 */
	protected Advancer13 advancer;

	/**
	 * The initial declaration of classes reflected or published by the local
	 * simulation
	 */
	protected Class[] fedTree;

	/**
	 * A mapping of handles to variable names and vice-versa, is resolved on a
	 * simulation-by-simulation basis at the beginning of a fedex.
	 */
	protected ClassLookup classLookup;

	/**
	 * The recipient of all events incoming from the RTI. They arrive in the
	 * extern queue during time advance and are then all executed during the
	 * normal execution of the RePast schedule.
	 */
	protected EventQueue externQueue = new EventQueue();

	/**
	 * A large catalogue of all published and discovered objects in the
	 * federation about which the local simulation is interested.
	 */
	protected ObjectLookup obLookup;

	/**
	 * The dispatcher is the FederateAmbassador interface provided to the RTI,
	 * it filters incoming calls, cloning significant objects and equeing the
	 * callbacks in this manager's {@link #externQueue}
	 */
	protected CallbackBuffer cbDispatcher;

	private Logger logger;

	/**
	 * Creates the skeleton of a LocalManager, establishing only the basic
	 * machinery for two-way communication with the RTI.
	 * 
	 * The {@link #join()}, {@link #couple()} and {@link #initDatabase()} 
	 * methods can then be used to create and join the federation in a stable
	 * way.
	 * 
	 * @param classes the set of classes which will be instantiated in the local
	 * model as either PublicObject or RemoteObject instances (or both).
	 * @param numNodes the total number of nodes which will be involved in this
	 * federation.
	 * @param federateID the unique ID String of this federate. Does not need to be 
	 * unique but may be useful for debugging output.
	 * @param fedName the name of the federation to create/join
	 * @param fedFile the .fed file to use to configure this federation. This 
	 * must contain the standard HLA_RePast object types needed to gracefully
	 * couple of the federates together (see {@link Coupler#initialise() Coupler})
	 * @param create whether this federate should attempt to create the federation
	 * execution. At least one of the federates should have this argument set to
	 * true. The LocalManager will handle the case of multiple federates attempting
	 * to create, but initialisation will probably be slightly quicker if only
	 * one does so.
	 * @throws RTIexception If <i>any</i> error occurs from the RTIambassador calls.
	 * @throws UnknownHostException If there is a problem attempting to obtain the
	 * local host machine's IP address.
	 * @throws ClassNotFoundException 
	 * @throws BadClassTreeException 
	 */
	protected LocalManager(Class[] classes, int numNodes, String federateID,
			String fedName, String fedFile, boolean create) 
										throws 	RTIexception, 
												UnknownHostException, 
												BadClassTreeException, 
												ClassNotFoundException {

		verifyClassTree(classes);
		
		this.federateID = federateID;
		this.federationName = fedName;
		this.fedFile = fedFile;
		this.create = create;
		this.numNodes = numNodes;
		this.classes = classes;
		
		localhost = InetAddress.getLocalHost();
		
		/*
		 * The only thing the constructor does is initialise the two-way 
		 * communication machinery between ourselves and the RTI:
		 */
		rtiAmb = new RTIambassador(); 		//for sending messages 	
		cbDispatcher = new CallbackBuffer(this);	//for receiving messages 	
	}

	protected void join() throws RTIexception {
		isLeader = false;
		if (create) {
			try {
				rtiAmb.createFederationExecution(federationName, fedFile);
				DEV_TOOLS.print("created federation");
				isLeader = true;
			} catch (FederationExecutionAlreadyExists e) {}
		}
		boolean joined = false;
		while (!joined) {
			try {
				rtiAmb.joinFederationExecution(federateID, federationName, cbDispatcher);
				DEV_TOOLS.print("joined federation");
				joined = true;
			} catch (FederationExecutionDoesNotExist e) {}
		}
	}
	
	protected void couple() throws RTIexception {
		DEV_TOOLS.print("<LocalManager::init> coupling federation...");
		DEV_TOOLS.indent();
		coupler = new Coupler(isLeader, numNodes);
		coupler.initialise();
		DEV_TOOLS.undent();
		
		DEV_TOOLS.print("<LocalManager::init> time synchronising...");
		DEV_TOOLS.indent();
		advancer = new Advancer13();
		/* we have to call this outside the constructor as the callback accesses the
		 * 'advancer' variable, which is null within the constructor.
		 */
		advancer.initialise();		
		
		sessionID = coupler.finalise(advancer.getTime());
		DEV_TOOLS.undent();
	}
	
	protected void initDatabase() throws RTIexception {
		classLookup = new ClassLookup(classes);
		obLookup = new ObjectLookup(classLookup, logger);
		DEV_TOOLS.print("<LocalManager::initDatabase> done");
	}

	/**
	 * Creates a LocalManager. Once the call returns without exception, the 
	 * local machine will be joined to a federation involving numNodes-1 other
	 * instances of HLA_RePast.  
	 * 
	 * @param classes the set of classes which will be instantiated in the local
	 * model as either PublicObject or RemoteObject instances (or both).
	 * @param numNodes the total number of nodes which will be involved in this
	 * federation.
	 * @param federateID the unique ID String of this federate. Does not need to be 
	 * unique but may be useful for debugging output.
	 * @param federationName the name of the federation to create/join
	 * @param fedFile the .fed file to use to configure this federation. This 
	 * must contain the standard HLA_RePast object types needed to gracefully
	 * couple of the federates together (see {@link Coupler#initialise() Coupler})
	 * @param create whether this federate should attempt to create the federation
	 * execution. At least one of the federates should have this argument set to
	 * true. The LocalManager will handle the case of multiple federates attempting
	 * to create, but initialisation will probably be slightly quicker if only
	 * one does so.
	 * @return A LocalManager connected to the required federation
	 * @throws RTIexception If <i>any</i> error occurs from the RTIambassador calls.
	 * @throws UnknownHostException If there is a problem attempting to obtain the
	 * local host machine's IP address.
	 */
	public static LocalManager createManager(	Class[] classes,
												int numNodes,
												String federateID,
												String federationName,
												String fedFile,
												boolean create) throws Exception {
		if (singletonManager != null)
			return singletonManager;
		
		singletonManager = new LocalManager(classes, 
											numNodes, 
											federateID, 
											federationName, 
											fedFile, 
											create);
		/* At this point we have the machinery to communicate with the RTI but
		 * we don't know whether the federation has been created and we haven't
		 * joined it.
		 */
		
		singletonManager.join();
		/* At this point we're joined to the federation but not time-constrained 
		 * and we also don't know whether there are the correct total number of 
		 * federates joined. */
		
		singletonManager.couple();
		/* At this point we're joined to the federation, we are time-synchronised
		 * and we know the federation has the right number of other nodes. We
		 * don't yet have a local database of objects set up
		 */
		
		singletonManager.initDatabase();	
		/* At this point we can handle object discoveries and attribute updates.
		 * Any callbacks received before this point (which may happen since we
		 * need to tick() during coupling above) may be flushed from the callback
		 * buffer, but they will go in to a second buffer (the extEventQueue to
		 * and will only be committed once that itself is flushed). 
		 */
		
		return singletonManager;
	}
	
	/**
	 * This form is the same as the larger form but always tries to create
	 * the federation.
	 * 
	 * @see #createManager(Class[], int, String, String, String, boolean)
	 * 
	 * @param classes
	 * @param numNodes
	 * @param federateID
	 * @param federationName
	 * @param fedFile
	 * @return a LocalManger connected to the required federation
	 * @throws Exception
	 */
	public static LocalManager createManager(	Class[] classes,
												int numNodes,
												String federateID,
												String federationName,
												String fedFile) throws Exception {
		return createManager(	classes, 
								numNodes, 
								federateID, 
								federationName, 
								fedFile, 
								true);
	}	

	/**
	 * This is the same as the larger form but sets the federate's ID to
	 * the local host name.
	 * 
	 * @see #createManager(Class[], int, String, String, String, boolean)
	 * 
	 * @param classes
	 * @param numNodes
	 * @param federationName
	 * @param fedFile
	 * @return a LocalManager, connected to the required federation
	 * @throws Exception
	 */
	public static LocalManager createManager(	Class[] classes,
												int numNodes,
												String federationName,
												String fedFile) throws Exception {
		return createManager(	classes, 
								numNodes, 
								InetAddress.getLocalHost().getHostName(), 
								federationName, 
								fedFile, 
								true);		
	}
	
	/**
	 * Left public for development code (should not be needed by model code)
	 * 
	 * @return the ambassador currently being used by this LocalManager
	 */
	public RTIambassador getAmbassador() {
		return rtiAmb;
	}

	/**
	 * @return an AttributeHandleSet obtained from the RTIambassador.
	 */
	public static AttributeHandleSet getHandleSet(int size)
			throws MemoryExhausted, ValueCountExceeded {
		return AttributeHandleSetFactory.create(size);
	}

	/**
	 * @param size
	 *            the size of the SuppliedAttributes enumeration
	 * @return a SuppliedAttributes enumeration for passing attribute values
	 */
	public static SuppliedAttributes getSuppliedAttributes(int size)
			throws MemoryExhausted, ValueCountExceeded,
			HandleValuePairMaximumExceeded {
		return SuppliedAttributesFactory.create(size);
	}

	/**
	 * @return the current time recorded for this federate.
	 */
	public double getTick() {
		return advancer.getTime();
	}

	/**
	 * The String id for this federate (not necessarily unique).
	 * @return the federate's ID
	 */
	public String getFederateID() {
		return this.federateID;
	}

	/**
	 * The name of the federation.
	 * @return the federation's name
	 */
	public String getFederationName() {
		return this.federationName;
	}

	/**
	 * The session ID for this federation (should be unique for each 
	 * federation execution, generated by the (randomly chosen) 
	 * federation leader during the coupling process)
	 * @return the randomly generated unique ID for this federation
	 * 	execution (useful for logging etc.)
	 */
	public Timestamp getSessionID() {
		return sessionID;
	}

	// //////////////////SCHEDULING STUFF/////////////////////////

	/**
	 * Obtains a RePast Schedule object that has been modified to comply with
	 * distributed operation (i.e. it advances in constraint with the
	 * federation).
	 * 
	 * An HLA_RePast model <b>MUST</b> use a Schedule object obtained in this
	 * way. If a standard schedule is used, time advance will be unconstrained,
	 * and, most probably, the simulation will stop making sense very quickly.
	 * 
	 * That said, time-constrained schedule execution is not a guarantee of
	 * semantically correct execution, conflict-resolution semantics and
	 * object registration must be correctly used as well.
	 * 
	 * @return the Schedule object for use with the host model
	 */
	public DistributedSchedule getSchedule() throws RTIexception {
		return new DistributedSchedule(advancer, externQueue);
	}
	
	
	/* single-threaded stuff callback handling */
	public void flushCallbackBuffer() {
		while (!cbDispatcher.isEmpty())
			cbDispatcher.executeNext();
	}
	public void flushExternalEventQueueSngThrd() {
		flushCallbackBuffer();
		if (singletonManager != null) {
			while (!singletonManager.externQueue.isEmpty())
				singletonManager.externQueue.next().execute();			
		}
	}
	void resetOwnership() throws RTIexception {
		if (singletonManager != null) {
			singletonManager.obLookup.resetOwnership();
		}
	}
	
	// ///////////////////////OBJECT CREATION////////////////////////

	/**
	 * Register a PublicObject with this LocalManager, handles registration of
	 * the object amongst the federation and sets certain accounting info for
	 * the object. Whilst it is not generally advisable for a PublicObject to
	 * interact with a local simulation <i> before</i> registration it is
	 * allowable, providing an effort is made to reach a stable point before
	 * registration. As a general rule it is advisable to register PublicObjects
	 * as the last command of a constructor.
	 * <p>
	 */
	public void registerPublicObject(PublicObject obj) throws RTIexception,
			IOException, BadClassException {
		obLookup.registerPublicObject(obj);
	}

	/**
	 * Explicitly delete the (locally registered) public object from the
	 * federation.
	 * 
	 * @param obj the object to delete
	 * @throws ObjectNotFoundException if the object with the given handle
	 * was never registered or if it was registered but has already been 
	 * deleted. (The only situation in which the latter might happen is if
	 * the object is cloned, then the original object is garbage collected. 
	 * If you are doing this, stop it, nobody is impressed.)
	 * @throws RTIexception in several cases, though if the federation is still 
	 * running happily, this is probably due to a failure in sanity checking in
	 * the internal HLA_RePast engine code leading to a delete call on a 
	 * non-existent object.
	 */
	public void deletePublicObject(PublicObject obj) 
					throws RTIexception, ObjectNotFoundException {
		obLookup.localDelete(obj);
	}
	
	/**
	 * Obtains a list of objects of type local which are reflecting remotely
	 * published instances of type global. This list will be of indeterminate
	 * size and will most likely begin empty (particularly if this is done
	 * towards the beginning of an execution) and will grow as more objects are
	 * published and discovered. This list is not synchronized but provided
	 * access is only made to it in the execute() methods of scheduled
	 * BasicActions there will be no concurrency issues.
	 * <p>
	 * Note that although global and local need not be unique, if the public
	 * attributes retrievable from local are not a strict subset of those
	 * retrievable from global, this list will remain at size 0 for ever.
	 * 
	 * @param global
	 *            the Class of the objects as they were published remotely
	 * @param local
	 *            the Class of the objects as they are to be stored in the list.
	 * @return a ReflectedList object populated with some number of instances of
	 *         class local.
	 * @throws BadClassException
	 *             if either class is not supported by this manager's internal
	 *             database (i.e. if the Class[] it was initialised with does
	 *             not contain one of the two classes).
	 */
//	public ReflectedList getRemoteObjects(Class global, Class local)
//			throws BadClassException, IOException {
//
//		print("remote objects requested \n \t global = " + global
//				+ "\n \t local = " + local);
//		return obLookup.createProxyList(global, local);
//	}

	public ProxyList getProxies(Class global, Class local)
			throws FederateInternalError, RTIinternalError,
			InstantiationException, IllegalAccessException {
		print("remote objects requested \n \t global = " + global
				+ "\n \t local = " + local);
		return obLookup.getProxyList(global, local);
	}
	
	/**
	 * Called by a simulation when a simulation-level event occurs (i.e. not an
	 * 'event' within a simulation, but an exterior event, such as the pausing
	 * or destroying of a simulation).
	 * 
	 * @param evt
	 *            the SimEvent type that has occurred
	 */
	public void simEventPerformed(SimEvent evt) {

		if (evt.getId() == SimEvent.END_EVENT
				|| evt.getId() == SimEvent.STOP_EVENT) {
			// model.removeSimEventListener(this);
			try {
				this.endParticipation();
			} catch (Exception e) {
				standardErrorProcedure(e);
			}
		}
	}

	private void standardErrorProcedure(Exception e) {

		DEV_TOOLS.showException(e);
	}

	/**
	 * Resign from the federation, removing all objects created and
	 * registered in the past by this federate.
	 * 
	 * A more powerful version of this method call is made when the 
	 * RePast STOP_EVENT or END_EVENT events are received from the 
	 * engine. Therefore modeller code should not need to independetly 
	 * call this method. 
	 * 
	 * An exeptional circumstance may be if the model wishes to do more 
	 * processing after resignation without producing a STOP_ or 
	 * END_EVENT. Note that, in this case, more hacking will be required
	 * since the DistributedSchedule that has been used up to this point
	 * will not advance forward in time after this call has returned.
	 * 
	 * @param destroy Also attempt to destroy the federation execution itself
	 */
	public void endParticipation(boolean destroy) {

		try {
			rtiAmb.resignFederationExecution(ResignAction.DELETE_OBJECTS_AND_RELEASE_ATTRIBUTES);
			DEV_TOOLS.print("resigned from federation");
		} catch (FederateNotExecutionMember e0) {
		} catch (RTIexception e1) {
			standardErrorProcedure(e1);
		}
		if (destroy) {
			boolean fedDestroyed = false;
			while (!fedDestroyed) {
				try {
					rtiAmb.destroyFederationExecution(federationName);
					DEV_TOOLS.print("federation destroyed");
					fedDestroyed = true;
				} catch (FederatesCurrentlyJoined e) {
				} catch (FederationExecutionDoesNotExist e) {
				} catch (RTIexception e) {
					standardErrorProcedure(e);
				}
			}
		}
	}

	protected void endParticipation() {

		try {
			rtiAmb.resignFederationExecution(ResignAction.DELETE_OBJECTS_AND_RELEASE_ATTRIBUTES);
			DEV_TOOLS.print("resigned from federation");
		} catch (FederateNotExecutionMember e0) {
		} catch (RTIexception e1) {
			standardErrorProcedure(e1);
		}
		try {
			rtiAmb.destroyFederationExecution(federationName);
			DEV_TOOLS.print("federation destroyed");
		} catch (FederatesCurrentlyJoined e) {
		} catch (FederationExecutionDoesNotExist e) {
		} catch (RTIexception e) {
			standardErrorProcedure(e);
		}
	}

	// /////////////////////////CALLBACKS////////////////////////////

	// received when some PublicObject is registered at some other federate and
	// we subscribe to that class of object.
	void discoverInstance(int theObject, int theClass) {
		if (!coupler.isCoupled()) {
			coupler.delegatedDiscover(theObject, theClass);
		} else {
			class DiscoverCall implements Call {
				int theObject, theClass;
				public DiscoverCall(int theObject, int theClass) {
					this.theObject = theObject; this.theClass = theClass;
				}
				public void execute() {
					print("discover instance callback " + theObject + " of class "
							+ theClass);
					try {
						obLookup.discoverRemoteObject(theObject, theClass);
					} catch (Exception e) {
						standardErrorProcedure(e);
					}					
				}				
			}
			externQueue.enque(new DiscoverCall(theObject, theClass), getTick());
		}
	}

	/* Received when some other federate has requested a refresh
	 * of a proxy whose master copy is owned by this federate
	 */
	void refreshRequest(int theObject) {

		print("refresh requested (" + theObject + ")");

		class RefreshCallBack implements Call {
			private int theObject;

			public RefreshCallBack(int theObject) {
				this.theObject = theObject;
			}

			public void execute() {
				try {
					obLookup.refreshObject(theObject);
				} catch (Exception e) {
					standardErrorProcedure(e);
				}
			}
		}
		
		if (obLookup == null)
			externQueue.enque(new RefreshCallBack(theObject), advancer.getTime());
		else {
			try {
				obLookup.refreshObject(theObject);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

	// ***These events are used to resolve the updating of a remote
	// variable.***//

	// received when we are updating a variable - if attrs contains the variable
	// that we want to update, we can update it, if it is empty then we cannot
	// update it.
	void ownershipAcquired(int theObject, AttributeHandleSet attrs) {

		try {
			obLookup.notifyOwnership(theObject, attrs);
		} catch (ObjectNotFoundException e) {
			print("could not find " + theObject + " in ObjectLookup");
			try {
				rtiAmb.unconditionalAttributeOwnershipDivestiture(theObject,
						attrs);
			} catch (RTIexception e1) {
				print("failed to re-divest on object not found exception");
			}
		} catch (ObjectDeletedException e) {
			try {
				rtiAmb.unconditionalAttributeOwnershipDivestiture(theObject,
						attrs);
			} catch (RTIexception e1) {
				print("failed to re-divest on object deletion exception");
			}
		}
	}

	void ownershipFailed(int theObject, AttributeHandleSet attrs) {

		try {
			obLookup.notifyOwnershipFailure(theObject, attrs);
		} catch (ObjectNotFoundException e) {
		} catch (ObjectDeletedException e) {
		}
	}

	// we will probably receive this quite regularly, when a clash occurs with
	// a CumulativeVariable or an ExclusiveVariable
	void releaseRequest(int theObject, AttributeHandleSet attrs) {

		print("<LocalManager::releaseRequest> ownership requested by remote federate for object " + theObject);
	}

	// when a PublicObject at some federate has been garbage-collected or 
	// explicitly delete and is no longer being simulated
	void removeInstance(int theObject, double time) {
			
		obLookup.removeInstance(theObject);
		print("object removed: " + theObject + " at time " + time);
	}

	// when the value of some PublicVariable has changed which we subscribe to
	void reflectValues(int theObject, ReflectedAttributes13 attrs, double time) {

		/* if this happens during federation coupling, it is meta-management
		 * so execute it out-of-band
		 */
		if (!coupler.isCoupled()) {
			try {
				coupler.delegatedReflect(theObject, attrs);
			} catch (Exception e) {
				standardErrorProcedure(e);
			}
		} 
		/* otherwise it needs to happen in-line with the distributed schedule
		 * algorithm, so we buffer it in the extern queue and flush all these
		 * events at once, after we have received a time grant to the next
		 * tick.
		 */
		else {

			try {
				externQueue.enque(new ReflectCallBack(theObject, attrs, time),
						time);
			} catch (Exception e) {
				standardErrorProcedure(e);
			}
		}
	}

	class ReflectCallBack implements Call {
		private int theObject;

		private ReflectedAttributes13 attrs;

		private double time;

		public ReflectCallBack(int theObject,
				ReflectedAttributes13 attrs, double time) {
			this.theObject = theObject;
			this.attrs = attrs;
			this.time = time;
		}

		public void execute() {

			try {
				obLookup.resolveUpdate(theObject, attrs, time);
			} catch (ObjectNotFoundException e) {
				// do nothing, doesn't really matter
			} catch (ObjectDeletedException e) {
				// again, it will come out in the wash
			} catch (IOException e) {
				standardErrorProcedure(e);
			} catch (RTIinternalError e) {
				standardErrorProcedure(e);
			} catch (FederateInternalError e) {
				standardErrorProcedure(e);
			}
		}
	}
	
	private List<LogicalTimeListener> LTListeners = new ArrayList<LogicalTimeListener>();
	/**
	 * Register a listener which will be notified whenever this federate
	 * is granted to advance its clock.
	 * 
	 * This should be used by internal engine components which wish to
	 * interact directly with the process of time advance. 
	 * 
	 * Model code which wishes to simply do 'something' model-wise at a 
	 * specific tick should *always* use the standard RePast scheduling
	 * procedures. 
	 * 
	 * Hijacking this method to execute arbitrary code in the model is not
	 * correct useage and will almost certainly result in unexpected results
	 * as the model is changed out-of-band with the synchronisation algorithm.
	 * 
	 * @param ltl the listener
	 */
	public void addLogicalTimeListener(LogicalTimeListener ltl) {
		LTListeners.add(ltl);
	}	
	void timeAdvanceGrant(double newTime) {
		if (!coupler.isCoupled())
			coupler.grantTo(newTime);
		else
			advancer.grantTo(newTime);
		for (LogicalTimeListener ltl : LTListeners)
			ltl.grantTo(newTime);
	}

	void regulationEnabled(double time) {
		advancer.regulating(time);
	}

	public void constrainedEnabled(double time) {
		advancer.constrain(time);
	}

	// ////////////// MISC - UTILITY METHODS /////////////////////

	/*
	 * Checks the provided Class[] to ensure that all classes contained within
	 * are subclasses of PublicObject. Note that it is not acceptable to provide
	 * PublicObject as an element of the tree since it is an abstract class and
	 * hence cannot be in the list given to the constructor. Any array
	 * containing PublicObject itself or any Class which is not a subclass of
	 * PublicObject will result in this method throwing a BadClassTreeException
	 */
	private void verifyClassTree(Class[] tree) throws BadClassTreeException,
			ClassNotFoundException {

		Class publicObject = PublicObject.class;
		Class object = Object.class;
		for (int i = 0; i < tree.length; i++) {
			if (tree[i] == publicObject)
				throw new BadClassTreeException(publicObject,
						PUBLIC_OBJECT_USAGE_VIOLATION);
			Class superClass = tree[i].getSuperclass();
			while (superClass != publicObject) {
				if (superClass == object)
					throw new BadClassTreeException(tree[i],
							NON_PUBLIC_OBJECT_VIOLATION);
				superClass = superClass.getSuperclass();
			}
		}
	}

	/**
	 * Print the given message to an appropriate output/log. For example if
	 * hasGui is false then message should be sent to System.out, if it returns
	 * true then in some cases it should go to a non-modal dialog
	 * 
	 * @param message
	 *            the message to print
	 */
	protected void print(String message) {

		message = new String(message + " <" + this.federateID + ">");
		DEV_TOOLS.print(message);
	}
}
