package com.integration.ncba.test.models.dto;

import com.integration.ncba.test.models.Language;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Country {
    private String capitalCity;
    private String phoneCode;
    private String continentCode;
    private List<Language> languages = new ArrayList<>();


}
