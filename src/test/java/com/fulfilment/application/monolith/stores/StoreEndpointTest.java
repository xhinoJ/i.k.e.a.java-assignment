package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreEndpointTest {

  private static final String PATH = "store";

  private long createStore(String name, int quantityProductsInStock) {
    return given()
        .contentType(ContentType.JSON)
        .body(
            "{\"name\":\""
                + name
                + "\",\"quantityProductsInStock\":"
                + quantityProductsInStock
                + "}")
        .when()
        .post(PATH)
        .then()
        .statusCode(201)
        .body(containsString(name))
        .extract()
        .jsonPath()
        .getLong("id");
  }

  @Test
  public void testCreateStore() {
    createStore("STORE-CREATE", 7);

    given().when().get(PATH).then().statusCode(200).body(containsString("STORE-CREATE"));
  }

  @Test
  public void testCreateStoreWithIdIsRejected() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"id\":9999,\"name\":\"STORE-CREATE-BAD-ID\",\"quantityProductsInStock\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(422);
  }

  @Test
  public void testCreateDuplicateStoreNameReturnsConflict() {
    createStore("STORE-DUPLICATE", 1);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-DUPLICATE\",\"quantityProductsInStock\":2}")
        .when()
        .post(PATH)
        .then()
        .statusCode(409)
        .body("code", is(409))
        .body("exceptionType", is("ConstraintViolationException"))
        .body("error", is("Request conflicts with the current state of the resource."))
        .body("error", not(containsString("ArcUndeclaredThrowableException")));
  }

  @Test
  public void testGetSingleStore() {
    long id = createStore("STORE-GET", 3);

    given().when().get(PATH + "/" + id).then().statusCode(200).body(containsString("STORE-GET"));
  }

  @Test
  public void testGetSingleStoreNotFound() {
    given().when().get(PATH + "/999999").then().statusCode(404);
  }

  @Test
  public void testUpdateStore() {
    long id = createStore("STORE-UPDATE", 1);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-UPDATED\",\"quantityProductsInStock\":9}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("STORE-UPDATED"));
  }

  @Test
  public void testUpdateStoreWithoutNameIsRejected() {
    long id = createStore("STORE-UPDATE-NO-NAME", 1);

    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":9}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(422);
  }

  @Test
  public void testUpdateStoreNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-UPDATE-MISSING\",\"quantityProductsInStock\":9}")
        .when()
        .put(PATH + "/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testPatchStore() {
    long id = createStore("STORE-PATCH", 1);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-PATCHED\",\"quantityProductsInStock\":4}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("STORE-PATCHED"));
  }

  @Test
  public void testPatchStoreWithoutNameIsRejected() {
    long id = createStore("STORE-PATCH-NO-NAME", 1);

    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":4}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(422);
  }

  @Test
  public void testPatchStoreNotFound() {
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-PATCH-MISSING\",\"quantityProductsInStock\":4}")
        .when()
        .patch(PATH + "/999999")
        .then()
        .statusCode(404);
  }

  @Test
  public void testPatchKeepsStockWhenOnlyNameIsSent() {
    long id = createStore("STORE-PATCH-NAME-ONLY", 7);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-PATCH-NAME-ONLY-UPDATED\"}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", is("STORE-PATCH-NAME-ONLY-UPDATED"))
        .body("quantityProductsInStock", is(7));
  }

  @Test
  public void testPatchCanSetStockToZero() {
    long id = createStore("STORE-PATCH-TO-ZERO", 7);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-PATCH-TO-ZERO\",\"quantityProductsInStock\":0}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("quantityProductsInStock", is(0));
  }

  @Test
  public void testPatchUpdatesStockWhenCurrentStockIsZero() {
    long id = createStore("STORE-PATCH-ZERO", 0);

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"STORE-PATCH-ZERO-UPDATED\",\"quantityProductsInStock\":5}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body(containsString("STORE-PATCH-ZERO-UPDATED"))
        .body("quantityProductsInStock", is(5));
  }

  @Test
  public void testDeleteStore() {
    long id = createStore("STORE-DELETE", 1);

    given().when().delete(PATH + "/" + id).then().statusCode(204);

    given().when().get(PATH + "/" + id).then().statusCode(404);
  }

  @Test
  public void testDeleteStoreNotFound() {
    given().when().delete(PATH + "/999999").then().statusCode(404);
  }
}
