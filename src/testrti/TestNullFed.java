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
package testrti;

import hla.rti13.java1.AttributeHandleSet;
import hla.rti13.java1.AttributeHandleSetFactory;
import hla.rti13.java1.AttributeNotKnown;
import hla.rti13.java1.CouldNotDiscover;
import hla.rti13.java1.EnableTimeConstrainedWasNotPending;
import hla.rti13.java1.EnableTimeRegulationWasNotPending;
import hla.rti13.java1.EncodingHelpers;
import hla.rti13.java1.EventRetractionHandle;
import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.FederateOwnsAttributes;
import hla.rti13.java1.FederatesCurrentlyJoined;
import hla.rti13.java1.FederationExecutionAlreadyExists;
import hla.rti13.java1.FederationExecutionDoesNotExist;
import hla.rti13.java1.FederationTimeAlreadyPassed;
import hla.rti13.java1.InvalidFederationTime;
import hla.rti13.java1.NullFederateAmbassador;
import hla.rti13.java1.ObjectClassNotKnown;
import hla.rti13.java1.ObjectNotKnown;
import hla.rti13.java1.RTIambassador;
import hla.rti13.java1.ReflectedAttributes;
import hla.rti13.java1.ResignAction;
import hla.rti13.java1.SuppliedAttributes;
import hla.rti13.java1.SuppliedAttributesFactory;
import hla.rti13.java1.TimeAdvanceWasNotInProgress;
import io.Bytes;
import manager.Advancer13;
import manager.Coupler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.sql.Timestamp;
import java.util.Random;

class TestNullFed extends NullFederateAmbassador {
   
  int numFederates;
  Coupler coupler;  
  Timestamp fedexid = null;
  
  Advancer13 advancer;
  
  String federateID;
  //Logger logger;
  
  NullFederateAmbassador fedAmb;
  RTIambassador rtiAmb;
  BufferedReader stopReader;
  Random generator; 
    
  int countryHandle;
  int instanceHandle;
  long pop = 10000;
  String countryName = "";
  
  boolean destroyed = false;
  
  
  TestNullFed(int numFederates, String federateID) throws Exception 
  { 
    this.numFederates = numFederates;
    this.federateID = federateID;
    rtiAmb = new RTIambassador();
    stopReader = new BufferedReader(new InputStreamReader(System.in));
    generator = new Random();
    //logger = new DBLogger();
    advancer = new Advancer13();
    
    this.countryName = "nation of " + federateID;
  }
  
  public static void main(String[] args) throws Exception 
  {  
  	int iterations = 50;
  	if (args.length > 2)
  		iterations = Integer.parseInt(args[2]);
  	String fedID = "";
  	if (args.length > 1)
  		fedID = args[1];
  	else
  		fedID = InetAddress.getLocalHost().getHostName();
    TestNullFed fed = new TestNullFed(Integer.parseInt(args[0]), fedID);
    
    fed.createAndJoin();
    fed.initialise();
    fed.finalise();
    fed.publishAndSubscribe();
    fed.registerObjects();
    fed.startLoop(iterations);
    fed.resignAndDestroy();
  }
  
  void createAndJoin() throws Exception 
  {
  	boolean isLeader = false;
    try {
		rtiAmb.createFederationExecution("HelloWorld", "HelloWorld.fed");
		System.out.println("created federation");
		isLeader = true;
    }
    catch (FederationExecutionAlreadyExists e) {}
    boolean joined = false;
    while (!joined) {
		try {
			rtiAmb.joinFederationExecution("Me", "HelloWorld", this);
			System.out.println("joined federation");
			joined = true;			
		}
		catch (FederationExecutionDoesNotExist e) {}
    }
    coupler = new Coupler(isLeader, numFederates);  
    advancer = new Advancer13();
  }  
  
  void initialise() throws Exception {
  	
	timeDeclare();
	coupler.initialise();
  }
  
  void finalise() throws Exception {
  	
  	this.fedexid = coupler.finalise(advancer.getTime()); 
  }

