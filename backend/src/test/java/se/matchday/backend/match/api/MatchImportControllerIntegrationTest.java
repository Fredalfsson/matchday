package se.matchday.backend.match.api;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import se.matchday.backend.TestcontainersConfiguration;
import se.matchday.backend.match.application.SeasonMatchImportResult;
import se.matchday.backend.match.application.SeasonMatchImporter;

@Import(TestcontainersConfiguration.class)
@AutoConfigureMockMvc
@SpringBootTest
class MatchImportControllerIntegrationTest {

  private final MockMvc mockMvc;

  @MockitoBean(enforceOverride = true)
  private SeasonMatchImporter seasonMatchImporter;

  @Autowired
  MatchImportControllerIntegrationTest(MockMvc mockMvc) {
    this.mockMvc = mockMvc;
  }

  @Test
  void rejectsAnUnauthenticatedImport() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/match-imports")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"season": 2026}
                    """))
        .andExpect(status().isUnauthorized());

    verifyNoInteractions(seasonMatchImporter);
  }

  @Test
  @WithMockUser
  void rejectsAnAuthenticatedUserWithoutTheMatchImporterRole() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/match-imports")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"season": 2026}
                    """))
        .andExpect(status().isForbidden());

    verifyNoInteractions(seasonMatchImporter);
  }

  @Test
  @WithMockUser(roles = "MATCH_IMPORTER")
  void rejectsAnInvalidSeason() throws Exception {
    mockMvc
        .perform(
            post("/api/v1/admin/match-imports")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"season": 0}
                    """))
        .andExpect(status().isBadRequest());

    verifyNoInteractions(seasonMatchImporter);
  }

  @Test
  @WithMockUser(roles = "MATCH_IMPORTER")
  void importsASeasonAndReturnsTheProcessedMatchCount() throws Exception {
    when(seasonMatchImporter.importSeason(2026)).thenReturn(new SeasonMatchImportResult(2026, 240));

    mockMvc
        .perform(
            post("/api/v1/admin/match-imports")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {"season": 2026}
                    """))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.season").value(2026))
        .andExpect(jsonPath("$.processedMatches").value(240));

    verify(seasonMatchImporter).importSeason(2026);
  }
}
