package org.eclipse.milo.platform.runtime.interfaces;

import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import org.bson.Document;
import org.eclipse.milo.opcua.sdk.server.nodes.UaNodeContext;
import org.eclipse.milo.platform.util.Props;

public class HistorianApi {
    String APP_SIMPLE_ARCHIVE_DATABASE = Props.getProperty("mongodb-database").toString();

    UaNodeContext uaNodeContext;
    MongoClient mongoClient;

    public HistorianApi(UaNodeContext uaNodeContext, MongoClient mongoClient) {
        this.uaNodeContext = uaNodeContext;
        this.mongoClient = mongoClient;
    }

    public FindIterable<Document> getNodeSimpleArchiveValues(String identifier, long startDate, long endDate) {
        MongoCollection<Document> simpleArchiveCollection = this.mongoClient.getDatabase(APP_SIMPLE_ARCHIVE_DATABASE).getCollection("SimpleArchive");
        Document findQuery = new Document("identifier", identifier).append("time", new Document("$gte", startDate)).append("time", new Document("$lte", endDate));
        FindIterable<Document> nodes = simpleArchiveCollection.find(findQuery);
        return nodes;
    }

}
