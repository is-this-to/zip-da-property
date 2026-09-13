package com.zipdaproperty.domain.property.entity;

import com.zipdaproperty.domain.property.constant.DisclosureLevel;
import com.zipdaproperty.domain.property.constant.LocationSource;
import com.zipdaproperty.global.context.ActorContext;
import com.zipdaproperty.global.entity.BaseAuditEntity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Filter;
import org.locationtech.jts.geom.Point;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

@Getter
@Entity
@Table(name = "property_address")
@Filter(name = "softDelete")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PropertyAddress extends BaseAuditEntity {

    /**
     * PropertyAddress 자체의 내부 식별자다.
     *
     * Property는 TSID를 사용하지만 PropertyAddress ID는
     * DB의 AUTO_INCREMENT 정책을 사용한다.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(
            name = "property_address_id",
            nullable = false,
            updatable = false
    )
    private Long propertyAddressId;

    /**
     * Property와 PropertyAddress의 논리적인 1:1 관계다
     *
     * JPA 연관관계는 사용하지만 DB의 물리 FK는 생성하지 않는다.
     * 실제 DB의 UNIQUE(property_id) 제약조건과 일치한다.
     */
    @OneToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "property_id",
            nullable = false,
            unique = true,
            foreignKey = @ForeignKey(
                    value = ConstraintMode.NO_CONSTRAINT
            )
    )
    private Property property;

    /**
     * 카카오 Local API가 반환한 10자리 법정동 코드다.
     * 내부 Region.regionCode와의 정합성 검증에 사용한다.
     */
    @Column(
            name = "legal_dong_code",
            nullable = false,
            length = 10
    )
    private String legalDongCode;

    /**
     * 정확한 도로명주소다.
     * 일반 공개 DTO에 포함하면 안 된다.
     */
    @Column(
            name = "exact_road_address",
            length = 300
    )
    private String exactRoadAddress;

    /**
     * 정확한 지번주소다.
     * 일반 공개 DTO에 포함하면 안 된다.
     */
    @Column(
            name = "exact_jibun_address",
            length = 300
    )
    private String exactJibunAddress;

    /**
     * 동·호수 등의 상세주소를 암호화한 값이다.
     * 평문 상세주소를 이 필드에 저장하면 안 된다.
     */
    @Column(
            name = "detail_address_encrypted",
            columnDefinition = "VARBINARY(1000)"
    )
    private byte[] detailAddressEncrypted;

    /**
     * 상세 주소 암호화에 사용한 키의 버전이다.
     *
     * detailAddressEncrypted와 함께 존재하거나
     * 두 필드 모두 null이어야 한다.
     */
    @Column(
            name = "detail_address_key_version",
            length = 50
    )
    private String detailAddressKeyVersion;

    /**
     * 카카오 Local API 등을 통해 확인한 실제 정확 좌표다.
     *
     * POINT의 X는 경도, Y는 위도다.
     * 일반 공개 DTO에는 절대 포함하지 않는다.
     */
    @Column(
            name = "exact_location",
            nullable = false,
            columnDefinition = "POINT SRID 4326"
    )
    private Point exactLocation;

    /**
     * 일반 사용자에게 공개할 수 있는 동 수준 주소다.
     */
    @Column(
            name = "public_address",
            nullable = false,
            length = 300
    )
    private String publicAddress;

    /**
     * 지도 검색과 마커 표시에 사용하는 비식별 공개 좌표다.
     *
     * 지도 bounds 검색에서는 exactLocation이 아니라
     * 이 publicLocation만 사용한다.
     */
    @Column(
            name = "public_location",
            nullable = false,
            columnDefinition = "POINT SRID 4326"
    )
    private Point publicLocation;

    /**
     * 위치 공개 수준이다.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "disclosure_level",
            nullable = false,
            length = 30
    )
    private DisclosureLevel disclosureLevel;

    /**
     * 주소, 법정동 코드, 좌표 검증이 완료된 UTC 시각이다.
     */
    @Column(
            name = "location_verified_at",
            nullable = false,
            columnDefinition = "DATETIME(6)"
    )
    private Instant locationVerifiedAt;

    /**
     * 위치 정보가 만들어진 출처다.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "location_source",
            nullable = false,
            length = 30
    )
    private LocationSource locationSource;

    private PropertyAddress(
            Property property,
            String legalDongCode,
            String exactRoadAddress,
            String exactJibunAddress,
            byte[] detailAddressEncrypted,
            String detailAddressKeyVersion,
            Point exactLocation,
            String publicAddress,
            Point publicLocation,
            DisclosureLevel disclosureLevel,
            Instant locationVerifiedAt,
            LocationSource locationSource,
            ActorContext actorContext
    ){
        super(actorContext);

        validateRequiredValues(
                property,
                legalDongCode,
                exactLocation,
                publicAddress,
                publicLocation,
                disclosureLevel,
                locationVerifiedAt,
                locationSource,
                actorContext
        );

        validateLegalDongCode(legalDongCode);
        validateDetailAddressKeyPair(
                detailAddressEncrypted,
                detailAddressKeyVersion
        );
        validatePoint(exactLocation, "정확 좌표");
        validatePoint(publicLocation, "공개 좌표");

        this.property = property;
        this.legalDongCode = legalDongCode.trim();
        this.exactRoadAddress = normalizeNullableText(exactRoadAddress);
        this.exactJibunAddress = normalizeNullableText(exactJibunAddress);
        this.detailAddressEncrypted = copyEncryptedAddress(detailAddressEncrypted);
        this.detailAddressKeyVersion = normalizeNullableText(detailAddressKeyVersion);
        this.exactLocation = exactLocation;
        this.publicAddress = publicAddress.trim();
        this.publicLocation = publicLocation;
        this.disclosureLevel = disclosureLevel;
        this.locationVerifiedAt = locationVerifiedAt;
        this.locationSource = locationSource;
    }

    /**
     * 새로운 PropertyAddress를 생성한다.
     */
    public static PropertyAddress create(
            Property property,
            String legalDongCode,
            String exactRoadAddress,
            String exactJibunAddress,
            byte[] detailAddressEncrypted,
            String detailAddressKeyVersion,
            Point exactLocation,
            String publicAddress,
            Point publicLocation,
            DisclosureLevel disclosureLevel,
            Instant locationVerifiedAt,
            LocationSource locationSource,
            ActorContext actorContext
    ){
        return new PropertyAddress(
                property,
                legalDongCode,
                exactRoadAddress,
                exactJibunAddress,
                detailAddressEncrypted,
                detailAddressKeyVersion,
                exactLocation,
                publicAddress,
                publicLocation,
                disclosureLevel,
                locationVerifiedAt,
                locationSource,
                actorContext
        );
    }
    /*
     * 기존 PropertyAddress 행의 주소와 위치를 변경한다.
     *
     * UNIQUE(property_id) 정책 때문에 주소가 변경되어도
     * 새로운 행을 추가하지 않고 현재 행을 수정한다.
     */
    public void changeAddress(
            String legalDongCode,
            String exactRoadAddress,
            String exactJibunAddress,
            byte[] detailAddressEncrypted,
            String detailAddressKeyVersion,
            Point exactLocation,
            String publicAddress,
            Point publicLocation,
            DisclosureLevel disclosureLevel,
            Instant locationVerifiedAt,
            LocationSource locationSource,
            ActorContext actorContext
    ){
        validateRequiredValues(
                this.property,
                legalDongCode,
                exactLocation,
                publicAddress,
                publicLocation,
                disclosureLevel,
                locationVerifiedAt,
                locationSource,
                actorContext
        );

        validateLegalDongCode(legalDongCode);
        validateDetailAddressKeyPair(detailAddressEncrypted, detailAddressKeyVersion);
        validatePoint(exactLocation, "정확 좌표");
        validatePoint(publicLocation, "공개 좌표");

        this.legalDongCode = legalDongCode.trim();
        this.exactRoadAddress = normalizeNullableText(exactRoadAddress);
        this.exactJibunAddress = normalizeNullableText(exactJibunAddress);
        this.detailAddressEncrypted = copyEncryptedAddress(detailAddressEncrypted);
        this.detailAddressKeyVersion = normalizeNullableText(detailAddressKeyVersion);
        this.exactLocation = exactLocation;
        this.publicAddress = publicAddress.trim();
        this.publicLocation = publicLocation;
        this.disclosureLevel = disclosureLevel;
        this.locationVerifiedAt = locationVerifiedAt;
        this.locationSource = locationSource;

        recordUpdate(actorContext);
    }

    /*
     * PropertyAddress를 소프트 삭제한다.
     */
    public void softDelete(
            ActorContext actorContext,
            Instant deletedAt,
            String deleteReason
    ){
        recordDeletion(
                actorContext,
                deletedAt,
                deleteReason
        );
    }

    private static void validateRequiredValues(
            Property property,
            String legalDongCode,
            Point exactLocation,
            String publicAddress,
            Point publicLocation,
            DisclosureLevel disclosureLevel,
            Instant locationVerifiedAt,
            LocationSource locationSource,
            ActorContext actorContext
    ){
        Objects.requireNonNull(
                property,
                "매물은 필수입니다."
        );

        Objects.requireNonNull(
                legalDongCode,
                "법정동 코드는 필수입니다."
        );

        Objects.requireNonNull(
                exactLocation,
                "정확 좌표는 필수입니다."
        );

        Objects.requireNonNull(
                publicAddress,
                "공개 주소는 필수입니다."
        );

        Objects.requireNonNull(
                publicLocation,
                "공개 좌표는 필수입니다."
        );

        Objects.requireNonNull(
                disclosureLevel,
                "위치 공개 수준은 필수입니다."
        );

        Objects.requireNonNull(
                locationVerifiedAt,
                "위치 검증 시각은 필수입니다."
        );

        Objects.requireNonNull(
                locationSource,
                "위치 출처는 필수입니다."
        );

        Objects.requireNonNull(
                actorContext,
                "작업 요청 정보는 필수입니다."
        );
        if(publicAddress.isBlank()){
            throw new IllegalArgumentException(
                    "공개 주소는 비어 있을 수 없습니다."
            );
        }
    }

    private static void validateLegalDongCode(String legalDongCode){
        String normalizedCode = legalDongCode.trim();

        if(!normalizedCode.matches("\\d{10}")){
            throw new IllegalArgumentException(
                    "법정동 코드는 10자리 숫자여야 합니다."
            );
        }
    }

    private static void validateDetailAddressKeyPair(byte[] detailAddressEncrypted, String detailAddressKeyVersion){
        boolean encryptedAddressExists = detailAddressEncrypted != null;
        boolean keyVersionExists = detailAddressKeyVersion != null && !detailAddressKeyVersion.isBlank();

        if(encryptedAddressExists != keyVersionExists){
            throw new IllegalArgumentException(
                    "상세주소 암호문과 암호화 키 버전은 함께 존재해야 합니다."
            );
        }

        if(detailAddressEncrypted != null && detailAddressEncrypted.length > 1000){
            throw new IllegalArgumentException("상세주소 암호문은 1000바이트 이하여야 합니다.");
        }
    }

    private static void validatePoint(Point point, String fieldName){
        if(point.isEmpty()){
            throw new IllegalArgumentException(
                    fieldName + "는 비어 있을 수 없습니다."
            );
        }

        if(point.getSRID() != 4326){
            throw new IllegalArgumentException(
                    fieldName + "의 SRID는 4326이어야 합니다."
            );
        }

        double longitude = point.getX();
        double latitude = point.getY();

        if(!Double.isFinite(longitude) || !Double.isFinite(latitude)){
            throw new IllegalArgumentException(
                    fieldName + "에는 유효한 좌표가 필요합니다."
            );
        }

        if(longitude < -180 || longitude > 180){
            throw new IllegalArgumentException(
                    fieldName + "의 경도 범위가 올바르지 않습니다."
            );
        }

        if(latitude < -90 || latitude > 90){
            throw new IllegalArgumentException(
                    fieldName + "의 위도 범위가 올바르지 않습니다."
            );
        }
    }

    private static String normalizeNullableText(String value){
        if(value == null) {
            return null;
        }

        String normalizedValue = value.trim();

        return normalizedValue.isEmpty()
                ? null
                : normalizedValue;
    }

    private static byte[] copyEncryptedAddress(byte[] encryptedAddress){
        if(encryptedAddress == null){
            return null;
        }

        return Arrays.copyOf(
                encryptedAddress,
                encryptedAddress.length
        );
    }

    /**
     * Lombok Getter가 byte[] 원본을 그대로 외부에 반환하지 않도록
     * 방어적 복사본을 반환한다.
     */
    public byte[] getDetailAddressEncrypted(){
        return copyEncryptedAddress(
                detailAddressEncrypted
        );
    }

}
