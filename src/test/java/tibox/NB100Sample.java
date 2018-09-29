package tibox;

import java.io.IOException;

import tijos.framework.component.modbus.rtu.ModbusClient;
import tijos.framework.component.rs485.TiRS485;
import tijos.framework.devicecenter.TiUART;
import tijos.framework.sensor.bc28.IDeviceEventListener;
import tijos.framework.util.Delay;



/**
 * NB-IOT 收到数据事件回调，电信云平台 通过onCoapDataArrived事件来进行发送数据到设备, onUDPDataArrived 可忽略
 */

class NBIOTEventListener implements IDeviceEventListener
{

	@Override
	public void onCoapDataArrived(byte []message) {
		System.out.println("onCoapDataArrived");
	}
	
	@Override
	public void onUDPDataArrived(byte [] packet) {
		System.out.println("onUDPDataArrived");
	}
}

/**
 * 
 * NB100 MODBUS Demo
 *
 */
public class NB100Sample {

	public static void main(String[] args) {
		
		try {
			
			//通讯参数
			TiRS485 rs485 = NB100.getRS485(9600, 8, 1, TiUART.PARITY_NONE);

			//MODBUS 客户端  
			//通讯超时2000 ms 读取数据前等待5ms
			ModbusClient modbusRtu = new ModbusClient(rs485, 2000, 5);
			
			//电信物联网平台分配的IP, 请换成实际的服务器IP
			String serverIp = "180.101.147.115";
			int port = 5683;

			//打开WORK LED
			NB100.turnOnLED(0);
			//NBIOT Network Connect
			NB100.networkConnet(serverIp, port);
			
			//电信云平台数据接收事件监听
			NB100.setNBEventListener(new NBIOTEventListener());
						
			//防止程序退出
			while(true) {				
				MonitorProcess(modbusRtu);
				Delay.msDelay(1000 * 60); //1分钟处理一次
				
				System.out.println("running..." + System.currentTimeMillis());
			}
	

		} catch (Exception e) {
			// 有任何异常都会进行捕捉并打印，实际应用中应进行错误处理
			e.printStackTrace();

		}
		
	}
	
	/**
	 * 通过RS485基于MODBUS协议读取设备数据并通过NBIOT上传至云平台
	 * 
	 * @param modbusRtu
	 * @param iotPlatform
	 */
	public static void MonitorProcess(ModbusClient modbusRtu) {
		try {
			// MODBUS Server 设备地址
			int serverId = 1;
			// Input Register 开始地址
			int startAddr = 0;

			// Read 2 registers from start address 读取个数
			int count = 2;
			
			//读取Holding Register 
			modbusRtu.InitReadHoldingsRequest(serverId, startAddr, count);

			try {
				int result = modbusRtu.execRequest();
				
				if (result == ModbusClient.RESULT_OK) {

					int temperature = modbusRtu.getResponseRegister(modbusRtu.getResponseAddress(), false);
					int humdity = modbusRtu.getResponseRegister(modbusRtu.getResponseAddress() + 1, false);

					System.out.println("temp = " + temperature + " humdity = " + humdity);

					//上报平台
					reportSensor(temperature, humdity);
					
				} else {
					System.out.println("Modbus Error: result = " + result);
				}

				
			} catch (Exception ex) {
				ex.printStackTrace();
			}

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}
	
	/**
	 * 数据上报, 数据格式通过 OceanConnect的插件开发功能中定义
	 * 
	 * @throws IOException
	 */
	public static void reportSensor(int temperature, int humidity) throws IOException {

		byte[] dataBuffer = new byte[5];

		dataBuffer[0] = 0; // message id
		dataBuffer[1] = (byte) (humidity >> 8);
		dataBuffer[2] = (byte) (humidity & 0xFF);
		dataBuffer[3] = (byte) (temperature >> 8);
		dataBuffer[4] = (byte) (temperature & 0xFF);
		
		try {

			 NB100.networkCoAPSend(dataBuffer);

		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
