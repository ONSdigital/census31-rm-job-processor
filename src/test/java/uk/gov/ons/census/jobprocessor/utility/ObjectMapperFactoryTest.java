package uk.gov.ons.census.jobprocessor.utility;

import static org.assertj.core.api.Assertions.assertThat;
import static uk.gov.ons.census.jobprocessor.testutils.JunkDataHelper.getJobRowData;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;
import uk.gov.ons.census.common.model.entity.EventType;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.EventDTO;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.EventHeaderDTO;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.InvalidAddressDTO;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.NewCase;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.PayloadDTO;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.RefusalDTO;
import uk.gov.ons.census.jobprocessor.model.dto.messaging.RefusalTypeDTO;

class ObjectMapperFactoryTest {
  private static final ObjectMapper OBJECT_MAPPER = ObjectMapperFactory.objectMapper();
  private static final String CASE_ID = "10000000001";
  private static final Instant CASE_EVENT_TIME = Instant.parse("2024-06-01T10:15:30Z");
  private static final OffsetDateTime MESSAGE_DATE_TIME =
      OffsetDateTime.parse("2024-06-01T10:15:30Z");
  private static final String ACTION = "CREATE";
  private static final String EVENT_SOURCE = "JOB_PROCESSOR";
  private static final String EVENT_CHANNEL = "RM";
  private static final String PROCESSED_BY = "norman";
  private static final String NEW_CASE_TOPIC = "event_new-case";
  private static final String REFUSAL_TOPIC = "event_refusal-received";
  private static final String INVALID_ADDRESS_TOPIC = "event_address-not-valid";
  private static final UUID JOB_ID = UUID.fromString("fa65fce2-3f08-478d-ab2d-999e8d2f6476");
  private static final UUID COLLECTION_EXERCISE_ID =
      UUID.fromString("f37b742d-f3dd-410b-a4b0-9b51d9c849ea");
  private static final UUID NEW_CASE_ID = UUID.fromString("c57a11a0-28b4-4c46-ae34-f36d2ff0dc42");
  private static final UUID REFUSAL_CASE_ID =
      UUID.fromString("4334e3c9-9501-438a-9583-f1f1e680892b");
  private static final UUID INVALID_CASE_ID =
      UUID.fromString("539414f6-9a83-4a0c-bf5a-b546f2188b59");
  private static final UUID NEW_CASE_MESSAGE_ID =
      UUID.fromString("7d836ce8-19f4-45be-a8b6-ac6704d3f2cd");
  private static final UUID REFUSAL_MESSAGE_ID =
      UUID.fromString("9f592de5-f72d-4b72-8ba9-fb3fdcb70ac4");
  private static final UUID INVALID_CASE_MESSAGE_ID =
      UUID.fromString("15fd38f1-d1c9-4a94-b319-e8695d11cd91");

  @Test
  void shouldPreserveFieldOrderAndWriteDatesAsIsoStrings() {
    CaseEvent caseEvent = new CaseEvent(CASE_ID, CASE_EVENT_TIME, ACTION);

    assertThat(OBJECT_MAPPER.writeValueAsString(caseEvent))
        .isEqualTo(
            "{\"caseId\":\"10000000001\",\"eventTime\":\"2024-06-01T10:15:30Z\",\"action\":\"CREATE\"}");
  }

  @Test
  void shouldSerializeNewCaseEventsUsingExistingWireFormat() {
    EventDTO eventDTO = buildNewCaseEvent();

    assertThat(OBJECT_MAPPER.writeValueAsString(eventDTO))
        .isEqualTo(
            "{\"header\":{"
                + expectedHeader(NEW_CASE_TOPIC, NEW_CASE_MESSAGE_ID, "NEW_CASE")
                + "},\"payload\":{\"newCase\":{"
                + "\"caseId\":\"c57a11a0-28b4-4c46-ae34-f36d2ff0dc42\","
                + "\"collectionExerciseId\":\"f37b742d-f3dd-410b-a4b0-9b51d9c849ea\","
                + "\"uprn\":\"0000\",\"estabUprn\":\"0000\",\"addressType\":\"HH\","
                + "\"estabType\":\"HOUSEHOLD\",\"addressLevel\":\"U\",\"abpCode\":\"0000\","
                + "\"organisationName\":\"\",\"addressLine1\":\"test \",\"addressLine2\":\"\","
                + "\"addressLine3\":\"\",\"townName\":\"Ponty\",\"postcode\":\"CF1 0AB\","
                + "\"latitude\":\"35.123456\",\"longitude\":\"45.876543\",\"oa\":\"0000\","
                + "\"lsoa\":\"0000\",\"msoa\":\"0000\",\"lad\":\"0000\",\"region\":\"SA\","
                + "\"htcWillingness\":\"1\",\"htcDigital\":\"1\",\"fieldCoordinatorId\":\"0000\","
                + "\"fieldOfficerId\":\"0000\",\"treatmentCode\":\"HH_ONS\","
                + "\"ceExpectedCapacity\":0,\"secureEstablishment\":false,\"printBatch\":\"01\"}}}");
  }

