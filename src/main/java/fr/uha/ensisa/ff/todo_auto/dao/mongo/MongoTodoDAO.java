package fr.uha.ensisa.ff.todo_auto.dao.mongo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.bson.types.ObjectId;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.lt;

import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.UpdateOptions;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.InsertOneResult;
import com.mongodb.client.result.UpdateResult;
import com.mongodb.client.MongoCursor;

import fr.uha.ensisa.ff.todo_auto.dao.TodoDAO;
import fr.uha.ensisa.ff.todo_auto.dao.UnknownListException;
import fr.uha.ensisa.ff.todo_auto.dao.UnknownUserException;
import fr.uha.ensisa.ff.todo_auto.dao.UserAlreadyExistsException;
import fr.uha.ensisa.ff.todo_auto.dao.dummy.DummyTask;
import fr.uha.ensisa.ff.todo_auto.dao.dummy.DummyUser;

public class MongoTodoDAO implements TodoDAO {

	private MongoDatabase database;

	public MongoTodoDAO(String url) {
		MongoClient mongoClient = MongoClients.create(url);
		database = mongoClient.getDatabase("todo");

	}

	@Override
	public void close() throws Exception {
		// TODO Auto-generated method stub

	}

	@Override
	public void registerUser(String user, String password) throws UserAlreadyExistsException {
		MongoCollection<Document> collection = database.getCollection("user");
		Bson projection = Projections.fields(Projections.include("password"), Projections.excludeId());
		Document docu = collection.find(eq("_id", user)).projection(projection).first();
		if (docu != null) {
			throw new UserAlreadyExistsException(user);
		}
		InsertOneResult result = collection.insertOne(new Document().append("_id", user).append("password", password));
	}

	@Override
	public String getUserPassword(String user) throws UnknownUserException {
		MongoCollection<Document> collection = database.getCollection("user");
		Bson projection = Projections.fields(Projections.include("password"), Projections.excludeId());
		Document docu = collection.find(eq("_id", user)).projection(projection).first();
		if (docu == null) {
			throw new UnknownUserException(user);
		}
		String password = (String) docu.get("password");
		return password;
	}

	@Override
	public String createDefaultTask(String user, String taskName) throws UnknownUserException {

		/* Check if the user exist */
		MongoCollection<Document> collectionUser = database.getCollection("user");
		Bson projection = Projections.fields(Projections.include("password"), Projections.excludeId());
		Document docu = collectionUser.find(eq("_id", user)).projection(projection).first();
		if (docu == null) {
			throw new UnknownUserException(user);
		}

		/* Add to Mongodb the task */
		MongoCollection<Document> collectionTask = database.getCollection("task");
		String id = String.valueOf(new ObjectId());
		collectionTask.insertOne(new Document().append("_id", id).append("list", "Default").append("user", user)
				.append("name", taskName).append("done", false));
		return id;
	}

	@Override
	public List<Map<String, Object>> getDefaultTasks(String user) throws UnknownUserException {

		MongoCollection<Document> collection = database.getCollection("task");
		Bson projection = Projections.fields(Projections.include("_id", "user", "name", "done"));
		MongoCursor<Document> cursor = collection.find(eq("list", "Default")).projection(projection).iterator();
		List<Map<String, Object>> listTasks = new ArrayList<>();

		while (cursor.hasNext()) {
			Document doc = cursor.next();
			String _user = (String) doc.get("user");
			if (_user.equals(user)) {
				Map<String, Object> tasks = new LinkedHashMap<>();
				tasks.put("id", doc.get("_id"));
				tasks.put("user", doc.get("user"));
				tasks.put("name", doc.get("name"));
				tasks.put("done", doc.get("done"));
				listTasks.add(tasks);
			}

		}
		return listTasks;

	}

	@Override
	public void setDefaultTaskDone(String user, String taskId, boolean done) throws UnknownUserException {
		MongoCollection<Document> collection = database.getCollection("task");
		Document query = new Document().append("_id", taskId).append("user", user);
		Bson updates = Updates.set("done", done);
		UpdateOptions options = new UpdateOptions().upsert(true);
		UpdateResult result = collection.updateOne(query, updates, options);
	}

	@Override
	public void renameDefaultTask(String user, String taskId, String newName) throws UnknownUserException {
		MongoCollection<Document> collection = database.getCollection("task");
		Document query = new Document().append("_id", taskId).append("user", user);
		Bson updates = Updates.set("name", newName);
		UpdateOptions options = new UpdateOptions().upsert(true);
		UpdateResult result = collection.updateOne(query, updates, options);

	}

