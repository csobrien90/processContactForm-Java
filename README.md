# Process Contact Form (Java)

*This is a Java application for deployment with AWS Lambda to act as a serverless backend for contact forms across many of my JAM stack websites. It uses the AWS SDK for Java to send emails via SES.*

## Getting Started

These instructions will help you get the application up and running for compilation and packaging on your local machine and deployment to AWS Lambda.

### Prerequisites

To build and run this application, you will need:

- Java 8 or later
- Apache Maven
- An AWS account with access to Lambda and SES

### Installing

- Clone this repository to your local machine
- Run `mvn package` to build the application
- Deploy the created target/process-contact-form-0.1.0.jar file to Lambda
- Set the required environment variables (`TARGET_{EMAIL}`, `SEND_FROM_{EMAIL}`, and `RECAPTCHA_KEY_{EMAIL}`) in the Lambda function

## Usage

To use the application, make a POST request to the resource /{email} with a JSON body that contains the following fields:

- `email`: the sender's email address
- `subject`: the subject of the message
- `message`: the content of the message
- `token`: a reCaptcha site key

The application will send an email to the configured recipient using SES.

## Contributing

If you would like to contribute to this project, please feel free to submit a pull request.

### Note

This is my first Java application - I learned the language specifically for this project. As such, the code is likely not very idiomatic; if you have any suggestions for improvement, please propose them in a pull request, open an issue, or [contact me directly](mailto:obrien.music@gmail.com)