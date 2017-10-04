/*
 * micro-mongo
 * ----------------------------------------------------------------
 * Version v0.1
 * Date Aug 16, 2016
 * Author Dinesh B
 * ----------------------------------------------------------------
 * List of Authors & Email:
 * Dinesh -> dinesh.bhavsar@siemens.com
 * Sid -> siddhartha.motghare@siemens.com
 * ----------------------------------------------------------------
 * 1. 1-Nov-2014 Dinesh B Initial version
 * ----------------------------------------------------------------
 * (C) 2014, Siemens Building Technologies, Inc.
 * ----------------------------------------------------------------
 */

package generic.mongo.microservices;

import java.util.Arrays;
import java.util.List;

import org.apache.catalina.connector.Connector;
import org.apache.coyote.http2.Http2Protocol;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.amqp.RabbitAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.embedded.EmbeddedMongoProperties;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatConnectorCustomizer;
import org.springframework.boot.context.embedded.tomcat.TomcatEmbeddedServletContainerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;

import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;

import generic.mongo.microservices.config.GhanaHandlerMethodArgumentResolver;

/**
 * @classDescription
 */
@Configuration
@SpringBootApplication
@ComponentScan(basePackageClasses = BasePackage.class)
@EnableAutoConfiguration(exclude = { RabbitAutoConfiguration.class, EmbeddedMongoAutoConfiguration.class, EmbeddedMongoProperties.class, DataSourceAutoConfiguration.class, MongoAutoConfiguration.class, MongoDataAutoConfiguration.class })
public class GhanaDBApplication extends WebMvcConfigurerAdapter {

	public static void main(String[] args) throws Exception {
		System.setProperty("spring.config.name", "micro-mongo");
		SpringApplication.run(GhanaDBApplication.class, args);
	}
	
	@Bean
	public EmbeddedServletContainerCustomizer tomcatCustomizer() {
	    return new EmbeddedServletContainerCustomizer() {

	        @Override
	        public void customize(ConfigurableEmbeddedServletContainer container) {
	            if (container instanceof TomcatEmbeddedServletContainerFactory) {
	                ((TomcatEmbeddedServletContainerFactory) container)
	                        .addConnectorCustomizers(new TomcatConnectorCustomizer() {
	                    @Override
	                    public void customize(Connector connector) {
	                        connector.addUpgradeProtocol(new Http2Protocol());
	                    }

	                });
	            }
	        }

	    };
	}
	
	@Override
	public void addArgumentResolvers(List<HandlerMethodArgumentResolver> argumentResolvers) {
		argumentResolvers.add(new GhanaHandlerMethodArgumentResolver());
	}

	@Value("${spring.data.mongodb.username}")
	private String username;
	@Value("${spring.data.mongodb.database}")
	private String database;
	@Value("${spring.data.mongodb.password}")
	private String password;
	@Value("${spring.data.mongodb.host}")
	private String host;
	@Value("${spring.data.mongodb.port}")
	private String port;
	
	@Bean
	public MongoClient mongoClient() throws Exception {
		/** DEV **/
		/*MongoCredential credential = MongoCredential.createCredential("kandapohe", "kandapohe", "m0ng0_k@nd@p0he".toCharArray());
		MongoClient mongoClient = new MongoClient(new ServerAddress("ec2-35-160-105-209.us-west-2.compute.amazonaws.com", 26101), Arrays.asList(credential));*/
		
		/** PROD **/
		/*MongoCredential credential = MongoCredential.createCredential("kandapohe", "kandapohe", "m0ng0_k@nd@p0he".toCharArray());
		MongoClient mongoClient = new MongoClient(new ServerAddress("ec2-35-154-212-219.ap-south-1.compute.amazonaws.com", 26101), Arrays.asList(credential));*/
		
		MongoCredential credential = MongoCredential.createCredential(username, database, password.toCharArray());
		MongoClient mongoClient = new MongoClient(new ServerAddress(host, Integer.parseInt(port)), Arrays.asList(credential));
		
		//MongoClient mongoClient = new MongoClient(new ServerAddress("localhost", 27017));
		
		//MongoClient mongoClient = new MongoClient("mongodb://admin:m0ng0_k@nd@p0he@ec2-54-190-197-189.us-west-2.compute.amazonaws.com:26101/kandapohe");
		return mongoClient;
	}
}