package com.zipdaproperty.domain.apartment.service;

import com.zipdaproperty.domain.apartment.entity.ApartmentComplex;
import com.zipdaproperty.domain.apartment.repository.ApartmentComplexRepository;
import com.zipdaproperty.domain.property.constant.PropertyType;
import com.zipdaproperty.global.error.custom.BusinessException;
import com.zipdaproperty.global.response.constant.CustomResponseCode;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ApartmentComplexValidationServiceTest {

    private static final Long COMPLEX_ID = 15L;
    private static final Long REGION_ID = 53390L;

    private final ApartmentComplexRepository repository =
            mock(ApartmentComplexRepository.class);

    private final ApartmentComplexValidationService service =
            new ApartmentComplexValidationService(repository);

    @Test
    void validateForProperty_nonApartmentWithoutComplex_passes() {
        service.validateForProperty(PropertyType.VILLA, null, REGION_ID);

        verify(repository, never())
                .findByApartmentComplexIdAndIsActiveTrueAndDeletedAtIsNull(COMPLEX_ID);
    }

    @Test
    void validateForProperty_nonApartmentWithComplex_rejects() {
        assertThatThrownBy(() -> service.validateForProperty(
                PropertyType.OFFICETEL,
                COMPLEX_ID,
                REGION_ID
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.INVALID_REQUEST)
        );
    }

    @Test
    void validateForProperty_apartmentWithoutComplex_rejects() {
        assertThatThrownBy(() -> service.validateForProperty(
                PropertyType.APARTMENT,
                null,
                REGION_ID
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.INVALID_REQUEST)
        );
    }

    @Test
    void validateForProperty_unknownComplex_rejects() {
        when(repository.findByApartmentComplexIdAndIsActiveTrueAndDeletedAtIsNull(COMPLEX_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.validateForProperty(
                PropertyType.APARTMENT,
                COMPLEX_ID,
                REGION_ID
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.NOT_FOUND_RESOURCE)
        );
    }

    @Test
    void validateForProperty_complexInOtherRegion_rejects() {
        ApartmentComplex apartmentComplex = apartmentComplex(999L);
        when(repository.findByApartmentComplexIdAndIsActiveTrueAndDeletedAtIsNull(COMPLEX_ID))
                .thenReturn(Optional.of(apartmentComplex));

        assertThatThrownBy(() -> service.validateForProperty(
                PropertyType.APARTMENT,
                COMPLEX_ID,
                REGION_ID
        )).isInstanceOfSatisfying(BusinessException.class, exception ->
                org.assertj.core.api.Assertions.assertThat(exception.getCustomResponseCode())
                        .isEqualTo(CustomResponseCode.PROPERTY_LOCATION_REGION_MISMATCH)
        );
    }

    @Test
    void validateForProperty_activeComplexInSameRegion_passes() {
        ApartmentComplex apartmentComplex = apartmentComplex(REGION_ID);
        when(repository.findByApartmentComplexIdAndIsActiveTrueAndDeletedAtIsNull(COMPLEX_ID))
                .thenReturn(Optional.of(apartmentComplex));

        service.validateForProperty(
                PropertyType.APARTMENT,
                COMPLEX_ID,
                REGION_ID
        );

        verify(repository)
                .findByApartmentComplexIdAndIsActiveTrueAndDeletedAtIsNull(COMPLEX_ID);
    }

    private ApartmentComplex apartmentComplex(Long regionId) {
        ApartmentComplex apartmentComplex = mock(ApartmentComplex.class);
        when(apartmentComplex.getRegionId()).thenReturn(regionId);
        return apartmentComplex;
    }
}
