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
package com.ghgande.j2mod.modbus.cmd;

import java.io.IOException;
import java.util.Arrays;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransport;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadMultipleRegistersResponse;
import com.ghgande.j2mod.modbus.net.ModbusMasterFactory;
import com.ghgande.j2mod.modbus.procimg.Register;

/**
 * Class that implements a simple command line tool for writing to an analog
 * output over a Modbus/TCP connection.
 * 
 * <p>
 * Note that if you write to a remote I/O with a Modbus protocol stack, it will
 * most likely expect that the communication is <i>kept alive</i> after the
 * first write message.
 * 
 * <p>
 * This can be achieved either by sending any kind of message, or by repeating
 * the write message within a given period of time.
 * 
 * <p>
 * If the time period is exceeded, then the device might react by turning off
 * all signals of the I/O modules. After this timeout, the device might require
 * a reset message.
 * 
 * @author Dieter Wimberger
 * @version 1.2rc1 (09/11/2004)
 * 
 * @author Julie Haugh
 * @version @version@ (@date@)
 */
public class ReadHoldingRegistersTest {

	private static void printUsage() {
		System.out.println("java com.ghgande.j2mod.modbus.cmd.ReadHoldingRegistersTest"
				+ " <connection [String]>"
				+ " <base [int]> <count [int]> {<repeat [int]>}");
	}

	public static void main(String[] args) {
		ModbusTransport transport = null;
		ModbusRequest req = null;
		ModbusTransaction trans = null;
		int ref = 0;
		int count = 0;
		int repeat = 1;
		int unit = 0;

		// 1. Setup parameters
		if (args.length < 3) {
			printUsage();
			System.exit(1);
		}

		try {
			try {
				transport = ModbusMasterFactory.createModbusMaster(args[0]);
				ref = Integer.parseInt(args[1]);
				count = Integer.parseInt(args[2]);

				if (args.length == 4)
					repeat = Integer.parseInt(args[3]);
			} catch (Exception ex) {
				ex.printStackTrace();
				printUsage();
				System.exit(1);
			}

			req = new ReadMultipleRegistersRequest(ref, count);

			req.setUnitID(unit);
			if (Modbus.debug)
				System.out.println("Request: " + req.getHexMessage());

			// 3. Prepare the transaction
			trans = transport.createTransaction();
			trans.setRequest(req);

			// 4. Execute the transaction repeat times

			for (int i = 0; i < repeat; i++) {
				trans.execute();
				ModbusResponse response = trans.getResponse();

				if (Modbus.debug)
					System.out.println("Response: "
							+ trans.getResponse().getHexMessage());
				
				if (! (response instanceof ReadMultipleRegistersResponse))
					continue;
				
				ReadMultipleRegistersResponse data = (ReadMultipleRegistersResponse) response;
				Register values[] = data.getRegisters();
				
				System.out.println("Data: " + Arrays.toString(values));
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		} finally {
			try {
				transport.close();
			} catch (IOException e) {
				// Do nothing.
			}
		}
	}
}
