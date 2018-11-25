package com.frosters.service;

import java.net.URI;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import com.frosters.model.OrderLineItem;
import com.frosters.model.Orders;
import com.frosters.repository.LineItemRepository;
import com.frosters.repository.OrderRepository;
import com.netflix.hystrix.contrib.javanica.annotation.HystrixCommand;

@Service
public class OrderService {

	@Autowired
	OrderRepository orderRepository;
	
	@Autowired
	LineItemRepository lineItemRepository;
	
	@Autowired
	MessageProducer msgProducer;
	
	@Autowired
	EmailService emailService;
	
	@Autowired
	RestTemplate restTemplate;
	
	@Autowired
	private DiscoveryClient discoveryClient;

	public String getCustomerAPIUrl() {
	    List<ServiceInstance> list = discoveryClient.getInstances("CUSTOMERAPI");
	    if (list != null && list.size() > 0 ) {
	        return list.get(0).getUri().toString();
	    }
	    return null;
	}
	
	public List<Orders> getOrdersOfCustomer(Long customerId){
		return orderRepository.findByCustomerId(customerId);
	}
	
	public Orders getOrderById(Long order) {
		return orderRepository.findById(order).get();
	}
	

	@Transactional(rollbackFor = Exception.class)
	public Orders addNewOrder(Orders orderRequest)
	{
		
		List<OrderLineItem> lineItems = orderRequest.getLineItems();

		//call promotion service and customer api for location
		String promotion = getPromotionsApplicable();
		System.out.println("Promotions fetched using Hystrix "+ promotion);
		String customerAPIURL = getCustomerAPIUrl();
		System.out.println("customer API URL from eureka"+ customerAPIURL);
		
		Orders order= orderRepository.save(orderRequest);
		
		order.setLineItems(null);
		
		lineItems.forEach(lineItem -> {
				lineItem.setOrder(order);
				lineItemRepository.save(lineItem);
			});
		
		
		emailService.sendOrderConfirmationEmail(order);
		msgProducer.produceMQMsg(order);
		return order;
	}
	
	@HystrixCommand(fallbackMethod="defaultPromotion")
	public String getPromotionsApplicable(){
		URI uri = URI.create("http://localhost:8008/getCurrentPromotion");

	    return this.restTemplate.getForObject(uri, String.class);
	
	}
	
	private String defaultPromotion() {
		return "RECALL";
	}
	
	public Orders updateOrder(Long orderId) throws Exception{
		Orders order = orderRepository.findById(orderId).get();
		order.setOrderStatus("SHIPPED");
		orderRepository.save(order);
		return order;
	}
	
	public void deleteOrder(Long orderId) {
		
		if(orderRepository.findById(orderId).isPresent()) {
		 lineItemRepository.deleteByOrder(orderRepository.findById(orderId).get());
		}
	}
	
	
}
