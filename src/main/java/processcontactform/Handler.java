package processcontactform;

// Import Lambda dependencies
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.lambda.runtime.LambdaLogger;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;

// Import SESv2 dependencies
import com.amazonaws.services.simpleemail.AmazonSimpleEmailService;
import com.amazonaws.services.simpleemail.AmazonSimpleEmailServiceClientBuilder;
import com.amazonaws.services.simpleemail.model.Body;
import com.amazonaws.services.simpleemail.model.Content;
import com.amazonaws.services.simpleemail.model.Destination;
import com.amazonaws.services.simpleemail.model.Message;
import com.amazonaws.services.simpleemail.model.SendEmailRequest;

// Import Gson dependencies
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

// Import Java dependencies
import java.util.Map;
import java.util.HashMap;

public class Handler implements RequestHandler<APIGatewayProxyRequestEvent, String> {
	Gson gson = new GsonBuilder().setPrettyPrinting().create();
	Gson gsonUtil = new Gson();

	public String handleRequest(APIGatewayProxyRequestEvent event, Context context) {
		// Set up logger
		LambdaLogger logger = context.getLogger();
		logger.log("EVENT: " + gson.toJson(event));

		// Get post data
		Map body = gsonUtil.fromJson(event.getBody(), Map.class);
		Map params = event.getPathParameters();

		// Confirm that all required fields are present
		String[] requiredFields = {"subject", "message", "email", "token"};
		for (String field : requiredFields) {
			if (body.get(field) == null) {
				HashMap error = new HashMap();
				error.put("statusCode", 400);
				error.put("message", "Missing required field: " + field);
				logger.log(gson.toJson(error));
				return gson.toJson(error);
			}
		}

		// Validate required fields and assign to variables
		String subject = body.get("subject").toString();
		String message = body.get("message").toString();
		String submitterEmail = body.get("email").toString();
		String token = body.get("token").toString();

		// Validate reCaptcha
		if (!reCaptchaIsValid(token)) {
			HashMap error = new HashMap();
			error.put("statusCode", 422);
			error.put("message", "reCaptcha validation failed");
			logger.log(gson.toJson(error));
			return gson.toJson(error);
		}

		// Lookup target email address
		String targetEmail = System.getenv("TARGET_" + params.get("email").toString().toUpperCase());
		if (targetEmail == null) {
			HashMap error = new HashMap();
			error.put("statusCode", 400);
			error.put("message", "Email address not found for " + params.get("email"));
			logger.log(gson.toJson(error));
			return gson.toJson(error);
		}

		// Send email with Amazon SES
		// TODO: SWITCH OUT MY EMAIL ADDRESS FOR THE TARGET EMAIL ADDRESS AFTER TESTING 
		String emailResponse = sendEmail("obrien.music@gmail.com", "obrien.music@gmail.com", submitterEmail, subject, message, logger);
		Map emailResponseMap = gsonUtil.fromJson(emailResponse, Map.class);
		if (!(boolean)emailResponseMap.get("success")) {
			HashMap error = new HashMap();
			error.put("statusCode", 500);
			error.put("message", "Email failed to send: " + emailResponseMap.get("error"));
			logger.log(gson.toJson(error));
			return gson.toJson(error);
		}

		// Create response hash map
		HashMap response = new HashMap();
		response.put("statusCode", 200);
		response.put("message", "Email successfully sent to " + targetEmail);
		return gson.toJson(response);
	}

	private boolean reCaptchaIsValid(String token) {
		return true;
	}

	private String sendEmail(String from, String to, String submitterEmail, String subject, String message, LambdaLogger logger) {
		try {
			// Setup SES client
			AmazonSimpleEmailService client = 
				AmazonSimpleEmailServiceClientBuilder.standard().build();
			
			// Build SES request
			SendEmailRequest request = new SendEmailRequest()
				.withSource(from)
				.withDestination(
					new Destination().withToAddresses(to))
				.withMessage(new Message()
					.withBody(new Body()
						.withHtml(new Content()
							.withCharset("UTF-8").withData("<body style='font-size: 16px;'><p>A message from your contact form!</p><ul style='list-style-type:none; margin: 0; padding: 0;'><li><strong>From:</strong> " + submitterEmail + "</li><li><strong>Subject:</strong> " + subject + "</li><li><strong>Message:</strong> " + message + "</li></ul></body>"))
						.withText(new Content()
							.withCharset("UTF-8").withData("A message from your contact form! From: " + submitterEmail + " | Subject: " + subject + " | Message: " + message)))
					.withSubject(new Content()
						.withCharset("UTF-8").withData(subject)));

			// Send email
			String response = client.sendEmail(request).getMessageId();

			// Log and return success object
			HashMap success = new HashMap(); 
			success.put("success", true);
			success.put("message", "Email sent to " + to + " with subject " + subject);
			success.put("responseMessageId", response);
			logger.log(gson.toJson(success));
			return gson.toJson(success);
		} catch (Exception e) {
			// Log and return error object
			HashMap error = new HashMap();
			error.put("success", false);
			error.put("error", e.getMessage());
			logger.log(gson.toJson(error));
			return gson.toJson(error);
		}
	}
}