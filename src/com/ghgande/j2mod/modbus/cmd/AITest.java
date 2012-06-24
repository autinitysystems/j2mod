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

import java.net.InetAddress;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.io.ModbusTCPTransaction;
import com.ghgande.j2mod.modbus.io.ModbusTransaction;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersRequest;
import com.ghgande.j2mod.modbus.msg.ReadInputRegistersResponse;
import com.ghgande.j2mod.modbus.net.TCPMasterConnection;

/**
 * Class that implements a simple commandline tool for reading an analog input.
 * 
 * @author Dieter Wimberger
 * @version 1.2rc1 (09/11/2004)
 */
public class AITest {

	public static void main(String[] args) {

		InetAddress addr = null;
		TCPMasterConnection con = null;
		ModbusRequest req = null;
		ModbusTransaction trans = null;
		int ref = 0;
		int count = 0;
		int repeat = 1;
		int port = Modbus.DEFAULT_PORT;
		int unit = 0;

		try {

			// 1. Setup parameters
			if (args.length < 3) {
				printUsage();
				System.exit(1);
			} else {
				try {
					String serverAddress = args[0];
					String parts[] = serverAddress.split(":");
					
					String address = parts[0];
					if (parts.length > 1) {
						port = Integer.parseInt(parts[1]);
						if (parts.length > 2)
							unit = Integer.parseInt(parts[2]);
					}
					addr = InetAddress.getByName(address);
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

			// 2. Open the connection
			con = new TCPMasterConnection(addr);
			con.setPort(port);
			con.connect();

			if (Modbus.debug)
				System.out.println("Connected to " + addr.toString() + ":"
						+ con.getPort());

			// 3. Prepare the request
			req = new ReadInputRegistersRequest(ref, count);
			req.setUnitID(unit);
			if (Modbus.debug)
				System.out.println("Request: " + req.getHexMessage());

			// 4. Prepare the transaction
			trans = new ModbusTCPTransaction(con);
			trans.setRequest(req);

			// 5. Execute the transaction repeat times
			int k = 0;
			do {
				trans.execute();

				ReadInputRegistersResponse res = (ReadInputRegistersResponse) trans
						.getResponse();
				if (Modbus.debug)
					System.out.println("Response: " + res.getHexMessage());

				for (int n = 0; n < res.getWordCount(); n++) {
					System.out.println("Word " + n + "="
							+ res.getRegisterValue(n));
				}

				k++;
			} while (k < repeat);

			// 6. Close the connection
			con.close();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}// main

	private static void printUsage() {
		System.out
				.println("java com.ghgande.j2mod.modbus.cmd.AITest <address{:port} [String]> <register [int16]> <wordcount [int16]> {<repeat [int]>}");
	}// printUsage

}// class AITest