  void publishAndSubscribe() throws Exception 
  {
    countryHandle = rtiAmb.getObjectClassHandle("Country");
    AttributeHandleSet handles = AttributeHandleSetFactory.create(2);
    handles.add(rtiAmb.getAttributeHandle("Population", countryHandle));
    handles.add(rtiAmb.getAttributeHandle("Name", countryHandle));
    rtiAmb.subscribeObjectClassAttributes(countryHandle, handles);
    rtiAmb.publishObjectClass(countryHandle, handles);
    System.out.println("subscriptions complete");	        
  }
  
  void registerObjects() throws Exception {
  	
  	instanceHandle = rtiAmb.registerObjectInstance(countryHandle);
  	System.out.println("registered object as " + instanceHandle);
  	/*
  	logger.open();
  	logger.logRegistration(fedexid, federateID, instanceHandle, countryHandle, advancer.getTime());
  	logger.close();
  	*/
  }
  
  void updateObject() throws Exception {
  	
  	int popHandle = rtiAmb.getAttributeHandle("Population", countryHandle);
  	int nameHandle = rtiAmb.getAttributeHandle("Name", countryHandle);
  	//logger.open();
	SuppliedAttributes supAtt = SuppliedAttributesFactory.create(2);
	double chance = generator.nextDouble();
	if (chance < 0.5)
		pop = (long)(pop * 1.25);
	else
		pop = (long)(pop * 0.9);
	byte[] popSer = Bytes.getBytes(pop);
	
	byte[] nameSer = Bytes.getBytes(countryName);
	supAtt.add(popHandle, popSer);
	supAtt.add(nameHandle, nameSer);
	System.out.println("updating " + instanceHandle + " to " + pop + " @ " + advancer.getTime());
	byte[] encTime = EncodingHelpers.encodeDouble(advancer.getTime());
	rtiAmb.updateAttributeValues(instanceHandle, supAtt, encTime, "");
	//logger.logUpdate(fedexid, federateID, instanceHandle, popHandle, advancer.getTime(), new String("" + pop), Logger.LOCAL);
  	//logger.close();
  }

  void timeDeclare() throws Exception 
  {
  	advancer.initialise();
    /*
    rtiAmb.enableTimeConstrained();
    while (!constrained)
    {
      rtiAmb.tick(0.1, 0.5);
    }
    rtiAmb.enableTimeRegulation(EncodingHelpers.encodeDouble(time), 
    							EncodingHelpers.encodeDouble(0));
    while (!regulating)
    {
      rtiAmb.tick(0.1, 0.5);
    }
    System.out.println("time declarations complete, time = " + time);
    */
  }
	
	void startLoop(int i) throws Exception {
		
		long startTime = System.currentTimeMillis();
		
		/*   
		double lastTime = time;
		requiredTime = time;
		*/        
		do 
		{	
			/*	
			if (time == requiredTime) {
				i--;
				updateObject();
				requiredTime = time + generator.nextInt(10);	      		
			}
			granted = false;
			NERACall(requiredTime);
			do {
				rtiAmb.tick(0.1, 0.5);
			} 
			while (!granted);
			*/
			int advanceAmount = 1 + generator.nextInt(9);
//			advancer.blockingAdvanceTo(advancer.getTime() + advanceAmount);
			advancer.advanceTo(advancer.getTime() + advanceAmount, true);
			//System.out.println("completed advance to " + advancer.getTime());
			updateObject();
			i--;
		}
		while (i > 0);
		long endTime = System.currentTimeMillis();
		long totalTime = (endTime - startTime) / 1000;
		System.out.println("main loop complete in " + totalTime + " secs");
	}

  void startLoop() throws Exception 
  {
    String iterations = prompt("iterations ? >>   ");
    int i = 50;    
    try { 
    	i = Integer.parseInt(iterations);
    }
    catch (NumberFormatException e) {}
	startLoop(i);
  }

  
  
  
  
  
  
  
  
