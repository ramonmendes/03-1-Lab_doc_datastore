package br.com.soulhave;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.inject.Named;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.response.NotFoundException;
import com.google.appengine.api.datastore.DatastoreService;
import com.google.appengine.api.datastore.DatastoreServiceFactory;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.EntityNotFoundException;
import com.google.appengine.api.datastore.FetchOptions;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.Filter;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;
import com.google.appengine.api.datastore.Transaction;
import com.google.appengine.api.datastore.TransactionOptions;
import com.google.appengine.api.users.User;

/**
 * Defines v1 of a helloworld API, which provides simple "greeting" methods.
 */
@Api(name = "helloworld", version = "v1", scopes = { Constants.EMAIL_SCOPE }, clientIds = {
		Constants.WEB_CLIENT_ID, Constants.ANDROID_CLIENT_ID,
		Constants.IOS_CLIENT_ID }, audiences = { Constants.ANDROID_AUDIENCE })
public class Greetings {

	public static ArrayList<HelloGreeting> greetings = new ArrayList<HelloGreeting>();

	static {
		greetings.add(new HelloGreeting("hello world!"));
		greetings.add(new HelloGreeting("goodbye world!"));
	}

	public HelloGreeting getGreeting(@Named("id") Integer id)
			throws NotFoundException {
		try {
			return greetings.get(id);
		} catch (IndexOutOfBoundsException e) {
			throw new NotFoundException("Greeting not found with an index: "
					+ id);
		}
	}

	public ArrayList<HelloGreeting> listGreeting() {
		return greetings;
	}

	@ApiMethod(name = "greetings.multiply", httpMethod = "post")
	public HelloGreeting insertGreeting(@Named("times") Integer times,
			HelloGreeting greeting) {
		HelloGreeting response = new HelloGreeting();
		StringBuilder responseBuilder = new StringBuilder();
		for (int i = 0; i < times; i++) {
			responseBuilder.append(greeting.getMessage());
		}
		response.setMessage(responseBuilder.toString());
		return response;
	}

	@ApiMethod(name = "greetings.authed", path = "hellogreeting/authed")
	public HelloGreeting authedGreeting(User user) {
		HelloGreeting response = new HelloGreeting("hello " + user.getEmail());
		return response;
	}
///////////////////////////////////////////////////////////////////////////
	@ApiMethod(name = "data.save", path = "data/save")
	public WrapperReturn salvarNovoItem(User user, @Named("boardName")String boardName,
			@Named("messageTitle")String messageTitle, @Named("messageText")String messageText) {
		return saveTransaction(boardName, messageTitle, messageText);
	}

	@ApiMethod(name = "data.saveXgroup", path = "data/saveXgroup")
	public WrapperReturn saveXgroup(User user) {
		return saveCrossGroupTransacion();
	}
	
	@ApiMethod(name = "data.consultaParent", path = "data/consultaParent")
	public WrapperReturn consultaParent(User user,@Named("key")String key) {
		try {
			return parent24(key);
		} catch (EntityNotFoundException e) {
			e.printStackTrace();
		}
		return new WrapperReturn(Boolean.FALSE);
	}
	
	private WrapperReturn parent24(String key) throws EntityNotFoundException{
		String valor = null;
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();

		Query query = new Query("Entity_24").setAncestor(KeyFactory.stringToKey(key));
		List<Entity> resultado = datastore.prepare(query).asList(FetchOptions.Builder.withDefaults());
		if(resultado!=null && resultado.size() > 0){
			valor = resultado.get(0).getKey().toString() + "::"+ resultado.get(0).getProperty("valor");
		}
		return new WrapperReturn(true,valor);
	}
	
	private WrapperReturn saveCrossGroupTransacion() {
		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		TransactionOptions options = TransactionOptions.Builder.withXG(true);
		Transaction txn = datastore.beginTransaction(options);
//		Transaction txn = datastore.beginTransaction();
		int cont = 25;
		
		Entity entity_parent = new Entity("Entity_root");
		entity_parent.setProperty("valor", "root");
    	datastore.put(txn, entity_parent);

		for (int i=0; i < cont; i++){
	    	Entity entity = new Entity("Entity_"+i,entity_parent.getKey());
	    	entity.setProperty("valor", (char)i+65);
	    	datastore.put(txn, entity);
	    	entity_parent = entity;
	    }

		txn.commit();
		
		return new WrapperReturn(Boolean.TRUE);

	}
	
	private WrapperReturn saveTransaction(String boardName,
			String messageTitle, String messageText) {

		DatastoreService datastore = DatastoreServiceFactory.getDatastoreService();
		Transaction txn = datastore.beginTransaction();
		
		try {

			Date postDate = new Date();

			Key messageBoardKey = KeyFactory.createKey("MessageBoard", boardName);

			Entity message = new Entity("Message", messageBoardKey);
			message.setProperty("message_title", messageTitle);
			message.setProperty("message_text", messageText);
			message.setProperty("post_date", postDate);
			datastore.put(txn, message);

			txn.commit();

			return new WrapperReturn(boardName, messageTitle, messageText);
		} catch (Exception e) {
			return new WrapperReturn(e.getMessage());
		} finally {
			if (txn.isActive()) {
				txn.rollback();
			}
		}

	}

}

class WrapperReturn {
	String boardName;
	String messageTitle;
	String messageText;
	Date postDate;
	String fail;
	Boolean status;
	String valor;

	public String getValor() {
		return valor;
	}

	public void setValor(String valor) {
		this.valor = valor;
	}

	public String getFail() {
		return fail;
	}

	public void setFail(String fail) {
		this.fail = fail;
	}

	public Boolean getStatus() {
		return status;
	}

	public void setStatus(Boolean status) {
		this.status = status;
	}
	
	public WrapperReturn(Boolean status, String valor){
		this.status = status;
		this.valor = valor;
	}

	public WrapperReturn(String boardName, String messageTitle,
			String messageText) {
		super();
		this.boardName = boardName;
		this.messageTitle = messageTitle;
		this.messageText = messageText;
		this.postDate = new Date();
	}

	public WrapperReturn(String fail) {
		this.fail = fail;
	}
	
	public WrapperReturn(Boolean status){
		this.status = status;
	}

	public WrapperReturn() {
		// TODO Auto-generated constructor stub
	}

	public String getBoardName() {
		return boardName;
	}

	public void setBoardName(String boardName) {
		this.boardName = boardName;
	}

	public String getMessageTitle() {
		return messageTitle;
	}

	public void setMessageTitle(String messageTitle) {
		this.messageTitle = messageTitle;
	}

	public String getMessageText() {
		return messageText;
	}

	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}

	public Date getPostDate() {
		return postDate;
	}

	public void setPostDate(Date postDate) {
		this.postDate = postDate;
	}

}
