package com.teamwork.animalshelter.service;

import com.teamwork.animalshelter.model.ProbationDataType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AnimalShetlerInfoService {
    @Value("${shetler.info.common-info}")
    private String commonInfo;

    @Value("${shetler.info.contact}")
    private String contacts;

    @Value("${shetler.info.accident-prevention}")
    private String accidentPrevention;

    @Value("${shetler.info.paperwork}")
    private String paperwork;

    @Value("${shetler.info.rules-of-first-contact}")
    private String rulesOfFirstContact;

    @Value("${shetler.info.transportation-animal}")
    private String transportationAnimal;

    @Value("${shetler.info.initial-handling-with-animal}")
    private String initialHandlingWithAnimal;

    @Value("${shetler.info.cinologists-recommendations}")
    private String cinologistsRecommendations;

    @Value("${shetler.info.refusing-reasons}")
    private String refusingReasons;

    @Value("${shetler.info.recommendations-home-for-puppy}")
    private String recommendationsHomeForPuppy;

    @Value("${shetler.info.recommendations-home-for-adult-dog}")
    private String recommendationsHomeForAdultDog;

    @Value("${shetler.info.recommendations-home-for-handicapped-dog}")
    private String recommendationsHomeForHandicappedDog;

    private  Map<String, ProbationDataType> parseResources(String resources) {

    }

    public Map<String, ProbationDataType> getCommonInfo() {

    }

    public Map<String, ProbationDataType> getContacts() {

    }

    public Map<String, ProbationDataType> getAccidentPrevention() {

    }

    public Map<String, ProbationDataType> getPaperwork() {

    }

    public Map<String, ProbationDataType> getRulesOfFirstContact() {

    }

    public Map<String, ProbationDataType> getTransportationAnimal() {

    }

    public Map<String, ProbationDataType> getInitialHandlingWithAnimal() {

    }

    public Map<String, ProbationDataType> getCinologistsRecommendations() {

    }

    public Map<String, ProbationDataType> getRefusingReasons() {

    }

    public Map<String, ProbationDataType> getRecommendationsHomeForPuppy() {

    }

    public Map<String, ProbationDataType> getRecommendationsHomeForAdultDog() {

    }

    public Map<String, ProbationDataType> getRecommendationsHomeForHandicappedDog() {

    }

}
