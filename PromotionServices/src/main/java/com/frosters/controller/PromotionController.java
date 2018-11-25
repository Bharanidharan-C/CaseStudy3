package com.frosters.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PromotionController {

	
	@GetMapping("/getCurrentPromotion")
	public String getCurrentPromotion() {
		return "PROMOTION50";
	}
}
