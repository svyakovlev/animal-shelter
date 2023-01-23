package com.teamwork.animalshelter.service;

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

    private  Map<String, Enum> parseResources(String resources) {

    }

    public Map<String, Enum> getCommonInfo() {

    }

    public Map<String, Enum> getContacts() {

    }

    public Map<String, Enum> getAccidentPrevention() {

    }

    public Map<String, Enum> getPaperwork() {

    }

    public Map<String, Enum> getRulesOfFirstContact() {

    }

    public Map<String, Enum> getTransportationAnimal() {

    }

    public Map<String, Enum> getInitialHandlingWithAnimal() {

    }

    public Map<String, Enum> getCinologistsRecommendations() {

    }

    public Map<String, Enum> getRefusingReasons() {

    }

    public Map<String, Enum> getRecommendationsHomeForPuppy() {

    }

    public Map<String, Enum> getRecommendationsHomeForAdultDog() {

    }

    public Map<String, Enum> getRecommendationsHomeForHandicappedDog() {

    }



}
