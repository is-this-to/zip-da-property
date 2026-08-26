package com.zipdaprojecttak.domain.property.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "property_favorite")
public class PropertyFavorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY) // favoriteId는 생성자를 직접 넣는 게 아니라 DB가 자동으로 만들어준다
    @Column(name = "favorite_id")
    private Long favoriteId;

    @Column(name = "member_id", nullable = false)
    private Long memberId;

    @Column(name = "property_id", nullable = false)
    private Long propertyId;

    @Column(name = "active_favorite_key",insertable = false,updatable = false)
    private String activeFavoriteKey;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "created_by_member_id")
    private Long createdByMemberId;

    @Column(name = "created_by_role")
    private String createdByRole;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "updated_by_member_id")
    private Long updatedByMemberId;

    @Column(name ="updated_by_role")
    private String updatedByRole;

    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    @Column(name = "deleted_by_member_id")
    private Long deletedByMemberId;

    @Column(name = "deleted_by_role")
    private String deletedByRole;

    @Column(name = "delete_reason")
    private String deletedReason;

    @Column(name = "action_source", nullable = false)
    private  String actionSource;

    protected PropertyFavorite() {

    } //JPA가 DB 데이터를 Entity로 만들 때 필요

    }
