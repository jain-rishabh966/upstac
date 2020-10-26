package org.upgrad.upstac.testrequests;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.web.server.ResponseStatusException;
import org.upgrad.upstac.config.security.UserLoggedInService;
import org.upgrad.upstac.testrequests.consultation.Consultation;
import org.upgrad.upstac.testrequests.consultation.ConsultationController;
import org.upgrad.upstac.testrequests.consultation.CreateConsultationRequest;
import org.upgrad.upstac.testrequests.consultation.DoctorSuggestion;
import org.upgrad.upstac.testrequests.flow.TestRequestFlowService;
import org.upgrad.upstac.testrequests.lab.LabResult;
import org.upgrad.upstac.testrequests.lab.TestStatus;
import org.upgrad.upstac.users.User;

import java.util.LinkedList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;


@ExtendWith(MockitoExtension.class)
class ConsultationControllerTest {

    @InjectMocks
    ConsultationController consultationController;

    @Mock
    private TestRequestUpdateService testRequestUpdateService;// TODO: Mock

    @Mock
    private TestRequestQueryService testRequestQueryService;// TODO: Mock

    @Mock
    TestRequestFlowService testRequestFlowService;// TODO: Mock

    @Mock
    private UserLoggedInService userLoggedInService;


    private TestRequest getMockTestRequest(User loggedInUser) {
        TestRequest mockTestRequest = new TestRequest();

        mockTestRequest.setRequestId(1L);
        mockTestRequest.setAge(20);
        mockTestRequest.setCreatedBy(loggedInUser);

        Consultation consultation = new Consultation();
        consultation.setSuggestion(DoctorSuggestion.NO_ISSUES);
        mockTestRequest.setConsultation(consultation);

        LabResult labResult = new LabResult();
        labResult.setResult(TestStatus.NEGATIVE);
        mockTestRequest.setLabResult(labResult);

        return mockTestRequest;
    }

    private void mockFindBy(User loggedInUser, RequestStatus status) {
        TestRequest mockTestRequest = getMockTestRequest(loggedInUser);
        mockTestRequest.setStatus(status);

        List<TestRequest> list = new LinkedList<>();
        list.add(mockTestRequest);

        Mockito.when(testRequestQueryService.findBy(status)).thenReturn(list);
    }

    private User initializeTest() {
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        return user;
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_update_the_request_status(){
        User loggedInUser = initializeTest();
        mockFindBy(loggedInUser, RequestStatus.LAB_TEST_COMPLETED);

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_COMPLETED);

        TestRequest completedRequest = new TestRequest();
        completedRequest.setStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        completedRequest.setConsultation(new Consultation());
        completedRequest.setRequestId(testRequest.getRequestId());

        Mockito.when(testRequestUpdateService.assignForConsultation(testRequest.getRequestId(), loggedInUser)).thenReturn(completedRequest);

        TestRequest dummyRequest = consultationController.assignForConsultation(testRequest.getRequestId());

        assertNotNull(dummyRequest.getConsultation());
        assertThat(dummyRequest.getRequestId(), is(equalTo(testRequest.getRequestId())));
        assertThat(dummyRequest.getStatus(), is(equalTo(RequestStatus.DIAGNOSIS_IN_PROCESS)));
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_assignForConsultation_with_valid_test_request_id_should_throw_exception(){
        User loggedInUser = initializeTest();

        Long InvalidRequestId= -34L;

        Mockito.when(testRequestUpdateService.assignForConsultation(InvalidRequestId, loggedInUser))
                .thenThrow((new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID")));

        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> {
            consultationController.assignForConsultation(InvalidRequestId);
        });

        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_valid_test_request_id_should_update_the_request_status_and_update_consultation_details(){
        User loggedInUser = initializeTest();
        mockFindBy(loggedInUser, RequestStatus.DIAGNOSIS_IN_PROCESS);

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);
        CreateConsultationRequest createConsultationRequest = this.getCreateConsultationRequest(testRequest);

        TestRequest completedRequest = new TestRequest();

        Consultation consultation = new Consultation();
        consultation.setSuggestion(DoctorSuggestion.NO_ISSUES);

        completedRequest.setStatus(RequestStatus.COMPLETED);
        completedRequest.setConsultation(consultation);
        completedRequest.setRequestId(testRequest.getRequestId());

        Mockito.when(testRequestUpdateService.updateConsultation(testRequest.getRequestId(), createConsultationRequest, loggedInUser)).thenReturn(completedRequest);

        TestRequest response = consultationController.updateConsultation(testRequest.getRequestId(), createConsultationRequest);

        assertThat(testRequest.getRequestId(), is(equalTo(response.getRequestId())));
        assertThat(RequestStatus.COMPLETED, is(equalTo(response.getStatus())));
        assertThat(testRequest.getConsultation().getSuggestion(), is(equalTo(response.getConsultation().getSuggestion())));
    }


    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_test_request_id_should_throw_exception(){
        User loggedInUser = initializeTest();
        mockFindBy(loggedInUser, RequestStatus.DIAGNOSIS_IN_PROCESS);
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);

        CreateConsultationRequest createConsultationRequest = this.getCreateConsultationRequest(testRequest);
        Long id = -10L;

        Mockito.when(testRequestUpdateService.updateConsultation(id, createConsultationRequest, loggedInUser))
                .thenThrow((new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID")));

        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> {
            consultationController.updateConsultation(id, createConsultationRequest);
        });

        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "doctor")
    public void calling_updateConsultation_with_invalid_empty_status_should_throw_exception(){
        User loggedInUser = initializeTest();
        mockFindBy(loggedInUser, RequestStatus.DIAGNOSIS_IN_PROCESS);
        TestRequest testRequest = getTestRequestByStatus(RequestStatus.DIAGNOSIS_IN_PROCESS);

        CreateConsultationRequest createConsultationRequest = this.getCreateConsultationRequest(testRequest);
        createConsultationRequest.setSuggestion(null);

        Mockito.when(testRequestUpdateService.updateConsultation(testRequest.getRequestId(), createConsultationRequest, loggedInUser))
                .thenThrow((new ResponseStatusException(HttpStatus.BAD_REQUEST)));

        assertThrows(ResponseStatusException.class, () -> {
            consultationController.updateConsultation(testRequest.getRequestId(), createConsultationRequest);
        });
    }

    public CreateConsultationRequest getCreateConsultationRequest(TestRequest testRequest) {
        if (testRequest == null || testRequest.getLabResult() == null || testRequest.getLabResult().getResult() == null) {
            return null;
        }

        CreateConsultationRequest createConsultationRequest = new CreateConsultationRequest();

        TestStatus testStatus = testRequest.getLabResult().getResult();

        if (testStatus.equals(TestStatus.NEGATIVE)) {
            createConsultationRequest.setSuggestion(DoctorSuggestion.NO_ISSUES);
            createConsultationRequest.setComments("Ok");
        } else if (testStatus.equals(TestStatus.POSITIVE)) {
            createConsultationRequest.setSuggestion(DoctorSuggestion.HOME_QUARANTINE);
            createConsultationRequest.setComments("Not Ok! Take Care and contact me if need be");
        }

        return createConsultationRequest;

    }

    private User createUser() {
        User user = new User();

        user.setId(1L);
        user.setUserName("Someone");
        return user;
    }

}