  @Test
  void shouldSerializeRefusalEventsUsingExistingWireFormat() {
    EventDTO eventDTO = buildRefusalEvent();

    assertThat(OBJECT_MAPPER.writeValueAsString(eventDTO))
        .isEqualTo(
            "{\"header\":{"
                + expectedHeader(REFUSAL_TOPIC, REFUSAL_MESSAGE_ID, "REFUSAL_RECEIVED")
                + "},\"payload\":{\"refusal\":{"
                + "\"caseId\":\"4334e3c9-9501-438a-9583-f1f1e680892b\",\"type\":\"HARD_REFUSAL\"}}}");
  }

  @Test
  void shouldSerializeInvalidAddressEventsUsingExistingWireFormat() {
    EventDTO eventDTO = buildInvalidAddressEvent();

    assertThat(OBJECT_MAPPER.writeValueAsString(eventDTO))
        .isEqualTo(
            "{\"header\":{"
                + expectedHeader(
                    INVALID_ADDRESS_TOPIC, INVALID_CASE_MESSAGE_ID, "ADDRESS_NOT_VALID")
                + "},\"payload\":{\"invalidAddress\":{\"caseId\":\"539414f6-9a83-4a0c-bf5a-b546f2188b59\","
                + "\"reason\":\"why\"}}}");
  }

  @Test
  void shouldIgnoreUnknownPropertiesWhenReadingJson() {
    CaseEvent caseEvent =
        OBJECT_MAPPER.readValue(
            "{\"caseId\":\"10000000001\",\"eventTime\":\"2024-06-01T10:15:30Z\","
                + "\"action\":\"CREATE\",\"extraField\":\"ignored\"}",
            CaseEvent.class);

    assertThat(caseEvent).isEqualTo(new CaseEvent(CASE_ID, CASE_EVENT_TIME, ACTION));
  }

