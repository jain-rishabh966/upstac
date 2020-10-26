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
import org.upgrad.upstac.testrequests.lab.CreateLabResult;
import org.upgrad.upstac.testrequests.lab.LabRequestController;
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
class LabRequestControllerTest {

    @InjectMocks
    LabRequestController labRequestController;

    @Mock
    TestRequestUpdateService testRequestUpdateService;

    @Mock
    TestRequestQueryService testRequestQueryService;

    @Mock
    UserLoggedInService userLoggedInService;


    private TestRequest getMockTestRequest(User loggedInUser) {
        TestRequest mockTestRequest = new TestRequest();

        mockTestRequest.setRequestId(1L);
        mockTestRequest.setAge(20);
        mockTestRequest.setCreatedBy(loggedInUser);
        mockTestRequest.setLabResult(new LabResult());
        return mockTestRequest;
    }

    private User initializeTest() {
        User user = createUser();
        Mockito.when(userLoggedInService.getLoggedInUser()).thenReturn(user);
        return user;
    }

    private User createUser() {
        User user = new User();
        user.setId(1L);
        user.setUserName("someone");
        return user;
    }

    private void mockFindBy(User loggedInUser, RequestStatus status) {
        TestRequest mockTestRequest = getMockTestRequest(loggedInUser);
        mockTestRequest.setStatus(status);

        List<TestRequest> list = new LinkedList<>();
        list.add(mockTestRequest);

        Mockito.when(testRequestQueryService.findBy(status)).thenReturn(list);
    }


    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_update_the_request_status(){
        User loggedInUser = initializeTest();
        mockFindBy(loggedInUser, RequestStatus.INITIATED);

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.INITIATED);
        Mockito.when(testRequestUpdateService.assignForLabTest(testRequest.getRequestId(), loggedInUser))
                .thenReturn(testRequest);

        TestRequest dummyTestRequest = labRequestController.assignForLabTest(testRequest.getRequestId());

        assertNotNull(dummyTestRequest.getLabResult());
        assertThat(dummyTestRequest.getRequestId(), is(equalTo(testRequest.getRequestId())));
        assertThat(dummyTestRequest.getStatus(), is(equalTo(RequestStatus.INITIATED)));
    }

    public TestRequest getTestRequestByStatus(RequestStatus status) {
        return testRequestQueryService.findBy(status).stream().findFirst().get();
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_assignForLabTest_with_valid_test_request_id_should_throw_exception(){
        User loggedInUser = initializeTest();
        Long InvalidRequestId= -34L;
        Mockito.when(testRequestUpdateService.assignForLabTest(InvalidRequestId, loggedInUser))
                .thenThrow((new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID")));

        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> labRequestController.assignForLabTest(InvalidRequestId));

        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_valid_test_request_id_should_update_the_request_status_and_update_test_request_details(){
        User loggedInUser = initializeTest();
        mockFindBy(loggedInUser, RequestStatus.LAB_TEST_IN_PROGRESS);

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = this.getCreateLabResult(testRequest);

        Mockito.when(testRequestUpdateService.updateLabTest(testRequest.getRequestId(), createLabResult, loggedInUser))
                .thenReturn(testRequest);
        testRequest.setStatus(RequestStatus.LAB_TEST_COMPLETED);


        TestRequest response = labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult);

        assertNotNull(response);
        assertThat(response.getRequestId(), is(equalTo(testRequest.getRequestId())));
        assertThat(response.getStatus(), is(equalTo(RequestStatus.LAB_TEST_COMPLETED)));
        assertThat(response.getLabResult().getResult(), is(equalTo(testRequest.getLabResult().getResult())));
    }


    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_test_request_id_should_throw_exception(){
        User loggedInUser = initializeTest();
        Long id = -10L;
        mockFindBy(loggedInUser, RequestStatus.LAB_TEST_IN_PROGRESS);


        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = this.getCreateLabResult(testRequest);

        Mockito.when(testRequestUpdateService.updateLabTest(id, createLabResult, loggedInUser))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid ID"));


        ResponseStatusException responseStatusException = assertThrows(ResponseStatusException.class, () -> labRequestController.updateLabTest(id, createLabResult));

        assertThat(responseStatusException.getMessage(), containsString("Invalid ID"));
    }

    @Test
    @WithUserDetails(value = "tester")
    public void calling_updateLabTest_with_invalid_empty_status_should_throw_exception(){
        User loggedInUser = initializeTest();
        mockFindBy(loggedInUser, RequestStatus.LAB_TEST_IN_PROGRESS);

        TestRequest testRequest = getTestRequestByStatus(RequestStatus.LAB_TEST_IN_PROGRESS);
        CreateLabResult createLabResult = this.getCreateLabResult(testRequest);
        createLabResult.setResult(null);

        Mockito.when(testRequestUpdateService.updateLabTest(testRequest.getRequestId(), createLabResult, loggedInUser))
                .thenThrow(new ResponseStatusException(HttpStatus.BAD_REQUEST, "ConstraintViolationException"));

        ResponseStatusException responseStatusException =
                assertThrows(ResponseStatusException.class, () -> labRequestController.updateLabTest(testRequest.getRequestId(), createLabResult));

        assertThat(responseStatusException.getMessage(), containsString("ConstraintViolationException"));
    }

    public CreateLabResult getCreateLabResult(TestRequest testRequest) {
        CreateLabResult createLabResult = new CreateLabResult();

        createLabResult.setBloodPressure("90");
        createLabResult.setComments("comment");
        createLabResult.setHeartBeat("80");
        createLabResult.setOxygenLevel("99");
        createLabResult.setResult(TestStatus.NEGATIVE);
        createLabResult.setTemperature("97");

        return createLabResult;
    }

}