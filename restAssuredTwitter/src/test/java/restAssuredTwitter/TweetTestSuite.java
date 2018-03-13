package restAssuredTwitter;

/*
 * Набор тестов для проверки запросов, отвечающих за постинг твитов и полдучение к ним доступа
 * https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/post-statuses-update
 * 
 * */

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.authentication.OAuthSignature;
import io.restassured.internal.util.IOUtils;
import io.restassured.response.Response;

import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
	 * Постинг должен проходить только от авторизованного пользователя. Попытаемся воспользоваться методом POST statuses/update без авторизации.
	 * В ответ должны получить сообщение со статусом 400 BAD REQUEST.
	 * */
	//@Test 
	void testCase01()
	{
		String message = "asa"; 	//отправляемое сообщение
		
		Response response = given().
				param("status", message).									//добавляем пустое сообщение
				with().
				post("/update.json");						
		
		response.then().statusCode(HttpStatus.SC_BAD_REQUEST);		
	}
	
	/*
	 * Проверяем запрос POST statuses/update, который добавляет твиты в ленту.
	 * Текст сообщения добавляется к запросу в параметре "status"
	 * При попытке отправить пустое сообщение в ответ мы должны получить JSON с ошибкой 170:
	 * {errors:[{"code":170,"Message":"some_error_message"}]}
	 * Пустое сообщение не должно появиться в ленте.
	 * */
	//@Test
	void testCase02() {
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
	 * Проверяем запрос POST statuses/update, который добавляет твиты в ленту.
	 * Текст сообщения добавляется к запросу в параметре "status"
	 * При попытке отправить хотя бы с одним символом в параметре "status" должен совершиться постинг сообщения
	 * Ответ должен иметь статус 200 OK и содержать JSON с информацией о твите
	 * Сообщение с отправленным символом должно появиться в ленте.
	 * */
	//@Test
	void testCase03() {
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
	//@Test
	void testCase04() {
		String message = "abc";	//отправляемое сообщение
		String search_request = String.format("find {it.text == '%s'}.id_str", message);
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
		body(search_request, equalTo(id));		//ищем в ответие первый твит содержащий такой же текст что и в message, и если id_str совпадает, то твит добавился в ленту.
	}
	
	/*
	 * Максимальная длина сообщения для отправки равна 280 символов.
	 * Проверяем в кейсе возможность отправки такого сообщения.
	 */
	//@Test
	void testCase05() throws IOException {
		String fileName = "longTweet.txt";											//файл src/test/resources с тестовой строкой на 285 символов
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();	
		InputStream is = classloader.getResourceAsStream(fileName);
		String message = new String(IOUtils.toByteArray(is)).substring(0, 280);		//считываем InputStream массив байтов, преобразуем в строку,
																					//выделяем полстроку длиной 280 символов и формируем 
																					//отправляемое сообщение нужной длины
		is.close();
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).								//добавляем сообщение из 280 символов
				with().
				post("/update.json");						
		
		response.then().statusCode(HttpStatus.SC_OK);					//отправка сообщения должна быть успешной, ожидаемый статус ответа 200 OK
		String id = response.jsonPath().get("id_str").toString();		//получаем из ответа присвоенный нашему твиту id
		
		GarbageTweetsHandler.addTweet(id, authData.consumer_1_Name);	//добавляем твит в сборщик мусора
	}
	
	
	/*
	 * Максимальная длина сообщения для отправки равна 280 символов.
	 * Проверяем в кейсе возможность не урезается ли сообщение максимальной длины после постинга.
	 */
	//@Test
	void testCase06() throws IOException {
		String fileName = "longTweet.txt";											//файл src/test/resources с тестовой строкой на 285 символов
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();	
		InputStream is = classloader.getResourceAsStream(fileName);
		String message = new String(IOUtils.toByteArray(is)).substring(0, 280);		//считываем InputStream массив байтов, преобразуем в строку,
																					//выделяем полстроку длиной 280 символов и формируем 
																					//отправляемое сообщение нужной длины
		is.close();
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).								//добавляем сообщение из 280 символов
				with().
				post("/update.json");						
		
		response.then().statusCode(HttpStatus.SC_OK);					//отправка сообщения должна быть успешной, ожидаемый статус ответа 200 OK
		String id = response.jsonPath().get("id_str").toString();		//получаем из ответа присвоенный нашему твиту id
		
		GarbageTweetsHandler.addTweet(id, authData.consumer_1_Name);	//добавляем твит в сборщик мусора
				
		//получаем твит с нужным id с помощью GET /show.json, передавая в него id созданного до этого твита
		//в ответном JSON поле "text" должно в себе содержать всё сообщение, отправленное в message
		given().
		auth().oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
		param("id", id).
		param("tweet_mode", "extended").		//параметр tweet_mode=extended для того чтобы в полученном в ответе JSON сообщение не было свёрнуто до 115 символов и ссылки
		when().
		get("/show.json").
		then().
		body("full_text", equalTo(message));
	}
	/*
	 * Максимальная длина сообщения для отправки равна 280 символов.
	 * Проверяем в кейсе невозможность отправки сообщения большей длины.
	 * Твит не должен запоститься. В ответ на запрос должен придти ответ со статусом 403 FORBIDDEN
	 * JSON должен содержать список ошибок, среди них должна быть ошибка "code":186
	 * {errors:[{"code":186,"Message":"some_error_message"}]}
	 */
	//@Test
	void testCase07() throws IOException {
		String fileName = "longTweet.txt";											//файл src/test/resources с тестовой строкой на 285 символов
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();	
		InputStream is = classloader.getResourceAsStream(fileName);
		String message = new String(IOUtils.toByteArray(is)).substring(0, 281);		//считываем InputStream массив байтов, преобразуем в строку,
																					//выделяем полстроку длиной 281 символов и формируем 
																					//отправляемое сообщение нужной длины
		is.close();
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).								//добавляем сообщение из 281 символов
				with().
				post("/update.json");						
		
		response.then().statusCode(HttpStatus.SC_FORBIDDEN);			//отправка сообщения не должна быть успешной, ожидаемый статус ответа 403 FORBIDDEN
		response.then().body("errors.code", hasItems(186));				//в JSON, получаемом в ответ, должна быть ошибка с кодом 186
	}
	
	/*
	 * Не должно быть возможности отправить 2 одинаковых твита подряд.
	 * Отправка первого должна завершиться со статусом 200 OK
	 * Отправка второго идентичного со статусом 403 FORBIDDEN
	 * JSON должен содержать список ошибок, среди них должна быть ошибка "code":187
	 * {"errors":[{"code":187,"message":"Status is a duplicate."}]}
	 * */
	//@Test
	void testCase08() {
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
		
		//попытка повторно отправить твит с тем же сообщением
		response = given().
		auth().
		oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
		param("status", message).									//добавляем сообщение из одного символа
		with().
		post("/update.json");
		response.then().statusCode(HttpStatus.SC_FORBIDDEN);		//отправка сообщения не должна быть успешной, ожидаемый статус ответа 403 FORBIDDEN
		response.then().body("errors.code", hasItems(187));			//в JSON, получаемом в ответ, должна быть ошибка с кодом 187
	}
}
