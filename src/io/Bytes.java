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
package io;

import hla.rti13.java1.EncodingHelpers;
import hla.rti13.java1.FederateInternalError;
import hla.rti13.java1.RTIinternalError;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.NotSerializableException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

public class Bytes {

	public static byte[] getBytes(int value) throws RTIinternalError{
		return EncodingHelpers.encodeInt(value);
	}

	public static int intValue(byte[] value) throws FederateInternalError {
		return EncodingHelpers.decodeInt(value);
	}

	public static byte[] getBytes(double value) throws RTIinternalError{
		return EncodingHelpers.encodeDouble(value);	
	}

	public static double doubleValue(byte[] value) throws FederateInternalError {
		return EncodingHelpers.decodeDouble(value);
	}

	public static byte[] getBytes(float value) throws RTIinternalError{
		return EncodingHelpers.encodeFloat(value);	
	}

	public static float floatValue(byte[] value) throws FederateInternalError {
		return EncodingHelpers.decodeFloat(value);
	}

	public static byte[] getBytes(short value) throws RTIinternalError{
		return EncodingHelpers.encodeShort(value);
	}

	public static short shortValue(byte[] value) throws FederateInternalError {
		return EncodingHelpers.decodeShort(value);
	}

	public static byte[] getBytes(long value) throws RTIinternalError{
		return EncodingHelpers.encodeLong(value);
	}

	public static long longValue(byte[] value) throws FederateInternalError {
		return EncodingHelpers.decodeLong(value);
	}
	
	/**
	*   Get a byte[] representation of the specified object, using
	*   standard Java serialisation.
	*   @param obj the object to be serialized
	*   @return the array representing the object
	*   @throws NotSerializableException if the given object does not implement
	*   the Serializable interface
	*   @throws FederateInternalError if some other error occurs while reading the object
	*/
	public static byte[] getBytes(Object obj) 
		throws RTIinternalError {
		
		try {		    
			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
			ObjectOutputStream out = new ObjectOutputStream(bytes);
			out.writeObject(obj);
			out.flush();
			return bytes.toByteArray();
		} 
		catch (InvalidClassException e) {
			throw new RTIinternalError("InvalidClass: obj.getClass().toString()");
		}
		catch (IOException e) {
			throw new RTIinternalError("IOException");
		}
	}

	/**
	*   Get the object represented by this byte[], using 
	*   standard Java serialisation
	*   @param value a byte[] representing an object in standard Java serialisation
	*   @return the Object represented
	*   @throws ClassNotFoundException if the current runtime does not contain the 
	*   class of the the de-serialized object.
	*   @throws InvalidClassException if the object is not a valid Class type for
	*   this Java runtime.
	*   @throws FederateInternalError if any other error is found in the byte[]
	*/
	public static Object objectValue(byte[] value)
		throws FederateInternalError{

		try {
			ObjectInputStream in = new ObjectInputStream(new ByteArrayInputStream(value));
			return in.readObject();
		}
		catch (Exception e) {
			e.printStackTrace();
			throw new FederateInternalError(e.getMessage());
		}
	}	
	
	/**
	*   Get a byte[] representing the specified value, using 
	*   standard Java serialization.
	*   @param value the boolean to be serialized
	*   @return the array representing the boolean
	*/
	public static byte[] getBytes(boolean value) throws RTIinternalError{
		return EncodingHelpers.encodeBoolean(value);
	}

	/**
	*   Get the short value represented by this byte[], using 
	*   standard Java serialisation
	*   @param value a byte[] representing a short in standard Java serialisation
	*   @return the short value represented
	*   @throws FederateInternalError if the byte[] is not a standard Java representation
	*   of a short type.
	*/
	public static boolean booleanValue(byte[] value) throws FederateInternalError {
		return EncodingHelpers.decodeBoolean(value);
	}
	
	
	
	
	/* HLA_GRID_RePast version (puts in to ascii string first, then serialises that) */
//	/**
//	*   Get a byte[] representation of the specified object, using
//	*   standard Java serialisation.
//	*   @param obj the object to be serialized
//	*   @return the array representing the object
//	*   @throws NotSerializableException if the given object does not implement
//	*   the Serializable interface
//	*   @throws FederateInternalError if some other error occurs while reading the object
//	*/
//	public static byte[] getBytes(Object obj) 
//		throws RTIinternalError {
//		
//		try {		    
//			ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//			ObjectOutputStream out = new ObjectOutputStream(bytes);
//			out.writeObject(obj);
//			out.flush();
//			String tempstr = Base64.encode(bytes.toByteArray());
////			System.out.println("String: " + tempstr);
//			return EncodingHelpers.encodeString(tempstr);
////			return tempstr.getBytes();
//		} 
//		catch (InvalidClassException e) {
//			throw new RTIinternalError("InvalidClass: obj.getClass().toString()");
//		}
//		catch (IOException e) {
//			throw new RTIinternalError("IOException");
//		}
//	}
//
//	/**
//	*   Get the object represented by this byte[], using 
//	*   standard Java serialisation
//	*   @param value a byte[] representing an object in standard Java serialisation
//	*   @return the Object represented
//	*   @throws ClassNotFoundException if the current runtime does not contain the 
//	*   class of the the de-serialized object.
//	*   @throws InvalidClassException if the object is not a valid Class type for
//	*   this Java runtime.
//	*   @throws FederateInternalError if any other error is found in the byte[]
//	*/
//	public static Object objectValue(byte[] value)
//		throws FederateInternalError{
//		
//		//The hlagrid seems to eat the last byte. This is a temporary solution for this problem.
////		int len = Array.getLength(value);
////		System.out.println("Byte: " + value[len-1] + " " + value[len-2]);
////		if ((len > 4) && (value[len-2] == 61))
////			value[len-1] = value[len-2];
//
//		String tempstr = EncodingHelpers.decodeString(value);
//		byte[] temp = Base64.decode(tempstr);
////		System.out.println("String: " + tempstr + "Bytes length = " +
////			Array.getLength(value));
//		Object obj = null;
//		try {
//			ObjectInputStream in = getObjectIn(temp);
//			obj = in.readObject();
//		}
//		catch (StreamCorruptedException e) {
//			System.out.println("StreamCorruptedException: " + e.getMessage());
//			throw new FederateInternalError("StreamCorruptedException");
//		}
//		catch (OptionalDataException e) {
//			System.out.println("OptionalDataException");
//			throw new FederateInternalError("OptionalDataException");
//		}
//		catch (ClassNotFoundException e) {
//			System.out.println("ClassNotFoundException");
//			throw new FederateInternalError("ClassNotFoundException");
//		}
//		catch (InvalidClassException e) {
//			System.out.println("InvalidClassException");
//			throw new FederateInternalError("InvalidClassException");
//		}
//		catch (IOException e) {
//			System.out.println("IOException");
//			throw new FederateInternalError("IOException");
//		}
//		return obj;
//	}
//	
//	private static ObjectInputStream getObjectIn(byte[] bytes) throws IOException{
//		return new ObjectInputStream(new ByteArrayInputStream(bytes));
//	}
}
