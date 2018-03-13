package restAssuredTwitter;

import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.*;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.Matchers.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.URLEncoder;

import io.restassured.authentication.OAuthSignature;
import io.restassured.internal.util.IOUtils;
import io.restassured.response.Response;

/*
 * Набор тестов для проверки корректности обработки хэштэгов
 * https://help.twitter.com/ru/using-twitter/how-to-use-hashtags
 * При успешном постинге объект хэштэгта добавляется в массив entities.hashtags. Текст сохраняется в поле text, 
 * расположение первой и последней буквы в массиве indices:[first, last].
 * */


class HashTagTestSuite {
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
	 * Отправим сообщение содержащее корректный хэштэг.
	 * В ответе должны получить 200 OK
	 * В JSON нужно проверить, что хэштэг добавился в массив entities.hashtags
	 * */
	//@Test
	void testCase01() {
		String text = "a ";
		String hashTag = "#b";
		String message = text + hashTag;
		
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).									//добавляем сообщение из одного символа
				with().
				post("/update.json");						
		
		response.then().statusCode(HttpStatus.SC_OK);					//отправка сообщения должна быть успешной, ожидаемый статус ответа 200 OK
		String id = response.jsonPath().get("id_str").toString();		//получаем из ответа присвоенный нашему твиту id
		GarbageTweetsHandler.addTweet(id, authData.consumer_1_Name);	//добавляем твит в сборщик мусора
		response.then().body("entities.hashtags.text", hasItem(hashTag.substring(1)));	//с помощью hashTag.substring(1) отделяем текст хэштэга и проверяем, совпадает ли он с текстом, записанным в entities.hashtags.text. 
		response.then().body("entities.hashtags[0].indices", hasItems(text.length(), text.length() + hashTag.length()));	//text.length() - позиция первой буквы в хэштэге, 
																															//text.length() + hashTag.length() - позиция последней буквы в хэштэге
																															//проверяем лежат ли в indices корректные значения расположения начального и конечного символа
	}
	
	/*
	 * Отправим сообщение содержащее некорректный хэштэг, состоящий из недопустимых символов
	 * В ответе должны получить 200 OK
	 * В JSON нужно проверить, что хэштэгов нет в массиве entities.hashtags
	 * */
	//@Test
	void testCase02() throws IOException {
		String fileName = "illegalHashTagSymbols.txt";									//файл src/test/resources/illegalHashTagSymbols.txt с тестовой строкой содержащей символы, которые не образуют хэштэг 
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();	
		InputStream is = classloader.getResourceAsStream(fileName);
		byte[] line = IOUtils.toByteArray(is);
		is.close();
		String message = "";
		
		//формируем строку вида "#символ_не_образующий_хэштэг #символ_не_образующий_хэштэг ..."
		for(byte b:line)
		{
			message += "#" + (char)b + " ";
		}

		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				header("Content-Type","application/x-www-form-urlencoded").
				param("status", URLEncoder.encode(message, "UTF-8")).									//добавляем строку и кодируем её в url-encoded для передачи 
				with().
				post("/update.json");						

		response.then().statusCode(HttpStatus.SC_OK);					//отправка сообщения должна быть успешной, ожидаемый статус ответа 200 OK
		String id = response.jsonPath().get("id_str").toString();		//получаем из ответа присвоенный нашему твиту id
		GarbageTweetsHandler.addTweet(id, authData.consumer_1_Name);	//добавляем твит в сборщик мусора
		response.then().body("entities.hashtags", is(empty()));			//проверяем, что массив хэштэгов пуст
	}
}