	@Override
	public void deleteDefaultTask(String user, String taskId) throws UnknownUserException {
		MongoCollection<Document> collection = database.getCollection("task");
		Bson query = eq("_id", taskId);
		DeleteResult result = collection.deleteOne(query);
	}

	@Override
	public String createList(String user, String name) throws UnknownUserException {
		/* Add to Mongodb the task */
		MongoCollection<Document> collectionTask = database.getCollection("list");
		String id = String.valueOf(new ObjectId());
		collectionTask.insertOne(new Document().append("_id", id).append("listName", name).append("user", user)
				.append("taskList", new Document()));
		return id;
		// liste name; id; user; liste de tasks (document)
	}

	@Override
	public Map<String, String> getLists(String user) throws UnknownUserException {
		MongoCollection<Document> collection = database.getCollection("list");
		Bson projection = Projections.fields(Projections.include("_id", "listName", "user"));
		MongoCursor<Document> cursor = collection.find(eq("user", user)).projection(projection).iterator();
		Map<String, String> list = new LinkedHashMap<>();

		while (cursor.hasNext()) {
			Document doc = cursor.next();
			String _user = (String) doc.get("user");
			if (_user.equals(user)) {
				list.put((String) doc.get("_id"), (String) doc.get("listName"));
			}
		}
		return list;
	}

	@Override
	public void deleteList(String user, String listId) throws UnknownUserException, UnknownListException {
		MongoCollection<Document> collection = database.getCollection("list");
		Bson query = eq("_id", listId);
		DeleteResult result = collection.deleteOne(query);
	}

	@Override
	public void renameList(String user, String listId, String newName)
			throws UnknownUserException, UnknownListException {
		MongoCollection<Document> collection = database.getCollection("list");
		Document query = new Document().append("_id", listId).append("user", user);
		Bson updates = Updates.set("listName", newName);
		UpdateOptions options = new UpdateOptions().upsert(true);
		UpdateResult result = collection.updateOne(query, updates, options);
	}

	@Override
	public List<Map<String, Object>> getTasksOfList(String user, String listId)
			throws UnknownUserException, UnknownListException {
		MongoCollection<Document> collection = database.getCollection("list");
		Bson projection = Projections.fields(Projections.include("_id", "listName", "user", "taskList"));
		MongoCursor<Document> cursor = collection.find(eq("_id", listId)).projection(projection).iterator();
		List<Map<String, Object>> listTasks = new ArrayList<>();
		
		while (cursor.hasNext()) {
			Document doc = cursor.next();
			Document docTask = (Document) doc.get("taskList");
			String _user = (String) doc.get("user");
			if (_user.equals(user)) {
				Map<String, Object> tasks = new LinkedHashMap<>();
				tasks.put("id", doc.get("_id"));
				tasks.put("user", doc.get("user"));
				tasks.put("name", docTask.get("taskName"));
				tasks.put("done", docTask.get("done"));
				listTasks.add(tasks);
			}

		}
		return listTasks;
	}

	@Override
	public String createListTask(String user, String listId, String taskName)
			throws UnknownUserException, UnknownListException {
		MongoCollection<Document> collectionListTask = database.getCollection("list");
		//collectionTask.insertOne(new Document().append("_id", id).append("listName", name).append("user", user)
		//		.append("taskList", new Document()));
		String id = String.valueOf(new ObjectId());
		Document query = new Document().append("_id", listId).append("user", user);
		/*Bson updates = Updates.setOnInsert("taskList", new Document().append("_id", id).append("taskName",taskName).append("done", false));
		UpdateOptions options = new UpdateOptions().upsert(true);
		UpdateResult result = collectionListTask.updateOne(query, updates, options);
		System.out.println(result);*/
		
		return id;
	}

	@Override
	public void renameListTask(String user, String listId, String taskId, String newTaskName)
			throws UnknownUserException, UnknownListException {
	

	}
	
	

	@Override
	public void setListTaskDone(String user, String listId, String taskId, boolean done)
			throws UnknownUserException, UnknownListException {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteListTask(String user, String listId, String taskId)
			throws UnknownUserException, UnknownListException {
		// TODO Auto-generated method stub

	}

}