  ///////       OVER-RIDDEN

  	public void timeAdvanceGrant(byte[] theTime)
    	throws InvalidFederationTime, TimeAdvanceWasNotInProgress,
    	FederationTimeAlreadyPassed, FederateInternalError
    {   	
		double time = EncodingHelpers.decodeDouble(theTime);		
		/*
		System.out.println("GRANTED: " + time);
		granted = true;
		*/		
		if (!coupler.isCoupled())      
			coupler.grantTo(time);
		else
			advancer.grantTo(time);
    }

  	public void timeConstrainedEnabled(byte[] newTime)
    	throws InvalidFederationTime, EnableTimeConstrainedWasNotPending, 
   	 	FederateInternalError
    {
      /*
      constrained = true;
      time = EncodingHelpers.decodeDouble(newTime);
      */
      advancer.constrain(EncodingHelpers.decodeDouble(newTime));
    }
  
  	public void timeRegulationEnabled(byte[] newTime)
    	throws InvalidFederationTime, EnableTimeRegulationWasNotPending, 
    	FederateInternalError
    {
    /*
      regulating = true;
      time = EncodingHelpers.decodeDouble(newTime);
      */
      advancer.regulating(EncodingHelpers.decodeDouble(newTime));
    }
    
	public void reflectAttributeValues(int theObject,
							   								ReflectedAttributes theAttributes,
							   								byte[] theTime,
							   								String theTag,
							   								EventRetractionHandle theHandle)
		  throws ObjectNotKnown, AttributeNotKnown, FederateOwnsAttributes,
			InvalidFederationTime, FederateInternalError { 
		
		try {
			if (!coupler.delegatedReflect(theObject, theAttributes)) {
				try {
					long refPop = Bytes.longValue(theAttributes.getValue(0));
					String refName = (String)Bytes.objectValue(theAttributes.getValue(1));
					double refTime = EncodingHelpers.decodeDouble(theTime);
					/*
					System.out.println(	"incoming: object: " + theObject +
													"/name: " + refName +  
													"/time: " + refTime + 
													"/pop: " + refPop);
					logger.open();
					logger.logUpdate(	fedexid, 
													federateID, 
													theObject, 
													rtiAmb.getAttributeHandle("Population", countryHandle), 
													refTime, 
													new String("" + refPop), 
													Logger.REMOTE);
					logger.close();
					*/
				}
				catch (Exception e) {
					System.out.println(e.getMessage() + "/// at reflectAttributeValues");		
				}	
			}
		}
		catch (Exception e) {e.printStackTrace();}				
	}
	
	public void discoverObjectInstance(int arg0, int arg1, String arg2)
		throws CouldNotDiscover, ObjectClassNotKnown, FederateInternalError {
			
		if (!coupler.delegatedDiscover(arg0, arg1))
			System.out.println("discovered instance " + arg0 + " of class " + arg1);
	}	
	 
  

  ///////        PRIVATE
  
	  String prompt(String s) throws Exception
	  {
	    System.out.print(s);
	    return stopReader.readLine();
	  }

	void NERACall(double targetTime) throws Exception {
		
		/*
		System.out.println("REQUEST: " + targetTime);
		granted = false;
		byte[] t = EncodingHelpers.encodeDouble(targetTime);
		rtiAmb.nextEventRequestAvailable(t);
		rtiAmb.tick();
		*/
		advancer.advanceTo(targetTime, false);
	}
	
	void resignAndDestroy() throws Exception 
	  { 
		rtiAmb.resignFederationExecution(ResignAction.DELETE_OBJECTS_AND_RELEASE_ATTRIBUTES);
		System.out.println("resigned from federation");
		try {
			rtiAmb.destroyFederationExecution("HelloWorld");
			System.out.println("federation destroyed");
		}
		catch (FederatesCurrentlyJoined e) {}
		catch (FederationExecutionDoesNotExist e) {}
		destroyed = true;    
	  }	
}
