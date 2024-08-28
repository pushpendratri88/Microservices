package com.brainstorm.order.service.impl;

import com.brainstorm.order.dto.OrderDTO;

import com.brainstorm.order.dto.OrderEntryDTO;
import com.brainstorm.order.dto.ProductDTO;
import com.brainstorm.order.entity.EcomOrder;
import com.brainstorm.order.entity.OrderEntry;
import com.brainstorm.order.exception.ResourceNotFoundException;
import com.brainstorm.order.repository.OrderEntryRepository;
import com.brainstorm.order.repository.OrderRepository;

import com.brainstorm.order.service.CustomerService;
import com.brainstorm.order.service.IOrderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class OrderServiceImpl implements IOrderService {
    @Autowired
    OrderRepository orderRepository;

    @Autowired
    OrderEntryRepository orderEntryRepository;


    @Autowired
    CustomerService customerService;

    @Autowired
    RestTemplate restTemplate;


    @Value("${product.service.url}")
    private String productServiceUrl;

    private static final Logger logger = LoggerFactory.getLogger(OrderServiceImpl.class);


    @Override
    public void createOrder(OrderDTO orderDTO) {
        EcomOrder ecomOrder =  mapToOrder(orderDTO);
        if(orderDTO != null && orderDTO.getCustomerId() != null
        && customerService != null){
            String customerId = customerService.getCustomer(orderDTO.getCustomerId()).getId();
            ecomOrder.setCustomerId(customerId);
        }
        orderRepository.saveAndFlush(ecomOrder);
        ecomOrder.getOrderEntryList().forEach(orderEntryTr -> {
            orderEntryTr.setOrder(ecomOrder);
            orderEntryRepository.saveAndFlush(orderEntryTr);
        });
    }

    @Override
    public OrderDTO fetchOrder(String orderId) {
        EcomOrder ecomOrder =  orderRepository.findById(orderId).orElseThrow(() ->new ResourceNotFoundException("Order", "OrderId", orderId));
        return mapToOrderDTO(ecomOrder,new OrderDTO());
    }

    @Override
    public void deleteOrder(String orderId) {
        EcomOrder ecomOrder =  orderRepository.findById(orderId).orElseThrow(() ->new ResourceNotFoundException("Order", "OrderId", orderId));
        orderRepository.delete(ecomOrder);
    }

    public  EcomOrder mapToOrder(OrderDTO orderDTO){
        EcomOrder ecomOrder = new EcomOrder();
        ecomOrder.setStatus(orderDTO.getOrderStatus());
        ecomOrder.setCreatedAt(LocalDateTime.now());
        ecomOrder.setOrderEntryList(mapToOrderEntry(orderDTO.getOrderEntriesDTO()));
        return ecomOrder;
    }

    public OrderDTO mapToOrderDTO(EcomOrder ecomOrder, OrderDTO orderDTO ){
        ArrayList<OrderEntryDTO>  orderEntryDTOList = new ArrayList<>();
        orderDTO.setOrderId(ecomOrder.getId());
        orderDTO.setOrderStatus(ecomOrder.getStatus());
        orderDTO.setCustomerId(ecomOrder.getCustomerId());
        ecomOrder.getOrderEntryList().forEach(orderEntry -> {
            OrderEntryDTO orderEntryDTO =  mapToOrderEntryDTO(orderEntry);
            orderEntryDTOList.add(orderEntryDTO);
        });
        orderDTO.setOrderEntriesDTO(orderEntryDTOList);
        return orderDTO;
    }

    public  List<OrderEntry> mapToOrderEntry(List<OrderEntryDTO> orderEntryListDTO){
        List<OrderEntry> orderEntryList = new ArrayList<>();

        orderEntryListDTO.forEach(orderEntryDTO -> {
            ProductDTO productDTO = getProductDTOFromProductService(orderEntryDTO.getProductId());
            OrderEntry orderEntry = new OrderEntry();
            orderEntry.setCreatedAt(LocalDateTime.now());
            orderEntry.setQuantity(orderEntryDTO.getQuantity());
            orderEntry.setPrice(productDTO.getPrice() * orderEntryDTO.getQuantity());
            orderEntry.setProductId(productDTO.getCode());
            OrderEntry orderEntryTr = orderEntryRepository.saveAndFlush(orderEntry);
            orderEntryList.add(orderEntryTr);
        });
        return orderEntryList;
    }




    public OrderEntryDTO mapToOrderEntryDTO(OrderEntry orderEntry){
        ProductDTO productDTO = getProductDTOFromProductService(orderEntry.getProductId());
        OrderEntryDTO orderEntryDTO = new OrderEntryDTO();
        orderEntryDTO.setId(orderEntry.getId());
        orderEntryDTO.setQuantity(orderEntry.getQuantity());
        orderEntryDTO.setPrice(productDTO.getPrice() * orderEntry.getQuantity());
        orderEntryDTO.setProductDTO(productDTO);
        orderEntryDTO.setProductId(productDTO.getCode());
        return orderEntryDTO;
    }

//    public Product mapToProduct(ProductDTO productDTO){
//        Product product = new Product();
//        product.setName(productDTO.getName());
//        product.setCreatedAt(LocalDateTime.now());
//        product.setCategory(productDTO.getCategory());
//        product.setPrice(productDTO.getPrice());
//        return product;
//    }
//
//    public  ProductDTO mapToProductDTO(Product product){
//        ProductDTO productDTO = new ProductDTO();
//        productDTO.setCode(product.getId());
//        productDTO.setName(product.getName());
//        productDTO.setCategory(productDTO.getCategory());
//        productDTO.setPrice(product.getPrice());
//        return productDTO;
//    }

    private ProductDTO getProductDTOFromProductService(String productId) {
        ProductDTO productDTO =  null;
        String ProductServiceurl = productServiceUrl + "/fetchProduct?productId=" + productId;
        logger.info("Requesting Product details from URL: ", ProductServiceurl);
        try {
            productDTO = restTemplate.getForObject(ProductServiceurl, ProductDTO.class);
        } catch (Exception e) {
            logger.error("Error while requesting customer details", e);
            throw e;
        }
        return productDTO;
    }
}
