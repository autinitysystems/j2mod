//License
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

/***
 * Java Modbus Library (jamod)
 * Copyright 2010, greenHouse Computers, LLC
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
 * Class implementing a <tt>Read File Record</tt> request.
 * 
 * @author Julie Haugh (jfh@ghgande.com)
 * @version jamod-1.2rc1-ghpc
 * 
 * @author jfhaugh (jfh@ghgande.com)
 * @version @version@ (@date@)
 */
public final class ReadFileRecordRequest extends ModbusRequest {
	public class RecordRequest {
		private int m_FileNumber;
		private int m_RecordNumber;
		private int m_WordCount;

		public int getFileNumber() {
			return m_FileNumber;
		}

		public int getRecordNumber() {
			return m_RecordNumber;
		}

		public int getWordCount() {
			return m_WordCount;
		}

		/**
		 * getRequestSize -- return the size of the response in bytes.
		 */
		public int getRequestSize() {
			return 7 + m_WordCount * 2;
		}

		public void getRequest(byte[] request, int offset) {
			request[offset] = 6;
			request[offset + 1] = (byte) (m_FileNumber >> 8);
			request[offset + 2] = (byte) (m_FileNumber & 0xFF);
			request[offset + 3] = (byte) (m_RecordNumber >> 8);
			request[offset + 4] = (byte) (m_RecordNumber & 0xFF);
			request[offset + 5] = (byte) (m_WordCount >> 8);
			request[offset + 6] = (byte) (m_WordCount & 0xFF);
		}

		public byte[] getRequest() {
			byte[] request = new byte[7];

			getRequest(request, 0);

			return request;
		}

		public RecordRequest(int file, int record, int count) {
			m_FileNumber = file;
			m_RecordNumber = record;
			m_WordCount = count;
		}
	}

	private int m_ByteCount;
	private RecordRequest[] m_Records;
	
	/**
	 * getRequestSize -- return the total request size.  This is useful
	 * for determining if a new record can be added.
	 * 
	 * @returns size in bytes of response.
	 */
	public int getRequestSize() {
		if (m_Records == null)
			return 1;
		
		int size = 1;
		for (int i = 0;i < m_Records.length;i++)
			size += m_Records[i].getRequestSize();
		
		return size;
	}
	
	/**
	 * getRequestCount -- return the number of record requests in this
	 * message.
	 */
	public int getRequestCount() {
		if (m_Records == null)
			return 0;
		
		return m_Records.length;
	}
	
	/**
	 * getRecord -- return the record request indicated by the reference
	 */
	public RecordRequest getRecord(int index) {
		return m_Records[index];
	}
	
	/**
	 * addRequest -- add a new record request.
	 */
	public void addRequest(RecordRequest request) {
		if (request.getRequestSize() + getRequestSize() > 248)
			throw new IllegalArgumentException();
		
		if (m_Records == null)
			m_Records = new RecordRequest[1];
		else {
			RecordRequest old[] = m_Records;
			m_Records = new RecordRequest[old.length + 1];
			
			System.arraycopy(old, 0, m_Records, 0, old.length);
		}
		m_Records[m_Records.length - 1] = request;
		
		setDataLength(getRequestSize());
	}

	/**
	 * createResponse -- create an empty response for this request.
	 */
	public ModbusResponse getResponse() {
		ReadFileRecordResponse response = null;

		response = new ReadFileRecordResponse();

		/*
		 * Copy any header data from the request.
		 */
		response.setHeadless(isHeadless());
		if (!isHeadless()) {
			response.setTransactionID(getTransactionID());
			response.setProtocolID(getProtocolID());
		}

		/*
		 * Copy the unit ID and function code.
		 */
		response.setUnitID(getUnitID());
		response.setFunctionCode(getFunctionCode());

		return response;
	}

	/**
	 * The ModbusCoupler doesn't have a means of reporting the slave state or ID
	 * information.
	 */
	public ModbusResponse createResponse() {
		throw new RuntimeException();
	}

	/**
	 * writeData -- output this Modbus message to dout.
	 */
	public void writeData(DataOutput dout) throws IOException {
		dout.write(getMessage());
	}

	/**
	 * readData -- read all the data for this request.
	 */
	public void readData(DataInput din) throws IOException {
		m_ByteCount = din.readUnsignedByte();

		int recordCount = m_ByteCount / 7;
		m_Records = new RecordRequest[recordCount];

		for (int i = 0; i < recordCount; i++) {
			if (din.readByte() != 6)
				throw new IOException();

			int file = din.readUnsignedShort();
			int record = din.readUnsignedShort();
			if (record < 0 || record >= 10000)
				throw new IOException();

			int count = din.readUnsignedShort();

			m_Records[i] = new RecordRequest(file, record, count);
		}
	}

	/**
	 * getMessage -- return the PDU message.
	 */
	public byte[] getMessage() {
		byte request[] = new byte[1 + 7 * m_Records.length];

		int offset = 0;
		request[offset++] = (byte) (request.length - 1);
		
		for (int i = 0; i < m_Records.length; i++) {
			m_Records[i].getRequest(request, offset);
			offset += 7;
		}
		return request;
	}

	/**
	 * Constructs a new <tt>Read File Record</tt> request instance.
	 */
	public ReadFileRecordRequest() {
		super();

		setFunctionCode(Modbus.READ_FILE_RECORD);

		/*
		 * Request size byte is all that is required.
		 */
		setDataLength(1);
	}
}