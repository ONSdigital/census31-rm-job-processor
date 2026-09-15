package uk.gov.ons.census.jobprocessor.utility;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.PayloadDTO;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.RefusalDTO;

class ObjectMapperFactoryTest {
  private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.objectMapper();
  private static final String CASE_ID = "10000000001";
  private static final Instant EVENT_TIME = Instant.parse("2024-06-01T10:15:30Z");
  private static final String ACTION = "CREATE";

  @Test
  void shouldPreserveFieldOrderAndWriteDatesAsIsoStrings() {
    CaseEvent caseEvent = new CaseEvent(CASE_ID, EVENT_TIME, ACTION);

    assertThat(OBJECT_MAPPER.writeValueAsString(caseEvent))
        .isEqualTo(
            "{\"caseId\":\"10000000001\",\"eventTime\":\"2024-06-01T10:15:30Z\",\"action\":\"CREATE\"}");
  }

  @Test
  void shouldIgnoreUnknownPropertiesWhenReadingJson() {
    CaseEvent caseEvent =
        OBJECT_MAPPER.readValue(
            "{\"caseId\":\"10000000001\",\"eventTime\":\"2024-06-01T10:15:30Z\","
                + "\"action\":\"CREATE\",\"extraField\":\"ignored\"}",
            CaseEvent.class);

    assertThat(caseEvent).isEqualTo(new CaseEvent(CASE_ID, EVENT_TIME, ACTION));
  }

  @Test
  void shouldOmitNullFieldsFromMessagingDtos() {
    PayloadDTO payloadDTO = new PayloadDTO();

    assertThat(OBJECT_MAPPER.writeValueAsString(payloadDTO)).isEqualTo("{}");

    RefusalDTO refusalDTO = new RefusalDTO();
    refusalDTO.setCaseId(UUID.fromString("123e4567-e89b-12d3-a456-426614174000"));

    assertThat(OBJECT_MAPPER.writeValueAsString(refusalDTO))
        .isEqualTo("{\"caseId\":\"123e4567-e89b-12d3-a456-426614174000\"}");
  }

  private record CaseEvent(String caseId, Instant eventTime, String action) {}
}
