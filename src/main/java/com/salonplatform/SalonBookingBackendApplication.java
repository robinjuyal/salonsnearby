package com.salonplatform;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@OpenAPIDefinition(
		info = @Info(
				title = "Salon Booking Platform API",
				version = "1.0.0",
				description = "Complete API documentation for Salon Booking and Queue Management Platform",
				contact = @Contact(
						name = "Salon Platform Team",
						email = "support@salonplatform.com"
				),
				license = @License(
						name = "MIT License",
						url = "https://opensource.org/licenses/MIT"
				)
		),
		servers = {
				@Server(
						url = "http://localhost:8080",
						description = "Local Development Server"
				),
				@Server(
						url = "https://your-app.railway.app",
						description = "Production Server"
				)
		}
)
public class SalonBookingBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(SalonBookingBackendApplication.class, args);

		System.out.println("\n" +
				"=============================================================\n" +
				"   Salon Booking Platform Started Successfully!              \n" +
				"=============================================================\n" +
				"   API Documentation: http://localhost:8080/swagger-ui.html  \n" +
				"   Health Check: http://localhost:8080/actuator/health       \n" +
				"   WebSocket: ws://localhost:8080/ws                         \n" +
				"=============================================================\n"
		);
	}
}
