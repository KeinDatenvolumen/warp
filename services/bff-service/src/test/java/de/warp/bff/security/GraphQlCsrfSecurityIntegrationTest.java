// [KI-Log Nr. KI-09] KI-generiert (Microsoft Copilot (GPT 5.6 Sol)): Sitzungsverwaltung, CSRF & Rate-Limiting (WARP-006)
package de.warp.bff.security;

import de.warp.bff.application.service.SessionService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request
        .SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class GraphQlCsrfSecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionService sessionService;

    @Test
    void shouldAllowGraphQlQueryWithoutCsrfToken()
            throws Exception {
        String requestBody = """
                {
                  "query": "query { __typename }"
                }
                """;

        mockMvc.perform(
                        post("/graphql")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk())
                .andExpect(
                        jsonPath("$.data.__typename")
                                .value("Query")
                );
    }

    @Test
    void shouldRejectGraphQlMutationWithoutCsrfToken()
            throws Exception {
        String requestBody = """
                {
                  "query": "mutation { _empty }"
                }
                """;

        mockMvc.perform(
                        post("/graphql")
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldAllowGraphQlMutationWithCsrfToken()
            throws Exception {
        String requestBody = """
                {
                  "query": "mutation { _empty }"
                }
                """;

        mockMvc.perform(
                        post("/graphql")
                                .with(csrf())
                                .contentType(
                                        MediaType.APPLICATION_JSON
                                )
                                .content(requestBody)
                )
                .andExpect(status().isOk());
    }
}