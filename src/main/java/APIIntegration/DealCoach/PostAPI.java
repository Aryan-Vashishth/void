package APIIntegration.DealCoach;

import com.fasterxml.jackson.databind.JsonNode;
import io.restassured.RestAssured;
import io.restassured.response.Response;
import org.testng.annotations.BeforeClass;
import static io.restassured.RestAssured.given;


public class PostAPI {

    private static String token=null;
    private String requestBody = "{\"clientId\":\"a8adeea1428e5ae6f5faa2b23ae19232\",\"clientSecret\":\"f05790ef356aa2eebc053ffdcb3933c8eefc2641dea7f09f5278e5de76f10df2\"}";

    public void postdealCoachAuth(){
        RestAssured.baseURI = "https://api.structuredweb.com";
        //Setting BasePath once
        RestAssured.basePath ="/auth-management/auth/token";
        Response response = given()
                .header("Content-Type", "application/json")
                .body(requestBody)
                .when()
                .post();
        token = response.getBody().jsonPath().getString("data.accessToken");
    }

    public static String getToken(){
        return token;
    }
}
