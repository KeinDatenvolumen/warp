package de.warp.bff.infrastructure.client;

import de.warp.bff.application.exception.SavedRouteNotFoundException;
import de.warp.bff.application.exception.StorageServiceUnavailableException;
import de.warp.bff.infrastructure.client.dto.StorageApiDtos.SavedRoutePage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withNoContent;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class StorageClientTest {

    private static final String BASE = "http://storage-service";

    private MockRestServiceServer mockServer;
    private StorageClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder().baseUrl(BASE);
        mockServer = MockRestServiceServer.bindTo(builder).build();
        client = new StorageClient(builder.build());
    }

    @Test
    void shouldListRoutesOfUser() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes"
                                + "?page=0&size=20&favorite=true"))
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(
                        """
                        {
                          "items": [],
                          "page": 0,
                          "size": 20,
                          "totalElements": 0,
                          "totalPages": 0
                        }
                        """,
                        MediaType.APPLICATION_JSON
                ));

        SavedRoutePage page = client.listSavedRoutes("usr_123", true, 0, 20);

        assertThat(page.items()).isEmpty();
        assertThat(page.size()).isEqualTo(20);
    }

    @Test
    void shouldTranslateNotFound() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes/route_x"))
                .andRespond(withStatus(HttpStatus.NOT_FOUND));

        assertThatThrownBy(() -> client.getSavedRoute("usr_123", "route_x"))
                .isInstanceOf(SavedRouteNotFoundException.class);
    }

    @Test
    void shouldTranslateServerErrorToUnavailable() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes/route_x"))
                .andRespond(withStatus(HttpStatus.SERVICE_UNAVAILABLE));

        assertThatThrownBy(() -> client.getSavedRoute("usr_123", "route_x"))
                .isInstanceOf(StorageServiceUnavailableException.class);
    }

    @Test
    void shouldDeleteRoute() {
        mockServer.expect(requestTo(
                        BASE + "/internal/v1/users/usr_123/saved-routes/route_1"))
                .andExpect(method(HttpMethod.DELETE))
                .andRespond(withNoContent());

        client.deleteSavedRoute("usr_123", "route_1");

        mockServer.verify();
    }
}