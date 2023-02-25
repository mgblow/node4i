package org.eclipse.milo.opcua.sdk.server.drivers;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Indexes;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.eclipse.milo.opcua.sdk.server.util.Props;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class MongoDriver {
    private MongoClient mongoClient;
    MongoCollection mongoCollection;

    public MongoDriver(String collectionName) {
        try{
            this.mongoClient = new MongoClient(Props.getProperty("mongodb-server").toString(), Integer.parseInt(Props.getProperty("mongodb-port").toString()));
            this.mongoCollection = mongoClient.getDatabase(Props.getProperty("mongodb-database").toString()).getCollection(collectionName);
        }catch (Exception e){
            LoggerFactory.getLogger(getClass()).error("can not connect to mongodb server.");
        }
    }

    public void dropIndexes() {
        this.mongoCollection.dropIndexes();
    }

    public void createIndex(String index) {
        this.mongoCollection.createIndex(Indexes.text(index));
    }

    public void saveDocument(Document document) {
        mongoCollection.insertOne(document);
    }

    public void saveDocument(String json) {
        Document document = Document.parse(json);
        mongoCollection.insertOne(document);
    }

    public void updateDocument(Document document) {
        BasicDBObject searchQuery = new BasicDBObject();
        searchQuery.append("_id", document.get("_id"));
        Document updateQuery = new Document();
        updateQuery.append("$set", document);
        this.mongoCollection.updateOne(searchQuery, updateQuery);
    }

    public Object findOneLastDocument(Map<String, String> query) {
        Document filter = new Document();
        query.forEach((key, value) -> {
            filter.append(key, value);
        });
        return this.mongoCollection.find(filter).limit(1).sort(new BasicDBObject("_id", -1)).iterator().next();
    }

    public MongoCursor findDocuments(Map<String, String> query) {
        Document filter = new Document();
        query.forEach((key, value) -> {
            filter.append(key, value);
        });
        return this.mongoCollection.find(filter).sort(new BasicDBObject("_id", -1)).iterator();
    }

    public List<Document> findDocuments(List<Bson> filters, boolean flag) {
        Bson filter = Filters.and(filters);
        final FindIterable<Document> itrDocuments = this.mongoCollection.find(filter);
        Iterator<Document> itr = itrDocuments.iterator();
        List<Document> documents = new ArrayList<>();
        itr.forEachRemaining(documents::add);
        return documents;
    }
}