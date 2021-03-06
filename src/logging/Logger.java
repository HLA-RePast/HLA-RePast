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

import java.sql.Timestamp;

/**
 * @author rzm
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public interface Logger {
	
	public static boolean LOCAL = true;
	public static boolean REMOTE = false;
	
	public void logUpdate(
		Timestamp sessionID,
		String federateID,
		int instanceHandle,
		int attributeHandle,
		double logTime, 
		String value, 
		boolean proximity) throws Exception;
		
	public void logDiscovery(
		Timestamp sessionID,
		String federateID,
		int instanceHandle,
		int classHandle, double logTime) throws Exception;
		
	public void logRegistration(
		Timestamp sessionID,
		String federateID,
		int instanceHandle,
		int classHandle,
		double logTime) throws Exception;
		
	public void logRelativeTimes(
		StopWatch repastTime,
		StopWatch hlaTime,
		int agents,
		int nodes,
		int gen_rate,
		int age_rate) throws Exception;
		
	public void open() throws Exception;
	
	public void close() throws Exception;
}
