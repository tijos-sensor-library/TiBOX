package tibox;

import java.io.IOException;

import tijos.framework.component.rs485.TiRS485;
import tijos.framework.devicecenter.TiGPIO;
import tijos.framework.devicecenter.TiUART;
import tijos.framework.sensor.bc28.IDeviceEventListener;
import tijos.framework.sensor.bc28.TiBC28;
import tijos.framework.util.Delay;

//Thread for LED flashing
class LedThread extends Thread {

	private int ledPin = 0;

	LedThread(int pin) {
		this.ledPin = pin;
	}

	public void run() {

		try {
			while (NB100.startFlash) {
				NB100.getLED().writePin(ledPin, 0); // On
				Delay.msDelay(500);
				NB100.getLED().writePin(ledPin, 1); // Off
				Delay.msDelay(500);
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}

/**
 * TiBOX NB100, it support: 1 RS485 Port (UART 1, GPIO PORT 2 PIN 4) 1 RS232
 * Port (UART 3) 1 BC28 NBIOT (UART 2) WORKING LED (GPIO PORT 1 PIN 8) NETWORK
 * LED (GPIO PORT 1 PIN 9)
 * 
 * @author TiJOS
 *
 */
public class NB100 {

	private static TiRS485 rs232;

	private static TiRS485 rs485;

	private static TiBC28 bc28;

	private static TiGPIO LED;

	public static boolean startFlash = false;

	/**
	 * Get RS232 of the NB100 Device
	 * 
	 * @return
	 * @throws IOException
	 */
	public static TiRS485 getRS232(int baudRate, int dataBitNum, int stopBitNum, int parity) throws IOException {
		if (rs232 == null) {
			// 485端口 - UART 1
			rs232 = new TiRS485(3, -1, -1); // only UART without GPIO
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
	public static TiRS485 getRS485(int baudRate, int dataBitNum, int stopBitNum, int parity) throws IOException {

		if (rs485 == null) {
			// 485端口 - UART 1, GPIO PORT 2 PIN 4
			rs485 = new TiRS485(1, 2, 4);
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
		
		if(bc28 != null)
			return ;
		
		// BC28-NBIOT使用UART2
		TiUART uart = TiUART.open(2);
		uart.setWorkParameters(8, 1, TiUART.PARITY_NONE, 9600);
		bc28 = new TiBC28(uart);

		int count = 10;
		// 查询模块射频功能状态
		if (!bc28.isMTOn()) {
			System.out.println("Turn ON MT ...");
			bc28.turnOnMT();
			while (!bc28.isMTOn()) {
				Delay.msDelay(2000);

				if (0 == count--) {
					bc28.turnOnMT();
					count = 10;
				}
			}
		}

		count = 10;
		// 查询网络是否激活
		if (!bc28.isNetworkActived()) {
			System.out.println("Active network ...");
			bc28.activeNetwork();
			Delay.msDelay(1000);
			while (!bc28.isNetworkActived()) {
				Delay.msDelay(1000);
				if (0 == count--) {
					bc28.activeNetwork();
					count = 10;
				}
			}
		}

		System.out.println(" IMSI : " + bc28.getIMSI());
		System.out.println(" IMEI : " + bc28.getIMEI());
		System.out.println(" RSSI : " + bc28.getRSSI());

		System.out.println(" Is Actived :" + bc28.isNetworkActived());
		System.out.println(" Is registered : " + bc28.isNetworkRegistred());

		System.out.println("Network Status : " + bc28.getNetworkStatus());

		System.out.println("IP Address " + bc28.getIPAddress());
		System.out.println("Date time " + bc28.getDateTime());

	}

	/**
	 * Connect to the IoT Cloud 
	 * @param serverIp  ip address of the cloud
	 * @param port 
	 * @throws IOException
	 */
	public static void networkConnet(String serverIp, int port) throws IOException {
		
		initNBIOT();

		// 设置自动连接
		bc28.configAutoConnect(true);

		// 设置服务器IP
		bc28.setCDPServer(serverIp, 5683);

		// 启用发送消息成功通知
		bc28.enableMsgNotification(true);

		// 启用新消息到达通知
		bc28.enableNewArriveMessage();
	}

	/**
	 * Send data to the Cloud via COAP protocol
	 * @param dataBuffer
	 * @throws IOException
	 */
	public static void networkCoAPSend(byte[] dataBuffer) throws IOException {
		initNBIOT();

		bc28.coapSend(dataBuffer);
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
	 * @param id :  0 - WORK LED 1 - NET LED
	 * @return
	 * @throws IOException
	 */
	public static void turnOnLED(int id) throws IOException {
		if(id == 0)
			getLED().writePin(8, 0);
		else
			getLED().writePin(9, 0);
	}

	/**
	 * Turn OFF working LED
	 * @param id :  0 - WORK LED 1 - NET LED
	 * @throws IOException
	 */
	public static void turnOffLED(int id) throws IOException {	
		if(id == 0)
			getLED().writePin(8, 1);
		else
			getLED().writePin(9, 1);
	}

	/**
	 * Start flashing working LED
	 * @param id :  0 - WORK LED 1 - NET LED 
	 * @throws IOException
	 */
	public static void startFlashLED(int id) throws IOException {
		initLED();
		startFlash = true;
		int pin = 0;
		if(id == 0)
			pin = 8;
		else 
			pin = 9;
		
		new LedThread(pin).start();

	}

	/**
	 * Stop flashing working LED, WORK AND LED will be stopped together
	 * @param id :  0 - WORK LED 1 - NET LED 
	 * @throws IOException
	 */
	public static void stopFlashLED(int id) throws IOException {
		startFlash = false;
	}

	/**
	 * Event listener for data received from NB-IOT
	 * @param eventListener
	 */
	public static void setNBEventListener(IDeviceEventListener eventListener) {
		bc28.setEventListener(eventListener);
	}
	
	/**
	 * Initialize GPIOs for LED WORKING LED: GPIO PORT 1 PIN 8 NETWORK LED: GPIO
	 * PORT 1 PIN 9
	 * 
	 * @throws IOException
	 */
	private static void initLED() throws IOException {
		if (LED == null) {
			LED = TiGPIO.open(1, 8, 9);
			LED.setWorkMode(8, TiGPIO.OUTPUT_PP);
			LED.setWorkMode(9, TiGPIO.OUTPUT_PP);
		}
	}

}
