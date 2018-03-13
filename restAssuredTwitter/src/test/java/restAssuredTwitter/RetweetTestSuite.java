package restAssuredTwitter;

import static io.restassured.RestAssured.*;
import static org.junit.jupiter.api.Assertions.*;

import static org.hamcrest.Matchers.*;

import org.apache.http.HttpStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.authentication.OAuthSignature;
import io.restassured.response.Response;

/*
 * Набор тестов для проверки работы механики ретвитов
 * https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/post-statuses-retweet-id
 * Первоначальное сообщение будет храниться в retweeted_status.
 * При успешном ретвите у ретвитнутого поста увеличивается retweet_count на 1, поле retweeted становится равным true.
 * */

class RetweetTestSuite {
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
	 * Проверяем возможность ретвита созданного сообщения через запрос POST statuses/retweet/:id.
	 * Должна быть возможность ретвитнуть собственное сообщение. При этом в случае успеха мы должны получить ответ 200 OK
	 * retweet_count у сообщения должен увеличиться на 1, retweeted должен стать true
	 * */
	//@Test
	void testCase01() {
		String message = "a";
		
		//создаём твит
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).
				with().
				post("/update.json");
		
		response.then().statusCode(HttpStatus.SC_OK); 			//постинг прошёл успешно
		
		String id = response.jsonPath().getString("id_str");
		int retweet_count = response.jsonPath().getInt("retweet_count");
		GarbageTweetsHandler.addTweet(id, authData.consumer_1_Name);

		//пытаемся заретвитить наш собственный твит
		response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				pathParam("tweet_id", id).
				with().
				post("/retweet/{tweet_id}.json");

		response.then().statusCode(HttpStatus.SC_OK); 							//ретвит завершился успешно
		response.then().root("retweeted_status").
						body("id_str", equalTo(id)).							//проверяем, что ретвит ссылается на твит созданный в начале выполнения тесткейса
						body("retweet_count", equalTo(retweet_count + 1)).		//проверяем, что количество ретвитов у первоначального сообщения увеличилось на 1
						body("retweeted", equalTo(true));						//провепяем, что у первоначального твита статус "retweeted" изменился на true
	}
	
	/*
	 * Проверяем невозможность накрутки количества ретвитов через запрос POST statuses/retweet/:id.
	 * Отправим запрос на ретвит для одного и того же сообщения два раза подряд. При попытке повторного ретвита мы должны получить ответ 403 FORBIDDEN
	 * Так же в JSON должна быть ошибка с кодом 327
	 * {"errors":[{"code":327,"message":"You have already retweeted this Tweet."}]}
	 * После попытки второго ретвита retweet_count у сообщения должен не должен увеличиться.
	 * */
	//@Test
	void testCase02() {
		String message = "a";
		
		//создаём твит
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).
				with().
				post("/update.json");
		
		response.then().statusCode(HttpStatus.SC_OK); 			//постинг прошёл успешно
		
		String id = response.jsonPath().getString("id_str");
		int retweet_count = response.jsonPath().getInt("retweet_count");
		GarbageTweetsHandler.addTweet(id, authData.consumer_1_Name);
		
		//пытаемся заретвитить наш собственный твит первый раз
		given().
		auth().
		oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
		pathParam("tweet_id", id).
		with().
		post("/retweet/{tweet_id}.json").
		then().
		statusCode(HttpStatus.SC_OK);							//первый ретвит должен завершиться успешно
				
		//пытаемся заретвитить наш собственный твит второй раз
		response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				pathParam("tweet_id", id).
				with().
				post("/retweet/{tweet_id}.json");

		response.then().statusCode(HttpStatus.SC_FORBIDDEN);	//попытка второго ретвита должна завершиться с ошибкой 403 FORBIDDEN
		response.then().body("errors.code", hasItem(327));		//в JSON должна содержаться ошибка 327
		
		//с помощью запроса GET /show.json получим доступ к первоначальному твиту и проверим, что количество ретвитов увеличилось только на 1
		given().
		auth().
		oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
		param("id", id).
		with().
		get("/show.json").
		then().
		body("retweet_count", equalTo(retweet_count + 1));
	}
	
	/*
	 * Проверяем возможность ретвита чужого сообщения.
	 * Создадим твит на одном из аккаунтов, затем ретвитнем его со второго аккаунта. Запрос должен завершиться со статусом 200 OK 
	 * Ретвитнутый твит должен появиться в ленье у второго аккаунта.
	 * */
	//@Test
	void testCase03()
	{
		String message = "a";
		
		//создаём твит с первого аккаунта
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).
				with().
				post("/update.json");
		
		response.then().statusCode(HttpStatus.SC_OK); 			//постинг прошёл успешно
				
		String id = response.jsonPath().getString("id_str");
		GarbageTweetsHandler.addTweet(id, authData.consumer_1_Name);
		
		//Ретвитим его со второго аккаунта
		response = given().
				auth().
				oauth(authData.consumer_2_Key, authData.consumer_2_Secret, authData.application_Token_2, authData.application_Secret_2, OAuthSignature.HEADER).
				pathParam("tweet_id", id).
				with().
				post("/retweet/{tweet_id}.json");

		response.then().statusCode(HttpStatus.SC_OK); 					//ретвит прошёл успешно
		String retweet_id = response.jsonPath().getString("id_str");	//запоминаем id ретвита
		
		String mathing_str = String.format("find {it.id_str == '%s'}", retweet_id);		//строка для поиска твита с retweet_id в возвращённом 
																						//GET /statuses/user_timeline списке
		
		//проверяем добавился ли ретвит в ленту второго аккаунта с помощью GET /statuses/user_timeline
		//в ответе должен придти JSON со списком твитов второго акка
		//одним из объектов должен быть ретвит созданный ретвит
		given().
		auth().
		oauth(authData.consumer_2_Key, authData.consumer_2_Secret, authData.application_Token_2, authData.application_Secret_2, OAuthSignature.HEADER).
		param("screen_name", authData.consumer_2_Name).
		with().
		get("/user_timeline.json").
		then().
		body(mathing_str.toString(), is(not(empty())));
	}
	
	/*
	 * Проверяем невозможность ретвита несуществующего сообщения.
	 * Создадим твит, затем удалим его и попытаемся ретвитнуть. Запрос должен завершиться со статусом 200 OK 
	 * 
	 * */
	@Test
	void testCase04()
	{
		String message = "a";
			
		//создаём твит с первого аккаунта
		Response response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				param("status", message).
				with().
				post("/update.json");
			
		response.then().statusCode(HttpStatus.SC_OK); 					//постинг прошёл успешно
		String id = response.jsonPath().getString("id_str");
		
		//удаляем твит
		given().
		auth().
		oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
		pathParam("id_str", id).									
		with().
		post("/destroy/{id_str}.json").
		then().
		statusCode(HttpStatus.SC_OK);									//удаление завершилось успешно
		
		//пытаемся ретвитнуть удалённый твит
		
		response = given().
				auth().
				oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
				pathParam("tweet_id", id).
				with().
				post("/retweet/{tweet_id}.json");
		
		System.out.println(response.asString());
		
		//response.then().statusCode(HttpStatus.SC_FORBIDDEN);	//попытка второго ретвита должна завершиться с ошибкой 403 FORBIDDEN
		//response.then().body("errors.code", hasItem(327));		//в JSON должна содержаться ошибка 327
	}
}
