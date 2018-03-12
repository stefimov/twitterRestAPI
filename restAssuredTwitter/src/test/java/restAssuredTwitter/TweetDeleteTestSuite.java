package restAssuredTwitter;

import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.authentication.OAuthSignature;
import io.restassured.response.Response;

/*
 * Набор тестов для проверки корректности удаления твитов
 * https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/post-statuses-destroy-id
 * */

class TweetDeleteTestSuite {
	
	//Добавляем переменную класса AuthorizationDataStorage чтоб иметь доступ к данным, необходимым для авторизации.
	static final AuthorizationDataStorage authData = new AuthorizationDataStorage();
		
	/*
	 * Начальные установки для набора тестов
	 * */
	@BeforeAll
	static void setUp()
	{
		//устанавливаем стандартный путь для запросов на взаимодействие с твитами
		baseURI = "https://api.twitter.com";
		basePath = "/1.1/statuses";
	}
		
	/*
	 * Функция выполняемая после каждого тест-кейса.
	 * Необходима для удаления созданных в тест-кейсе твитов.
	 * */
	@AfterEach
	void clearTweets()
	{
		GarbageTweetsHandler.clearGarbage();
	}
	
	/*
	 * Проверка на удаление твита по id с помощью POST /destroy/:id.json
	 * Сначала мы создадим твит, затем удалим его. В ответе мы должны получить 200 OK
	 * */
	//@Test
	void testCase01()
	{
		String message = "a";	//отправляемое сообщение
		
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).									//добавляем сообщение из одного символа
				with().
				post("/update.json");						
		
		response.then().statusCode(HttpStatus.SC_OK);					//отправка сообщения должна быть успешной, ожидаемый статус ответа 200 OK
		String id = response.jsonPath().get("id_str").toString();		//получаем из ответа присвоенный нашему твиту id
		
		//удаляем твит методом /destroy/:id.json, передавая в pathParam id твита
		response = given().
		auth().
		oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
		pathParam("id_str", id).									
		with().
		post("/destroy/{id_str}.json");
		
		response.then().statusCode(HttpStatus.SC_OK);			//удаление должно завершиться ответом со статусом 200 OK
		response.then().body("id_str", equalTo(id));			//в ответе должен придти JSON с удалённым твитом, сверяем ID
	}
	
	/*
	 * Проверка на возможность получить доступ к удалённому твиту через метод GET /show.json
	 * Сначала мы создадим твит, затем удалим его. После того как тваит удалён попробуем получить к нему доступ через GET /show.json
	 * В ответ мы должны получить 404 NOT FOUND
	 * */
	//@Test
	void testCase02()
	{
		String message = "a";	//отправляемое сообщение
		
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).									//добавляем сообщение из одного символа
				with().
				post("/update.json");						
		
		response.then().statusCode(HttpStatus.SC_OK);					//отправка сообщения должна быть успешной, ожидаемый статус ответа 200 OK
		String id = response.jsonPath().get("id_str").toString();		//получаем из ответа присвоенный нашему твиту id
		
		//удаляем твит методом /destroy/:id.json, передавая в pathParam id твита
		given().
		auth().
		oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
		pathParam("id_str", id).									
		with().
		post("/destroy/{id_str}.json").
		then().
		statusCode(HttpStatus.SC_OK);			//удаление должно завершиться ответом со статусом 200 OK
		
		//пытаемся получить доступ к удалённому твиту
		given().
		auth().oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
		param("id", id).
		when().
		get("/show.json").
		then().
		statusCode(HttpStatus.SC_NOT_FOUND);
	}
	
	/*
	 * Проверка на удаление твита по id с помощью POST /destroy/:id.json без авторизации
	 * Сначала мы создадим твит, затем попытаемся удалим его без авторизации. В ответе мы должны получить 400 BAD REQUEST
	 * */
	//@Test
	void testCase03()
	{
		String message = "a";	//отправляемое сообщение
		
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).									//добавляем сообщение из одного символа
				with().
				post("/update.json");						
		
		response.then().statusCode(HttpStatus.SC_OK);					//отправка сообщения должна быть успешной, ожидаемый статус ответа 200 OK
		String id = response.jsonPath().get("id_str").toString();		//получаем из ответа присвоенный нашему твиту id
		
		GarbageTweetsHandler.addTweet(id, authData.consumer_1_Name);	//добавляем твит в сборщик мусора
		
		//пытаемся удалить твит без авторизации методом /destroy/:id.json, передавая в pathParam id твита
		response = given().
				pathParam("id_str", id).									
				with().
				post("/destroy/{id_str}.json");
		
		response.then().statusCode(HttpStatus.SC_BAD_REQUEST);			//удаление должно завершиться ответом со статусом 400 BAD REQUEST
	}
	
	/*
	 * Проверка на удаление чужого твита по id с помощью POST /destroy/:id.json с авторизацией
	 * Сначала мы создадим твит на одном из аккаунтов, затем попытаемся удалим его авторизацовавшись под другим аккаунтом. 
	 * В ответе мы должны получить 403 FORBIDDEN. В JSON должен быть указан код ошибки 183
	 * {"code":183,"message":"You may not delete another user's status."}
	 * */
	//@Test
	void testCase04()
	{
		String message = "a";	//отправляемое сообщение
		
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).									//добавляем сообщение из одного символа
				with().
				post("/update.json");						
		
		response.then().statusCode(HttpStatus.SC_OK);					//отправка сообщения должна быть успешной, ожидаемый статус ответа 200 OK
		String id = response.jsonPath().get("id_str").toString();		//получаем из ответа присвоенный нашему твиту id
		
		GarbageTweetsHandler.addTweet(id, authData.consumer_1_Name);	//добавляем твит в сборщик мусора
		
		//пытаемся удалить твит со второго аккаунта /destroy/:id.json, передавая в pathParam id твита
		response = given().
				auth().
				oauth(authData.consumer_2_Key, authData.consumer_2_Secret, authData.application_Token_2, authData.application_Secret_2, OAuthSignature.HEADER).
				pathParam("id_str", id).									
				with().
				post("/destroy/{id_str}.json");
		
		response.then().statusCode(HttpStatus.SC_FORBIDDEN);			//попытка удаления должна завершиться ответом со статусом 403 FORBIDDEN
		response.then().body("errors.code", hasItems(183));				//в JSON, получаемом в ответ, должна быть ошибка с кодом 183
	}
}
