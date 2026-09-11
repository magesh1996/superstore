package com.superstore.order.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.superstore.order.entity.OrdHdrEntity;

public interface OrdHdrRepository extends JpaRepository<OrdHdrEntity, Long> 
{
    // Basic CRUD is automatically available
    List<OrdHdrEntity> findAllByOrderByOrdNo();    
}