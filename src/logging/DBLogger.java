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
 * Created on 22-Nov-2003
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package logging;

import java.net.InetAddress;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.sql.Timestamp;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class DBLogger implements Logger {
	
	private Connection con;

	public DBLogger(boolean notifyInstantiation) {
		
		if (!notifyInstantiation)
			return;
		try {
			String hostName = "'" + InetAddress.getLocalHost().toString() + "'";
			open();
			String notifyString = 
				"INSERT INTO instantiation VALUES (" + hostName + ", 'now');";
			Statement stmt = con.createStatement();
			stmt.execute(notifyString);		
			close();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public DBLogger() {
		
		this(false);
	}

	/* (non-Javadoc)
	 * @see hla_past.logging.Logger#LogUpdate(java.sql.Timestamp, int, int, double, byte[])
	 */
	public synchronized void logUpdate(
		Timestamp sessionID,
		String federateID,
		int instanceHandle,
		int attributeHandle,
		double logTime, 
		String value, 
		boolean proximity)
		throws Exception {
		
		if (con == null)
			throw new IllegalStateException("connection was not open");
		String table = "";
		if (proximity == Logger.LOCAL)
			table = "localUpdate";
		else
			table = "remoteUpdate";
		//build update string...
		String updateString = "INSERT INTO " + table + " VALUES('" +
											sessionID.toString() + "', " +
											"'" + federateID + "', " +
											instanceHandle + ", " +
											attributeHandle + ", " +
											logTime + ", " + 
											value + 
											");";
		Statement stmt = con.createStatement();
		stmt.execute(updateString);
	}

	/* (non-Javadoc)
	 * @see hla_past.logging.Logger#LogDiscovery(java.sql.Timestamp, int, int, double)
	 */
	public synchronized void logDiscovery(
		Timestamp sessionID,
		String federateID,
		int instanceHandle,
		int classHandle, double logTime)
		throws Exception {
		
		if (con == null)
			throw new IllegalStateException("connection was not open");
		// build update String...
		String updateString = "INSERT INTO discover VALUES('" +
											sessionID.toString() + "', " +
											"'" + federateID + "', " +
											instanceHandle + ", " +
											classHandle + ", " +
											logTime + ");";
		Statement stmt = con.createStatement();
		stmt.execute(updateString);
	}
	
	/*
	public void logRun(int time, int agents, int gen_rate, int age_rate, int world_size) {
		
		try {
			String updateString = "INSERT INTO twtimings VALUES('" +
												time + "', " +
												agents + ", " +
												gen_rate + ", " +
												age_rate + ", " +
												world_size + ");";
			Statement stmt = con.createStatement();
			stmt.execute(updateString);
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	*/
	
	public void logRun(int time, int nodes, int worldsize, int gen_rate, int age_rate) 
		throws Exception {
		
		/*
		String updateString = 	"INSERT INTO twpar64 VALUES(" +
												time + ", " +
												nodes + ");";
		*/
		String updateString = 	"INSERT INTO const32 VALUES(" + 
												nodes + ", " + time + ", " + gen_rate + ", " + age_rate + ");";
		Statement stmt = con.createStatement();
		stmt.execute(updateString);
	}
	
	public void logJunkRun(String id, int time, String domain) throws Exception {
		
		String updateString = 	"INSERT INTO junk VALUES('" + 
												id + "', " +
												time + "," +												"0, " +
												"'" + domain + "');";
		Statement stmt = con.createStatement();
		stmt.execute(updateString);
	}
	
	/* (non-Javadoc)
	 * @see hla_past.logging.Logger#logRegistration(java.sql.Timestamp, java.lang.String, int, int, double)
	 */
	public void logRegistration(
		Timestamp sessionID,
		String federateID,
		int instanceHandle,
		int classHandle,
		double logTime)
		throws Exception {
		
			if (con == null)
				throw new IllegalStateException("connection was not open");
			// build update String...
			String updateString = "INSERT INTO register VALUES('" +
												sessionID.toString() + "', " +
												"'" + federateID + "', " +
												instanceHandle + ", " +
												classHandle + ", " +
												logTime + ");";		
			Statement stmt = con.createStatement();
			stmt.execute(updateString);
	}

	
	public static String escapeArray(byte[] input) {
		
		String returnVal = "'{";
		for (int i = 0; i < input.length; i++) {
			if (i > 0)
				returnVal = returnVal + ", ";
			returnVal = returnVal + input[i];
		}
		return returnVal + "}'";
	}
	
	public void open() throws Exception {
		
		if (con == null)
			con = connect();
	}
	
	public synchronized void close() throws Exception {
		
		if (con != null) {
			con.close();
			con = null;
		}
	}
	
	private Connection connect() throws Exception {
		
		return DriverManager.getConnection(	"jdbc:postgresql://dbteach/rzm", 
																		"rzm", 
																		"slamogec");
	}
	
	static {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (Exception e) { System.out.println("driver loading failed during loading of DBLogger");}
	}


	public void logRelativeTimes(StopWatch repastTime, StopWatch hlaTime, int agents, int nodes, int gen_rate, int age_rate) throws Exception {
		
		if (con == null)
			throw new IllegalStateException("connection was not open");
		// build update String...
		String updateString = "INSERT INTO relative_timings VALUES (" +			agents + ", " + nodes + ", " +				(int)(hlaTime.getTotal() / 1000) + ", " + (int)(repastTime.getTotal() / 1000) + ")";	
		Statement stmt = con.createStatement();
		stmt.execute(updateString);
	}
}
