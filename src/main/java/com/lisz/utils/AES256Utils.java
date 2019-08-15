package com.lisz.utils;


import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import org.apache.tomcat.util.codec.binary.Base64;

public class AES256Utils {
	
	 /*
     * 此处使用AES-128-ECB加密模式，key需要为16位。
     */
	public final static String SKEY = "abcdefg123456788";
	
	// 加密
    public static String Encrypt(String sSrc) {
    	try {
    		 byte[] raw = SKEY.getBytes("utf-8");
    	        SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
    	        Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");//"算法/模式/补码方式"
    	        cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
    	        byte[] encrypted = cipher.doFinal(sSrc.getBytes("utf-8"));
    	        return new Base64().encodeToString(encrypted);//此处使用BASE64做转码功能，同时能起到2次加密的作用。
		} catch (Exception e) {
			return null;
		}
       
    }
 
    // 解密
    public static String Decrypt(String sSrc) {
        try {
            byte[] raw = SKEY.getBytes("utf-8");
            SecretKeySpec skeySpec = new SecretKeySpec(raw, "AES");
            Cipher cipher = Cipher.getInstance("AES/ECB/PKCS5Padding");
            cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            byte[] encrypted1 = new Base64().decode(sSrc);//先用base64解密
            try {
                byte[] original = cipher.doFinal(encrypted1);
                String originalString = new String(original,"utf-8");
                return originalString;
            } catch (Exception e) {
                System.out.println(e.toString());
                return null;
            }
        } catch (Exception ex) {
            return null;
        }
    }
}