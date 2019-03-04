package Model;

import java.security.MessageDigest;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.Types;
import java.util.Base64;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

public class Authentication {

	public static String GeneratePassword(String passwordToHash, byte[] salt) throws NoSuchAlgorithmException {
		String generatedPassword = null;
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(salt);
			byte[] bytes = md.digest(passwordToHash.getBytes());
			StringBuilder sb = new StringBuilder();
			for (int i = 0; i < bytes.length; i++) {
				sb.append(Integer.toString((bytes[i] & 0xff) + 0x100, 16).substring(1));
			}
			generatedPassword = sb.toString();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
		return generatedPassword;
	}

	// Add salt
	public static byte[] GenerateSalt() throws NoSuchAlgorithmException {
		SecureRandom sr = SecureRandom.getInstance("SHA1PRNG");
		byte[] salt = new byte[16];
		sr.nextBytes(salt);
		return salt;
	}

	public static String GetUserSalt(String username) throws NoSuchAlgorithmException {
		String callStatement = "{? = call Salt(?)}";
		JSONObject json = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		JSONObject inputObject = new JSONObject();
		inputObject.put("type", Types.VARCHAR);
		inputObject.put("value", username);
		jsonArray.add(inputObject);
		json.put("call_statement", callStatement);
		json.put("out_type", Types.OTHER);
		json.put("input_array", jsonArray);
		return json.toString();
	}

	public static String CreateUser(String username, String password, String user_type)
			throws NoSuchAlgorithmException {
		byte[] salt = GenerateSalt();
		password = GeneratePassword(password, salt);
		String callStatement = "{ call Add_User( ?, ?, ?, ? ) }";
		JSONObject json = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		JSONObject inputUsername = new JSONObject();
		JSONObject inputPassword = new JSONObject();
		JSONObject inputUserType = new JSONObject();
		JSONObject inputSalt = new JSONObject();
		inputUsername.put("type", Types.VARCHAR);
		inputUsername.put("value", username);
		inputPassword.put("type", Types.VARCHAR);
		inputPassword.put("value", password);
		inputUserType.put("type", Types.VARCHAR);
		inputUserType.put("value", user_type);
		inputSalt.put("type", Types.VARCHAR);
		inputSalt.put("value", Base64.getEncoder().encodeToString(salt));
		jsonArray.add(inputUsername);
		jsonArray.add(inputPassword);
		jsonArray.add(inputUserType);
		jsonArray.add(inputSalt);
		json.put("out_type", 0);
		json.put("call_statement", callStatement);
		json.put("input_array", jsonArray);
		return json.toString();
	}

	public static String LoginUser(String username, String password, String salt) throws NoSuchAlgorithmException {
		byte[] byteSalt = Base64.getDecoder().decode(salt);
		password = GeneratePassword(password, byteSalt);
		String callStatement = "{? = call Login_User( ?,? )}";
		JSONObject json = new JSONObject();
		JSONArray jsonArray = new JSONArray();
		JSONObject inputUsername = new JSONObject();
		JSONObject inputPassword = new JSONObject();
		inputUsername.put("type", Types.VARCHAR);
		inputUsername.put("value", username);
		inputPassword.put("type", Types.VARCHAR);
		inputPassword.put("value", password);
		jsonArray.add(inputUsername);
		jsonArray.add(inputPassword);
		json.put("call_statement", callStatement);
		json.put("out_type", Types.OTHER);
		json.put("input_array", jsonArray);
		return json.toString();
	}

}