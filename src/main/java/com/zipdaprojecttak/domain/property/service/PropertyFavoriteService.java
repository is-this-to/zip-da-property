package com.zipdaprojecttak.domain.property.service;

import com.zipdaprojecttak.domain.property.repository.PropertyFavoriteRepository;
import org.springframework.stereotype.Service;

@Service
public class PropertyFavoriteService {
    private final PropertyFavoriteRepository propertyFavoriteRepository;

    public PropertyFavoriteService(
            PropertyFavoriteRepository propertyFavoriteRepository
    ) {
        this.propertyFavoriteRepository = propertyFavoriteRepository;
    }

}
