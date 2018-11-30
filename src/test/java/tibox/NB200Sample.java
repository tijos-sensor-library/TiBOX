package tibox;

import java.io.IOException;

import tijos.framework.component.modbus.rtu.ModbusClient;
import tijos.framework.component.serialport.TiSerialPort;
import tijos.framework.devicecenter.TiUART;
import tijos.framework.util.Delay;

public class NB200Sample {
	public static void main(String[] args) {
		
		try {
			
			//通讯参数
			TiSerialPort rs485 = NB200.getRS485(9600, 8, 1, TiUART.PARITY_NONE);

			//MODBUS 客户端  
			//通讯超时2000 ms 读取数据前等待5ms
			ModbusClient modbusRtu = new ModbusClient(rs485, 2000, 5);
			
			//打开WORK LED
			NB200.startFlashLED();
								
			//防止程序退出
			int count = 10;
			while(count -- > 0) {				
				MonitorProcess(modbusRtu);
				Delay.msDelay(1000 * 6); //1分钟处理一次
				
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

	
	}
}
