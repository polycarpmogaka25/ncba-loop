package com.integration.ncba.test.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "country_info")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CountryInfo {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String isoCode;

    private String name;
    private String capitalCity;
    private String phoneCode;
    private String continentCode;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.EAGER, orphanRemoval = true)
    @JoinColumn(name = "country_id")
    private List<Language> languages = new ArrayList<>();
}