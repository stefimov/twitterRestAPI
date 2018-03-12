package restAssuredTwitter;

import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.baseURI;
import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/*
 * Набор тестов для проверки работы механики ретвитов
 * https://developer.twitter.com/en/docs/tweets/post-and-engage/api-reference/post-statuses-retweet-id
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
	 * 
	 * 
	 * */
	//@Test
	void testCase01() {
		
	}

}
