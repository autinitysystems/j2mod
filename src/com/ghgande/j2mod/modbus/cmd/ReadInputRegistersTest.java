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
package com.ghgande.j2mod.modbus.cmd;

import java.util.Arrays;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusException;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransport;
import com.ghgande.j2mod.modbus.msg.ExceptionResponse;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.net.ModbusMasterFactory;
import com.ghgande.j2mod.modbus.procimg.InputRegister;
import com.ghgande.j2mod.modbus.procimg.Register;

/**
 * Class that implements a simple command line tool for reading an analog input.
 * 
 * @author Dieter Wimberger
 * @version 1.2rc1 (09/11/2004)
 * 
 * @author Julie Haugh
 * @version 0.97 (8/12/12)
 */
public class ReadInputRegistersTest {

	private static void printUsage() {
		System.out
				.println("java com.ghgande.j2mod.modbus.cmd.ReadInputRegistersTest <address{:port} [String]> <register [int16]> <wordcount [int16]> {<repeat [int]>}");
	}

	public static void main(String[] args) {
		ModbusTransport transport = null;
		ModbusRequest req = null;
		ModbusTransaction trans = null;
		int ref = 0;
		int count = 0;
		int repeat = 1;
		int unit = 0;

		try {

			// 1. Setup parameters
			if (args.length < 3) {
				printUsage();
				System.exit(1);
			} else {
				try {
					transport = ModbusMasterFactory.createModbusMaster(args[0]);
					ref = Integer.parseInt(args[1]);
					count = Integer.parseInt(args[2]);
					if (args.length == 4) {
						repeat = Integer.parseInt(args[3]);
					}
				} catch (Exception ex) {
					ex.printStackTrace();
					printUsage();
					System.exit(1);
				}
			}

			req = new ReadInputRegistersRequest(ref, count);
			req.setUnitID(unit);
			if (Modbus.debug)
				System.out.println("Request: " + req.getHexMessage());

			// 4. Prepare the transaction
			trans = transport.createTransaction();
			trans.setRequest(req);

			// 5. Execute the transaction repeat times
			for (int k = 0;k < repeat;k++) {
				try {
					trans.execute();
				} catch (ModbusException x) {
					System.err.println(x.getMessage());
					continue;
				}

				ModbusResponse res = (ReadInputRegistersResponse) trans
						.getResponse();

				if (Modbus.debug)
					System.out.println("Response: " + res.getHexMessage());

				if (res instanceof ExceptionResponse) {
					ExceptionResponse exception = (ExceptionResponse) res;
					System.out.println(exception);
					continue;
				}

				ReadInputRegistersResponse data = (ReadInputRegistersResponse) res;
				InputRegister[] values = data.getRegisters();
				System.out.println("Data: " + Arrays.toString(values));
			}

			// 6. Close the connection
			transport.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
}
