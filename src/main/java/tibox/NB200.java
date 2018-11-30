package tibox;

import java.io.IOException;

import tijos.framework.component.serialport.TiSerialPort;
import tijos.framework.devicecenter.TiGPIO;
import tijos.framework.platform.lpwan.TiNBIoT;
import tijos.framework.util.Delay;

//Thread for LED flashing
class NB200LedThread extends Thread {

	private int ledPin = 0;

	NB200LedThread(int pin) {
		this.ledPin = pin;
	}

	public void run() {

		try {
			while (NB200.startFlash) {
				NB200.getLED().writePin(ledPin, 0); // On
				Delay.msDelay(500);
				NB200.getLED().writePin(ledPin, 1); // Off
				Delay.msDelay(500);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}


public class NB200 {

	private static TiSerialPort rs232;

	private static TiSerialPort rs485;

	private static TiGPIO LED;

	public static boolean startFlash = false;

	/**
	 * Get RS232 of the NB100 Device
	 * 
	 * @return
	 * @throws IOException
	 */
	public static TiSerialPort getRS232(int baudRate, int dataBitNum, int stopBitNum, int parity) throws IOException {
		if (rs232 == null) {
			// 232端口 - UART 1
			rs232 = new TiSerialPort(2, -1, -1); // only UART without GPIO
			rs232.open(baudRate, dataBitNum, stopBitNum, parity);
		}

		return rs485;
	}

	/**
	 * Get RS485 port of the NB100 device
	 * 
	 * @return
	 * @throws IOException
	 */
	public static TiSerialPort getRS485(int baudRate, int dataBitNum, int stopBitNum, int parity) throws IOException {

		if (rs485 == null) {
			// 485端口 - UART 1, GPIO PORT 2 PIN 3
			rs485 = new TiSerialPort(3, 2, 3);
		}

		return rs485;
	}
	
	/**
	 * Initialize NBIOT Module and Network
	 * 
	 * @return
	 * @throws IOException
	 */
	private static void initNBIOT() throws IOException {

		TiNBIoT.getInstance().startup(60);
		
		System.out.println(" IMSI : " + TiNBIoT.getInstance().getIMSI());
		System.out.println(" IMEI : " + TiNBIoT.getInstance().getIMEI());
		System.out.println(" RSSI : " + TiNBIoT.getInstance().getRSSI());

		System.out.println("IP Address " + TiNBIoT.getInstance().getPDPIP());
		System.out.println("Date time " + TiNBIoT.getInstance().getUTCTime());

	}


	/**
	 * Get GPIO of the LEDs
	 * 
	 * @return
	 * @throws IOException
	 */
	public static TiGPIO getLED() throws IOException {
		initLED();
		return LED;
	}

	/**
	 * Turn ON working LED
	 * 
	 * @param id
	 *            : 0 - WORK LED 1 - NET LED
	 * @return
	 * @throws IOException
	 */
	public static void turnOnLED() throws IOException {
			getLED().writePin(12, 0);
	}

	/**
	 * Turn OFF working LED
	 * 
	 * @param id
	 *            : 0 - WORK LED 1 - NET LED
	 * @throws IOException
	 */
	public static void turnOffLED() throws IOException {
		getLED().writePin(12, 1);
	}

	/**
	 * Start flashing working LED
	 * 
	 * @param id
	 *            : 0 - WORK LED 
	 * @throws IOException
	 */
	public static void startFlashLED() throws IOException {
		
		initLED();
		
		startFlash = true;
		int pin = 12;
		new NB200LedThread(pin).start();

	}

	/**
	 * Stop flashing working LED, WORK AND LED will be stopped together
	 * 
	 * @param id
	 *            : 0 - WORK LED 1 - NET LED
	 * @throws IOException
	 */
	public static void stopFlashLED(int id) throws IOException {
		startFlash = false;
	}

	/**
	 * Initialize GPIOs for LED WORKING LED: GPIO PORT 1 PIN 8 NETWORK LED: GPIO
	 * PORT 1 PIN 9
	 * 
	 * @throws IOException
	 */
	private static void initLED() throws IOException {
		if (LED == null) {
			LED = TiGPIO.open(0, 12);
			LED.setWorkMode(12, TiGPIO.OUTPUT_PP);
		}
	}
}
