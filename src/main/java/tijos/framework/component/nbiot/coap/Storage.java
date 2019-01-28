package tijos.framework.component.nbiot.coap;

import java.io.IOException;

import tijos.framework.platform.util.KeyValueStorage;
import tijos.framework.util.LittleBitConverter;

/**
 * @author andy
 */
public class Storage {

    /**
     * 存储区引用
     */
    static Storage storage = null;
    static String group = "nbiot.coap";

    /**
     * 键值存储器引用
     */
    KeyValueStorage _storage = null;

    /**
     * 构造
     */
    private Storage() {
        this._storage = KeyValueStorage.getInstance();
    }

    /**
     * 获取引用
     *
     * @return 引用
     */
    public static Storage getInstance() {
        if (storage == null) {
            storage = new Storage();
        }
        return storage;
    }

    /**
     * 清除存储器
     *
     * @throws IOException
     */
    public void clean() throws IOException {
        this._storage.deleteGroup(group);
    }

    /**
     * 删除键
     *
     * @param key 键名
     * @throws IOException
     */
    public void delete(String key) throws IOException {
        this._storage.deleteKey(group, key);
    }

    /**
     * 写字符串
     *
     * @param key   键名
     * @param value 字符串
     * @throws IOException
     */
    public void writeString(String key, String value) throws IOException {
        this._storage.writeValue(group, key, value.getBytes());
    }

    /**
     * 写整形
     *
     * @param key   键名
     * @param value 整数值
     * @throws IOException
     */
    public void writeInteger(String key, int value) throws IOException {
        this._storage.writeValue(group, key, LittleBitConverter.GetBytes(value));
    }

    /**
     * 写数组
     *
     * @param key   键名
     * @param value 数组
     * @throws IOException
     */
    public void writeArray(String key, byte[] value) throws IOException {
        this._storage.writeValue(group, key, value);
    }

    /**
     * 写浮点数
     *
     * @param key   键名
     * @param value 浮点数值
     * @throws IOException
     */
    public void writeFloat(String key, float value) throws IOException {
        int fval = Float.floatToIntBits(value);
        this._storage.writeValue(group, key, LittleBitConverter.GetBytes(fval));
    }

    /**
     * 读字符串
     *
     * @param key 键名
     * @return 字符串
     * @throws IOException
     */
    public String readString(String key) throws IOException {
        byte[] value = this._storage.readValue(group, key);
        if (value == null) {
            return null;
        }
        return new String(value);
    }

    /**
     * 读整型
     *
     * @param key 键名
     * @return 整数
     * @throws IOException
     */
    public int readInteger(String key) throws IOException {
        byte[] value = this._storage.readValue(group, key);
        if (value == null) {
            return Integer.MIN_VALUE;
        }
        return LittleBitConverter.ToInt32(value, 0);
    }

    /**
     * 读数组
     *
     * @param key 键名
     * @return 数组
     * @throws IOException
     */
    public byte[] readArray(String key) throws IOException {
        byte[] value = this._storage.readValue(group, key);
        return value;
    }

    /**
     * 读浮点数
     *
     * @param key 键名
     * @return 数组
     * @throws IOException
     */
    public float readFloat(String key) throws IOException {
        byte[] value = this._storage.readValue(group, key);
        int fval = LittleBitConverter.ToInt32(value, 0);
        return Float.intBitsToFloat(fval);
    }
}
