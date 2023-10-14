package usecase;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SomeSourceFileTest {

    @Test
    public void someMethodTest() {
//        MongoClient mongoClient = MongoClients.create("mongodb://localhost:27017");
        String actual = new SomeSourceFile().someMethod();
        assertEquals("someMethod", actual);
    }
}
