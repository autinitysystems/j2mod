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
package com.ghgande.j2mod.modbus.io;


import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import com.ghgande.j2mod.modbus.Modbus;
import com.ghgande.j2mod.modbus.ModbusIOException;
import com.ghgande.j2mod.modbus.msg.ModbusMessage;
import com.ghgande.j2mod.modbus.msg.ModbusRequest;
import com.ghgande.j2mod.modbus.msg.ModbusResponse;
import com.ghgande.j2mod.modbus.util.ModbusUtil;

/**
 * Class that implements the ModbusRTU transport
 * flavor.
 *
 * @author John Charlton
 * @author Dieter Wimberger
 *
 * @version 1.2rc1 (09/11/2004)
 */
public class ModbusRTUTransport
    extends ModbusSerialTransport {

  private InputStream m_InputStream;    //wrap into filter input
  private OutputStream m_OutputStream;      //wrap into filter output

  private byte[] m_InBuffer;
  private BytesInputStream m_ByteIn;         //to read message from
  private BytesOutputStream m_ByteInOut;     //to buffer message to
  private BytesOutputStream m_ByteOut;      //write frames
  private byte[] lastRequest = null;
  private	boolean osIsKnown = false;
  private	boolean osIsWindows = false;

  public void writeMessage(ModbusMessage msg) throws ModbusIOException {
    try {
      int len;
      synchronized (m_ByteOut) {
        // first clear any input from the receive buffer to prepare
        // for the reply since RTU doesn't have message delimiters
        clearInput();
        //write message to byte out
        m_ByteOut.reset();
        msg.setHeadless();
        msg.writeTo(m_ByteOut);
        len = m_ByteOut.size();
        int[] crc = ModbusUtil.calculateCRC(m_ByteOut.getBuffer(), 0, len);
        m_ByteOut.writeByte(crc[0]);
        m_ByteOut.writeByte(crc[1]);
        //write message
        len = m_ByteOut.size();
        byte buf[] = m_ByteOut.getBuffer();
        m_OutputStream.write(buf, 0, len);     //PDU + CRC
        m_OutputStream.flush();
        if(Modbus.debug)
        	System.err.println("Sent: " + ModbusUtil.toHex(buf, 0, len));
        // clears out the echoed message
        // for RS485
        if (m_Echo) {
          readEcho(len);
        }
        lastRequest = new byte[len];
        System.arraycopy(buf, 0, lastRequest, 0, len);
      }

    } catch (Exception ex) {
      throw new ModbusIOException("I/O failed to write");
    }

  }//writeMessage

  //This is required for the slave that is not supported
  public ModbusRequest readRequest() throws ModbusIOException {
    throw new RuntimeException("Operation not supported.");
  } //readRequest

  /**
   * Clear the input if characters are found in the input stream.
   *
   * @throws ModbusIOException
   */
  public void clearInput() throws IOException {
    if (m_InputStream.available() > 0) {
      int len = m_InputStream.available();
      byte buf[] = new byte[len];
      m_InputStream.read(buf, 0, len);
      if(Modbus.debug)
    	  System.err.println("Clear input: " + ModbusUtil.toHex(buf, 0, len));
    }
  }//cleanInput

  public ModbusResponse readResponse()
      throws ModbusIOException {

    boolean done = false;
    ModbusResponse response = null;
    int dlength = 0;

    try {
      do {
        //1. read to function code, create request and read function specific bytes
        synchronized (m_ByteIn) {
          int uid = m_InputStream.read();
          if (uid != -1) {
            int fc = m_InputStream.read();
            m_ByteInOut.reset();
            m_ByteInOut.writeByte(uid);
            m_ByteInOut.writeByte(fc);

            //create response to acquire length of message
            response = ModbusResponse.createModbusResponse(fc);
            response.setHeadless();

            // With Modbus RTU, there is no end frame.  Either we assume
            // the message is complete as is or we must do function
            // specific processing to know the correct length.  To avoid
            // moving frame timing to the serial input functions, we set the
            // timeout and to message specific parsing to read a response.
            getResponse(fc, m_ByteInOut);
            dlength = m_ByteInOut.size() - 2; // less the crc
            if (Modbus.debug)
            	System.err.println("Response: " +
            			ModbusUtil.toHex(
            					m_ByteInOut.getBuffer(), 0, dlength + 2));

            m_ByteIn.reset(m_InBuffer, dlength);

            //check CRC
            int[] crc = ModbusUtil.calculateCRC(m_InBuffer, 0, dlength); //does not include CRC
            if (ModbusUtil.unsignedByteToInt(m_InBuffer[dlength]) != crc[0]
                && ModbusUtil.unsignedByteToInt(m_InBuffer[dlength + 1]) != crc[1]) {
            	if (Modbus.debug)
            		System.err.println("CRC should be " + crc[0] + ", " + crc[1]);
              throw new IOException("CRC Error in received frame: " + dlength + " bytes: " + ModbusUtil.toHex(m_ByteIn.getBuffer(), 0, dlength));
            }
          } else {
            throw new IOException("Error reading response");
          }

          //read response
          m_ByteIn.reset(m_InBuffer, dlength);
          if (response != null) {
            response.readFrom(m_ByteIn);
          }
          done = true;
        }//synchronized
      } while (!done);
      return response;
    } catch (Exception ex) {
    	if (Modbus.debug) {
    		System.err.println("Last request: " + ModbusUtil.toHex(lastRequest));
    		System.err.println(ex.getMessage());
    	}
    	throw new ModbusIOException("I/O exception - failed to read");
    }
  }//readResponse

  /**
   * Prepares the input and output streams of this
   * <tt>ModbusRTUTransport</tt> instance.
   *
   * @param in the input stream to be read from.
   * @param out the output stream to write to.
   * @throws IOException if an I\O error occurs.
   */
  public void prepareStreams(InputStream in, OutputStream out)
      throws IOException {
    m_InputStream = in;   //new RTUInputStream(in);
    m_OutputStream = out;

    m_ByteOut = new BytesOutputStream(Modbus.MAX_MESSAGE_LENGTH);
    m_InBuffer = new byte[Modbus.MAX_MESSAGE_LENGTH];
    m_ByteIn = new BytesInputStream(m_InBuffer);
    m_ByteInOut = new BytesOutputStream(m_InBuffer);
  } //prepareStreams

  public void close() throws IOException {
    m_InputStream.close();
    m_OutputStream.close();
  }//close
  
  private void getResponse(int fn, BytesOutputStream out)
    throws IOException {
    int bc = -1, bc2 = -1, bcw = -1;
    int inpBytes = 0;
    byte inpBuf[] = new byte[256];
    
    int tmOut = m_CommPort.getReceiveTimeout();
    if (tmOut == 0) {
    	try {
			m_CommPort.enableReceiveTimeout(250);
		} catch (UnsupportedCommOperationException e) {
			// Who cares ...
		}
    }
    
    if (! osIsKnown) {
    	String osName = System.getProperty("os.name");
		if (osName.toLowerCase().startsWith("win"))
			osIsWindows = true;
		
		osIsKnown = true;
    }
    
    try {
    	if ((fn & 0x80) == 0) {
    		switch (fn) {
    		case 0x01:
    		case 0x02:
    		case Modbus.READ_MULTIPLE_REGISTERS:
    		case 0x04:
    		case 0x0C:
    		case 0x11:  // report slave ID version and run/stop state
    		case 0x14:  // read log entry (60000 memory reference)
    		case 0x15:  // write log entry (60000 memory reference)
    		case 0x17:
    			// read the byte count;
    			bc = m_InputStream.read();
    			out.write(bc);
    			
    			int remaining = bc + 2;
    			int offset = 0;
    			int	read = 0;
    			int	loopCount = 0;
    			// now get the specified number of bytes and the 2 CRC bytes
    			while (remaining > 0 && loopCount++ < 5) {
    				if (! osIsWindows)
    					setReceiveThreshold(remaining);
    				
    				inpBytes = m_InputStream.read(inpBuf, 0, remaining);
    				if (inpBytes > 0) {
    					out.write(inpBuf, 0, inpBytes);
    					read += inpBytes;
    					remaining -= inpBytes;
    					loopCount = 0;
    				}
    				if (remaining > 0) {
    					Thread.yield();
    				}
    			}
    			if (Modbus.debug && remaining > 0) {
    				System.err.println("Error: looking for " + (bc+2) +
    						" bytes, received " + read);
    			}
    			m_CommPort.disableReceiveThreshold();
    			break;
    		case Modbus.WRITE_COIL:
    		case Modbus.WRITE_SINGLE_REGISTER:
    		case 0x0B:
    		case 0x0F:
    		case Modbus.WRITE_MULTIPLE_REGISTERS:
    			// read status: only the CRC remains after address and function code
    			setReceiveThreshold(6);
    			inpBytes = m_InputStream.read(inpBuf, 0, 6);
    			out.write(inpBuf, 0, inpBytes);
    			m_CommPort.disableReceiveThreshold();
    			break;
    		case 0x07:
    		case 0x08:
    			// read status: only the CRC remains after address and function code
    			setReceiveThreshold(3);
    			inpBytes = m_InputStream.read(inpBuf, 0, 3);
    			out.write(inpBuf, 0, inpBytes);
    			m_CommPort.disableReceiveThreshold();
    			break;
    		case 0x16:
    			// eight bytes in addition to the address and function codes
    			setReceiveThreshold(8);
    			inpBytes = m_InputStream.read(inpBuf, 0, 8);
    			out.write(inpBuf, 0, inpBytes);
    			m_CommPort.disableReceiveThreshold();
    			break;
    		case 0x18:
    			// read the byte count word
    			bc = m_InputStream.read();
    			out.write(bc);
    			bc2 = m_InputStream.read();
    			out.write(bc2);
    			bcw = ModbusUtil.makeWord(bc, bc2);
    			// now get the specified number of bytes and the 2 CRC bytes
    			setReceiveThreshold(bcw+2);
    			inpBytes = m_InputStream.read(inpBuf, 0, bcw + 2);
    			out.write(inpBuf, 0, inpBytes);
    			m_CommPort.disableReceiveThreshold();
    			break;
    		case 0x2b:
    			// read the subcode. We only support 0x0e.
    			int sc = m_InputStream.read();
    			if (sc != 0x0e)
    				throw new IOException("Invalid subfunction code");

    			out.write(sc);
    			// next few bytes are just copied.
    			setReceiveThreshold(5);
    			int id, fieldCount;
    			int cnt = m_InputStream.read(inpBuf, 0, 5);
    			out.write(inpBuf, 0, cnt);
    			id = (int) inpBuf[0];
    			fieldCount = (int) inpBuf[4];
    			for (int i = 0;i < fieldCount;i++) {
    				setReceiveThreshold(1);
    				id = m_InputStream.read();
    				out.write(id);
    				int len = m_InputStream.read();
    				out.write(len);
    				setReceiveThreshold(len);
    				len = m_InputStream.read(inpBuf, 0, len);
    				out.write(inpBuf, 0, len);
    			}
    			if (fieldCount == 0) {
    				setReceiveThreshold(1);
    				int err = m_InputStream.read();
    				out.write(err);
    			}
    			// now get the 2 CRC bytes
    			setReceiveThreshold(2);
    			inpBytes = m_InputStream.read(inpBuf, 0, 2);
    			out.write(inpBuf, 0, 2);
    			m_CommPort.disableReceiveThreshold();
    			m_CommPort.disableReceiveTimeout();
    		}
    	} else {
    		// read the exception code, plus two CRC bytes.
    		setReceiveThreshold(3);
    		inpBytes = m_InputStream.read(inpBuf, 0, 3);
    		out.write(inpBuf, 0, 3);
    		m_CommPort.disableReceiveThreshold();

    	}
    } catch (IOException e) {
      m_CommPort.disableReceiveThreshold();
      throw new IOException("getResponse serial port exception");
    }
    if (tmOut == 0)
    	m_CommPort.disableReceiveTimeout();
    
  }//getResponse
  
} //ModbusRTUTransport
