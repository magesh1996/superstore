package com.superstore.order.entity;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
//public class OrdDtlIdEntity implements Serializable
public class OrdDtlIdEntity
{
	//private static final long serialVersionUID = 1L;

	@Column(name = "ORD_NO", length = 10, nullable = false)
    private Long ordNo;

    @Column(name = "LINE_NO", length = 10, nullable = false)
    private Long lineNo;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrdDtlIdEntity)) return false;
        OrdDtlIdEntity that = (OrdDtlIdEntity) o;
        return Objects.equals(ordNo, that.ordNo) &&
               Objects.equals(lineNo, that.lineNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(ordNo, lineNo);
    }
}