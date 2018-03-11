package restAssuredTwitter;

import static io.restassured.RestAssured.given;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;

import io.restassured.authentication.OAuthSignature;


/*
 * Класс в котором хранятся твиты, созданные в отдельно взятом тест-кейсе
 * после завершения выполнения тест-кейса, все созданные твиты должны быть удалены, чтоб не мешать повторным прогонам.
 * */

public class GarbageTweetsHandler {
	//Скрытый мап, в котором бужут храниться пары id_твита : автор
	private static Map<String, String> tweetsToRemove = new HashMap<String, String>();
	//переменная класса, в котором хранятся ключи и токены
	private static final AuthorizationDataStorage authData = new AuthorizationDataStorage();
	
	//Метод для добавления пары id_твита : автор в пул
	public static void addTweet(String id_str, String name)
	{
		tweetsToRemove.put(id_str, name);
	}
	
	//Публичный метод для запуска удаления твитов созданных в тест-кейсе
	public static void clearGarbage()
	{
		
		//в цикле проходим по всем элементам tweetsToRemove, определяем автора сообщения и передаём id_твита и данные для авторизации 
		// в метод deleteTweet для выполнения удаления
		for(Map.Entry<String, String> pair : tweetsToRemove.entrySet())
		{
			if(pair.getValue().equals(authData.consumer_1_Name))
				deleteTweet(pair.getKey(), authData.consumer_1_Key, authData.consumer_1_Secret);
			else
				deleteTweet(pair.getKey(), authData.consumer_2_Key, authData.consumer_2_Secret);	
		}
		
		//очищаем список после того как все твиты были удалены
		tweetsToRemove.clear();
	}
	
	//в методе выполняем запрос POST /destroy/tweet_id.json для удаления необходимого твита
	private static void deleteTweet(String id_str, String key, String secret)
	{
		given().
		auth().
		oauth(key, secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
		pathParam("id_str", id_str).									
		with().
		post("https://api.twitter.com/1.1/statuses/destroy/{id_str}.json").
		then().
		statusCode(HttpStatus.SC_OK);
	}
}
