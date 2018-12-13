package tibox;

import java.io.IOException;

import tijos.framework.appcenter.TiOTA;
import tijos.framework.component.nbiot.coap.Network;
import tijos.framework.component.serialport.TiSerialPort;
import tijos.framework.devicecenter.TiGPIO;
import tijos.framework.util.Delay;
import tijos.framework.util.json.JSONException;
import tijos.framework.util.json.JSONObject;
import tijos.framework.util.json.JSONTokener;

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

		return rs232;
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
	 * Startup NBIoT Network
	 * 
	 * @return
	 */
	public static void networkStartup() throws IOException {
		Network.getInstance().startUp();
		if(Network.getInstance().isPSM())
		{
			Network.getInstance().disablePSM();
		}
	}

	/**
	 * Get NBIoT IMEI
	 * 
	 * @return
	 * @throws IOException
	 */
	public static String networkGetIMEI() throws IOException {
		return Network.getInstance().getIMEI();
	}
	
	/**
	 * Get NBIoT RSSI signal strength
	 * @return
	 * @throws IOException
	 */
	public static int networkGetRSSI() throws IOException {
		return Network.getInstance().getRSSI();
	}

	/**
	 * Connect to the COAP Server
	 * 
	 * @param url
	 *            COAP server url
	 * @throws IOException
	 */
	public static void networkCoAPConnect(String url) throws IOException {
		Network.getInstance().connect(url);
	}

	/**
	 * POST text to COAP Server, JSON is recommended
	 * 
	 * @param uri
	 *            uri containing data
	 * @param jsonText
	 *            JSON text to be sent
	 * @throws IOException
	 */
	public static void networkCoAPPOST(String uri, String jsonText) throws IOException {
		Network.getInstance().send(uri, jsonText);
	}

	/**
	 * GET text from COAP Server, JSON is recommended
	 * 
	 * @param topic
	 *            uri to get
	 * @return text
	 * @throws IOException
	 */
	public static String networkCoAPGET(String uri) throws IOException {
		return Network.getInstance().receive(uri);
	}

	/**
	 * GET OTA request parameters from server
	 * 
	 * @param OTAUri
	 * @return
	 * @throws IOException
	 */
	public static String networkGetOTARequest(String OTAUri) throws IOException {
		String otaRequest = networkCoAPGET(OTAUri);
		return otaRequest;
	}

	/**
	 * start OTA process
	 * 
	 * @param otaRequest
	 *            otaRequest from cloud
	 * @return
	 */
	public static void networkOTA(String productKey, String otaAppName, String otaRequest) throws IOException {
		try {
			JSONTokener jsonTokener = new JSONTokener(otaRequest);
			JSONObject otaProps = (JSONObject) jsonTokener.nextValue();
			String server = otaProps.getString("server");
			String appname = otaProps.getString("appname");
			int appsize = otaProps.getInt("appsize");
			int taskid = otaProps.getInt("taskid");

			TiOTA ota = TiOTA.getInstance(otaAppName);
			ota.execute(server, productKey, networkGetIMEI(), appname, appsize, taskid);

		} catch (JSONException ex) {
			throw new IOException(ex.getMessage());
		}

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
	 * @throws IOException
	 */
	public static void stopFlashLED() throws IOException {
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
