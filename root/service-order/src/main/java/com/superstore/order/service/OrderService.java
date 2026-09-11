package com.superstore.order.service;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
//import org.slf4j.MDC;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import com.superstore.order.config.kafka.KafkaHealthService;
import com.superstore.order.entity.OrdDtlEntity;
import com.superstore.order.entity.OrdHdrEntity;
import com.superstore.order.mapper.OrderMapper;
import com.superstore.order.pojo.OrderPojo;
import com.superstore.order.repository.OrdDtlRepository;
import com.superstore.order.repository.OrdHdrRepository;

@Service
public class OrderService {

    // agnostic SLF4J facade logger.
    // loggerName printed in LOG file.
    private static final Logger logger = LoggerFactory.getLogger(OrderService.class);

    private final OrderMapper orderMapper;

    private final OrdHdrRepository ordHdrRepository;
    private final OrdDtlRepository ordDtlRepository;

    private static final String TOPIC = "topic-order-created";
    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final KafkaHealthService kafkaHealthService;

    public OrderService(OrderMapper orderMapper,
                        OrdHdrRepository ordHdrRepository, 
                        OrdDtlRepository ordDtlRepository, 
                        KafkaTemplate<String, Object> kafkaTemplate, 
                        KafkaHealthService kafkaHealthService) {
        this.orderMapper = orderMapper;
        this.ordHdrRepository = ordHdrRepository;
        this.ordDtlRepository = ordDtlRepository;
        this.kafkaTemplate = kafkaTemplate;
        this.kafkaHealthService = kafkaHealthService;
    }

    public List<OrderPojo> getAllOrder() {

        // List<OrderPojo> listOrderPojo = new ArrayList<>();
        // List<OrdHdrEntity> listOrdHdrEntity = ordHdrRepository.findAllByOrderByOrdNo();
        // for (OrdHdrEntity ordHdrEntity : listOrdHdrEntity) {
        //     OrderPojo orderPojo = new OrderPojo();
        //     OrderPojo.OrdHdr hdrPojo = new OrderPojo.OrdHdr();
        //     hdrPojo.setOrdNo(ordHdrEntity.getOrdNo());
        //     hdrPojo.setMobile(ordHdrEntity.getMobile());
        //     orderPojo.setOrdHdr(hdrPojo);
        //     List<OrdDtlEntity> listOrdDtlEntity = ordDtlRepository.findByIdOrdNoOrderByIdLineNo(ordHdrEntity.getOrdNo());
        //     List<OrderPojo.OrdDtl> listDtlPojo = new ArrayList<>();
        //     for (OrdDtlEntity ordDtlEntity : listOrdDtlEntity) {
        //         OrderPojo.OrdDtl dtlPojo = new OrderPojo.OrdDtl();
        //         dtlPojo.setLineNo(ordDtlEntity.getId().getLineNo());
        //         dtlPojo.setCompany(ordDtlEntity.getCompany());
        //         dtlPojo.setSku(ordDtlEntity.getSku());
        //         dtlPojo.setQty(ordDtlEntity.getQty());
        //         listDtlPojo.add(dtlPojo);
        //     }
        //     orderPojo.setOrdDtl(listDtlPojo);
        //     listOrderPojo.add(orderPojo);
        // }

        List<OrderPojo> listOrderPojo = new ArrayList<>();
        
        List<OrdHdrEntity> listOrdHdrEntity = ordHdrRepository.findAllByOrderByOrdNo();
        
        for (OrdHdrEntity ordHdrEntity : listOrdHdrEntity) {
            
            OrderPojo orderPojo = new OrderPojo();
            orderPojo.setOrdHdr(orderMapper.ordHdrEntityToOrdHdrPojo(ordHdrEntity));
            List<OrdDtlEntity> listOrdDtlEntity = ordDtlRepository.findByIdOrdNoOrderByIdLineNo(ordHdrEntity.getOrdNo());
            orderPojo.setOrdDtl(orderMapper.listOrdDtlEntityToListOrdDtlPojo(listOrdDtlEntity));
            
            listOrderPojo.add(orderPojo);
        }

        return listOrderPojo;
    }

