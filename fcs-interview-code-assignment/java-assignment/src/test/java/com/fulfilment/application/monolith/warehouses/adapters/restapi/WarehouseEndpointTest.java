package com.fulfilment.application.monolith.warehouses.adapters.restapi;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.containsString;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class WarehouseEndpointTest {

  private static final String PATH = "warehouse";

  @Test
  public void testTheLifecycleOfAWarehouseUnit() {
    String id =
        given()
            .contentType(ContentType.JSON)
            .body(payload("MWH.T01", "EINDHOVEN-001", 30, 10))
            .when()
            .post(PATH)
            .then()
            .statusCode(200)
            .body("businessUnitCode", is("MWH.T01"))
            .extract()
            .path("id");

    given()
        .when()
        .get(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("location", is("EINDHOVEN-001"))
        .body("capacity", is(30))
        .body("stock", is(10));

    String replacementId =
        given()
            .contentType(ContentType.JSON)
            .body(payload("MWH.T01", "EINDHOVEN-001", 40, 10))
            .when()
            .post(PATH + "/MWH.T01/replacement")
            .then()
            .statusCode(200)
            .body("capacity", is(40))
            .extract()
            .path("id");

    // the replaced unit keeps the business unit code, but it is not an active unit anymore
    given().when().get(PATH + "/" + id).then().statusCode(404);

    given().when().delete(PATH + "/" + replacementId).then().statusCode(204);

    given().when().get(PATH + "/" + replacementId).then().statusCode(404);

    given().when().get(PATH).then().statusCode(200).body(not(containsString("MWH.T01")));
  }

  @Test
  public void testCreateWarehouseWithAnAlreadyUsedBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body(payload("MWH.012", "AMSTERDAM-001", 10, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(400)
        .body("error", containsString("MWH.012"));
  }

  @Test
  public void testCreateWarehouseOnAnUnknownLocation() {
    given()
        .contentType(ContentType.JSON)
        .body(payload("MWH.T10", "ROTTERDAM-001", 10, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseHoldingMoreStockThanItsCapacity() {
    given()
        .contentType(ContentType.JSON)
        .body(payload("MWH.T11", "AMSTERDAM-002", 10, 20))
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseBeyondTheCapacityOfTheLocation() {
    given()
        .contentType(ContentType.JSON)
        .body(payload("MWH.T12", "ZWOLLE-002", 60, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testCreateWarehouseOnALocationThatIsFull() {
    given()
        .contentType(ContentType.JSON)
        .body(payload("MWH.T20", "HELMOND-001", 20, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(200);

    given()
        .contentType(ContentType.JSON)
        .body(payload("MWH.T21", "HELMOND-001", 20, 5))
        .when()
        .post(PATH)
        .then()
        .statusCode(400);
  }

  @Test
  public void testGetAnUnknownWarehouseUnit() {
    given().when().get(PATH + "/999999").then().statusCode(404);
    given().when().get(PATH + "/not-a-number").then().statusCode(404);
  }

  @Test
  public void testReplaceAnUnknownWarehouseUnit() {
    given()
        .contentType(ContentType.JSON)
        .body(payload("MWH.999", "AMSTERDAM-001", 10, 5))
        .when()
        .post(PATH + "/MWH.999/replacement")
        .then()
        .statusCode(404);
  }

  @Test
  public void testReplaceAWarehouseUnitWithADifferentStock() {
    given()
        .contentType(ContentType.JSON)
        .body(payload("MWH.012", "AMSTERDAM-001", 60, 42))
        .when()
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(400);
  }

  @Test
  public void testReplaceAWarehouseUnitUsingAnotherBusinessUnitCode() {
    given()
        .contentType(ContentType.JSON)
        .body(payload("MWH.001", "AMSTERDAM-001", 50, 5))
        .when()
        .post(PATH + "/MWH.012/replacement")
        .then()
        .statusCode(400);
  }

  private static String payload(String businessUnitCode, String location, int capacity, int stock) {
    return "{\"businessUnitCode\":\""
        + businessUnitCode
        + "\",\"location\":\""
        + location
        + "\",\"capacity\":"
        + capacity
        + ",\"stock\":"
        + stock
        + "}";
  }
}
