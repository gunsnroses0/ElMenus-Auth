import com.rabbitmq.client.*;

import Commands.Command;
import Commands.LoginCommand;
import Commands.SignUpCommand;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeoutException;

public class AuthService {
	private static final String RPC_QUEUE_NAME = "auth-request";

	public static void main(String[] argv) {

		// initialize thread pool of fixed size
		final ThreadPoolExecutor executor = (ThreadPoolExecutor) Executors.newFixedThreadPool(4);

		ConnectionFactory factory = new ConnectionFactory();
		String host = System.getenv("RABBIT_MQ_SERVICE_HOST");
		factory.setHost(host);
		Connection connection = null;
		try {
			connection = factory.newConnection();
			final Channel channel = connection.createChannel();

			channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);

			System.out.println(" [x] Awaiting RPC requests");

			Consumer consumer = new DefaultConsumer(channel) {
				@Override
				public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
						byte[] body) throws IOException {
					AMQP.BasicProperties replyProps = new AMQP.BasicProperties.Builder()
							.correlationId(properties.getCorrelationId()).build();
					System.out.println("Responding to corrID: " + properties.getCorrelationId());

					try {
						String message = new String(body, "UTF-8");
						JSONParser parser = new JSONParser();
						JSONObject messageBody = (JSONObject) parser.parse(message);
						String command = (String) messageBody.get("command");
						Command cmd = null;
						switch (command) {
						case "CreateAuth":
							String url = messageBody.get("uri").toString();
							url = url.substring(1);
							String[] parametersArray = url.split("/");
							if (parametersArray[1].equals("signup")) {
								cmd = new SignUpCommand();
							} else if (parametersArray[1].equals("login")) {
								cmd = new LoginCommand();
							}
							break;
//						case "RetrieveRestaurants":
//							cmd = new RetrieveRestaurant();
//							break;
//						case "UpdateRestaurants":
//							cmd = new UpdateRestaurant();
//							break;
//						case "DeleteRestaurants":
//							cmd = new DeleteRestaurant();
//							break;
						}
						HashMap<String, Object> props = new HashMap<String, Object>();
						props.put("channel", channel);
						props.put("properties", properties);
						props.put("replyProps", replyProps);
						props.put("envelope", envelope);
						props.put("body", message);

						cmd.init(props);
						executor.submit(cmd);
					} catch (RuntimeException e) {
						System.out.println(" [.] " + e.toString());
						e.printStackTrace();
					} catch (ParseException e) {
						e.printStackTrace();
					} finally {
						synchronized (this) {
							this.notify();
						}
					}
				}
			};

			channel.basicConsume(RPC_QUEUE_NAME, false, consumer);
		} catch (IOException | TimeoutException e) {
			e.printStackTrace();
		}
	}

	public static String getCommand(String message) throws ParseException {
		JSONParser parser = new JSONParser();
		JSONObject messageJson = (JSONObject) parser.parse(message);
		String result = messageJson.get("command").toString();
		return result;
	}
}