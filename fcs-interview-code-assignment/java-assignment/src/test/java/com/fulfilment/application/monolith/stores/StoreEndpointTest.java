package com.fulfilment.application.monolith.stores;

import static io.restassured.RestAssured.given;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@QuarkusTest
public class StoreEndpointTest {

  private static final String PATH = "store";

  @Inject LegacyStoreSyncRecorder recorder;

  @BeforeEach
  public void resetRecorder() {
    recorder.clear();
  }

  @Test
  public void testCreatedStoreIsPropagatedToTheLegacySystem() {
    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"HEMNES\",\"quantityProductsInStock\":7}")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .body("name", is("HEMNES"))
            .extract()
            .path("id");

    assertEquals(1, recorder.events().size());

    StoreChangedEvent event = recorder.events().get(0);
    assertEquals(StoreChangedEvent.Change.CREATED, event.change());
    assertEquals("HEMNES", event.name());
    assertEquals(7, event.quantityProductsInStock());
    assertEquals(id.longValue(), event.id().longValue());
  }

  @Test
  public void testRolledBackStoreIsNotPropagatedToTheLegacySystem() {
    // TONSTAD is already taken and the name is unique, so the insert only fails on commit
    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"TONSTAD\",\"quantityProductsInStock\":1}")
        .when()
        .post(PATH)
        .then()
        .statusCode(500);

    assertTrue(recorder.events().isEmpty());
  }

  @Test
  public void testUpdatedStoreIsPropagatedWithTheStoredValues() {
    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"MALM\",\"quantityProductsInStock\":2}")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    recorder.clear();

    given()
        .contentType(ContentType.JSON)
        .body("{\"name\":\"MALM\",\"quantityProductsInStock\":9}")
        .when()
        .put(PATH + "/" + id)
        .then()
        .statusCode(200);

    assertEquals(1, recorder.events().size());

    StoreChangedEvent event = recorder.events().get(0);
    assertEquals(StoreChangedEvent.Change.UPDATED, event.change());
    assertEquals(id.longValue(), event.id().longValue());
    assertEquals(9, event.quantityProductsInStock());
  }

  @Test
  public void testPatchOnlyAppliesTheProvidedFields() {
    Integer id =
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"LACK\",\"quantityProductsInStock\":4}")
            .when()
            .post(PATH)
            .then()
            .statusCode(201)
            .extract()
            .path("id");

    given()
        .contentType(ContentType.JSON)
        .body("{\"quantityProductsInStock\":11}")
        .when()
        .patch(PATH + "/" + id)
        .then()
        .statusCode(200)
        .body("name", is("LACK"))
        .body("quantityProductsInStock", is(11));
  }
}
