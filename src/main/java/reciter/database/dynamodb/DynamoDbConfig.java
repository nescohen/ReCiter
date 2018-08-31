package reciter.database.dynamodb;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClient;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBClientBuilder;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.exceptions.DynamoDBLocalServiceException;
import com.amazonaws.services.dynamodbv2.local.main.ServerRunner;
import com.amazonaws.services.dynamodbv2.local.server.DynamoDBProxyServer;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;

import org.apache.commons.cli.ParseException;
import org.apache.commons.lang3.StringUtils;
import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import org.socialsignin.spring.data.dynamodb.repository.config.EnableDynamoDBRepositories;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;

import com.amazonaws.services.dynamodbv2.AmazonDynamoDB;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMappingException;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;
import com.amazonaws.services.dynamodbv2.model.AttributeDefinition;
import com.amazonaws.services.dynamodbv2.model.CreateTableRequest;
import com.amazonaws.services.dynamodbv2.model.KeySchemaElement;
import com.amazonaws.services.dynamodbv2.model.KeyType;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;
import com.amazonaws.services.dynamodbv2.model.ProvisionedThroughput;
import com.amazonaws.services.dynamodbv2.model.ScalarAttributeType;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Configuration
@EnableDynamoDBRepositories
        (basePackages = "reciter.database.dynamodb.repository", dynamoDBMapperConfigRef = "dynamoDBMapperConfig")
public class DynamoDbConfig {

    private String amazonDynamoDBEndpoint = System.getenv("AMAZON_DYNAMODB_ENDPOINT");

    private String amazonAWSAccessKey = System.getenv("AMAZON_AWS_ACCESS_KEY");

    private String amazonAWSSecretKey = System.getenv("AMAZON_AWS_SECRET_KEY");
    
    private String basePackage = "reciter.database.dynamodb.model";
    
    @Value("${aws.dynamoDb.local.port}")
    private String dynamoDbLocalPort;
    
    @Value("${aws.dynamoDb.local.dbpath}")
    private String dynamoDbPath;
    
    @Value("${aws.dynamoDb.local}")
    private boolean isDynamoDbLocal;
    
    private static final Long READ_CAPACITY_UNITS = 5L;
    private static final Long WRITE_CAPACITY_UNITS = 5L;

    @Bean
    public AmazonDynamoDB amazonDynamoDB() {
    	AmazonDynamoDB amazonDynamoDB = null;
    	
    	if(isDynamoDbLocal) {
    		log.info("Using dynamodb local with port - " + dynamoDbLocalPort + " and dbPath - " + dynamoDbPath);
    		DynamoDBProxyServer server = null;
    		
    		try {
    			server = ServerRunner.createServerFromCommandLineArgs(new String[]{
					"-dbPath", 	this.dynamoDbPath,"-port", this.dynamoDbLocalPort
				});
				server.start();
				
				amazonDynamoDB = AmazonDynamoDBClientBuilder.standard().withEndpointConfiguration(
	        			new AwsClientBuilder.EndpointConfiguration("http://localhost:" + this.dynamoDbLocalPort, "us-west-2"))
	    				 .withCredentials(new AWSStaticCredentialsProvider(new AWSCredentials() {
							
							@Override
							public String getAWSSecretKey() {
								return "dummy";
							}
							
							@Override
							public String getAWSAccessKeyId() {
								return "dummy";
							}
						}))
	        			.build();
			} catch (Exception e) {
				log.error(e.getLocalizedMessage());
			} 
    		/*finally {
                if(server != null) {
                    try {
						server.stop();
					} catch (Exception e) {
						log.error(e.getLocalizedMessage());
					}
                }
                if(amazonDynamoDB != null) {
                	amazonDynamoDB.shutdown();
                }
            } */
    		
    		 
    	} else {
    		log.info("Using dynamodb AWS with endpoint - " + amazonDynamoDBEndpoint);
    		 amazonDynamoDB = new AmazonDynamoDBClient(amazonAWSCredentials());
    		if (!StringUtils.isEmpty(amazonDynamoDBEndpoint)) {
                amazonDynamoDB.setEndpoint(amazonDynamoDBEndpoint);
            }
    	}
    	if(amazonDynamoDB != null) {
    		createTables(amazonDynamoDB);
    	} else {
    		log.info("aws.dynamoDb.local needs to have a boolean value set in application.propeties");
    	}
        return amazonDynamoDB;
    }

    @Bean
    public AWSCredentials amazonAWSCredentials() {
        return new BasicAWSCredentials(amazonAWSAccessKey, amazonAWSSecretKey);
    }
    
    @Bean
    public DynamoDBMapperConfig dynamoDBMapperConfig() {
    	DynamoDBMapperConfig.Builder builder = new DynamoDBMapperConfig.Builder();
    	return builder.build();
    }
    
    /**
     * Creates the necessary tables.
     */
    private void createTables(AmazonDynamoDB amazonDynamoDB) {
    	
    	//Sometimes using Mac(OSX) there might be issues with sqllite being added to java library path. In that case copy the libsqlite4java jar to Extensions folder in /Library/Java/Extensions
    	//sudo cp ~/.m2/repository/com/almworks/sqlite4java/libsqlite4java-osx/1.0.392/libsqlite4java-osx-1.0.392.dylib /Library/Java/Extensions This will allow local dynamoDb to use sqllite for all purposes.
    	ListTablesResult listTablesResult = amazonDynamoDB.listTables();

        ClassPathScanningCandidateComponentProvider scanner =
                new ClassPathScanningCandidateComponentProvider(false);

        scanner.addIncludeFilter(new AnnotationTypeFilter(DynamoDBTable.class));

        for (BeanDefinition bd : scanner.findCandidateComponents(basePackage)) {
            Class<?> clazz = null;
            try {
                clazz = Class.forName(bd.getBeanClassName());
            } catch (ClassNotFoundException e) {
                // Never will happen. Do nothing
            }

            String tableName = clazz.getAnnotation(DynamoDBTable.class).tableName(); //String.format("%s-%s" , environment, clazz.getAnnotation(DynamoDBTable.class).tableName());

            if (!listTablesResult.getTableNames().contains(tableName)) {
                String keyName = null;
                for (Field field : clazz.getDeclaredFields()) {
                    if (field.isAnnotationPresent(DynamoDBHashKey.class)) {
                        keyName = field.getName();
                        break;
                    }
                }

                if (keyName == null) {
                    throw new DynamoDBMappingException(String.format("The class %s has a not valid Dynamo Hash Key",
                                                                     bd.getBeanClassName()));
                }

                List<AttributeDefinition> attributeDefinitions = new ArrayList<AttributeDefinition>();
                attributeDefinitions.add(new AttributeDefinition().withAttributeName(keyName).withAttributeType(ScalarAttributeType.S));

                List<KeySchemaElement> keySchemaElements = new ArrayList<KeySchemaElement>();
                keySchemaElements.add(new KeySchemaElement().withAttributeName(keyName).withKeyType(KeyType.HASH));

                CreateTableRequest request =
                        new CreateTableRequest()
                                                .withTableName(tableName)
                                                .withKeySchema(keySchemaElements)
                                                .withAttributeDefinitions(attributeDefinitions)
                                                .withProvisionedThroughput(
                                                                           new ProvisionedThroughput().withReadCapacityUnits(READ_CAPACITY_UNITS)
                                                                                                      .withWriteCapacityUnits(WRITE_CAPACITY_UNITS));

                amazonDynamoDB.createTable(request);
            }

        }
    	
    }
    
    
}