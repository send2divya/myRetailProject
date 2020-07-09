package com.example.demo;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

@RestController
@SpringBootApplication
public class ProductApplication extends SpringBootServletInitializer {
	public static void main(String[] args) {
		SpringApplication.run(ProductApplication.class, args);
	}

	@Autowired
	private ProductsRepository repository;

	@RequestMapping(method = RequestMethod.GET, value = "/products/{productId}")
	public Product findByid(@PathVariable String productId) {
		Product externalProduct = getExternalAPIData(productId); // Get External Product
                
               long count  = repository.count();
                System.out.print("count==="+count);
		Product product = repository.findByid(productId); // Finds product by ID From DB
		product.setProductId(externalProduct.getProductId());
		product.setName(externalProduct.getName());
		return product;
	}

	@RequestMapping(method = RequestMethod.PUT, value = "/products/{id}")
	public String updateByID(@PathVariable String id, @RequestBody Product in_product) {
		JSONObject jsonString = new JSONObject();
		try {
			if (!in_product.getProductId().equals(id)) { // If IDs don't match, kick back response
				jsonString.put("code", "409");
				jsonString.put("response", "ids did not match");
				return jsonString.toString();
			} else if (in_product.getCurrent_price().getValue() == null
					|| in_product.getCurrent_price().getValue().isEmpty()) { // If price is empty, kick back response

				jsonString.put("code", "406");
				jsonString.put("response", "price is null or empty");
				return jsonString.toString();
			}
		} catch (JSONException e) {
			e.printStackTrace();
		}

		Product product = repository.findByid(id); // Finds product by ID From DB
		product.setCurrent_price(in_product.getCurrent_price()); // Update price

		try {
			jsonString.put("code", "200");
			jsonString.put("response", "Price has been updated");
		} catch (JSONException e) {
			e.printStackTrace();
		}
		repository.save(product);
		return jsonString.toString();
	}

	@SuppressWarnings("unchecked")
	private Product getExternalAPIData(String id) {
		RestTemplate restTemplate = new RestTemplate();
		Product product = new Product();
		String URL = "https://redsky.target.com/v2/pdp/tcin/" + id
				+ "?excludes=taxonomy,price,promotion,bulk_ship,rating_and_review_reviews,rating_and_review_statistics,question_answer_statistics";

		ObjectMapper mapper = new ObjectMapper();
		@SuppressWarnings("rawtypes")
		Map<String, Map> map;
		ResponseEntity<String> response = restTemplate.getForEntity(URL, String.class);
		HttpStatus responseCode = response.getStatusCode();
		try {
			map = mapper.readValue(response.getBody(), Map.class); // Get JSON
			Map<String, Map> productMap = map.get("product");
			Map<String, Map> itemMap = productMap.get("item"); // For Mapping
			Map<String, String> itemMapString = productMap.get("item"); // To Grab ID
			Map<String, String> productDescriptionMap = itemMap.get("product_description");

			if (responseCode == HttpStatus.OK) {
				product.setProductId(itemMapString.get("tcin"));
				product.setName(productDescriptionMap.get("title"));
			}
		} catch (Exception e) {
			System.out.println(responseCode);
		}

		return product;
	}

	@RequestMapping("/about")
	public String about() {
		return "Author: Billy Lee 2018";
	}

}
