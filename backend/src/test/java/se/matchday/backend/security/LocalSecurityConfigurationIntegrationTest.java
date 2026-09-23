package se.matchday.backend.security;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.matchday.backend.TestcontainersConfiguration;
import se.matchday.backend.match.application.SeasonMatchImportResult;
import se.matchday.backend.match.application.SeasonMatchImporter;

@ActiveProfiles("local")
@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest(
    properties = {
      "matchday.security.local.username=local-operator",
      "matchday.security.local.password=test-password"
    })
class LocalSecurityConfigurationIntegrationTest {

  private final MockMvc mockMvc;

  @MockitoBean(enforceOverride = true)
  private SeasonMatchImporter seasonMatchImporter;

  @Autowired
  LocalSecurityConfigurationIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void rejectsMissingCredentials() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/match-imports")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"season": 2026}
                    """))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(seasonMatchImporter);
  }

  @Test
  void rejectsInvalidCredentials() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/match-imports")
                .with(httpBasic("local-operator", "wrong-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"season": 2026}
                    """))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(seasonMatchImporter);
  }

  @Test
  void importsWithValidLocalCredentialsWithoutACsrfToken() throws Exception {
    when(seasonMatchImporter.importSeason(2026)).thenReturn(new SeasonMatchImportResult(2026, 240));

    mockMvc
        .perform(
            post("/api/v1/admin/match-imports")
                .with(httpBasic("local-operator", "test-password"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"season": 2026}
                    """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.season").value(2026))
        .andExpect(jsonPath("$.processedMatches").value(240));

    verify(seasonMatchImporter).importSeason(2026);
  }
}
