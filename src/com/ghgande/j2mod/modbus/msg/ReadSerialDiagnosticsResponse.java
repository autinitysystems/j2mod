//License
/***
 * Java Modbus Library (jamod)
 * Copyright (c) 2002-2004, jamod development team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ***/

/***
 * Java Modbus Library (j2mod)
 * Copyright (c) 2010-2012, greenHouse Gas and Electric
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are
 * met:
 *
 * Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * Neither the name of the author nor the names of its contributors
 * may be used to endorse or promote products derived from this software
 * without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDER AND CONTRIBUTORS ``AS
 * IS'' AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR
 * PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 ***/
package com.ghgande.j2mod.modbus.msg;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

import com.ghgande.j2mod.modbus.Modbus;

/**
 * Class implementing a <tt>ReadSerialDiagnosticsResponse</tt>.
 * 
 * @author Julie Haugh (jfh@ghgande.com)
 * 
 * @version @version@ (@date@)
 */
public final class ReadSerialDiagnosticsResponse extends ModbusResponse {

	/*
	 * Message fields.
	 */
	private int m_Function;
	private	short	m_Data[] = new short[0];

	/**
	 * getFunction -- get the sub-function.
	 * 
	 * @return
	 */
	public int getFunction() {
		return m_Function;
	}

	/**
	 * setStatus -- set the device's status.
	 * 
	 * @param status
	 */
	public void setFunction(int function) {
		m_Function = function;
		
		int size = 0;
		
		switch (function) {
		case 0:
		case 1:
		case 2:
		case 3:
		case 4:
		case 10:
		case 11:
		case 12:
		case 13:
		case 14:
		case 15:
		case 16:
		case 17:
		case 18:
		case 20:
			size = 1;
			break;
		}
		m_Data = new short[size];
		setDataLength(2 + (size * 2));
	}
	
	/**
	 * getWordCount -- get the number of words in m_Data.
	 */
	public int getWordCount() {
		if (m_Data != null)
			return m_Data.length;
		
		return 0;
	}
	
	/**
	 * getData -- return the first data item.
	 */
	public int getData() {
		return getData(0);
	}
	
	/**
	 * getData -- get the data item at the index
	 */
	public int getData(int index) {
		if (index < 0 || index > getWordCount())
			throw new IndexOutOfBoundsException();
		
		return m_Data[index];
	}
	
	/**
	 * setData -- set the first data item.
	 */
	public void setData(int value) {
		setData(0, value);
	}

	/**
	 * setData -- get the data item at the index
	 */
	public void setData(int index, int value) {
		if (index < 0 || index > getWordCount())
			throw new IndexOutOfBoundsException();
		
		m_Data[index] = (short) value;
	}

	/**
	 * writeData -- output the completed Modbus message to dout
	 */
	public void writeData(DataOutput dout) throws IOException {
		dout.write(getMessage());
	}

	/**
	 * readData -- input the Modbus message from din. If there was a header,
	 * such as for Modbus/TCP, it will have been read already.
	 */
	public void readData(DataInput din) throws IOException {
		m_Function = din.readShort() & 0xFFFF;
		
		setFunction(m_Function);
		
		if (m_Data.length > 0) {
			for (int i = 0;i < m_Data.length;i++)
				m_Data[i] = din.readShort();
		}
	}

	/**
	 * getMessage -- format the message into a byte array.
	 */
	public byte[] getMessage() {
		byte result[] = new byte[2 + (getWordCount() * 2)];

		result[0] = (byte) (m_Function >> 8);
		result[1] = (byte) (m_Function & 0xFF);
		for (int i = 0;i < getWordCount();i++) {
			result[2 + (i * 2)] = (byte) (m_Data[i] >> 8);
			result[3 + (i * 2)] = (byte) (m_Data[i] & 0xFF);
		}
		return result;
	}

	/**
	 * Constructs a new <tt>ReadSerialDiagnosticsResponse</tt> instance.
	 */
	public ReadSerialDiagnosticsResponse() {
		super();

		setFunctionCode(Modbus.READ_SERIAL_DIAGNOSTICS);
		setDataLength(2);
	}
}
