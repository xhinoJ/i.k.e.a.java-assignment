package com.fulfilment.application.monolith.products;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.core.IsNot.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class ProductEndpointTest {

  @Test
  public void testCrudProduct() {
    final String path = "product";

    // List all, should have all 3 products the database has initially:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(containsString("TONSTAD"), containsString("KALLAX"), containsString("BESTÅ"));

    // Create a product so this test does not depend on (or destroy) the seeded data:
    long id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"PRODUCT-TO-DELETE\",\"stock\":1}")
            .when()
            .post(path)
            .then()
            .statusCode(201)
            .extract()
            .jsonPath()
            .getLong("id");

    // Delete the created product:
    given().when().delete(path + "/" + id).then().statusCode(204);

    // List all, the created product should be missing now:
    given()
        .when()
        .get(path)
        .then()
        .statusCode(200)
        .body(not(containsString("PRODUCT-TO-DELETE")), containsString("TONSTAD"));
  }
}
