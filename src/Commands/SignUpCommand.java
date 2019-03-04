package Commands;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Envelope;

import Model.Authentication;;

public class SignUpCommand extends ConcreteCommand {
	public void execute() throws NoSuchAlgorithmException {
		this.consume("r3");
		HashMap<String, Object> props = parameters;
		Channel channel = (Channel) props.get("channel");
		JSONParser parser = new JSONParser();
		String username = "";
		String password = "";
		String user_type = "";
		try {
			JSONObject body = (JSONObject) parser.parse((String) props.get("body"));
//	        System.out.println("The BODY is: " + body.toString());
			JSONObject params = (JSONObject) parser.parse(body.get("body").toString());
//			System.out.println("The params are: " + body.toString());
			username = params.get("username").toString();
			password = params.get("password").toString();
			user_type = params.get("user_type").toString();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		AMQP.BasicProperties properties = (AMQP.BasicProperties) props.get("properties");
		AMQP.BasicProperties replyProps = (AMQP.BasicProperties) props.get("replyProps");
		Envelope envelope = (Envelope) props.get("envelope");
		String response = Authentication.CreateUser(username, password, user_type);
		sendMessage("database", properties.getCorrelationId(), response);
	}

	@Override
	public void handleApi(HashMap<String, Object> service_parameters) {
		HashMap<String, Object> props = parameters;
		AMQP.BasicProperties properties = (AMQP.BasicProperties) props.get("properties");
		AMQP.BasicProperties replyProps = (AMQP.BasicProperties) props.get("replyProps");
//		String serviceBody = service_parameters.get("body").toString();
		String response = "";
		Envelope envelope = (Envelope) props.get("envelope");
		try {
			channel.basicPublish("", properties.getReplyTo(), replyProps, response.getBytes("UTF-8"));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}
