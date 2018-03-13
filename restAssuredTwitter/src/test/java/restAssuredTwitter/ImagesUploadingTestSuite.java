package restAssuredTwitter;

import static io.restassured.RestAssured.basePath;
import static io.restassured.RestAssured.baseURI;
import static io.restassured.RestAssured.given;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import io.restassured.authentication.OAuthSignature;
import io.restassured.internal.util.IOUtils;
import io.restassured.response.Response;

/*
 * Набор тестов для проверки постинга медиа-файлов
 * https://developer.twitter.com/en/docs/media/upload-media/api-reference/post-media-upload
 * После успешной отправки файла через POST media/upload в ответе приходит "media_id", 
 * который можно добавить к своему твиту при постинге POST statuses/update 
 * */

class ImagesUploadingTestSuite {
	//Добавляем переменную класса AuthorizationDataStorage чтоб иметь доступ к данным, необходимым для авторизации.
	static final AuthorizationDataStorage authData = new AuthorizationDataStorage();
				
	/*
	 * Начальные установки для набора тестов
	 * */
	@BeforeAll
	static void setUp()
	{
		//устанавливаем стандартный путь для запросов на взаимодействие с твитами
		baseURI = "https://upload.twitter.com";
		basePath = "/1.1/media";
	}
	
	/*
	 * отправка изображений происходит через POST media/upload
	 * ограничение по размеру для изображения 5Mb
	 * в этом тест-кейсе мы пытаемся загрузить картинку размером чуть меньше 5Мб
	 * в ответе должно придти 200 OK
	 * */
	@Test
	void test01() throws IOException {
		String fileName = "picNear5MB.png";											//файл из src/test/resources с тестовой картинкой размером чуть меньше 5Mb
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();	
		InputStream is = classloader.getResourceAsStream(fileName);
		String data = Base64.encodeBase64String(IOUtils.toByteArray(is));
		is.close();
		
		Response response = given().
        auth().oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
        param("media_data", data).
        when().
        post("/upload.json");
		
		response.then().statusCode(HttpStatus.SC_OK);
	}
	
	/*
	 * отправка изображений происходит через POST media/upload
	 * ограничение по размеру для изображения 5Mb
	 * в этом тест-кейсе мы пытаемся загрузить картинку размером чуть больше 5Мб
	 * загрузка должна завершиться ошибкой 413 PAYLOAD TOO LARGE
	 * NOTE: но, видимо, установленное ограничение отличается от указанного в документации, поэтому тест с 5.1Мб и 6Мб файлами фейлится. Не фейлится с файлом размером 8Мб+.
	 * */
	@Test
	void test02() throws IOException {
		String fileName = "picBigger5MB.png";											//файл из src/test/resources с тестовой картинкой размером чуть больше 5Mb
		ClassLoader classloader = Thread.currentThread().getContextClassLoader();	
		InputStream is = classloader.getResourceAsStream(fileName);
		String data = Base64.encodeBase64String(IOUtils.toByteArray(is));
		is.close();
		
		Response response = given().
        auth().oauth(authData.consumer_1_Key, authData.consumer_1_Secret, authData.application_Token, authData.application_Secret, OAuthSignature.HEADER).
        param("media_data", data).
        when().
        post("/upload.json");
		
		response.then().statusCode(HttpStatus.SC_REQUEST_TOO_LONG);
	}
	
	
}
