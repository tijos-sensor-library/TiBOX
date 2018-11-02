package tibox;

import tijos.framework.sensor.bc28.IDeviceEventListener;
import tijos.framework.util.Delay;

/**
 * NB-IOT 收到数据事件回调，UDP数据通过onUDPDataArrived获得
 */

class NBIOTUDPEventListener implements IDeviceEventListener {

	@Override
	public void onCoapDataArrived(byte[] message) {
		System.out.println("onCoapDataArrived");
	}

	@Override
	public void onUDPDataArrived(byte[] packet) {
		System.out.println("onUDPDataArrived " + new String(packet));
	}
}

public class N100UDPSample {

	public static void main(String[] args) {

		int socketId  = -1;
		try {
			// UDP Server IP, 请换成实际的服务器IP
			String serverIp = "47.92.248.3";
			int port = 9876;

			// 打开WORK LED
			NB100.turnOnLED(0);

			//NB-IoT 数据接收事件监听
			NB100.setNBEventListener(new NBIOTUDPEventListener());

			//创建UDP Socket, 绑定本地9999端口
			socketId = NB100.networkUDPCreate(9999);
			if(socketId < 0 ) {
				System.out.println("Failed to create socket.");
				return ;
			}

			//发送数据
			String data = "test";
			int count = 0;
			while(count ++ < 10) 
			{
				NB100.networkUDPSend(socketId, serverIp, port, data.getBytes());
				
				Delay.msDelay(10000);
			}

			// 防止程序退出
			while (true) {
				Delay.msDelay(1000 * 60); // 1分钟处理一次

				System.out.println("running..." + System.currentTimeMillis());
			}
		} catch (Exception ex) {
			ex.printStackTrace();
		}
		finally {
			if(socketId > 0 )
				NB100.networkUDPClose(socketId);
		}
	}

}
