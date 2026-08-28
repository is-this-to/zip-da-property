package com.zipdaproject.domain.property.service;

import com.zipdaproject.domain.property.repository.PropertyFavoriteRepository;
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