    public void saveOrder(OrderPojo orderPojo)
	{
        try {
            // ObjectMapper mapper = new ObjectMapper();
            // OrderPojo orderPojo = mapper.readValue(order, OrderPojo.class);
            // OrdHdrEntity ordHdrEntity = new OrdHdrEntity();
            // ordHdrEntity.setMobile(orderPojo.getOrdHdr().getMobile());
            // ordHdrEntity.setOrdNo(orderPojo.getOrdHdr().getOrdNo());
            
            OrdHdrEntity ordHdrEntity = orderMapper
            .ordHdrPojoToOrdHdrEntity(orderPojo.getOrdHdr());

            // push structured contextual data into MDC (Mapped Diagnostic Context).

            // METHOD 1
            // pass a simple string inside our callback :
            // MDC.put("serviceName", "service-order");

            // METHOD 2
            // or re-bind dynamically using the class package name on the main thread.
            // String dynamicServiceName = "service-" + this.getClass().getPackageName().split("\\.")[2];
            // MDC.put("serviceName", dynamicServiceName);

            // METHOD 3
            // moved to MdcAspectConfig.java

            MDC.put("ordNo", ordHdrEntity.getOrdNo().toString());
            MDC.put("mobile", ordHdrEntity.getMobile().toString());
            
            ordHdrRepository.save(ordHdrEntity);

            // List<OrdDtlEntity> listOrdDtlEntity = new ArrayList<>();
            // for (OrderPojo.OrdDtl dtl : orderPojo.getOrdDtl()) {
            //     OrdDtlEntity ordDtlEntity = new OrdDtlEntity();
            //     ordDtlEntity.setId(new OrdDtlIdEntity(ordHdrEntity.getOrdNo(), dtl.getLineNo()));
            //     ordDtlEntity.setCompany(dtl.getCompany());
            //     ordDtlEntity.setSku(dtl.getSku());
            //     ordDtlEntity.setQty(dtl.getQty());
            //     listOrdDtlEntity.add(ordDtlEntity);
            // }

            List<OrdDtlEntity> listOrdDtlEntity = orderMapper
            .listOrdDtlPojoToListOrdDtlEntity(orderPojo.getOrdHdr().getOrdNo(), orderPojo.getOrdDtl());

            ordDtlRepository.saveAll(listOrdDtlEntity);
            
            System.out.println("order # [" + ordHdrEntity.getOrdNo() + "] is created successfully!");
            logger.info("order created successfully!");

            if (kafkaHealthService.isKafkaUp()) {

                //send order to kafka topic.
                //kafkaTemplate.send(TOPIC, orderPojo);
                kafkaTemplate.send(TOPIC, orderPojo).whenComplete((result, ex) -> {
                    
                    if (ex == null) {
                        System.out.println("order # [" + ordHdrEntity.getOrdNo() + "] sent to kafka topic (" + TOPIC + ") with message : [" + orderPojo + "]");
                        logger.info("order successfully sent to kafka : [{}]", result.getRecordMetadata().offset());
                        logger.info("order # [{}] sent to kafka topic ({}) with message : [{}]", ordHdrEntity.getOrdNo(), TOPIC, orderPojo);
                    } else {
                        logger.error("failed to send order to Kafka. Triggering fallback database routine...", ex);
                        //fallbackService.saveToLocalDatabase(order);
                    }
                });
            } else {
                System.err.println("kafka server is DOWN, skipping publishing, route to fallback database/queue.");
                logger.error("kafka server is DOWN, skipping publishing, route to fallback database/queue.");
                //execute fallback strategy here.
                //fallbackService.saveToLocalDatabase(order);
            }
        }
        finally {
            MDC.remove("ordNo");
            MDC.remove("mobile");
        }
    }
}