package com.teamwork.animalshelter.service;

import com.teamwork.animalshelter.exception.WrongStringResources;
import com.teamwork.animalshelter.model.ProbationDataType;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
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

    private Map<String, Map<String, ProbationDataType>> cacheInfo = new HashMap<>();

    private  Map<String, ProbationDataType> parseResources(String resources) {
        String[] array = resources.split(";");
        if (resources.isEmpty()) return null;
        if (array.length % 2 != 0) {
            throw new WrongStringResources(resources);
        }
        Map<String, ProbationDataType> result = new LinkedHashMap<>();
        ProbationDataType value;
        for (int i = 1; i < array.length; i+=2) {
            try {
                value = ProbationDataType.valueOf(array[i-1].strip().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new WrongStringResources(resources);
            }
            result.put(array[i].strip(), value);
        }
        return result;
    }

    private Map<String, ProbationDataType> processCache(String key, String resource) {
        if (cacheInfo.containsKey(key)) {
            return cacheInfo.get(key);
        } else {
            Map<String, ProbationDataType> result = parseResources(resource);
            cacheInfo.put(key, result);
            return result;
        }
    }

    public Map<String, ProbationDataType> getCommonInfo() {
        return processCache("getCommonInfo", commonInfo);
    }

    public Map<String, ProbationDataType> getContacts() {
        return processCache("getContacts", contacts);
    }

    public Map<String, ProbationDataType> getAccidentPrevention() {
        return processCache("getAccidentPrevention", accidentPrevention);
    }

    public Map<String, ProbationDataType> getPaperwork() {
        return processCache("getPaperwork", paperwork);
    }

    public Map<String, ProbationDataType> getRulesOfFirstContact() {
        return processCache("getRulesOfFirstContact", rulesOfFirstContact);
    }

    public Map<String, ProbationDataType> getTransportationAnimal() {
        return processCache("getTransportationAnimal", transportationAnimal);
    }

    public Map<String, ProbationDataType> getInitialHandlingWithAnimal() {
        return processCache("getInitialHandlingWithAnimal", initialHandlingWithAnimal);
    }

    public Map<String, ProbationDataType> getCinologistsRecommendations() {
        return processCache("getCinologistsRecommendations", cinologistsRecommendations);
    }

    public Map<String, ProbationDataType> getRefusingReasons() {
        return processCache("getRefusingReasons", refusingReasons);
    }

    public Map<String, ProbationDataType> getRecommendationsHomeForPuppy() {
        return processCache("getRecommendationsHomeForPuppy", recommendationsHomeForPuppy);
    }

    public Map<String, ProbationDataType> getRecommendationsHomeForAdultDog() {
        return processCache("getRecommendationsHomeForAdultDog", recommendationsHomeForAdultDog);
    }

    public Map<String, ProbationDataType> getRecommendationsHomeForHandicappedDog() {
        return processCache("getRecommendationsHomeForHandicappedDog", recommendationsHomeForHandicappedDog);
    }
}
