package com.joshlong.springtips.bites;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

/**
 * This was taken from <a href
 * ="https://www.javacodemonk.com/create-hmacsha256-signature-in-java-3421c36d">this blog
 * post</a>. Thanks, JavaCodeMonk.com!
 */
abstract class HmacUtils {

	static String generateHmac256(String message, byte[] key) throws InvalidKeyException, NoSuchAlgorithmException {
		var bytes = hmac("HmacSHA256", key, message.getBytes());
		return bytesToHex(bytes);
	}

	private static byte[] hmac(String algorithm, byte[] key, byte[] message)
			throws NoSuchAlgorithmException, InvalidKeyException {
		var mac = Mac.getInstance(algorithm);
		mac.init(new SecretKeySpec(key, algorithm));
		return mac.doFinal(message);
	}

	private static String bytesToHex(byte[] bytes) {
		var hexArray = "0123456789abcdef".toCharArray();
		var hexChars = new char[bytes.length * 2];
		for (int j = 0, v; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

}
