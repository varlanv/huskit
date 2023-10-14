package usecase;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.result.InsertOneResult;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.Objects;

public class MongoLogic {

    public void run() {
        MongoClient mongoClient = MongoClients.create(System.getenv("MONGO_CONNECTION_STRING"));
        MongoDatabase test = mongoClient.getDatabase("test");
        MongoCollection<Document> testCollection = test.getCollection("test_collection");
        ObjectId objectId = new ObjectId();
        String key = "key";
        String value = "value";
        InsertOneResult insertOneResult = testCollection.insertOne(
                new Document()
                        .append("_id", objectId)
                        .append(key, value)
        );
        Document doc = testCollection.find(Filters.eq("_id", insertOneResult.getInsertedId())).first();
        if (!Objects.equals(objectId, doc.get("_id"))) {
            throw new RuntimeException();
        }
        if (!Objects.equals(value, doc.get(key))) {
            throw new RuntimeException();
        }
    }
}
