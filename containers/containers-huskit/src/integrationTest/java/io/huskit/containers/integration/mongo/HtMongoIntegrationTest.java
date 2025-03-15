package io.huskit.containers.integration.mongo;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.model.Filters;
import io.huskit.common.HtConstants;
import io.huskit.common.Log;
import io.huskit.containers.integration.HtMongo;
import io.huskit.gradle.commontest.DockerIntegrationTest;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.MongoDBContainer;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;

class HtMongoIntegrationTest implements DockerIntegrationTest {

    @Test
    @Disabled
    @DisplayName("mongo test")
    void mongo_test() {
        System.out.println("Huskit container total memory before - "
            + Runtime.getRuntime().totalMemory() / 1024 / 1024
            + " Huskit container free memory before - "
            + Runtime.getRuntime().freeMemory() / 1024 / 1024);
        var millis = System.currentTimeMillis();
        var subject = HtMongo.fromImage(HtConstants.Mongo.DEFAULT_IMAGE)
            .withContainerSpec(spec -> spec.reuse().enabledWithCleanupAfter(Duration.ofSeconds(60)))
            .withLogger(Log.fakeVerbose())
            .start();

        var connectionString = subject.connectionString();
        System.out.println("Mongo huskit container create time - " + Duration.ofMillis(System.currentTimeMillis() - millis));
        verifyMongoConnection(connectionString);
        System.out.println("Mongo huskit container full time - " + Duration.ofMillis(System.currentTimeMillis() - millis));
        System.gc();
        System.out.println("Huskit container total memory after - "
            + Runtime.getRuntime().totalMemory() / 1024 / 1024
            + " Huskit container free memory after - "
            + Runtime.getRuntime().freeMemory() / 1024 / 1024);

        assertThat(subject.connectionString()).isEqualTo(subject.connectionString());
        assertThat(subject.id()).isNotBlank();
        assertThat(subject.properties()).containsEntry(HtConstants.Mongo.DEFAULT_CONNECTION_STRING_ENV, subject.connectionString());
        assertThat(subject.properties()).containsEntry(HtConstants.Mongo.DEFAULT_DB_NAME_ENV, HtConstants.Mongo.DEFAULT_DB_NAME);
        assertThat(subject.properties()).containsKey(HtConstants.Mongo.DEFAULT_PORT_ENV);
    }

    @Test
    @Disabled
    @DisplayName("testcontainers")
    void testcontainers() {
        System.out.println("Testcontainers total memory before - "
            + Runtime.getRuntime().totalMemory() / 1024 / 1024
            + " Testcontainers free memory before - "
            + Runtime.getRuntime().freeMemory() / 1024 / 1024);
        var time = System.currentTimeMillis();
        var mongoDbContainer = new MongoDBContainer(HtConstants.Mongo.DEFAULT_IMAGE).withReuse(true);
        mongoDbContainer.start();
        System.out.println("Mongo testcontainer create time - " + Duration.ofMillis(System.currentTimeMillis() - time));
        verifyMongoConnection(mongoDbContainer.getConnectionString());
        System.out.println("Mongo testcontainer full time - " + Duration.ofMillis(System.currentTimeMillis() - time));
        System.gc();
        System.out.println("Testcontainers total memory after - "
            + Runtime.getRuntime().totalMemory() / 1024 / 1024
            + " Testcontainers free memory after - "
            + Runtime.getRuntime().freeMemory() / 1024 / 1024);
        mongoDbContainer.stop();
    }

    private void verifyMongoConnection(String connectionString) {
        try (MongoClient mongoClient = MongoClients.create(connectionString)) {
            var test = mongoClient.getDatabase("test");
            var testCollection = test.getCollection("test_collection");
            var objectId = new ObjectId();
            var key = "key";
            var value = "value";
            var insertOneResult = testCollection.insertOne(
                new Document()
                    .append("_id", objectId)
                    .append(key, value)
            );
            var doc = testCollection.find(Filters.eq("_id", insertOneResult.getInsertedId())).first();
            assertThat(objectId).isEqualTo(doc.get("_id"));
            assertThat(value).isEqualTo(doc.get(key));
        }
    }
}
