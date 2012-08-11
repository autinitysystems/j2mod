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
 * Copyright 2012, Julianne Frances Haugh
 * d/b/a greenHouse Gas and Electric
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
package com.ghgande.j2mod.modbus.net;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusCoupler;
import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.io.ModbusTransport;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.util.SerialParameters;

/**
 * Class that implements a ModbusSerialListener.<br>
 * If listening, it accepts incoming requests passing them on to be handled.
 * 
 * @author Dieter Wimberger
 * @version 1.2rc1 (09/11/2004)
 * 
 * @author Julie Haugh Code cleanup in prep to refactor with ModbusListener
 *         interface
 */
public class ModbusSerialListener implements Runnable {
	private boolean m_Listening;
	private boolean m_Running = true;
	private SerialConnection m_SerialCon;

	/**
	 * run
	 * 
	 * Listen for incoming messages and process.
	 */
	public void run() {
		try {
			m_Listening = true;
			m_SerialCon.open();
			
			ModbusTransport transport = m_SerialCon.getModbusTransport();

			while (m_Running) {
				if (m_Listening) {
					try {
						// 1. read the request
						ModbusRequest request = transport.readRequest();
						ModbusResponse response = null;

						// test if Process image exists
						if (ModbusCoupler.getReference().getProcessImage() == null) {
							response = request.createExceptionResponse(
									Modbus.ILLEGAL_FUNCTION_EXCEPTION);
						} else {
							response = request.createResponse();
						}

						if (Modbus.debug)
							System.out.println("Request:"
									+ request.getHexMessage());
						if (Modbus.debug)
							System.out.println("Response:"
									+ response.getHexMessage());

						transport.writeMessage(response);
					} catch (ModbusIOException ex) {
						ex.printStackTrace();
						continue;
					}
				} else {
					/*
					 * Not listening -- read and discard the request so the
					 * input doesn't get clogged up.
					 */
					transport.readRequest();
				}
			}
		} catch (Exception e) {
			// FIXME: this is a major failure, how do we handle this
			e.printStackTrace();
		} finally {
			if (m_SerialCon != null) {
				m_SerialCon.close();
			}
		}
	}

	/**
	 * Sets the listening flag of this <tt>ModbusTCPListener</tt>.
	 * 
	 * @param b
	 *            true if listening (and accepting incoming connections), false
	 *            otherwise.
	 */
	public void setListening(boolean b) {
		m_Listening = b;
	}

	/**
	 * Tests if this <tt>ModbusTCPListener</tt> is listening and accepting
	 * incoming connections.
	 * 
	 * @return true if listening (and accepting incoming connections), false
	 *         otherwise.
	 */
	public boolean isListening() {
		return m_Listening;
	}

	/**
	 * Stops this interface.
	 */
	public void stop() {
		m_Listening = false;
		m_Running = false;
	}

	/**
	 * Constructs a new <tt>ModbusSerialListener</tt> instance.
	 * 
	 * @param params
	 *            a <tt>SerialParameters</tt> instance.
	 * @param run
	 *            the interface is started immediately without a thread.
	 */
	public ModbusSerialListener(SerialParameters params, boolean run) {
		m_SerialCon = new SerialConnection(params);
		if (run)
			run();
	}
	
	/**
	 * Constructs a new <tt>ModbusSerialListener</tt> instance.
	 * 
	 * @param params
	 *            a <tt>SerialParameters</tt> instance.
	 *            
	 * @deprecated
	 */
	public ModbusSerialListener(SerialParameters params) {
		m_SerialCon = new SerialConnection(params);
		run();
	}
}
