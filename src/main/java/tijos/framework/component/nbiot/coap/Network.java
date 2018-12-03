package tijos.framework.component.nbiot.coap;

import java.io.IOException;

import tijos.framework.networkcenter.coap.CoAPClient;
import tijos.framework.networkcenter.coap.ICoAPMessageListener;
import tijos.framework.platform.TiPower;
import tijos.framework.platform.lpwan.TiNBIoT;
import tijos.framework.platform.util.SharedBuffer;
import tijos.framework.util.Delay;

/**
 * 
 * 
 * @author andy
 *
 */
public class Network implements ICoAPMessageListener {
	/**
	 * 连接超时, 70秒
	 */
	static final int NBIOT_CONNECT_TIMEOUT = 70; // 70s
	/**
	 * 待机最小时间, 120秒
	 */
	static final int NBIOT_STANDBY_MIN_TIMEOUT = 120; // 120s
	/**
	 * COAP传输超时, 10秒
	 */
	static final int NBIOT_COAP_TIMEOUT = (10 * 1000); // 10s
	/**
	 * 唤醒模式为：复位
	 */
	static final int NBIOT_WAKEUP_TYPE_RESET = 0;
	/**
	 * 唤醒模式为：外部触发
	 */
	static final int NBIOT_WAKEUP_TYPE_EXTERN = 1;
	/**
	 * 唤醒模式为：自动
	 */
	static final int NBIOT_WAKEUP_TYPE_AUTO = 2;
	/**
	 * 网络引用
	 */
	static Network network = null;

	/**
	 * NBIoT、Power等引用
	 */
	TiNBIoT _nbiot = null;
	TiPower _power = null;
	CoAPClient _coap = null;
	SharedBuffer _buffer = null;
	Object _sync = null;

	/**
	 * 自动睡眠时间阶梯表
	 */
	int[] _table = null;

	/**
	 * Json引用
	 */
	String _json = null;
	boolean _result = false;

	/**
	 * "GET"响应收到回调
	 */
	@Override
	public void getResponseArrived(String uri, int msgid, boolean result, int msgCode, byte[] payload) {
		System.out.println("getResponseArrived, uri:" + uri + ", msgid:" + msgid + ", result:" + result + ", msgCode:"
				+ msgCode + ", payload:" + ((payload != null) ? new String(payload) : null));
		this._result = result;
		if (payload != null)
			this._json = new String(payload);
		synchronized (this._sync) {
			this._sync.notify();
		}
	}

	/**
	 * "POST"响应收到回调
	 */
	@Override
	public void postResponseArrived(String uri, int msgid, boolean result, int msgCode) {
		System.out.println(
				"postResponseArrived, uri:" + uri + ", msgid:" + msgid + ", result:" + result + ", msgCode:" + msgCode);
		this._result = result;
		synchronized (this._sync) {
			this._sync.notify();
		}
	}

	/**
	 * "PUT"响应收到回调
	 */
	@Override
	public void putResponseArrived(String uri, int msgid, boolean result, int msgCode) {
	}

	/**
	 * "DELETE"响应收到回调
	 */
	@Override
	public void deleteResponseArrived(String uri, int msgid, boolean result, int msgCode) {
	}

	/**
	 * 构造
	 */
	private Network() {
		this._nbiot = TiNBIoT.getInstance();
		this._buffer = SharedBuffer.getInstance();
		this._power = TiPower.getInstance();
		this._table = new int[] { 5 * 60, 2 * 60 * 60, 6 * 60 * 60, 12 * 60 * 60, 24 * 60 * 60 };
	}

	/**
	 * COAP初始化
	 */
	private void init() {
		this._coap = CoAPClient.getInstance();
		this._sync = new Object();
		System.out.println("init()");
	}

	/**
	 * COAP反初始化
	 */
	private void deInit() {
		this._coap = null;
		this._sync = null;
		System.out.println("deInit()");
	}

	/**
	 * 获取网络引用
	 * 
	 * @return 引用
	 */
	public static Network getInstance() {
		if (network == null) {
			network = new Network();
		}
		return network;
	}

