package com.ustglobal.demo.route;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWithRouteBuilder;
import org.apache.camel.builder.NotifyBuilder;
import org.apache.camel.component.mock.MockEndpoint;
import org.apache.camel.model.ModelCamelContext;
import org.apache.camel.model.RouteDefinition;
import org.apache.camel.test.spring.CamelSpringBootRunner;
import org.apache.camel.test.spring.UseAdviceWith;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.annotation.DirtiesContext.ClassMode;
import org.springframework.test.context.ActiveProfiles;

@RunWith(CamelSpringBootRunner.class)
//@MockEndpoints
@DirtiesContext(classMode = ClassMode.AFTER_EACH_TEST_METHOD)
@ActiveProfiles("test")
@UseAdviceWith
@SpringBootApplication
@SpringBootTest(classes = CamelDemoRouteTest.class)
public class CamelDemoRouteTest {

	@Autowired
	ProducerTemplate producerTemplate;

	@Autowired
	ModelCamelContext camelContext;

	@Test
	public void testInputFolderToTestSedaRoute() throws Exception {
		// context should not be started because we enabled @UseAdviceWith
		assertFalse(camelContext.getStatus().isStarted());

		RouteDefinition routeToAdvice = camelContext.getRouteDefinition("InputFolderToTestSedaRoute");

		routeToAdvice.adviceWith(camelContext, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				//Producer template not required if file:// is enabled as it will pick the test file from the location specified
				//	replaceFromWith("file://<YOUR_TEST_DATA_FOLDER>");
				replaceFromWith("seda:start");
				weaveAddLast().to("mock:result");
			}
		});
		// manual start camel
		camelContext.start();
		MockEndpoint mockResultEndpoint = camelContext.getEndpoint("mock:result", MockEndpoint.class);

		// Given
		String message = "sampleMessage";
		mockResultEndpoint.expectedBodiesReceived(message);

		// When
		producerTemplate.sendBody("seda:start", message);

		// Then
		mockResultEndpoint.assertIsSatisfied();

	}
	
	
	@Test
	public void testTestSedaToOutputFolderRoute() throws Exception {
		// context should not be started because we enabled @UseAdviceWith
		assertFalse(camelContext.getStatus().isStarted());

		RouteDefinition routeToAdvice = camelContext.getRouteDefinition("TestSedaToOutputFolderRoute");

		routeToAdvice.adviceWith(camelContext, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
				//replaceFromWith("seda:start");
				//weaveAddLast().to("mock:result");
				//mockEndpointsAndSkip("file://{{outputFolder}}");
				
				/*interceptSendToEndpoint("seda:errorQueue")
				.to("mock:error");*/
				
				interceptSendToEndpoint("file://*")
				.skipSendToOriginalEndpoint()
				.to("mock:result");
				/*.process(new Processor() {
					
					@Override
					public void process(Exchange exchange) throws Exception {
						// TODO Auto-generated method stub
						
					}
				});*/
				
			}
		});
		// manual start camel
		camelContext.start();
		MockEndpoint mockResultEndpoint = camelContext.getEndpoint("mock:result", MockEndpoint.class);

		// Given
		String message = "sampleMessage";
		mockResultEndpoint.expectedBodiesReceived(message);

		// When
		producerTemplate.sendBody("seda:testSeda", message);

		// Then
		mockResultEndpoint.assertIsSatisfied();

	}
	
	
	@Ignore
	@Test
	public void testExceptionInInputFolderToTestSedaRoute() throws Exception {
		// context should not be started because we enabled @UseAdviceWith
		assertFalse(camelContext.getStatus().isStarted());

		RouteDefinition routeToAdvice = camelContext.getRouteDefinition("InputFolderToTestSedaRoute");

		routeToAdvice.adviceWith(camelContext, new AdviceWithRouteBuilder() {
			@Override
			public void configure() throws Exception {
			
				replaceFromWith("seda:start");
				
				interceptSendToEndpoint("seda://testSeda")
				.skipSendToOriginalEndpoint()
				.process(new Processor() {
					
					@Override
					public void process(Exchange exchange) throws Exception {
						throw new RuntimeException("CUSTOM EXCEPTION!!!");
						
					}
				});
			}
		});
		
		NotifyBuilder errorRouteNotifier = new NotifyBuilder(camelContext)
				   // .wereSentTo("seda:errorQueue")
					.fromRoute("ErrorHandlingRoute*")
				    .whenReceived(1)
				    .create();
		
		
		// manual start camel
		camelContext.start();
		
		
		
		
		
		// Given
		String message = "sampleMessage";
		// When
		producerTemplate.sendBody("seda:start", message);
		
		boolean done = errorRouteNotifier.matches(5, TimeUnit.SECONDS);
		assertTrue("Should have thrown Exception and caught at errorQueue", done);

		

	}


}
