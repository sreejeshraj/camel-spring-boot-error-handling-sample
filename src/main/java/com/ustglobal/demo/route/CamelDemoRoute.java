package com.ustglobal.demo.route;

import java.util.HashMap;
import java.util.Map;

import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Component
@ConfigurationProperties(prefix = "camel-demo-route")
@Data
@EqualsAndHashCode(callSuper = true)

public class CamelDemoRoute extends RouteBuilder {

	@Override
	public void configure() throws Exception {

		// @formatter:off
		
		//errorHandler(deadLetterChannel("seda:errorQueue").maximumRedeliveries(5).redeliveryDelay(1000));
		
		errorHandler(deadLetterChannel("seda:errorQueue").useOriginalMessage());
		
		onException(Exception.class)
		.routeId("myExceptionRoute")
		//.log("****EXCEPTION_CAUGHT BEFORE:${exchangeProperty.EXCEPTION_CAUGHT}****")
		.handled(true)
		.process(new ExchangePropertyPrinterProcessor())
		//.log("****EXCEPTION_CAUGHT AFTER:${exchangeProperty.EXCEPTION_CAUGHT}****")
		.log("**** myExceptionRoute - body:${body}");
		
		//from("timer://myTimer?fixedRate=true&period=60000")
		from("file://D:/Temp/input")
		.routeId("myFileRoute")
		.setBody(constant("MyCustomBodyString"))
		.log("**** After setting body - body:${body}")
		.process(new ExceptionSettingProcessor())
		//.id("ExceptionSettingProcessor")
		.log("**** After Setting Exception - body:${body})");
		
		from("seda:errorQueue")
		.routeId("myErrorHandlerRoute")
		.log("**** myErrorHandlerRoute - body:${body}");
				
		
		// @formatter:on
	}

	private final class ExchangePropertyPrinterProcessor implements Processor {
		@Override
		public void process(Exchange exchange) throws Exception {
			Exception exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, Exception.class);
			System.out.println("***** Exception message from property: "+exception+"*****");
			
			Exception exceptionFromExchange = exchange.getException();
			System.out.println("***** Exception message from exchange.getException(): "+exceptionFromExchange+"*****");
			
		}
	}

	private final class ExceptionSettingProcessor implements Processor {
		@Override
		public void process(Exchange exchange) throws Exception {
			Exception myRuntimeException = new RuntimeException("My Runtime Exception !!!");
			//exchange.setException(myRuntimeException);
			throw myRuntimeException;
		}
	}

}
