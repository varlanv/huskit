package io.huskit.containers.testcontainers.mongo;

import io.huskit.log.Log;
import io.huskit.containers.model.MongoStartedContainer;
import io.huskit.containers.model.id.ContainerId;
import io.huskit.containers.model.port.ContainerPort;
import io.huskit.containers.model.port.FixedContainerPort;
import io.huskit.containers.model.request.MongoRequestedContainer;
import io.huskit.containers.model.reuse.MongoContainerReuse;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class MongoContainer implements MongoStartedContainer {

    private final Log log;
    private final AtomicLong counter = new AtomicLong();
    private final MongoRequestedContainer mongoRequestedContainer;
    @Getter
    private volatile MongoDBContainer mongoDBContainer;

    @Override
    public ContainerId id() {
        return mongoRequestedContainer.id();
    }

    @Override
    public ContainerPort port() {
        return new FixedContainerPort(getAndStartIfNeeded().getFirstMappedPort());
    }

    @Override
    public void start() {
        getAndStartIfNeeded();
    }

    @Override
    public void close() throws Exception {
        if (mongoDBContainer != null) {
            if (mongoRequestedContainer.containerReuse().dontStop()) {
                String dropCommand = "mongo --eval 'db.adminCommand(\"listDatabases\").databases.forEach(d => {if(![\"admin\", \"config\", \"local\"].includes(d.name)) { db.getSiblingDB(d.name).dropDatabase();} });'";
                mongoDBContainer.execInContainer("/bin/sh", "-c", dropCommand);
            } else {
                log.info("Stopping mongo container [{}]", mongoRequestedContainer.id());
                long before = System.currentTimeMillis();
                mongoDBContainer.stop();
                log.info("Stopped mongo container [{}] in [{}] ms", mongoRequestedContainer.id(), System.currentTimeMillis() - before);
            }
        }
    }

    @Override
    public String connectionString() {
        MongoContainerReuse mongoContainerReuse = mongoRequestedContainer.containerReuse();
        String connectionString = getAndStartIfNeeded().getConnectionString();
        if (mongoContainerReuse.allowed() && mongoContainerReuse.newDatabaseForEachRequest()) {
            String dbName = mongoRequestedContainer.databaseName() + "_" + counter.incrementAndGet();
            String result = connectionString + "/" + dbName;
            log.info("Reusable connection string - " + result);
            return result;
        } else {
            log.info("Non reusable connection string - " + connectionString);
            return connectionString;
        }
    }

    @Override
    public Map<String, String> environment() {
        String connectionStringEnvironmentVariableName = "MONGO_CONNECTION_STRING";
        String dbNameEnvironmentVariable = "MONGO_DB_NAME";
        String portEnvironmentVariableName = "MONGO_PORT";
        MongoContainerReuse mongoContainerReuse = mongoRequestedContainer.containerReuse();
        String connectionString = getAndStartIfNeeded().getConnectionString();
        if (mongoContainerReuse.allowed() && mongoContainerReuse.newDatabaseForEachRequest()) {
            String dbName = mongoRequestedContainer.databaseName() + "_" + counter.incrementAndGet();
            String result = connectionString + "/" + dbName;
            log.info("Reusable connection string - " + result);
            return Map.of(
                    connectionStringEnvironmentVariableName, result,
                    portEnvironmentVariableName, String.valueOf(port().number()),
                    dbNameEnvironmentVariable, dbName
            );
        } else {
            log.info("Non reusable connection string - " + connectionString);
            return Map.of(
                    connectionStringEnvironmentVariableName, connectionString,
                    portEnvironmentVariableName, String.valueOf(port().number()),
                    dbNameEnvironmentVariable, mongoRequestedContainer.databaseName()
            );
        }
    }

    private MongoDBContainer getAndStartIfNeeded() {
        if (mongoDBContainer == null) {
            synchronized (this) {
                if (mongoDBContainer == null) {
                    TestContainersDelegate.setReuse();
                    mongoDBContainer = new MongoDBContainer(
                            DockerImageName.parse(
                                    mongoRequestedContainer.image().value()
                            ).asCompatibleSubstituteFor("mongo")
                    );
                    if (mongoRequestedContainer.containerReuse().allowed()) {
                        mongoDBContainer = mongoDBContainer.withLabel("id", id().toString());
                    } else {
                        mongoDBContainer = mongoDBContainer.withLabel("id", mongoRequestedContainer.source().value() + id().toString());
                    }
                    mongoDBContainer = mongoDBContainer.withReuse(true);
                    startAndLog(mongoRequestedContainer.containerReuse().newDatabaseForEachRequest());
                }
            }
        } else {
            log.info("Using existing mongo container [{}]", mongoRequestedContainer.id());
        }
        return mongoDBContainer;
    }

    private void startAndLog(boolean isReuse) {
        long before = System.currentTimeMillis();
        mongoDBContainer.start();
        if (isReuse) {
            log.info("Started mongo reusable container [{}] in [{}] ms", mongoRequestedContainer.id(), System.currentTimeMillis() - before);
        } else {
            log.info("Started mongo container [{}] in [{}] ms", mongoRequestedContainer.id(), System.currentTimeMillis() - before);
        }
    }
}
