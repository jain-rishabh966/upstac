package org.upgrad.upstac.testrequests.consultation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.validation.annotation.Validated;
import org.upgrad.upstac.testrequests.TestRequest;
import org.upgrad.upstac.users.User;

import javax.transaction.Transactional;
import java.time.LocalDate;

@Service
@Validated
public class ConsultationService {

    @Autowired
    private ConsultationRepository consultationRepository;

    private static Logger logger = LoggerFactory.getLogger(ConsultationService.class);


    @Transactional
    public Consultation assignForConsultation( TestRequest testRequest, User doctor) {
        // Implement this method to assign the consultation service
        Consultation consultation = new Consultation();

        consultation.setDoctor(doctor);
        consultation.setRequest(testRequest);

        return consultationRepository.save(consultation);
    }

    public Consultation updateConsultation(TestRequest testRequest , CreateConsultationRequest createConsultationRequest) {
        // Implement this method to update the consultation

        Consultation consultation = new Consultation();

        consultation.setSuggestion(createConsultationRequest.getSuggestion());
        consultation.setComments(createConsultationRequest.getComments());
        consultation.setUpdatedOn(LocalDate.now());

        return consultationRepository.save(consultation);

    }

}