	/**
	 * 启动NB-IoT
	 * 
	 * @throws NetworkException
	 */
	public void startUp() throws NetworkException {
		try {
			System.out.println("connect...");
			this._nbiot.startup(NBIOT_CONNECT_TIMEOUT);
			System.out.println("IP:" + this._nbiot.getPDPIP());
			System.out.println("CI:" + this._nbiot.getCI());
			System.out.println("TAC:" + this._nbiot.getTAC());
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_NBIOT_CONN_FAIL, e.getMessage());
		}
	}

	/**
	 * 连接到COAP服务器
	 * 
	 * @param url 服务器连接
	 * @throws NetworkException
	 */
	public void connect(String url) throws NetworkException {
		try {
			this.init();
			this._coap.setMessageListener(this);
			this._coap.setMessageType(CoAPClient.CON);
			this._coap.connect(url);
			System.out.println("RSSI:" + this._nbiot.getRSSI());
		} catch (IOException e) {
			this.deInit();
			throw new NetworkException(NetworkException.NETWORK_COAP_CONN_FAIL, e.getMessage());
		}
	}

	/**
	 * 断开与服务器的连接
	 * 
	 * @throws NetworkException
	 */
	public void disconnect() throws NetworkException {
		try {
			int curr = (int) System.currentTimeMillis();
			while (this._coap != null && this._coap.isBusy()) {
				if ((int) System.currentTimeMillis() - curr > NBIOT_COAP_TIMEOUT) {
					throw new IOException("disconnect timeout");
				}
				Delay.msDelay(10);
			}
			this._coap.disconnect();
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_COAP_CONN_FAIL, e.getMessage());
		} finally {
			this.deInit();
		}
	}

	/**
	 * 发送JSON数据到服务器
	 * 
	 * @param uri      资源路径
	 * @param JsonText JSON串
	 * @throws NetworkException
	 */
	public void send(String uri, String JsonText) throws NetworkException {
		try {
			this._result = false;
			System.out.println("send, uri:" + uri + ", JsonText:" + JsonText);
			this._coap.post(uri, CoAPClient.APPLICATION_JSON, JsonText.getBytes());
			synchronized (this._sync) {
				this._sync.wait(NBIOT_COAP_TIMEOUT);
			}
			if (!this._result)
				throw new IOException("send fail");
		} catch (IOException | InterruptedException e) {
			throw new NetworkException(NetworkException.NETWORK_COAP_POST_FAIL, e.getMessage());
		}
	}

	/**
	 * 从服务器接收JSON数据
	 * 
	 * @param uri 资源路径
	 * @return 接收JSON串
	 * @throws NetworkException
	 */
	public String receive(String uri) throws NetworkException {
		try {
			this._result = false;
			this._json = null;
			System.out.println("receive, uri:" + uri);
			this._coap.get(uri, CoAPClient.APPLICATION_JSON);
			synchronized (this._sync) {
				this._sync.wait(NBIOT_COAP_TIMEOUT);
			}
			if (!this._result)
				throw new IOException("receive fail");
			String json = this._json;
			System.out.println("json:" + json);
			return json;

		} catch (IOException | InterruptedException e) {
			throw new NetworkException(NetworkException.NETWORK_COAP_GET_FAIL, e.getMessage());
		}
	}

	/**
	 * 获取IMEI
	 * 
	 * @return IMEI串
	 * @throws NetworkException
	 */
	public String getIMEI() throws NetworkException {
		try {
			return this._nbiot.getIMEI();
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_NBIOT_ERROR, e.getMessage());
		}
	}

	/**
	 * 获取ICCID
	 * 
	 * @return ICCID串
	 * @throws NetworkException
	 */
	public String getICCID() throws NetworkException {
		try {
			return this._nbiot.getICCID();
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_NBIOT_ERROR, e.getMessage());
		}
	}

	/**
	 * 获取PDP IP
	 * 
	 * @return PDP IP串
	 * @throws NetworkException
	 */
	public String getPDPIP() throws NetworkException {
		try {
			return this._nbiot.getPDPIP();
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_NBIOT_ERROR, e.getMessage());
		}
	}

	/**
	 * 获取CID
	 * 
	 * @return CID串
	 * @throws NetworkException
	 */
	public int getCI() throws NetworkException {
		try {
			return this._nbiot.getCI();
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_NBIOT_ERROR, e.getMessage());
		}
	}

	/**
	 * 获取RSSI
	 * 
	 * @return RSSI值
	 * @throws NetworkException
	 */
	public int getRSSI() throws NetworkException {
		try {
			return this._nbiot.getRSSI();
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_NBIOT_ERROR, e.getMessage());
		}
	}

	/**
	 * 获取BER
	 * 
	 * @return BER值
	 * @throws NetworkException
	 */
	public int getBER() throws NetworkException {
		try {
			return this._nbiot.getBER();
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_NBIOT_ERROR, e.getMessage());
		}
	}

	/**
	 * 进入睡眠
	 * 
	 * @param autoWakeupTime 唤醒超时, 单位：秒
	 * @throws NetworkException
	 */
	public void sleep(int autoWakeupTime) throws NetworkException {
		try {
			int curr = (int) System.currentTimeMillis();
			while (this._coap != null && this._coap.isBusy()) {
				if ((int) System.currentTimeMillis() - curr > NBIOT_COAP_TIMEOUT) {
					throw new IOException("sleep timeout");
				}
				Delay.msDelay(10);
			}
			int time = autoWakeupTime;
			if (time < NBIOT_STANDBY_MIN_TIMEOUT) {
				time = NBIOT_STANDBY_MIN_TIMEOUT;
			}
			// 普通睡眠启动, 则自动睡眠清除
			this._buffer.write(new byte[] { 0 }, 0, 0, 1);
			this._power.standby(time);
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_UNKONWN_ERROR, e.getMessage());
		}
	}

	/**
	 * 进入自动睡眠
	 * 
	 * @throws NetworkException
	 */
	public void autoSleep() throws NetworkException {
		try {
			int curr = (int) System.currentTimeMillis();
			while (this._coap != null && this._coap.isBusy()) {
				if ((int) System.currentTimeMillis() - curr > NBIOT_COAP_TIMEOUT) {
					throw new IOException("auto sleep timeout");
				}
				Delay.msDelay(10);
			}
			byte[] buf = new byte[1];
			this._buffer.read(buf, 0, 0, 1);
			if (buf[0] >= this._table.length) {
				throw new IOException("sleep table overflow");
			}
			int time = this._table[buf[0]];
			buf[0] += 1;
			this._buffer.write(buf, 0, 0, 1);
			this._power.standby(time);
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_UNKONWN_ERROR, e.getMessage());
		}
	}

	/**
	 * 复位
	 * 
	 * @throws NetworkException
	 */
	public void reset() throws NetworkException {
		try {
			TiPower.getInstance().reboot(0);
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_UNKONWN_ERROR, e.getMessage());
		}
	}

	/**
	 * 获取唤醒类型
	 * 
	 * @return 类型
	 * @throws NetworkException
	 */
	public int getWakeupType() throws NetworkException {
		try {
			return this._power.getStartupMode();
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_UNKONWN_ERROR, e.getMessage());
		}
	}

	/**
	 * 判断PSM是否开启
	 * 
	 * @return 是或否
	 * 
	 * @throws NetworkException
	 */
	public boolean isPSM() throws NetworkException {
		try {
			return (this._nbiot.getPSM() == null) ? false : true;
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_UNKONWN_ERROR, e.getMessage());
		}
	}

	/**
	 * 关闭PSM
	 * 
	 * @throws NetworkException
	 */
	public void disablePSM() throws NetworkException {
		try {
			this._nbiot.disablePSM();
		} catch (IOException e) {
			throw new NetworkException(NetworkException.NETWORK_UNKONWN_ERROR, e.getMessage());
		}
	}

	/**
	 * 配置自动睡眠时间阶梯表
	 * 
	 * @param RetryTime 阶梯表
	 */
	public void configAutoSleep(int[] RetryTime) {
		this._table = RetryTime;
	}
}
