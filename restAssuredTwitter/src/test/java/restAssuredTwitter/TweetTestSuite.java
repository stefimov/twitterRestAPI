package restAssuredTwitter;

/*
 * Набор тестов для проверки запросов, отвечающих за постинг и удаление твитов
 * https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/post-statuses-update
 * 
 * */

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.authentication.OAuthSignature;
import io.restassured.response.Response;

import static org.hamcrest.Matchers.*;
import static io.restassured.RestAssured.*;  

class TweetTestSuite {
	
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
	
	@AfterEach
	void clearTweets()
	{
		GarbageTweetsHandler.clearGarbage();
	}
	
	/*
	 * Проверяем метод POST statuses/update, который добавляет твиты в ленту.
	 * Текст сообщения добавляется к запросу в параметре "status"
	 * При попытке отправить пустое сообщение в "status" в ответ мы должны получить JSON с ошибкой 170:
	 * {errors:[{"code":170,"Message":"some_error_message"}]}
	 * Пустое сообщение не должно появиться в ленте.
	 * */
	@Test
	void testCase01() {
		String message = ""; 	//отправляемое сообщение
		
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).									//добавляем пустое сообщение
				with().
				post("/update.json");						
		
		response.then().statusCode(HttpStatus.SC_FORBIDDEN);	//отправка сообщения не должна быть успешной, ожидаемый статус ответа 403 Forbidden
		response.then().body("errors.code", hasItems(170));		//проверяем есть ли в отправленом в ответ JSON сообщение об ошибке с "code" = 170
	}
	
	/*
	 * Проверяем метод POST statuses/update, который добавляет твиты в ленту.
	 * Текст сообщения добавляется к запросу в параметре "status"
	 * При попытке отправить хотя бы с одним символом в параметре "status" должен совершиться постинг сообщения
	 * Ответ должен иметь статус 200 OK и содержать JSON с информацией о твите
	 * Сообщение с отправленным символом должно появиться в ленте.
	 * */
	@Test
	void testCase02() {
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
	}
	
	
	/*
	 * Проверяем действительно ли добавиляется твит в нашу ленту.
	 * Для этого постим твит с произвольным текстом и сохраняем его "id_str".
	 * Затем при помощи запроса GET /user_timeline.json с параметром "screen_name", в котором указываем имя нашего аккаунта, 
	 * мы получаем JSON со списком твитов, совершённых с данного аккаунта. В JSON мы ищем твит с текстом сообщения message и проверяем, совпадает
	 * ли его "id_str" со "id_str" из JSON, полученного в ответ на отправку сообщения.
	 * */
	@Test
	void testCase03() {
		String message = "abc";	//отправляемое сообщение
		
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).									//добавляем сообщение из одного символа
				with().
				post("/update.json");		
		
		String id = response.jsonPath().get("id_str").toString();			//получаем из ответа присвоенный нашему твиту id
		
		GarbageTweetsHandler.addTweet(id, authData.consumer_1_Name);		//добавляем твит в сборщик мусора
		
		//делаем запрос GET /user_timeline.json и ищем отправленный твит
		given().
		auth().
		oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
		param("screen_name", authData.consumer_1_Name).									
		with().
		get("/user_timeline.json").
		then().
		body("find {it.text == 'abc'}.id_str", equalTo(id));		//ищем в ответие первый твит содержащий такой же текст что и в message, и если id_str совпадает, то твит добавился в ленту.
	}
	
	/*
	 * Максимальная длина сообщения для отправки равна 280 символов.
	 * Проверяем в кейсе возможность отправки такого сообщения.
	 */
}