  @Test
  void shouldIgnoreTrailingTokensWhenReadingJson() {
    CaseEvent caseEvent =
        OBJECT_MAPPER.readValue(
            "{\"caseId\":\"10000000001\",\"eventTime\":\"2024-06-01T10:15:30Z\","
                + "\"action\":\"CREATE\"} true",
            CaseEvent.class);

    assertThat(caseEvent).isEqualTo(new CaseEvent(CASE_ID, CASE_EVENT_TIME, ACTION));
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

  private static EventDTO buildNewCaseEvent() {
    Map<String, String> jobRowData = getJobRowData();

    NewCase newCase = new NewCase();
    newCase.setCaseId(NEW_CASE_ID);
    newCase.setCollectionExerciseId(COLLECTION_EXERCISE_ID);
    newCase.setUprn(jobRowData.get("UPRN"));
    newCase.setEstabUprn(jobRowData.get("ESTAB_UPRN"));
    newCase.setAddressType(jobRowData.get("ADDRESS_TYPE"));
    newCase.setEstabType(jobRowData.get("ESTAB_TYPE"));
    newCase.setAddressLevel(jobRowData.get("ADDRESS_LEVEL"));
    newCase.setAbpCode(jobRowData.get("ABP_CODE"));
    newCase.setOrganisationName(jobRowData.get("ORGANISATION_NAME"));
    newCase.setAddressLine1(jobRowData.get("ADDRESS_LINE1"));
    newCase.setAddressLine2(jobRowData.get("ADDRESS_LINE2"));
    newCase.setAddressLine3(jobRowData.get("ADDRESS_LINE3"));
    newCase.setTownName(jobRowData.get("TOWN_NAME"));
    newCase.setPostcode(jobRowData.get("POSTCODE"));
    newCase.setLatitude(jobRowData.get("LATITUDE"));
    newCase.setLongitude(jobRowData.get("LONGITUDE"));
    newCase.setOa(jobRowData.get("OA"));
    newCase.setLsoa(jobRowData.get("LSOA"));
    newCase.setMsoa(jobRowData.get("MSOA"));
    newCase.setLad(jobRowData.get("LAD"));
    newCase.setRegion(jobRowData.get("REGION"));
    newCase.setHtcWillingness(jobRowData.get("HTC_WILLINGNESS"));
    newCase.setHtcDigital(jobRowData.get("HTC_DIGITAL"));
    newCase.setFieldCoordinatorId(jobRowData.get("FIELDCOORDINATOR_ID"));
    newCase.setFieldOfficerId(jobRowData.get("FIELDOFFICER_ID"));
    newCase.setTreatmentCode(jobRowData.get("TREATMENT_CODE"));
    newCase.setCeExpectedCapacity(Integer.parseInt(jobRowData.get("CE_EXPECTED_CAPACITY")));
    newCase.setSecureEstablishment(Boolean.parseBoolean(jobRowData.get("CE_SECURE")));
    newCase.setPrintBatch(jobRowData.get("PRINT_BATCH"));

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setNewCase(newCase);

    return buildEvent(NEW_CASE_TOPIC, NEW_CASE_MESSAGE_ID, EventType.NEW_CASE, payloadDTO);
  }

  private static EventDTO buildRefusalEvent() {
    RefusalDTO refusalDTO = new RefusalDTO();
    refusalDTO.setCaseId(REFUSAL_CASE_ID);
    refusalDTO.setType(RefusalTypeDTO.HARD_REFUSAL);

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setRefusal(refusalDTO);

    return buildEvent(REFUSAL_TOPIC, REFUSAL_MESSAGE_ID, EventType.REFUSAL_RECEIVED, payloadDTO);
  }

  private static EventDTO buildInvalidAddressEvent() {
    InvalidAddressDTO invalidAddressDTO = new InvalidAddressDTO();
    invalidAddressDTO.setCaseId(INVALID_CASE_ID);
    invalidAddressDTO.setReason("why");

    PayloadDTO payloadDTO = new PayloadDTO();
    payloadDTO.setInvalidAddress(invalidAddressDTO);

    return buildEvent(
        INVALID_ADDRESS_TOPIC, INVALID_CASE_MESSAGE_ID, EventType.ADDRESS_NOT_VALID, payloadDTO);
  }

  private static EventDTO buildEvent(
      String topic, UUID messageId, EventType eventType, PayloadDTO payloadDTO) {
    EventDTO eventDTO = new EventDTO();
    eventDTO.setHeader(buildEventHeader(topic, messageId, eventType));
    eventDTO.setPayload(payloadDTO);
    return eventDTO;
  }

  private static EventHeaderDTO buildEventHeader(
      String topic, UUID messageId, EventType eventType) {
    EventHeaderDTO eventHeaderDTO = new EventHeaderDTO();
    eventHeaderDTO.setVersion(Constants.EVENT_SCHEMA_VERSION);
    eventHeaderDTO.setTopic(topic);
    eventHeaderDTO.setSource(EVENT_SOURCE);
    eventHeaderDTO.setChannel(EVENT_CHANNEL);
    eventHeaderDTO.setDateTime(MESSAGE_DATE_TIME);
    eventHeaderDTO.setMessageId(messageId);
    eventHeaderDTO.setCorrelationId(JOB_ID);
    eventHeaderDTO.setOriginatingUser(PROCESSED_BY);
    eventHeaderDTO.setMessageType(eventType);
    return eventHeaderDTO;
  }

  private static String expectedHeader(String topic, UUID messageId, String messageType) {
    return "\"version\":\""
        + Constants.EVENT_SCHEMA_VERSION
        + "\",\"topic\":\""
        + topic
        + "\",\"source\":\""
        + EVENT_SOURCE
        + "\",\"channel\":\""
        + EVENT_CHANNEL
        + "\",\"dateTime\":\""
        + MESSAGE_DATE_TIME
        + "\",\"messageId\":\""
        + messageId
        + "\",\"correlationId\":\""
        + JOB_ID
        + "\",\"originatingUser\":\""
        + PROCESSED_BY
        + "\",\"messageType\":\""
        + messageType
        + "\"";
  }

  private record CaseEvent(String caseId, Instant eventTime, String action) {}
}
