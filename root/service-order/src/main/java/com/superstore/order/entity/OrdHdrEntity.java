package com.superstore.order.entity;

import java.time.LocalDateTime;
import java.util.Objects;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "ORDER_HEADER")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class) //required for auditing to work
public class OrdHdrEntity {

    @Id
    @Column(name = "ORD_NO", nullable = false)
    private Long ordNo;

    @Column(name = "MOBILE", nullable = false)
    private Long mobile;

    @Column(name = "CREATE_USER", length = 50, nullable = false, updatable = false)
    @CreatedBy
    private String createUser;

    @Column(name = "CREATE_TSTAMP", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createTstamp; //set automatically when entity is saved

    @Column(name = "MODIFY_USER", length = 50, nullable = false)
    @LastModifiedBy
    private String modifyUser;

    @Column(name = "MODIFY_TSTAMP", nullable = false)
    @LastModifiedDate
    private LocalDateTime modifyTstamp; //updated automatically on save/update

    @Builder.Default
    @Column(name = "ACTIVE_FLAG", length = 1, nullable = false)
    private String activeFlag = "Y";

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrdHdrEntity)) return false;
        OrdHdrEntity that = (OrdHdrEntity) o;
        return Objects.equals(ordNo, that.ordNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ordNo);
    }
}