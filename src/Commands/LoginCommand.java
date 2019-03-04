package Commands;

import com.rabbitmq.client.AMQP;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import Model.Authentication;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.HashMap;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.SignatureException;
import io.jsonwebtoken.UnsupportedJwtException;

public class LoginCommand extends ConcreteCommand {

	public void execute() throws NoSuchAlgorithmException {
		this.consume("r1");
		HashMap<String, Object> props = parameters;
		String username = "";
		Channel channel = (Channel) props.get("channel");
		JSONParser parser = new JSONParser();
		try {
			JSONObject body = (JSONObject) parser.parse((String) props.get("body"));
			System.out.println(body.toString());
			JSONObject params = (JSONObject) parser.parse(body.get("body").toString());
			username = (String) params.get("username");
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (Exception e1) {
			e1.printStackTrace();
		}

		AMQP.BasicProperties properties = (AMQP.BasicProperties) props.get("properties");
		AMQP.BasicProperties replyProps = (AMQP.BasicProperties) props.get("replyProps");
		Envelope envelope = (Envelope) props.get("envelope");
		String response = "";
		response = Authentication.GetUserSalt(username);
		sendMessage("database", properties.getCorrelationId(), response);

	}

	// Sample method to construct a JWT
	private String createJWT(String username, String user_type, String secret) throws UnsupportedEncodingException {
		// The JWT signature algorithm we will be using to sign the token
		SignatureAlgorithm signatureAlgorithm = SignatureAlgorithm.HS256;
		String jwt = Jwts.builder().claim("username", username).claim("user_type", user_type)
				.signWith(signatureAlgorithm, secret.getBytes("UTF-8")).compact();
		System.out.println(jwt);
		return jwt;
	}

	private HashMap<String, String> ParseJWT(String jwt) throws ExpiredJwtException, UnsupportedJwtException, MalformedJwtException, SignatureException, IllegalArgumentException, UnsupportedEncodingException {
		HashMap<String, String> credentials = new HashMap<String, String>();
		String secret = "secret";
		Jws<Claims> claims = Jwts.parser().setSigningKey(secret.getBytes("UTF-8")).parseClaimsJws(jwt);
		System.out.println(claims.getSignature());
		System.out.println(claims.getBody().get("username"));
		credentials.put("username", (String) claims.getBody().get("username"));
		credentials.put("user_type", (String) claims.getBody().get("user_type"));
		return credentials;
	}

	@Override
	public void handleApi(HashMap<String, Object> service_parameters) {
		HashMap<String, Object> props = parameters;
		AMQP.BasicProperties properties = (AMQP.BasicProperties) props.get("properties");
		AMQP.BasicProperties replyProps = (AMQP.BasicProperties) props.get("replyProps");

		try {
			JSONParser parser = new JSONParser();
			JSONArray serviceBody = (JSONArray) parser.parse((String) service_parameters.get("body"));
			if (!serviceBody.isEmpty()) {
				JSONObject message = (JSONObject) serviceBody.get(0);
				System.out.println("Message: " + message);
				if (!message.containsKey("salt")) {
					String username = message.get("username").toString();
					String user_type = message.get("user_type").toString();
					String jwt = createJWT(username, user_type, "secret");
					ParseJWT(jwt);
					JSONObject response = new JSONObject();
					response.put("JWT", jwt);

					// TODO Re-map al UUID to old replyTo
					System.out.println("Sending to server :" + response.toString());
					System.out.println("replying to: " + properties.getReplyTo().toString());
					channel.basicPublish("", properties.getReplyTo(), replyProps, response.toString().getBytes());
				} else {
					JSONObject serverBody = (JSONObject) parser.parse((String) props.get("body"));
					JSONObject params = (JSONObject) parser.parse(serverBody.get("body").toString());
					String username = params.get("username").toString();
					String password = params.get("password").toString();
					String salt = message.get("salt").toString();
					String response = Authentication.LoginUser(username, password, salt);
					sendMessage("database", properties.getCorrelationId(), response);
				}
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		}
	}
}
