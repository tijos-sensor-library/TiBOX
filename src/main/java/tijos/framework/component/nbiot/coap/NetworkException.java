package tijos.framework.component.nbiot.coap;

import java.io.IOException;

/**
 * NetworkException
 * 
 * @author adny
 *
 */
public class NetworkException extends IOException {

	/**
	 * NB-IoT错误
	 */
	public static final int NETWORK_NBIOT_ERROR = -1;
	/**
	 * NB-IoT连接错误
	 */
	public static final int NETWORK_NBIOT_CONN_FAIL = -2;
	/**
	 * COAP连接错误
	 */
	public static final int NETWORK_COAP_CONN_FAIL = -3;
	/**
	 * COAP发送失败
	 */
	public static final int NETWORK_COAP_POST_FAIL = -4;
	/**
	 * COAP接收失败
	 */
	public static final int NETWORK_COAP_GET_FAIL = -5;
	/**
	 * 未知错误
	 */
	public static final int NETWORK_UNKONWN_ERROR = -6;

	/**
	 * 错误码与消息
	 */
	int _code = 0;
	String _msg = null;

	/**
	 * 构造
	 * 
	 * @param code 错误码
	 * @param msg  错误消息
	 */
	public NetworkException(int code, String msg) {
		this._code = code;
		this._msg = msg;
	}

	/**
	 * 构造
	 * 
	 * @param code 错误码
	 */
	public NetworkException(int code) {
		this._code = code;
	}

	/**
	 * 获取错误码
	 * 
	 * @return 错误码
	 */
	public int getErrCode() {
		return this._code;
	}

	/**
	 * 获取错误消息
	 * 
	 * @return 消息
	 */
	public String getErrMsg() {
		return this._msg;
	}
}
