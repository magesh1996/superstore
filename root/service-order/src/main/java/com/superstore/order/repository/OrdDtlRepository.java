package com.superstore.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.superstore.order.entity.OrdDtlEntity;
import com.superstore.order.entity.OrdDtlIdEntity;

public interface OrdDtlRepository extends JpaRepository<OrdDtlEntity, OrdDtlIdEntity> 
{
    List<OrdDtlEntity> findByIdOrdNo(Long ordNo);

    List<OrdDtlEntity> findByIdOrdNoOrderByIdLineNo(Long ordNo);
}